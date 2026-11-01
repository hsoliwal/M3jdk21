/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Materializes deterministic input bundles used by the shell-level integration proof. */
public final class M3ExecutionProfileFixture {
    private static final String RUNTIME = "0123456789abcdef0123456789abcdef01234567";
    private static final String WORKLOAD = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String TARGET = "linux-x86_64";

    private M3ExecutionProfileFixture() {
    }

    /**
     * Writes {@code input-a} and {@code input-b} beneath the supplied empty root.
     *
     * @param args one output-root argument
     * @throws IOException when fixture bundles cannot be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: <output-root>");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(root)) {
            try (var stream = Files.list(root)) {
                if (stream.findAny().isPresent()) {
                    throw new IllegalStateException("fixture output root must be empty");
                }
            }
        } else {
            Files.createDirectories(root);
        }

        M3ExecutionProfileObservation load = M3ExecutionProfileObservation.create(
                M3ExecutionProfileKind.CLASS_LOAD,
                "jvm:class:example/Main",
                "-",
                M3ExecutionProfilePhase.STARTUP,
                1L,
                "evidence/load.trace",
                "1111111111111111111111111111111111111111111111111111111111111111");
        M3ExecutionProfileObservation reflection = M3ExecutionProfileObservation.create(
                M3ExecutionProfileKind.REFLECTIVE_MEMBER,
                "jvm:method:example/Config#value()Ljava/lang/String;",
                "jvm:class:example/Main",
                M3ExecutionProfilePhase.STARTUP,
                2L,
                "evidence/reflection.trace",
                "2222222222222222222222222222222222222222222222222222222222222222");
        M3ExecutionProfileObservation compiled = M3ExecutionProfileObservation.create(
                M3ExecutionProfileKind.COMPILED_METHOD,
                "jvm:method:example/Main#run()V",
                "compiler:c2",
                M3ExecutionProfilePhase.STEADY_STATE,
                3L,
                "evidence/compile.trace",
                "3333333333333333333333333333333333333333333333333333333333333333");

        M3ExecutionProfileBundle.create(
                RUNTIME, WORKLOAD, TARGET, TARGET, "fixture-capture-a", "1", List.of(load, reflection))
                .write(root.resolve("input-a"));
        M3ExecutionProfileBundle.create(
                RUNTIME, WORKLOAD, TARGET, TARGET, "fixture-capture-b", "1", List.of(load, compiled))
                .write(root.resolve("input-b"));
    }
}
