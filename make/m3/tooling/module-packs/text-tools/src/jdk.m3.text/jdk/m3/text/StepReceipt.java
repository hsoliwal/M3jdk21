/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

/** Per-node content lineage in deterministic execution order. */
public record StepReceipt(String nodeId, String bindingSha256, String inputSha256, String outputSha256) {
  public StepReceipt {
    nodeId = Ids.id(nodeId);
    bindingSha256 = Ids.sha256(bindingSha256);
    inputSha256 = Ids.sha256(inputSha256);
    outputSha256 = Ids.sha256(outputSha256);
  }
  public String sha256() {
    return new Canonical("m3-unix4j-step-v1").text(nodeId).text(bindingSha256)
        .text(inputSha256).text(outputSha256).finish();
  }
}
