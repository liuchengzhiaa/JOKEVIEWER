package io.github.liuchengzhiaa.jokeviewer.pkg;

import java.io.*;

/** Package byte transform. No desktop executable or third-party runtime is bundled. */
public final class ResourceCodec {
  private ResourceCodec() {}

  public static byte[] key(String text) {
    int seed = text.hashCode();
    byte[] key = new byte[1024];
    for (int i = 0; i < key.length; i++) {
      seed = ((seed * 214013 + 2531011) >>> 16) & 65535;
      key[i] = (byte) seed;
    }
    return key;
  }

  public static byte[] transform(byte[] data, String text) {
    byte[] k = key(text), out = new byte[data.length];
    for (int i = 0; i < data.length; i++) out[i] = (byte) (data[i] ^ k[i % k.length]);
    return out;
  }

  public static InputStream stream(InputStream input, String text) {
    return new FilterInputStream(input) {
      private final byte[] k = key(text);
      private long position;

      @Override
      public int read() throws IOException {
        int b = in.read();
        return b < 0 ? -1 : (b ^ (k[(int) (position++ % 1024)] & 255));
      }

      @Override
      public int read(byte[] b, int o, int n) throws IOException {
        int count = in.read(b, o, n);
        for (int i = 0; i < count; i++) b[o + i] ^= k[(int) (position++ % 1024)];
        return count;
      }

      @Override
      public long skip(long n) throws IOException {
        long s = in.skip(n);
        position += s;
        return s;
      }

      @Override
      public boolean markSupported() {
        return false;
      }

      @Override
      public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
      }
    };
  }
}
