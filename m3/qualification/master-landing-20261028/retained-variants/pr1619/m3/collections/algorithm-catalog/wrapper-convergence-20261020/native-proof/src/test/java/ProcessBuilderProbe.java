/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Qualification-only execution of the installed image's process-launch path.
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class ProcessBuilderProbe {
    public static void main(String[] args) throws Exception {
        if (!"POSIX_SPAWN".equals(System.getProperty("jdk.lang.Process.launchMechanism"))) {
            throw new AssertionError("Run this proof with the POSIX_SPAWN mechanism.");
        }
        String shell = execute(List.of("/bin/sh", "-c", "printf M3_PROCESS_SHELL_PASS"));
        if (!shell.equals("M3_PROCESS_SHELL_PASS")) {
            throw new AssertionError("Unexpected child shell result: " + shell);
        }
        String childJava = execute(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version"));
        if (!childJava.contains("version \"21.")) {
            throw new AssertionError("Child JVM did not execute: " + childJava);
        }
        System.out.println("PROCESS_BUILDER_PASS mechanism=POSIX_SPAWN shell=true childJvm=true");
    }

    private static String execute(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) throw new AssertionError("Child exit " + exit + ": " + output);
        return output;
    }
}
