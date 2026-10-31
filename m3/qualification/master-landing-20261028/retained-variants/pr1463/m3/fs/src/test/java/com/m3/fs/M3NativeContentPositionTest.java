// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class M3NativeContentPositionTest {
  @Test void freshOffAndRequiredVmsAgreeOnPositionedEndianLanes() throws Exception {
    org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
    for (String mode : List.of("off", "required")) {
      List<String> command = new ArrayList<>();
      command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
      command.addAll(List.of("-Xcheck:jni", "-Dm3fs.native=" + mode,
          "-Dm3fs.native.threshold=2147483647", "-Dm3fs.grep.native.threshold=2147483647",
          "-Dm3fs.utf16.native.threshold=2147483647"));
      for (String property : List.of("m3.fs.native.path", "m3.fs.native.pathFile")) {
        String value = System.getProperty(property);
        if (value != null) command.add("-D" + property + "=" + value);
      }
      command.addAll(List.of("-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
          M3NativeContentPositionChecks.class.getName()));
      Process child = new ProcessBuilder(command).redirectErrorStream(true).start();
      if (!child.waitFor(30, TimeUnit.SECONDS)) { child.destroyForcibly(); fail("content probe timed out: " + mode); }
      String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, child.exitValue(), output);
      assertTrue(output.contains("CONTENT_POSITION_PASS mode=" + mode), output);
    }
  }
}
