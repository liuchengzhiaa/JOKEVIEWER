package io.github.liuchengzhiaa.jokeviewer.pkg;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ArchivePolicy {
  public static final long MAX_PACKAGE = 2L * 1024 * 1024 * 1024, MAX_RESOURCE = 256L * 1024 * 1024;
  public static final int MAX_CONFIG = 8 * 1024 * 1024,
      MAX_COVER = 8 * 1024 * 1024,
      MAX_ENTRIES = 20000;

  private ArchivePolicy() {}

  public static String safeName(String name) throws IOException {
    if (name == null
        || name.isEmpty()
        || name.startsWith("/")
        || name.contains("\\")
        || name.contains(":")
        || name.indexOf(0) >= 0) throw new IOException("Unsafe archive path");
    for (String s : name.split("/", -1))
      if (s.equals("..") || s.equals(".") || s.isEmpty())
        throw new IOException("Unsafe archive path");
    return name;
  }

  public static Map<String, ZipEntry> index(ZipFile zip) throws IOException {
    Map<String, ZipEntry> files = new LinkedHashMap<>();
    Set<String> all = new HashSet<>();
    long sum = 0;
    int count = 0;
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
      ZipEntry e = entries.nextElement();
      if (++count > MAX_ENTRIES) throw new IOException("Too many archive entries");
      String n = e.getName();
      if (e.isDirectory()) n = n.endsWith("/") ? n.substring(0, n.length() - 1) : n;
      safeName(n);
      if (!all.add(n)) throw new IOException("Duplicate archive path");
      if (e.isDirectory()) continue;
      long cap = n.toLowerCase(Locale.ROOT).endsWith(".lpk") ? MAX_PACKAGE : MAX_RESOURCE;
      if (e.getSize() < 0 || e.getSize() > cap) throw new IOException("Archive resource too large");
      sum = Math.addExact(sum, e.getSize());
      if (sum > MAX_PACKAGE) throw new IOException("Archive expands beyond limit");
      files.put(n, e);
    }
    return files;
  }

  public static long copy(InputStream in, OutputStream out, long maximum) throws IOException {
    byte[] b = new byte[64 * 1024];
    long total = 0;
    for (int n; (n = in.read(b)) != -1; ) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
      total += n;
      if (total > maximum) throw new IOException("Resource exceeds safety limit");
      out.write(b, 0, n);
    }
    return total;
  }

  public static byte[] bytes(InputStream in, long maximum) throws IOException {
    try (in;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      copy(in, out, maximum);
      return out.toByteArray();
    }
  }
}
