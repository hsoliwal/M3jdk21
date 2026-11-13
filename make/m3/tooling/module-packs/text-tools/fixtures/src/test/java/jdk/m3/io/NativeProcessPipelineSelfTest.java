// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/** No network, shell or GNU dependencies: a child Java executable exercises real OS pipes. */
public final class NativeProcessPipelineSelfTest {
  private static int assertions;
  private NativeProcessPipelineSelfTest() { }

  private static void check(boolean condition, String message) {
    assertions++;
    if (!condition) throw new AssertionError(message);
  }

  private static NativeCommand java(String operation) throws Exception {
    Path runtime = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
            .contains("windows") ? "java.exe" : "java");
    Path classes = Path.of(NativeProcessPipelineSelfTest.class.getProtectionDomain()
        .getCodeSource().getLocation().toURI());
    return NativeCommand.of(runtime, "-Xmx32m", "-cp", classes.toString(),
        Child.class.getName(), operation);
  }

  public static void main(String[] args) throws Exception {
    NativeProcessPipeline runtime = new NativeProcessPipeline();
    byte[] payload = new byte[] {0, 1, 13, 10, (byte) 0xff, 'a', 'b', 'C', '\n'};
    try (NativePipelineResult result = runtime.run(List.of(java("pass")),
        new ByteArrayInputStream(payload), Duration.ofSeconds(12))) {
      check(result.success(), "one-stage exit 0");
      check(Arrays.equals(payload, result.stdout().bytes()), "exact binary bytes");
      check(result.stderr(0).size() == 0, "empty stderr");
      check(!result.stdout().spilled(), "small hot result");
    }
    try (NativePipelineResult result = runtime.run(List.of(java("upper"), java("suffix")),
        new ByteArrayInputStream("abc\n".getBytes(StandardCharsets.UTF_8)),
        Duration.ofSeconds(12))) {
      check(result.exitCodes().equals(List.of(0, 0)), "per-stage status");
      check(new String(result.stdout().bytes(), StandardCharsets.UTF_8).equals("ABC\n!"),
          "pipe ordered byte semantics");
    }
    try (NativePipelineResult result = runtime.run(List.of(java("fail")),
        InputStream.nullInputStream(), Duration.ofSeconds(12))) {
      check(!result.success() && result.exitCodes().equals(List.of(7)), "nonzero preserved");
      check(new String(result.stderr(0).bytes(), StandardCharsets.UTF_8)
          .equals("diagnostic\n"), "stderr exact");
    }
    Path spilledFile;
    try (NativePipelineResult result = runtime.run(List.of(java("emit-large")),
        InputStream.nullInputStream(), Duration.ofSeconds(12))) {
      check(result.success(), "large output exit 0");
      check(result.stdout().size() == 6L * 1024 * 1024, "full large output");
      check(result.stdout().spilled(), "automatically spooled when Xmx is small");
      spilledFile = result.stdout().spillPath();
      check(Files.isRegularFile(spilledFile), "hot->cold spool exists");
      try (InputStream captured = result.stdout().openInput()) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long total = 0;
        byte[] block = new byte[4096];
        for (int len; (len = captured.read(block)) != -1;) {
          total += len;
          for (int i = 0; i < len; i++) checkByte(block[i] == 'x');
          md.update(block, 0, len);
        }
        check(total == result.stdout().size(), "out-of-core stream complete");
        check(md.digest().length == 32, "SHA-256 consumable streaming");
      }
    }
    check(!Files.exists(spilledFile), "spool deleted on close");
    try (NativePipelineResult result = runtime.run(List.of(java("stderr-large")),
        InputStream.nullInputStream(), Duration.ofSeconds(12))) {
      check(result.stderr(0).size() == 3L * 1024 * 1024, "stderr never deadlocks");
      check(result.stderr(0).spilled(), "stderr spills separately");
    }
    try {
      runtime.run(List.of(java("sleep")), InputStream.nullInputStream(),
          Duration.ofMillis(200));
      throw new AssertionError("timeout not enforced");
    } catch (TimeoutException expected) { check(true, "timeout enforced"); }
    try {
      runtime.run(List.of(), InputStream.nullInputStream(), Duration.ofSeconds(1));
      throw new AssertionError("empty stage list accepted");
    } catch (IllegalArgumentException expected) { check(true, "empty stage list rejected"); }
    try {
      new NativeCommand(Path.of("/tmp/not-admitted"), List.of("unsafe\0arg"));
      throw new AssertionError("NUL argv accepted");
    } catch (IllegalArgumentException expected) { check(true, "NUL argv rejected"); }
    try {
      HostExecutable.find("../../bin/sh");
      throw new AssertionError("path traversal accepted");
    } catch (IllegalArgumentException expected) { check(true, "traversal rejected"); }
    java.util.concurrent.atomic.AtomicInteger variableBudget =
        new java.util.concurrent.atomic.AtomicInteger(8192);
    Path pressureFile;
    try (SpoolingCapture capture = new SpoolingCapture(variableBudget::get)) {
      capture.write(new byte[4096]);
      check(!capture.spilled(), "initially hot under sufficient headroom");
      variableBudget.set(1024);
      capture.write(42);
      check(capture.spilled(), "pressure change spills automatically on next write");
      capture.finish();
      check(capture.size() == 4097, "cold size retained");
      check(capture.bytes()[4096] == 42, "cold content retained");
      pressureFile = capture.spillPath();
    }
    check(!Files.exists(pressureFile), "pressure spill cleaned");
    check(HostExecutable.find("m3-this-command-does-not-exist-9973").isEmpty(),
        "missing host tool is not invented");
    check(HeapCaptureBudget.perStream(3) <= 4 * 1024 * 1024,
        "Xmx-aware bounded memory budget");
    System.out.println("NativeProcessPipelineSelfTest PASS " + assertions + " checks");
  }

  private static void checkByte(boolean ok) {
    if (!ok) throw new AssertionError("spooled byte mismatch");
  }

  /** Separate Java process used as a platform-neutral OS-executable fixture. */
  public static final class Child {
    private Child() { }

    public static void main(String[] args) throws Exception {
      switch (args[0]) {
        case "pass" -> System.in.transferTo(System.out);
        case "upper" -> {
          byte[] buffer = new byte[8192];
          for (int length; (length = System.in.read(buffer)) != -1;) {
            for (int i = 0; i < length; i++) {
              if (buffer[i] >= 'a' && buffer[i] <= 'z') buffer[i] -= 32;
            }
            System.out.write(buffer, 0, length);
          }
        }
        case "suffix" -> {
          System.in.transferTo(System.out);
          System.out.print('!');
        }
        case "fail" -> {
          System.err.print("diagnostic\n");
          System.exit(7);
        }
        case "emit-large" -> emit(System.out, (byte) 'x', 6 * 1024 * 1024);
        case "stderr-large" -> emit(System.err, (byte) 'z', 3 * 1024 * 1024);
        case "sleep" -> Thread.sleep(10_000);
        default -> throw new IllegalArgumentException(args[0]);
      }
    }

    private static void emit(java.io.OutputStream out, byte value, int size) throws IOException {
      byte[] block = new byte[8192];
      Arrays.fill(block, value);
      for (int count = 0; count < size; count += block.length) out.write(block);
      out.flush();
    }
  }
}
