package io.github.liuchengzhiaa.jokeviewer;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.github.liuchengzhiaa.jokeviewer.pkg.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final AtomicInteger generation = new AtomicInteger();
  private Future<?> pending;
  private TextView status, detail, model;
  private ImageView cover;
  private Spinner models;
  private Button cancel;
  private PackageInspector.Result result;
  private Uri selected;
  private File imports;
  private final int text = 0xFFE8EDF4, muted = 0xFFA4AFBF, accent = 0xFFD8BA83;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    imports = new File(getCacheDir(), "package-imports");
    if (!imports.isDirectory() && !imports.mkdirs()) {
      finish();
      return;
    }
    makeUi();
    if (saved != null && saved.getString("uri") != null) load(Uri.parse(saved.getString("uri")));
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView label(String value, int size, int color) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(8), 0, dp(8));
    return v;
  }

  private GradientDrawable panel(int color) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(16));
    return d;
  }

  private void makeUi() {
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(0xFF11151C);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(22), dp(22), dp(22), dp(24));
    scroll.addView(root);
    setContentView(scroll);
    scroll.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          int top = insets.getSystemWindowInsetTop(), bottom = insets.getSystemWindowInsetBottom();
          root.setPadding(dp(22), dp(22) + top, dp(22), dp(24) + bottom);
          return insets;
        });
    root.addView(label(getString(R.string.app_name), 30, text));
    root.addView(label(getString(R.string.subtitle), 16, accent));
    root.addView(label(getString(R.string.version), 12, muted));
    TextView scope = label(getString(R.string.scope), 14, accent);
    scope.setBackground(panel(0xFF24231F));
    scope.setPadding(dp(16), dp(14), dp(16), dp(14));
    root.addView(scope);
    Button open = new Button(this);
    open.setText(R.string.open);
    open.setAllCaps(false);
    open.setTextColor(0xFF171B20);
    open.setBackground(panel(accent));
    LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(-1, dp(56));
    button.topMargin = dp(20);
    root.addView(open, button);
    open.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(Intent.ACTION_OPEN_DOCUMENT)
                  .addCategory(Intent.CATEGORY_OPENABLE)
                  .setType("*/*")
                  .addFlags(
                      Intent.FLAG_GRANT_READ_URI_PERMISSION
                          | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
          startActivityForResult(intent, 10);
        });
    cancel = new Button(this);
    cancel.setText(R.string.cancel);
    cancel.setAllCaps(false);
    cancel.setVisibility(View.GONE);
    root.addView(cancel);
    cancel.setOnClickListener(v -> cancelImport());
    status = label(getString(R.string.empty), 14, muted);
    root.addView(status);
    cover = new ImageView(this);
    cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
    cover.setBackground(panel(0xFF1B232E));
    root.addView(cover, new LinearLayout.LayoutParams(-1, dp(220)));
    root.addView(label(getString(R.string.static_cover), 12, muted));
    root.addView(label(getString(R.string.models), 19, text));
    models = new Spinner(this);
    root.addView(models, new LinearLayout.LayoutParams(-1, dp(56)));
    model = label(getString(R.string.engine_pending), 14, text);
    model.setTextIsSelectable(true);
    root.addView(model);
    root.addView(label(getString(R.string.details), 19, text));
    detail = label("—", 13, muted);
    detail.setTextIsSelectable(true);
    root.addView(detail);
    models.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int i, long id) {
            showModel(i);
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
  }

  @Override
  protected void onActivityResult(int request, int code, Intent data) {
    super.onActivityResult(request, code, data);
    if (request != 10 || code != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    try {
      if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        getContentResolver()
            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException ignored) {
      /* One-time readable grants remain usable for this import. */
    }
    load(uri);
  }

  private void cancelImport() {
    selected = null;
    generation.incrementAndGet();
    if (pending != null) pending.cancel(true);
    cancel.setVisibility(View.GONE);
    status.setText(R.string.cancelled);
  }

  private void load(Uri uri) {
    selected = uri;
    int ticket = generation.incrementAndGet();
    if (pending != null) pending.cancel(true);
    result = null;
    cover.setImageDrawable(null);
    models.setAdapter(null);
    detail.setText("—");
    model.setText(R.string.engine_pending);
    status.setText(R.string.working);
    cancel.setVisibility(View.VISIBLE);
    pending =
        executor.submit(
            () -> {
              File local = null;
              try {
                local = File.createTempFile("import-", ".zip", imports);
                try (InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(local)) {
                  if (in == null) throw new IOException("无法读取所选文件");
                  ArchivePolicy.copy(in, out, ArchivePolicy.MAX_PACKAGE);
                }
                PackageInspector.Result inspected = PackageInspector.inspect(local, imports);
                Bitmap image = decodeCover(inspected.cover);
                runOnUiThread(
                    () -> {
                      if (ticket != generation.get() || isDestroyed()) {
                        if (image != null) image.recycle();
                        return;
                      }
                      result = inspected;
                      cancel.setVisibility(View.GONE);
                      status.setText(getString(R.string.complete, result.models.size()));
                      if (image != null) cover.setImageBitmap(image);
                      else cover.setContentDescription(getString(R.string.no_cover));
                      StringBuilder warnings = new StringBuilder();
                      for (String warning : result.warnings)
                        warnings.append(getString(R.string.warnings, warning)).append('\n');
                      detail.setText(
                          getString(
                              R.string.detail_template,
                              result.container,
                              result.format,
                              result.entries,
                              result.models.size(),
                              warnings));
                      String[] names = new String[result.models.size()];
                      for (int i = 0; i < names.length; i++) names[i] = result.models.get(i).name;
                      ArrayAdapter<String> a =
                          new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                      a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                      models.setAdapter(a);
                      showModel(0);
                    });
              } catch (Exception e) {
                runOnUiThread(
                    () -> {
                      if (ticket != generation.get() || isDestroyed()) return;
                      cancel.setVisibility(View.GONE);
                      String why = e.getMessage();
                      status.setText(
                          getString(
                              R.string.failed,
                              why == null ? getString(R.string.unknown_error) : why));
                    });
              } finally {
                if (local != null && !local.delete()) local.deleteOnExit();
              }
            });
  }

  private Bitmap decodeCover(byte[] bytes) {
    if (bytes == null) return null;
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    if (opts.outWidth <= 0
        || opts.outHeight <= 0
        || opts.outWidth > 32768
        || opts.outHeight > 32768) return null;
    opts.inJustDecodeBounds = false;
    opts.inSampleSize = 1;
    while (opts.outWidth / opts.inSampleSize > 1600 || opts.outHeight / opts.inSampleSize > 1600)
      opts.inSampleSize *= 2;
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
  }

  private void showModel(int index) {
    if (result == null || index < 0 || index >= result.models.size()) return;
    PackageInspector.Model m = result.models.get(index);
    StringBuilder actions = new StringBuilder();
    for (String name : m.actionNames) actions.append(name).append('\n');
    model.setText(
        getString(
            R.string.model_template, m.name, m.engine, m.motions, m.found, m.references, actions));
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    if (selected != null) out.putString("uri", selected.toString());
  }

  @Override
  public void onDestroy() {
    generation.incrementAndGet();
    if (pending != null) pending.cancel(true);
    executor.shutdownNow();
    super.onDestroy();
  }
}
