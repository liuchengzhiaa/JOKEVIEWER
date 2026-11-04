package io.github.liuchengzhiaa.jokeviewer.pkg;

import static org.junit.Assert.*;

import java.io.*;
import java.util.zip.*;
import org.junit.Test;

public class ArchivePolicyTest {
  @Test
  public void rejectsUnsafePaths() throws Exception {
    for (String s :
        new String[] {"", "/x", "../x", "a/../x", "./x", "C:/x", "a//b", "a/", "a\\b", "x\u0000y"})
      assertThrows(s, IOException.class, () -> ArchivePolicy.safeName(s));
    assertThrows(IOException.class, () -> ArchivePolicy.safeName(null));
  }

  @Test
  public void allowsUnicodeAndHashedPaths() throws Exception {
    assertEquals("人物/abc.bin", ArchivePolicy.safeName("人物/abc.bin"));
  }

  @Test
  public void boundedCopyRejectsOverflow() throws Exception {
    assertThrows(
        IOException.class,
        () ->
            ArchivePolicy.copy(
                new ByteArrayInputStream(new byte[5]), new ByteArrayOutputStream(), 4));
  }

  @Test
  public void exactBoundIsAllowed() throws Exception {
    assertEquals(
        4,
        ArchivePolicy.copy(new ByteArrayInputStream(new byte[4]), new ByteArrayOutputStream(), 4));
  }

  @Test
  public void interruptCancelsCopy() throws Exception {
    try {
      Thread.currentThread().interrupt();
      assertThrows(
          InterruptedIOException.class,
          () ->
              ArchivePolicy.copy(
                  new ByteArrayInputStream(new byte[4]), new ByteArrayOutputStream(), 4));
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  public void indexRejectsTraversal() throws Exception {
    File f = File.createTempFile("traversal", ".zip");
    try {
      try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f))) {
        z.putNextEntry(new ZipEntry("../bad"));
        z.write(1);
        z.closeEntry();
      }
      try (ZipFile z = new ZipFile(f)) {
        assertThrows(IOException.class, () -> ArchivePolicy.index(z));
      }
    } finally {
      f.delete();
    }
  }
}
