// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome.aix;

import com.m3.llm.chrome.ChromeMlRunner;
import com.m3.llm.chrome.ChromeMlRunnerResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Real plan/snapshot/multipass classes; runner outcomes are fixtures, not native inference. */
public final class AiXCompletionBoundarySelfTest {
    private static int checks;
    private AiXCompletionBoundarySelfTest() { }
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static AiXMechanicalPlan.Plan source(Path file, String text) throws Exception {
        Files.writeString(file, text);
        return AiXMechanicalPlan.build(AiXSourceSnapshot.read(file), new AiXMechanicalPlan.Limits(256, 256));
    }
    private static AiXChromeMlMultipass.Result complete(AiXMechanicalPlan.Plan plan, int fanout) throws Exception {
        AtomicInteger n = new AtomicInteger();
        ChromeMlRunner runner = (request, sink) -> new ChromeMlRunnerResult(" summary " + n.getAndIncrement() + " ", 1, "", "");
        return AiXChromeMlMultipass.run(plan, runner, new AiXChromeMlMultipass.Config(fanout, 128, 128));
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aix-completion-contract-"); Path file = directory.resolve("source.txt");
        try {
            if (args.length > 0 && args[0].equals("compatibility")) {
                for (int i = 0; i < 12; i++) {
                    var plan = source(file, "class X { String s = \"MUST NOT rewrite code\"; }\r\n😀".repeat(i * 3));
                    for (int fanout : new int[]{2, 3, 6, 8}) {
                        var r = complete(plan, fanout);
                        System.out.println("VALID " + i + " " + fanout + " " + r.candidateRoot() + " " + r.modelCalls());
                    }
                }
                return;
            }
            var plan = source(file, "MUST NOT lose a source range. 😀\n".repeat(40));
            int rejected = 0, incorrectlyAccepted = 0;
            var statuses = new Integer[]{null, -1, 0, 2, 9};
            for (Integer status : statuses) {
                // Exercise first and later leaf failure, and failure at the first reduction.
                for (int failAt : new int[]{0, 1, plan.chunks().size()}) {
                    AtomicInteger calls = new AtomicInteger();
                    try {
                        AiXChromeMlMultipass.run(plan, (request, sink) -> {
                            int i = calls.getAndIncrement();
                            return new ChromeMlRunnerResult("looks like a valid summary", i == failAt ? status : Integer.valueOf(1), "private", "private");
                        });
                        incorrectlyAccepted++;
                        System.out.println("FAIL incomplete accepted status=" + status + " position=" + failAt);
                    } catch (IllegalStateException expected) {
                        check(expected.getMessage().equals("MODEL_CANDIDATE_NATIVE_INCOMPLETE"));
                        check(calls.get() == failAt + 1); rejected++;
                    }
                }
            }
            AtomicInteger calls = new AtomicInteger();
            try {
                AiXChromeMlMultipass.run(plan, (r,s) -> { calls.incrementAndGet(); return null; });
                throw new AssertionError("missing null result refusal");
            } catch (IllegalStateException expected) {
                check(expected.getMessage().equals("MODEL_CANDIDATE_NATIVE_INCOMPLETE")); check(calls.get() == 1);
            } catch (NullPointerException oldDefect) {
                incorrectlyAccepted++; System.out.println("FAIL null completion has no typed refusal");
            }
            var good = complete(plan, 6); check(!good.promotionAuthorized());
            check(good.candidates().stream().allMatch(AiXChromeMlMultipass.Candidate::candidateOnly));
            check(good.sourceSha256().equals(plan.sourceSha256()));
            check(plan.chunks().size() == 6);
            System.out.println("COMPLETION_BOUNDARY rejectedStatuses=" + rejected + " failures=" + incorrectlyAccepted + " assertions=" + checks);
            if (incorrectlyAccepted != 0) throw new AssertionError("native completion not enforced: " + incorrectlyAccepted);
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
    }
}
