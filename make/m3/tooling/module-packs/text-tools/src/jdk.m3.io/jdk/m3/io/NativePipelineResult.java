// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Owns all spool files; always close this result after consuming its byte streams. */
public final class NativePipelineResult implements AutoCloseable {
  private final List<Integer> exitCodes;
  private final SpoolingCapture stdout;
  private final List<SpoolingCapture> stderrs;

  NativePipelineResult(List<Integer> exitCodes, SpoolingCapture stdout,
      List<SpoolingCapture> stderrs) {
    this.exitCodes = List.copyOf(exitCodes);
    this.stdout = Objects.requireNonNull(stdout, "stdout");
    this.stderrs = List.copyOf(stderrs);
  }

  public List<Integer> exitCodes() { return exitCodes; }
  public boolean success() { return exitCodes.stream().allMatch(code -> code == 0); }
  public SpoolingCapture stdout() { return stdout; }
  public SpoolingCapture stderr(int stage) { return stderrs.get(stage); }

  @Override public void close() throws IOException {
    IOException error = null;
    for (SpoolingCapture capture : stderrs) {
      try { capture.close(); }
      catch (IOException e) { if (error == null) error = e; else error.addSuppressed(e); }
    }
    try { stdout.close(); }
    catch (IOException e) { if (error == null) error = e; else error.addSuppressed(e); }
    if (error != null) throw error;
  }
}
