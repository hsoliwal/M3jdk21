/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.List;

/** Success-only content receipt. Integrity is not a signature or a proof of host trust. */
public record Receipt(String planSha256, String registrySha256, String policySha256,
                      String inputSha256, String outputSha256, List<StepReceipt> steps) {
  public Receipt {
    planSha256 = Ids.sha256(planSha256); registrySha256 = Ids.sha256(registrySha256);
    policySha256 = Ids.sha256(policySha256); inputSha256 = Ids.sha256(inputSha256);
    outputSha256 = Ids.sha256(outputSha256); steps = List.copyOf(steps);
    if (steps.isEmpty()) throw new IllegalArgumentException("receipt needs execution steps");
  }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-receipt-v1").text(planSha256)
        .text(registrySha256).text(policySha256).text(inputSha256).text(outputSha256)
        .number(steps.size());
    steps.forEach(s -> hash.text(s.sha256()));
    return hash.finish();
  }
}
