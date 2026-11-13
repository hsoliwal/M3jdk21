/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.List;
import java.util.Set;

/** Dependency-free framing/admission/receipt demo, explicitly not a Unix4j runtime test. */
public final class CoreDemo {
  private CoreDemo() {}
  public static void main(String[] args) {
    Binding identity = new Binding("identity", "copy immutable input lines",
        new Canonical("core-demo-definition-v1").text("identity").finish(), Set.of(Capability.TEXT),
        (input, output) -> input.lines().forEach(output));
    Registry registry = new Registry(List.of(identity));
    M3UnixRuntime runtime = new M3UnixRuntime(registry, Limits.standard(), Set.of(Capability.TEXT));
    Plan plan = new Plan(List.of(Node.of("copy", identity)), "copy");
    TextFrame input = TextFrame.fromString("Synexia\r\nUnix4j\nM3");
    ExecutionResult first = runtime.execute(plan, input);
    ExecutionResult second = runtime.replay(plan, input, first);
    System.out.println("CORE_RUNTIME_PASS output=" + first.output().sha256());
    System.out.println("RECEIPT_VERIFY_PASS receipt=" + first.receipt().sha256());
    System.out.println("REPLAY_PASS equal=" + first.equals(second));
    System.out.println("DONOR_RUNTIME_NOT_EXERCISED");
  }
}
