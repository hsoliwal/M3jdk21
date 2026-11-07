/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Executes a bounded training run and normalizes its profile. */
public final class M3Trainer {
    /** Retained training result. */
    public record Result(Path directory, Path classList, Path recording, Path profile,
                         M3Profile normalizedProfile, M3Process.Result process) {
    }

    private M3Trainer() {
    }

    /** Runs the application with HotSpot class-list and JFR capture. */
    public static Result train(M3JdkTools tools, String classPath, String mainClass,
                               List<String> vmArguments, List<String> applicationArguments,
                               Path output, Duration timeout) throws IOException, InterruptedException {
        Path directory = M3Files.freshDirectory(output);
        Path classList = directory.resolve("classes.classlist");
        Path recording = directory.resolve("training.jfr");
        List<String> command = new ArrayList<>();
        command.add(tools.java().toString());
        command.add("-Xshare:off");
        command.add("-XX:DumpLoadedClassList=" + classList);
        command.add("-XX:StartFlightRecording=filename=" + recording + ",settings=profile,dumponexit=true");
        command.addAll(vmArguments);
        command.add("-cp");
        command.add(classPath);
        command.add(mainClass);
        command.addAll(applicationArguments);
        M3Process.Result process = M3Process.requireSuccess(command, directory, timeout, Map.of());
        M3Files.writeUtf8(directory.resolve("application.stdout.log"), process.stdout());
        M3Files.writeUtf8(directory.resolve("application.stderr.log"), process.stderr());
        M3Profile profile = M3ProfileIO.readHotSpotClassList(classList)
                .merge(M3JfrProfileReader.read(recording));
        Path profileFile = directory.resolve("profile.tsv");
        M3ProfileIO.write(profileFile, profile);
        M3ProfileIO.writeLoadedClasses(directory.resolve("loaded-classes.txt"), profile);
        String properties = "schema=1\n"
                + "mainClass=" + escapeProperty(mainClass) + "\n"
                + "javaRuntimeIdentity=" + tools.runtimeIdentity() + "\n"
                + "exitCode=" + process.exitCode() + "\n";
        M3Files.writeUtf8(directory.resolve("training.properties"), properties);
        M3TrainingManifest.seal(directory);
        M3TrainingManifest.verify(directory);
        return new Result(directory, classList, recording, profileFile, profile, process);
    }

    private static String escapeProperty(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
