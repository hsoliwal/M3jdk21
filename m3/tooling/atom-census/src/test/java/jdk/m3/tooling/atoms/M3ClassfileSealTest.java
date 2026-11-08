// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Independent Java21 classfile identity and budget-admission proof. */
public final class M3ClassfileSealTest {
    private static int assertions;

    private M3ClassfileSealTest() { }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static M3CompanionPlan.Request request(String module, String name) {
        return new M3CompanionPlan.Request(module, name);
    }

    @FunctionalInterface private interface Throwing { void call() throws Exception; }

    private static void refuses(Class<? extends Throwable> expected, Throwing action) {
        try {
            action.call();
            throw new AssertionError("expected " + expected.getSimpleName());
        } catch (Throwable failure) {
            check(expected.isInstance(failure), "unexpected exception " + failure);
        }
    }

    private static String directDigest(M3CompanionPlan.Request request) throws Exception {
        var module = ModuleFinder.ofSystem().find(request.moduleName()).orElseThrow();
        try (var reader = module.open()) {
            String path = request.binaryName().replace('.', '/') + ".class";
            ByteBuffer data = reader.read(path).orElseThrow();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(data);
                return HexFormat.of().formatHex(digest.digest());
            } finally {
                reader.release(data);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        check(M3ClassfileSeal.SCHEMA.equals("M3_CLASSFILE_SHA256_V1"), "schema version pinned");
        var string = request("java.base", "java.lang.String");
        var sealed = M3ClassfileSeal.snapshot(string, 1_000_000L);
        check(sealed.moduleName().equals("java.base"), "source module preserved");
        check(sealed.binaryName().equals("java.lang.String"), "binary name preserved");
        check(sealed.bytes() > 500L, "nonempty class file streamed");
        check(sealed.sha256().matches("[0-9a-f]{64}"), "digest is proper hex SHA256");
        check(sealed.sha256().equals(directDigest(string)), "independent ByteBuffer byte oracle");
        check(sealed.equals(M3ClassfileSeal.snapshot(string, sealed.bytes())),
                "exact byte budget accepted; idempotent identity");
        refuses(IllegalStateException.class,
                () -> M3ClassfileSeal.snapshot(string, sealed.bytes() - 1));
        refuses(IllegalStateException.class, () -> M3ClassfileSeal.snapshot(string, 16));
        var entry = request("java.base", "java.util.Map$Entry");
        check(M3ClassfileSeal.snapshot(entry, 1_000_000).sha256().equals(directDigest(entry)),
                "nested binary-name resource matches independent byte oracle");
        var xml = request("java.xml", "org.w3c.dom.Node");
        check(M3ClassfileSeal.snapshot(xml, 1_000_000).sha256().equals(directDigest(xml)),
                "exported non-java package gets exact byte seal");
        var sax = request("java.xml", "org.xml.sax.InputSource");
        check(M3ClassfileSeal.snapshot(sax, 1_000_000).sha256().equals(directDigest(sax)),
                "second xml API gets exact byte seal");
        refuses(IllegalArgumentException.class,
                () -> M3ClassfileSeal.snapshot(request("java.base", "java.util.ArrayList$SubList"), 100_000));
        refuses(IllegalArgumentException.class,
                () -> M3ClassfileSeal.snapshot(request("java.base", "jdk.internal.misc.Unsafe"), 100_000));
        refuses(IllegalArgumentException.class,
                () -> M3ClassfileSeal.snapshot(request("java.desktop", "java.lang.String"), 100_000));
        refuses(IllegalArgumentException.class,
                () -> M3ClassfileSeal.snapshot(request("java.base", "java.lang.Nonexistent_999"), 100_000));
        refuses(IllegalArgumentException.class,
                () -> M3ClassfileSeal.snapshot(string, 0));
        refuses(NullPointerException.class,
                () -> M3ClassfileSeal.snapshot(null, 1000));
        check(M3CompanionPlan.inspect(List.of(string), Set.of()).getFirst().decision()
                == M3CompanionPlan.Decision.REVIEW,
                "sealing never changes planner's REVIEW state");
        System.out.println("M3ClassfileSealTest PASS assertions=" + assertions
                + " stringClassBytes=" + sealed.bytes() + " stringSha256=" + sealed.sha256());
    }
}
