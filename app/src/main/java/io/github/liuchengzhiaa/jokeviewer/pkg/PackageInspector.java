package io.github.liuchengzhiaa.jokeviewer.pkg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

public final class PackageInspector {
  public static final class Model {
    public String name, engine;
    public int motions, references, found;
    public final List<String> actionNames = new ArrayList<>();
  }

  public static final class Result {
    public String container, format, title;
    public int entries;
    public byte[] cover;
    public final List<Model> models = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
  }

  public static Result inspect(File source, File scratch) throws IOException {
    try {
      return inspectJson(source, scratch);
    } catch (JSONException | IllegalArgumentException e) {
      throw new IOException("模型包配置不是受支持的 JSON", e);
    }
  }

  private static Result inspectJson(File source, File scratch) throws IOException, JSONException {
    if (source.length() > ArchivePolicy.MAX_PACKAGE) throw new IOException("包文件超过 2 GiB 安全上限");
    Result r = new Result();
    File nested = null;
    try (ZipFile outerZip = new ZipFile(source)) {
      Map<String, ZipEntry> outer = ArchivePolicy.index(outerZip);
      List<String> configs = new ArrayList<>();
      for (String n : outer.keySet())
        if (n.equals("config.json") || n.endsWith("/config.json")) configs.add(n);
      JSONObject context = null;
      if (configs.size() > 1) throw new IOException("WPK 含多个外层配置，暂不支持");
      if (configs.size() == 1) {
        r.container = "WPK";
        String p = configs.get(0),
            prefix = p.contains("/") ? p.substring(0, p.lastIndexOf('/') + 1) : "";
        context =
            json(
                ArchivePolicy.bytes(
                    outerZip.getInputStream(outer.get(p)), ArchivePolicy.MAX_CONFIG));
        r.title = context.optString("title", "");
        String lpk = context.optString("lpkFile", "");
        if (lpk.isEmpty()) throw new IOException("此 WPK 不是可识别的模型包（没有 LPK 引用）");
        String lpkName = ArchivePolicy.safeName(prefix + ArchivePolicy.safeName(lpk));
        ZipEntry entry = outer.get(lpkName);
        if (entry == null) throw new IOException("WPK 引用的 LPK 资源不存在");
        String preview = context.optString("previewFile", "");
        if (!preview.isEmpty()) {
          ZipEntry cover =
              outer.get(ArchivePolicy.safeName(prefix + ArchivePolicy.safeName(preview)));
          if (cover != null && cover.getSize() <= ArchivePolicy.MAX_COVER)
            r.cover = ArchivePolicy.bytes(outerZip.getInputStream(cover), ArchivePolicy.MAX_COVER);
          else r.warnings.add("封面缺失或超过 8 MiB，已跳过");
        }
        nested = File.createTempFile("inner-", ".lpk", scratch);
        try (InputStream in = outerZip.getInputStream(entry);
            OutputStream out = new FileOutputStream(nested)) {
          ArchivePolicy.copy(in, out, ArchivePolicy.MAX_PACKAGE);
        }
        try (ZipFile inner = new ZipFile(nested)) {
          readLpk(inner, context, r);
        }
      } else {
        r.container = "LPK";
        readLpk(outerZip, null, r);
      }
      return r;
    } finally {
      if (nested != null && !nested.delete()) nested.deleteOnExit();
    }
  }

  private static JSONObject json(byte[] data) throws JSONException, IOException {
    String s = new String(data, StandardCharsets.UTF_8);
    if (s.startsWith("\uFEFF")) s = s.substring(1);
    if (!s.trim().startsWith("{")) throw new JSONException("Expected JSON object");
    validateJsonComplexity(s);
    return new JSONObject(s);
  }

  private static String md5(String s) throws IOException {
    try {
      byte[] hash = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder b = new StringBuilder();
      for (byte v : hash) b.append(String.format(Locale.ROOT, "%02x", v & 255));
      return b.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }

  private static void readLpk(ZipFile zip, JSONObject outer, Result r)
      throws IOException, JSONException {
    Map<String, ZipEntry> entries = ArchivePolicy.index(zip);
    r.entries = entries.size();
    ZipEntry header = entries.get("config.mlve");
    if (header == null) header = entries.get(md5("config.mlve"));
    if (header == null) throw new IOException("未找到 LPK 配置索引 config.mlve");
    JSONObject meta =
        json(ArchivePolicy.bytes(zip.getInputStream(header), ArchivePolicy.MAX_CONFIG));
    r.format = meta.optString("type", "");
    if (!r.format.equals("STM_1_0") && !r.format.equals("STD2_0"))
      throw new IOException("暂不支持此 LPK 内部格式：" + r.format);
    if (r.format.equals("STM_1_0")
        && (outer == null
            || outer.optString("fileId").isEmpty()
            || outer.optString("metaData").isEmpty()))
      throw new IOException("此 LPK 需要对应 WPK 的元数据，请导入原 WPK");
    if (r.title == null || r.title.isEmpty()) r.title = meta.optString("name", "LPK");
    JSONArray chars = meta.optJSONArray("list");
    if (chars == null) throw new IOException("LPK 没有模型清单");
    int total = 0;
    for (int i = 0; i < chars.length(); i++) {
      JSONObject ch = chars.getJSONObject(i);
      JSONArray costumes = ch.optJSONArray("costume");
      if (costumes == null) continue;
      for (int j = 0; j < costumes.length(); j++) {
        if (++total > 256) throw new IOException("模型配置数超过安全上限");
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        JSONObject costume = costumes.getJSONObject(j);
        String path = ArchivePolicy.safeName(costume.getString("path"));
        ZipEntry e = entries.get(path);
        if (e == null) throw new IOException("模型配置引用缺失");
        byte[] raw = ArchivePolicy.bytes(zip.getInputStream(e), ArchivePolicy.MAX_CONFIG);
        String key =
            meta.optString("id")
                + (r.format.equals("STM_1_0")
                    ? outer.optString("fileId") + path + outer.optString("metaData")
                    : path);
        JSONObject config;
        try {
          config = json(raw);
        } catch (JSONException | IOException ex) {
          config = json(ResourceCodec.transform(raw, key));
        }
        Model model = new Model();
        String character = ch.optString("character", "");
        model.name =
            (character.isEmpty() ? r.title : character)
                + " / "
                + costume.optString("name", "model");
        model.engine =
            config.has("skeleton") && config.has("atlases")
                ? "Spine"
                : config.has("FileReferences") || config.has("model")
                    ? "Live2D"
                    : "未知（type=" + config.optInt("type", -1) + "）";
        JSONObject motions = config.optJSONObject("motions");
        if (motions != null) {
          Iterator<String> groups = motions.keys();
          while (groups.hasNext()) {
            String group = groups.next();
            JSONArray list = motions.optJSONArray(group);
            if (list == null) continue;
            model.motions += list.length();
            for (int k = 0; k < list.length() && model.actionNames.size() < 32; k++) {
              JSONObject motion = list.optJSONObject(k);
              if (motion != null) model.actionNames.add(group + ":" + motion.optString("name", ""));
            }
          }
        }
        List<String> refs = new ArrayList<>();
        if (config.has("skeleton")) refs.add(config.optString("skeleton"));
        JSONArray atlases = config.optJSONArray("atlases");
        if (atlases != null)
          for (int k = 0; k < atlases.length(); k++) {
            JSONObject atlas = atlases.getJSONObject(k);
            refs.add(atlas.optString("atlas"));
            JSONArray textures = atlas.optJSONArray("textures");
            if (textures != null)
              for (int t = 0; t < textures.length(); t++) refs.add(textures.getString(t));
          }
        for (String ref : refs) {
          ArchivePolicy.safeName(ref);
          model.references++;
          if (entries.containsKey(ref)) model.found++;
        }
        if (model.references != model.found) r.warnings.add("配置引用存在缺失：" + model.name);
        r.models.add(model);
      }
    }
    if (r.models.isEmpty()) throw new IOException("包内没有可识别的模型配置");
  }

  private static void validateJsonComplexity(String s) throws IOException {
    boolean quoted = false, escaped = false;
    int depth = 0, tokens = 0;
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (quoted) {
        if (escaped) escaped = false;
        else if (ch == '\\') escaped = true;
        else if (ch == '"') quoted = false;
        continue;
      }
      if (ch == '"') quoted = true;
      else if (ch == '{' || ch == '[') {
        if (++depth > 64) throw new IOException("JSON nesting limit exceeded");
        tokens++;
      } else if (ch == '}' || ch == ']') {
        depth--;
        tokens++;
      } else if (ch == ',' || ch == ':') tokens++;
      if (tokens > 250000) throw new IOException("JSON complexity limit exceeded");
    }
  }
}
