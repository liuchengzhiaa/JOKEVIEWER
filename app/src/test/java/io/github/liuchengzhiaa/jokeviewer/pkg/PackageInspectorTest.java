package io.github.liuchengzhiaa.jokeviewer.pkg;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.*;
import org.junit.Test;

public class PackageInspectorTest {
  static byte[] zip(Map<String, byte[]> files) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(out)) {
      for (var e : files.entrySet()) {
        z.putNextEntry(new ZipEntry(e.getKey()));
        z.write(e.getValue());
        z.closeEntry();
      }
    }
    return out.toByteArray();
  }

  static byte[] text(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  static byte[] lpk(String type, String id, String config, String outerId, String metadata)
      throws Exception {
    String entry = "model.bin",
        seed = id + (type.equals("STM_1_0") ? outerId + entry + metadata : entry);
    String meta =
        new JSONObject()
            .put("type", type)
            .put("id", id)
            .put("name", "Synthetic")
            .put(
                "list",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("character", "Synthetic")
                            .put(
                                "costume",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("name", "Default")
                                            .put("path", entry)))))
            .toString();
    Map<String, byte[]> files = new LinkedHashMap<>();
    files.put("config.mlve", text(meta));
    files.put(entry, ResourceCodec.transform(text(config), seed));
    files.put("skeleton.bin", text("{}"));
    files.put("atlas.bin", text("page.png"));
    files.put("texture.bin", new byte[] {1, 2, 3});
    return zip(files);
  }

  static final String CFG =
      "{\"skeleton\":\"skeleton.bin\",\"atlases\":[{\"atlas\":\"atlas.bin\",\"textures\":[\"texture.bin\"]}],\"motions\":{\"idle\":[{\"name\":\"idle\"}]}}";

  static PackageInspector.Result read(byte[] b) throws Exception {
    File dir =
        new File(System.getProperty("java.io.tmpdir"), "jokeviewer-test-" + UUID.randomUUID());
    assertTrue(dir.mkdir());
    File src = new File(dir, "source.zip");
    try {
      try (FileOutputStream out = new FileOutputStream(src)) {
        out.write(b);
      }
      return PackageInspector.inspect(src, dir);
    } finally {
      for (File f : Objects.requireNonNull(dir.listFiles())) assertTrue(f.delete());
      assertTrue(dir.delete());
    }
  }

  @Test
  public void readsStd2Lpk() throws Exception {
    var r = read(lpk("STD2_0", "fixture", CFG, "", ""));
    assertEquals("LPK", r.container);
    assertEquals(1, r.models.size());
    assertEquals("Spine", r.models.get(0).engine);
    assertEquals(3, r.models.get(0).found);
    assertEquals(1, r.models.get(0).motions);
  }

  @Test
  public void readsNestedStmWpk() throws Exception {
    var config =
        text(
            "{\"lpkFile\":\"inner.lpk\",\"title\":\"Fixture\",\"fileId\":\"outer\",\"metaData\":\"metadata\"}");
    var r =
        read(
            zip(
                Map.of(
                    "123/config.json",
                    config,
                    "123/inner.lpk",
                    lpk("STM_1_0", "fixture", CFG, "outer", "metadata"))));
    assertEquals("WPK", r.container);
    assertEquals("Fixture", r.title);
    assertEquals(1, r.models.size());
  }

  @Test
  public void isolatedStmNeedsWpkContext() throws Exception {
    assertThrows(
        IOException.class, () -> read(lpk("STM_1_0", "fixture", CFG, "outer", "metadata")));
  }

  @Test
  public void unknownFormatFailsClosed() throws Exception {
    assertThrows(IOException.class, () -> read(lpk("FUTURE", "fixture", CFG, "", "")));
  }

  @Test
  public void missingReferenceIsReported() throws Exception {
    var r = read(lpk("STD2_0", "fixture", CFG.replace("skeleton.bin", "absent.bin"), "", ""));
    assertEquals(2, r.models.get(0).found);
    assertFalse(r.warnings.isEmpty());
  }

  @Test
  public void corruptZipHasFriendlyFailure() {
    assertThrows(IOException.class, () -> read(text("not a ZIP")));
  }

  @Test
  public void ambiguousOuterConfigRejected() throws Exception {
    assertThrows(
        IOException.class,
        () -> read(zip(Map.of("a/config.json", text("{}"), "b/config.json", text("{}")))));
  }

  @Test
  public void hostileOuterPathRejected() throws Exception {
    assertThrows(
        IOException.class,
        () -> read(zip(Map.of("config.json", text("{\"lpkFile\":\"../outer.lpk\"}")))));
  }

  @Test
  public void excessiveJsonNestingRejected() throws Exception {
    String deep = "{\"nested\":" + "[".repeat(100) + "0" + "]".repeat(100) + "}";
    assertThrows(IOException.class, () -> read(lpk("STD2_0", "fixture", deep, "", "")));
  }
}
