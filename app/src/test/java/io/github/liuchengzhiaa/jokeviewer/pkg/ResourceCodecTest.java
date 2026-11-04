package io.github.liuchengzhiaa.jokeviewer.pkg;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

public class ResourceCodecTest {
  @Test
  public void transformIsInvolution() {
    byte[] b = new byte[3007];
    new Random(42).nextBytes(b);
    assertArrayEquals(
        b, ResourceCodec.transform(ResourceCodec.transform(b, "fixture/资源.bin"), "fixture/资源.bin"));
  }

  @Test
  public void streamAgreesAcrossKeyBoundary() throws Exception {
    byte[] b = new byte[3007];
    new Random(11).nextBytes(b);
    try (InputStream in = ResourceCodec.stream(new ByteArrayInputStream(b), "fixture.bin")) {
      assertArrayEquals(ResourceCodec.transform(b, "fixture.bin"), in.readAllBytes());
    }
  }

  @Test
  public void skipAdvancesKey() throws Exception {
    byte[] b = new byte[2048];
    try (InputStream in = ResourceCodec.stream(new ByteArrayInputStream(b), "x")) {
      assertEquals(1111, in.skip(1111));
      assertArrayEquals(
          Arrays.copyOfRange(ResourceCodec.transform(b, "x"), 1111, b.length), in.readAllBytes());
    }
  }

  @Test
  public void emptyInputIsEmpty() {
    assertEquals(0, ResourceCodec.transform(new byte[0], "x").length);
  }
}
