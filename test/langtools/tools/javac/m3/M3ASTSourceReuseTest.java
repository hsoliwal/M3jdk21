/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

/* @test
 * @summary Exact verified immutable source avoids redundant digests while rereads and edit refusal remain
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.m3:+open
 * @run main/othervm M3ASTSourceReuseTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class M3ASTSourceReuseTest {
    private static int checks;
    private static long digestBytes;

    /** Counts actual production SHA-256 input; the delegate is the original SUN implementation. */
    public static final class CountingDigest extends MessageDigestSpi {
        private final MessageDigest delegate;
        public CountingDigest() {
            try { delegate = MessageDigest.getInstance("SHA-256", "SUN"); }
            catch (NoSuchAlgorithmException | NoSuchProviderException failure) {
                throw new AssertionError(failure);
            }
        }
        @Override protected void engineUpdate(byte value) {
            digestBytes++;
            delegate.update(value);
        }
        @Override protected void engineUpdate(byte[] bytes, int offset, int length) {
            digestBytes += length;
            delegate.update(bytes, offset, length);
        }
        @Override protected byte[] engineDigest() { return delegate.digest(); }
        @Override protected void engineReset() { delegate.reset(); }
        @Override protected int engineGetDigestLength() { return 32; }
    }

    private static final class CountingProvider extends Provider {
        private static final long serialVersionUID = 1L;
        CountingProvider() {
            super("M3SourceDigestCounter", "1.0", "Exact source digest-work observation");
            put("MessageDigest.SHA-256", CountingDigest.class.getName());
        }
    }

    private static final class Source extends SimpleJavaFileObject {
        String text;
        final boolean fresh;
        int reads;
        Source(String name, boolean fresh) {
            super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
            this.fresh = fresh;
            text = "/*" + "x".repeat(49152) + "*/ class " + name
                    + " { String s = \"Aa\"; } class " + name + "Peer {} class "
                    + name + "Third {}";
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return fresh ? new String(text.toCharArray()) : text;
        }
    }

    public static void main(String[] args) throws Exception {
        Provider provider = new CountingProvider();
        check(Security.insertProviderAt(provider, 1) == 1, "counter provider installed first");
        try (StandardJavaFileManager files = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            Path work = Files.createTempDirectory("m3-source-reuse-");
            scenario(files, work, "SameText", false, false, false);
            scenario(files, work, "EqualText", true, false, false);
            scenario(files, work, "ClearedText", true, true, false);
            scenario(files, work, "HashCollisionEdit", true, false, true);
            scenario(files, work, "ClearedEdit", true, true, true);
        } finally {
            Security.removeProvider(provider.getName());
        }
        check(Security.getProvider(provider.getName()) == null, "counter provider removed");
        System.out.println("M3_AST_SOURCE_REUSE checks=" + checks);
    }

    private static void scenario(StandardJavaFileManager files, Path work, String name,
                                 boolean fresh, boolean cleared, boolean edit) throws Exception {
        Source input = new Source(name, fresh);
        String original = input.text;
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files,
                new DiagnosticCollector<>(), List.of("-proc:none", "-d", work.toString()),
                null, List.of(input));
        M3AST session = M3AST.open(task);
        String[] pinned = new String[1];
        long[] analyzedBytes = {0};
        int[] analyzed = {0};
        Object[] firstSeal = {null};
        M3AST.Unit[] expired = new M3AST.Unit[1];
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                M3AST.Source source = unit.source();
                check(unit.source() == source, "event source identity");
                check(source.text().equals(original), "exact parsed text");
                check(source.utf16Sha256().equals(scalarDigest(original)), "parse digest matches scalar");
                pinned[0] = source.text(); // Keep warm-cache cases deterministic without relying on GC.
                firstSeal[0] = seal(session, input.toUri());
                if (cleared) {
                    check(firstSeal[0] instanceof Reference<?>, "source payload is weakly referenced");
                    ((Reference<?>) firstSeal[0]).clear();
                    check(((Reference<?>) firstSeal[0]).get() == null, "cache explicitly expired");
                }
                if (edit) {
                    input.text = original.replace("\"Aa\"", "\"BB\"");
                    check(!input.text.equals(original), "edit differs exactly");
                    check(input.text.length() == original.length(), "collision edit length unchanged");
                    check(input.text.hashCode() == original.hashCode(), "collision edit String hash unchanged");
                }
                check(input.reads == 1, "PARSE adds no input reread");
            }

            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                int previousReads = input.reads;
                long before = digestBytes;
                if (edit) {
                    IOException first = refused(unit);
                    check(refused(unit) == first, "refusal instance memoized in event");
                    check(first.getMessage().equals("M3AST source changed since parsing: " + input.toUri()),
                            "unchanged edit refusal message");
                } else {
                    M3AST.Source source = unit.source();
                    check(unit.source() == source, "success instance memoized in event");
                    check(source.text().equals(original), "exact analyzed content");
                    check(source.utf16Sha256().equals(scalarDigest(original)), "exact analyzed digest");
                    if (fresh) check(source.text() != pinned[0], "equal content has a distinct String owner");
                    if (cleared && analyzed[0] == 0) {
                        Object renewed = seal(session, input.toUri());
                        check(renewed != firstSeal[0], "successful fallback replaces expired metadata");
                        check(renewed instanceof Reference<?> && ((Reference<?>) renewed).get() == source.text(),
                                "renewed seal weakly references verified text");
                        pinned[0] = source.text();
                    }
                }
                long delta = digestBytes - before;
                long expected = edit || cleared && analyzed[0] == 0 ? original.length() * 2L : 0;
                check(delta == expected, "exact digest work: " + name + " event=" + analyzed[0]
                        + " expected=" + expected + " actual=" + delta);
                check(input.reads == previousReads + 1, "one required reread per event");
                analyzedBytes[0] += delta;
                analyzed[0]++;
                expired[0] = unit;
            }
        });
        check(task.call(), "handled source requests preserve compilation");
        M3AST.Summary summary = session.summary();
        check(analyzed[0] == 3 && summary.analyzedTypes() == 3, "all three type callbacks execute");
        check(input.reads == 4 && summary.sourceReads() == 3 && summary.capturedSources() == 1,
                "source capture and mandatory reread counts unchanged");
        check(summary.closed() && summary.completed() && !summary.consumerFailed(), "normal completion");
        check(pinned[0] != null, "fixture keeps verified owner alive across callbacks");
        check(seals(session).isEmpty(), "session close releases source seals");
        try { expired[0].source(); throw new AssertionError("expired callback escaped"); }
        catch (IllegalStateException expected) { checks++; }
        System.out.println("scenario=" + name + " utf16Units=" + original.length()
                + " analyzedDigestBytes=" + analyzedBytes[0] + " sourceReads=" + summary.sourceReads());
    }

    private static IOException refused(M3AST.Unit unit) throws Exception {
        try { unit.source(); throw new AssertionError("edited source accepted"); }
        catch (IOException expected) { return expected; }
    }

    private static Object seal(M3AST session, URI uri) throws Exception {
        Object result = seals(session).get(uri);
        check(result != null, "captured source has a seal");
        return result;
    }

    private static Map<?, ?> seals(M3AST session) throws Exception {
        Field field = session.getClass().getDeclaredField("seals");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(session);
    }

    private static String scalarDigest(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256", "SUN");
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
