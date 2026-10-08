/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary M3 String canonical owner/coordinate representation survives GC, CDS, JFR and JVMCI consumers
 * @library /test/lib
 * @run main M3StringRepresentationConsumersTest
 */

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class M3StringRepresentationConsumersTest {
    private static final String PASS = "M3_STRING_REPRESENTATION_CONSUMER_PASS";

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args[0].equals("child")) {
            child();
            return;
        }

        run("serial", List.of("-XX:+UseSerialGC"), null);
        run("parallel", List.of("-XX:+UseParallelGC"), null);
        run("g1", List.of("-XX:+UseG1GC"), null);
        run("zgc", List.of("-XX:+UseZGC"), null);
        run("cds", List.of("-Xshare:on"), null);

        Path recording = Path.of("m3-string-representation-consumers.jfr").toAbsolutePath();
        Files.deleteIfExists(recording);
        run("jfr", List.of("-XX:StartFlightRecording=filename=" + recording
                + ",settings=profile,dumponexit=true"), recording);
        check(Files.isRegularFile(recording) && Files.size(recording) > 0L,
                "JFR recording missing");
        Files.deleteIfExists(recording);

        run("jvmci", List.of("-XX:+EnableJVMCI"), null);
        System.out.println("M3_STRING_REPRESENTATION_CONSUMERS_PASS");
    }

    private static void run(String label, List<String> modeOptions, Path artifact) throws Exception {
        ArrayList<String> command = new ArrayList<>();
        command.add("-Xmx256m");
        command.add("-XX:+UnlockExperimentalVMOptions");
        command.add("-XX:+UseM3StringStorage");
        command.addAll(modeOptions);
        command.add(M3StringRepresentationConsumersTest.class.getName());
        command.add("child");
        OutputAnalyzer output = ProcessTools.executeProcess(
                ProcessTools.createLimitedTestJavaProcessBuilder(command.toArray(String[]::new)));
        output.shouldHaveExitValue(0);
        output.shouldContain(PASS);
        if (artifact != null) {
            check(Files.exists(artifact), label + " artifact absent");
        }
    }

    private static void child() throws Exception {
        Field m3 = String.class.getDeclaredField("m3");
        m3.setAccessible(true);
        Class<?> valueType = Class.forName("java.lang.M3String");
        Field owner = valueType.getDeclaredField("owner");
        owner.setAccessible(true);
        Field coordinate = valueType.getDeclaredField("value");
        coordinate.setAccessible(true);

        long digest = 1L;
        for (int round = 0; round < 8; round++) {
            ArrayList<String> retained = new ArrayList<>();
            for (int item = 0; item < 512; item++) {
                String left = new String(("left-" + round + "-" + item + "-\u0100").toCharArray());
                String right = new String(("\uD83D\uDE42-right-" + item).toCharArray());
                String joined = left.concat(right);
                String sliced = joined.substring(2, joined.length() - 2);
                Object storage = m3.get(sliced);
                check(storage != null, "M3 storage absent");
                check(owner.get(storage) != null, "owner absent");
                long span = coordinate.getLong(storage);
                check(span != 0L, "nonempty coordinate must be nonzero");
                digest = digest * 31L + sliced.hashCode();
                retained.add(sliced);
            }
            for (String value : retained) {
                check(value.length() > 0, "retained String empty");
                digest ^= value.codePointCount(0, value.length());
            }
            retained = null;
            System.gc();
        }
        check(digest != 0L, "digest");
        System.out.println(PASS + "|digest=" + Long.toUnsignedString(digest));
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
