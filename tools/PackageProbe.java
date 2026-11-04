import io.github.liuchengzhiaa.jokeviewer.pkg.PackageInspector;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local-only probe; pass UTF-8 list files on Windows to avoid argv codepage loss. */
public class PackageProbe {
  public static void main(String[] args) throws Exception {
    File scratch = new File(args[0]);
    if (!scratch.isDirectory() && !scratch.mkdirs()) throw new IOException("scratch");
    List<String> paths =
        args.length == 3 && args[1].equals("--list")
            ? Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8)
            : Arrays.asList(args).subList(1, args.length);
    int passed = 0, index = 0;
    for (String input : paths) {
      String source = input.startsWith("\uFEFF") ? input.substring(1) : input;
      if (source.isBlank()) continue;
      index++;
      try {
        var r = PackageInspector.inspect(new File(source), scratch);
        System.out.println(
            "sample="
                + index
                + " result=ok container="
                + r.container
                + " format="
                + r.format
                + " models="
                + r.models.size()
                + " engine="
                + r.models.get(0).engine);
        passed++;
      } catch (Exception e) {
        System.out.println("sample=" + index + " result=unsupported error=" + e.getMessage());
      }
    }
    System.out.println("passed=" + passed + " total=" + index);
    if (passed == 0) System.exit(1);
  }
}
