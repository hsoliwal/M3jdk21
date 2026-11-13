/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

/** Immutable complete output and its content-bound success receipt. */
public record ExecutionResult(TextFrame output, Receipt receipt) {
  public ExecutionResult {
    java.util.Objects.requireNonNull(output); java.util.Objects.requireNonNull(receipt);
    if (!output.sha256().equals(receipt.outputSha256())) {
      throw new IllegalArgumentException("output/receipt mismatch");
    }
  }
}
