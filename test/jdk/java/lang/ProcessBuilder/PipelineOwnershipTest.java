/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test id=portable
 * @summary Pipeline startup preserves builder settings and closes failed-stage resources
 * @run main/othervm/timeout=180 PipelineOwnershipTest portable
 */

/*
 * @test id=descriptors
 * @requires (os.family == "linux")
 * @summary Failed and successful pipelines retain no intermediate pipe descriptors
 * @run main/othervm/timeout=180 PipelineOwnershipTest descriptors
 */

/*
 * @test id=security
 * @summary Descriptor cleanup must preserve the primary launch failure under a SecurityManager
 * @run main/othervm/timeout=180 -Djava.security.manager=allow PipelineOwnershipTest security
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PipelineOwnershipTest {
    private static final byte[] PAYLOAD = "pipeline-\u03bb-\uD83D\uDE03\n".getBytes(StandardCharsets.UTF_8);
    private static final String JAVA = Path.of(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
    private static int checks;

    private record Settings(ProcessBuilder builder, ProcessBuilder.Redirect input,
            ProcessBuilder.Redirect output, ProcessBuilder.Redirect error, boolean merge,
            List<String> command, java.io.File directory) {
        Settings(ProcessBuilder builder) {
            this(builder, builder.redirectInput(), builder.redirectOutput(), builder.redirectError(),
                    builder.redirectErrorStream(), new ArrayList<>(builder.command()), builder.directory());
        }
        void check() {
            require(builder.redirectInput() == input, "input redirect changed");
            require(builder.redirectOutput() == output, "output redirect changed");
            require(builder.redirectError() == error, "error redirect changed");
            require(builder.redirectErrorStream() == merge, "merge setting changed");
            require(builder.command().equals(command), "command changed");
            require(java.util.Objects.equals(builder.directory(), directory), "directory changed");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("child")) {
            if (args[1].equals("error")) {
                System.err.write('!');
                System.err.flush();
            }
            System.in.transferTo(System.out);
            return;
        }
        if (args[0].equals("portable")) portable();
        else if (args[0].equals("descriptors")) descriptors();
        else if (args[0].equals("security")) security();
        else throw new AssertionError("Unknown mode: " + args[0]);
        System.out.println("PipelineOwnershipTest " + args[0] + ": " + checks + " checks passed");
    }

    private static ProcessBuilder child() { return child("echo"); }

    private static ProcessBuilder child(String mode) {
        return new ProcessBuilder(JAVA, "-cp", System.getProperty("java.class.path"),
                PipelineOwnershipTest.class.getName(), "child", mode);
    }

    private static List<Settings> settings(List<ProcessBuilder> builders) {
        List<Settings> result = new ArrayList<>();
        for (ProcessBuilder builder : builders) if (builder != null) result.add(new Settings(builder));
        return result;
    }

    private static void portable() throws Exception {
        List<ProcessBuilder> builders = List.of(child(), child(), child());
        List<Settings> original = settings(builders);
        for (int repeat = 0; repeat < 3; repeat++) {
            exchange(builders, PAYLOAD);
            original.forEach(Settings::check);
        }

        ProcessBuilder shared = child();
        List<ProcessBuilder> repeatedObject = List.of(shared, shared);
        List<Settings> sharedSettings = settings(repeatedObject);
        for (int repeat = 0; repeat < 2; repeat++) {
            exchange(repeatedObject, PAYLOAD);
            sharedSettings.forEach(Settings::check);
        }

        ProcessBuilder error = child("error").redirectErrorStream(true);
        List<ProcessBuilder> merged = List.of(error, child());
        List<Settings> mergedSettings = settings(merged);
        byte[] mergedExpected = new byte[PAYLOAD.length + 1];
        mergedExpected[0] = '!';
        System.arraycopy(PAYLOAD, 0, mergedExpected, 1, PAYLOAD.length);
        exchange(merged, mergedExpected);
        mergedSettings.forEach(Settings::check);
        redirectedEndpoints();

        expectFailure(IOException.class, List.of(child(), missing()), true);
        expectFailure(IndexOutOfBoundsException.class,
                List.of(child(), new ProcessBuilder(List.of())), true);
        ProcessBuilder nullCommand = new ProcessBuilder(new ArrayList<>(Arrays.asList(JAVA, null)));
        expectFailure(NullPointerException.class, List.of(child(), nullCommand), true);
        expectFailure(NullPointerException.class, Arrays.asList(child(), null), true);
        expectFailure(IllegalArgumentException.class,
                List.of(child(), child().redirectInput(ProcessBuilder.Redirect.INHERIT)), true);
        expectFailure(IllegalArgumentException.class,
                List.of(child(), child().redirectOutput(ProcessBuilder.Redirect.INHERIT), child()), true);
        expectFailure(IllegalArgumentException.class,
                List.of(child().redirectOutput(ProcessBuilder.Redirect.DISCARD), child()), true);
        require(ProcessBuilder.startPipeline(List.of()).isEmpty(), "empty pipeline must stay empty");
        try {
            ProcessBuilder.startPipeline(null);
            throw new AssertionError("null builders accepted");
        } catch (NullPointerException expected) { checks++; }

        // The existing cleanup contract preserves an already set interrupt flag.
        List<ProcessBuilder> interrupted = List.of(child(), missing());
        List<Settings> interruptedSettings = settings(interrupted);
        Set<Long> before = children();
        Thread.currentThread().interrupt();
        try {
            ProcessBuilder.startPipeline(interrupted);
            throw new AssertionError("missing command accepted while interrupted");
        } catch (IOException expected) {
            require(Thread.currentThread().isInterrupted(), "cleanup lost interruption");
        } finally {
            Thread.interrupted();
        }
        interruptedSettings.forEach(Settings::check);
        noNewChildren(before);
    }

    private static void redirectedEndpoints() throws Exception {
        Path input = Files.createTempFile("pipeline-owner-input", ".bin");
        Path output = Files.createTempFile("pipeline-owner-output", ".bin");
        try {
            Files.write(input, PAYLOAD);
            List<ProcessBuilder> builders = List.of(child().redirectInput(input.toFile()),
                    child().redirectOutput(output.toFile()));
            List<Settings> original = settings(builders);
            for (int repeat = 0; repeat < 2; repeat++) {
                List<Process> processes = ProcessBuilder.startPipeline(builders);
                try { finish(processes); } finally { close(processes); }
                require(Arrays.equals(PAYLOAD, Files.readAllBytes(output)), "endpoint redirects changed");
                original.forEach(Settings::check);
            }
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    @SuppressWarnings("removal")
    private static void security() throws Exception {
        ProcessBuilder refused = missing();
        SecurityException primary = new SecurityException("pipeline startup refused");
        boolean[] armed = {false};
        int[] descriptorReads = {0};
        Set<Long> beforeChildren = children();
        Set<String> beforePipes = System.getProperty("os.name").equals("Linux") ? pipes() : null;
        List<ProcessBuilder> builders = List.of(child(), refused);
        List<Settings> original = settings(builders);
        Throwable observed = null;
        System.setSecurityManager(new SecurityManager() {
            @Override public void checkPermission(java.security.Permission permission) { }
            @Override public void checkExec(String command) {
                if (command.equals(refused.command().getFirst())) {
                    armed[0] = true;
                    throw primary;
                }
            }
            @Override public void checkRead(java.io.FileDescriptor descriptor) {
                if (armed[0]) {
                    descriptorReads[0]++;
                    throw new SecurityException("cleanup attempted an unrelated descriptor read");
                }
            }
        });
        try {
            ProcessBuilder.startPipeline(builders);
        } catch (Throwable failure) {
            observed = failure;
        } finally {
            System.setSecurityManager(null);
        }
        try {
            require(observed == primary, "cleanup masked the primary startup exception: " + observed);
            require(descriptorReads[0] == 0, "descriptor disposal requested read permission");
            original.forEach(Settings::check);
            noNewChildren(beforeChildren);
            if (beforePipes != null) noNewPipes(beforePipes, "security-denied launch");
        } finally {
            // A failed candidate must not leave the regression's children alive.
            ProcessHandle.current().children().filter(p -> !beforeChildren.contains(p.pid()))
                    .forEach(ProcessHandle::destroyForcibly);
        }
    }

    private static ProcessBuilder missing() {
        return new ProcessBuilder(Path.of(System.getProperty("java.io.tmpdir"),
                "absent-pipeline-owner-command-" + ProcessHandle.current().pid()).toString());
    }

    private static void exchange(List<ProcessBuilder> builders, byte[] expected) throws Exception {
        List<Process> processes = ProcessBuilder.startPipeline(builders);
        try {
            processes.getFirst().getOutputStream().write(PAYLOAD);
            processes.getFirst().getOutputStream().close();
            byte[] actual = processes.getLast().getInputStream().readAllBytes();
            require(Arrays.equals(expected, actual), "pipeline output changed");
            for (int index = 0; index < processes.size() - 1; index++) {
                require(processes.get(index).getInputStream().read() == -1,
                        "intermediate stdout must remain inaccessible");
            }
            for (int index = 1; index < processes.size(); index++) {
                try {
                    processes.get(index).getOutputStream().write(1);
                    throw new AssertionError("intermediate stdin became accessible");
                } catch (IOException expectedFailure) { checks++; }
            }
            finish(processes);
        } finally { close(processes); }
    }

    private static void finish(List<Process> processes) throws Exception {
        for (Process process : processes) {
            require(process.waitFor(15, TimeUnit.SECONDS), "child did not exit");
            require(process.exitValue() == 0, "unexpected child exit");
            require(process.getErrorStream().readAllBytes().length == 0, "unexpected child stderr");
        }
    }

    private static void close(List<Process> processes) throws IOException {
        for (Process process : processes) {
            if (process.isAlive()) process.destroyForcibly();
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }

    private static void expectFailure(Class<? extends Exception> type,
            List<ProcessBuilder> builders, boolean checkSettings) throws Exception {
        List<Settings> original = settings(builders);
        Set<Long> before = children();
        try {
            ProcessBuilder.startPipeline(builders);
            throw new AssertionError("Expected " + type.getName());
        } catch (Exception failure) {
            require(type.isInstance(failure), "wrong primary exception: " + failure);
        }
        if (checkSettings) original.forEach(Settings::check);
        noNewChildren(before);
    }

    private static Set<Long> children() {
        Set<Long> result = new HashSet<>();
        ProcessHandle.current().children().filter(ProcessHandle::isAlive)
                .forEach(child -> result.add(child.pid()));
        return result;
    }

    private static void noNewChildren(Set<Long> before) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Set<Long> remaining;
        do {
            remaining = children();
            remaining.removeAll(before);
            if (remaining.isEmpty()) { checks++; return; }
            Thread.sleep(10);
        } while (System.nanoTime() - deadline < 0);
        throw new AssertionError("pipeline left children alive: " + remaining);
    }

    private static Set<String> pipes() throws IOException {
        Set<String> result = new HashSet<>();
        try (var paths = Files.list(Path.of("/proc/self/fd"))) {
            for (Path path : paths.toList()) {
                try {
                    String link = Files.readSymbolicLink(path).toString();
                    if (link.startsWith("pipe:")) result.add(path.getFileName() + "=" + link);
                } catch (NoSuchFileException closedConcurrently) { }
            }
        }
        return result;
    }

    private static void descriptors() throws Exception {
        // Start the JVM's process/reaper machinery before taking a resource snapshot.
        exchange(List.of(child()), PAYLOAD);
        Set<String> before = pipes();
        for (int repeat = 0; repeat < 24; repeat++) {
            expectFailure(IOException.class, List.of(child(), missing()), false);
        }
        noNewPipes(before, "failed native launch");
        for (int repeat = 0; repeat < 24; repeat++) {
            expectFailure(NullPointerException.class, Arrays.asList(child(), null), false);
        }
        noNewPipes(before, "failed Java validation");
        for (int repeat = 0; repeat < 8; repeat++) {
            exchange(List.of(child(), child(), child()), PAYLOAD);
        }
        noNewPipes(before, "successful pipeline");
    }

    private static void noNewPipes(Set<String> before, String operation) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        Set<String> extra;
        do {
            extra = pipes();
            extra.removeAll(before);
            if (extra.isEmpty()) {
                checks++;
                System.out.println(operation + ": zero additional pipe descriptors");
                return;
            }
            Thread.sleep(10);
        } while (System.nanoTime() - deadline < 0);
        throw new AssertionError(operation + " leaked " + extra.size() + " pipe descriptors: " + extra);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
}
