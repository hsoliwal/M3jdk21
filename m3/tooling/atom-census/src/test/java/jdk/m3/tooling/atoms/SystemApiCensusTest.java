// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.util.List;

/** Java 21 executable proof; no third-party parser or product JNI required. */
public final class SystemApiCensusTest {
    private static int assertions;
    private SystemApiCensusTest() { }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void refuses(Class<? extends Throwable> expected, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected " + expected.getSimpleName());
        } catch (Throwable actual) {
            check(expected.isInstance(actual), "unexpected refusal: " + actual);
        }
    }

    public static void main(String[] arguments) throws IOException {
        List<SystemApiCensus.ApiType> base = SystemApiCensus.scanModule("java.base", 50000);
        check(base.contains(new SystemApiCensus.ApiType("java.base", "java.lang.String")),
                "String resource must exist in exported java.lang");
        check(base.contains(new SystemApiCensus.ApiType("java.base", "java.util.Map$Entry")),
                "nested Map.Entry resource must retain binary identity");
        check(base.equals(SystemApiCensus.scanModule("java.base", 50000)),
                "repeat scan must be stable");
        check(base.stream().map(SystemApiCensus.ApiType::binaryName).distinct().count() == base.size(),
                "duplicates not allowed");

        List<SystemApiCensus.ApiType> xml = SystemApiCensus.scanModule("java.xml", 50000);
        check(xml.contains(new SystemApiCensus.ApiType("java.xml", "org.w3c.dom.Node")),
                "public non-java package resource must be discovered");
        check(xml.contains(new SystemApiCensus.ApiType("java.xml", "org.xml.sax.InputSource")),
                "another exported non-java package must be discovered");
        check(xml.stream().noneMatch(row -> row.binaryName().startsWith("com.sun.org.apache.xerces.internal.")),
                "unexported XML internals must not be nominated");
        check(xml.equals(SystemApiCensus.scanModule("java.xml", 50000)),
                "java.xml scan must be stable");
        refuses(IllegalArgumentException.class, () -> {
            try { SystemApiCensus.scanModule("java..base", 50000); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
        });
        refuses(IllegalArgumentException.class, () -> {
            try { SystemApiCensus.scanModule("module.that.does.not.exist", 50000); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
        });
        refuses(IllegalStateException.class, () -> {
            try { SystemApiCensus.scanModule("java.base", 1); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
        });
        refuses(IllegalStateException.class, () -> {
            try { SystemApiCensus.scanInstalled(1, 50000); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
        });
        List<SystemApiCensus.ApiType> all = SystemApiCensus.scanInstalled(256, 50000);
        check(all.containsAll(List.of(new SystemApiCensus.ApiType("java.base", "java.lang.String"),
                    new SystemApiCensus.ApiType("java.xml", "org.w3c.dom.Node"))),
                "complete installed-module census must retain both samples");
        check(all.equals(SystemApiCensus.scanInstalled(256, 50000)),
                "whole census must be stable across repeats");
        check(all.size() > xml.size(), "whole census must be larger than one module");
        System.out.println("M3SystemApiCensusTest PASS assertions=" + assertions
                + " installedCandidateResources=" + all.size()
                + " baseCandidateResources=" + base.size());
    }
}
