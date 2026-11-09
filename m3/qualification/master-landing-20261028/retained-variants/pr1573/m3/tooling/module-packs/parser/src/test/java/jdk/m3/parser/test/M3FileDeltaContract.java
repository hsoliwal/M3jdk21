/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3File.ByteChange;
import jdk.m3.parser.M3File.SourceDelta;

/** Independent reconstruction and source/encoding identity contracts. */
public final class M3FileDeltaContract {
    private static final URI SOURCE = URI.create("memory:///Example.java");
    private static final char[] ALPHABET = {'a', 'b', '\0', '\r', '\n', '\ud800', '\udc00'};
    private static int checks;

    private M3FileDeltaContract() { }

    public static void main(String[] args) throws Exception {
        examples();
        identities();
        generated();
        refusal();
        System.out.println("M3_FILE_DELTA_PASS checks=" + checks + " generatedPairs=8192");
    }

    private static void examples() {
        window("", "", 0, 0, 0);
        window("abc", "abc", 0, 0, 0);
        window("abc", "abXc", 2, 2, 3);
        window("abXc", "abc", 2, 3, 2);
        window("abc", "axc", 1, 2, 2);
        window("", "abc", 0, 0, 3);
        window("abc", "", 0, 3, 0);
        window("aaaa", "aaa", 3, 4, 3);
        window("abba", "acca", 1, 3, 3);
        window("aXbYc", "aPbQc", 1, 4, 4);
        window("\ud83d\ude00", "\ud83d\ude01", 1, 2, 2);
        window("\ud800", "\udc00", 0, 1, 1);
        String[] carriers = {
            "class T { String s = \"/* not a comment */\"; }",
            "// class Fake { }\nclass T { }",
            "String s = \"\"\"\nclass Fake {}\n\"\"\";",
            "Pattern.compile(\"[a-z]+\\\\d?\");",
            "class T { String s = \"\\u0061\"; }"
        };
        for (String before : carriers) {
            verify(before, before + "\n");
            verify(before, "/*" + before);
            verify(before, before.replace('a', 'x'));
        }
    }

    private static void identities() throws Exception {
        M3File original = M3File.of(SOURCE, "same");
        M3File renamed = M3File.of(URI.create("memory:///Renamed.java"), "same");
        SourceDelta moved = renamed.changesFrom(original);
        check(!moved.sameUri() && !moved.charactersChanged(), "rename is independent of content");
        check(moved.byteChange() == ByteChange.UNKNOWN, "missing byte seals are unknown");
        check(original.changesFrom(original).byteChange() == ByteChange.UNKNOWN,
                "object identity does not invent byte identity");
        StringBuilder mutable = new StringBuilder("before");
        M3File snapshot = M3File.of(SOURCE, mutable);
        mutable.append("after");
        check(!snapshot.changesFrom(M3File.of(SOURCE, "before")).charactersChanged(),
                "mutable input was captured once");
        Path file = Files.createTempFile("m3-source-delta-", ".java");
        try {
            Files.writeString(file, "same", StandardCharsets.UTF_8);
            M3File utf8 = M3File.read(file, StandardCharsets.UTF_8, 64);
            M3File repeat = M3File.read(file, StandardCharsets.UTF_8, 64);
            check(repeat.changesFrom(utf8).byteChange() == ByteChange.MATCHING_SEALS,
                    "equal original bytes");
            Files.writeString(file, "same", StandardCharsets.UTF_16LE);
            M3File utf16 = M3File.read(file, StandardCharsets.UTF_16LE, 64);
            SourceDelta encoding = utf16.changesFrom(utf8);
            check(encoding.sameUri() && !encoding.charactersChanged(), "encoding preserves characters");
            check(encoding.byteChange() == ByteChange.DIFFERENT_SEALS, "encoding changes raw identity");
            M3File characters = M3File.of(utf8.uri(), utf8.text());
            check(characters.changesFrom(utf8).byteChange() == ByteChange.UNKNOWN,
                    "one missing seal is unknown");
            check(utf8.changesFrom(characters).byteChange() == ByteChange.UNKNOWN,
                    "unknown is symmetric");
            Files.writeString(file, "sAme", StandardCharsets.UTF_8);
            SourceDelta edited = M3File.read(file, StandardCharsets.UTF_8, 64).changesFrom(utf8);
            check(edited.charactersChanged() && edited.byteChange() == ByteChange.DIFFERENT_SEALS,
                    "both content lanes changed");
            check(utf8.text().equals("same") && utf16.text().equals("same"), "old snapshots remain valid");
        } finally {
            Files.delete(file);
        }
    }

    private static void generated() {
        Random random = new Random(0x4d3346494c45L);
        for (int i = 0; i < 8192; i++) {
            String before = randomText(random, random.nextInt(65));
            int start = random.nextInt(before.length() + 1);
            int end = start + random.nextInt(before.length() - start + 1);
            String after = switch (i & 3) {
                case 0 -> new String(before.toCharArray());
                case 1 -> before.substring(0, start) + randomText(random, random.nextInt(17))
                        + before.substring(end);
                case 2 -> randomText(random, random.nextInt(65));
                default -> before.substring(0, start) + before.substring(end);
            };
            verify(before, after);
        }
    }

    private static String randomText(Random random, int length) {
        char[] value = new char[length];
        for (int i = 0; i < length; i++) value[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        return new String(value);
    }

    private static void window(String before, String after, int start, int oldEnd, int newEnd) {
        SourceDelta actual = verify(before, after);
        check(actual.start() == start && actual.previousEnd() == oldEnd && actual.currentEnd() == newEnd,
                "expected edit window");
    }

    private static SourceDelta verify(String before, String after) {
        M3File oldFile = M3File.of(SOURCE, before);
        M3File newFile = M3File.of(SOURCE, after);
        SourceDelta delta = newFile.changesFrom(oldFile);
        check(delta.sameUri(), "stable URI");
        check(delta.charactersChanged() == !before.equals(after), "exact character disposition");
        check(delta.byteChange() == ByteChange.UNKNOWN, "no inferred encoding");
        check(delta.start() >= 0 && delta.previousEnd() <= before.length()
                && delta.currentEnd() <= after.length(), "bounded window");
        String reconstructed = before.substring(0, delta.start())
                + after.substring(delta.start(), delta.currentEnd()) + before.substring(delta.previousEnd());
        check(reconstructed.equals(after), "exact reconstruction independent of hashes");
        check(before.substring(0, delta.start()).equals(after.substring(0, delta.start())), "prefix unchanged");
        check(before.substring(delta.previousEnd()).equals(after.substring(delta.currentEnd())), "suffix unchanged");
        if (delta.charactersChanged()) {
            check(delta.start() == Math.min(before.length(), after.length())
                    || before.charAt(delta.start()) != after.charAt(delta.start()), "maximal prefix");
            check(delta.previousEnd() == delta.start() || delta.currentEnd() == delta.start()
                    || before.charAt(delta.previousEnd() - 1) != after.charAt(delta.currentEnd() - 1),
                    "maximal non-overlapping suffix");
        } else {
            check(delta.start() == 0 && delta.previousEnd() == 0 && delta.currentEnd() == 0,
                    "canonical unchanged window");
        }
        SourceDelta reverse = oldFile.changesFrom(newFile);
        check(reverse.start() == delta.start() && reverse.previousEnd() == delta.currentEnd()
                && reverse.currentEnd() == delta.previousEnd(), "reverse symmetry");
        check(oldFile.text() == before && newFile.text() == after, "original immutable payload retained");
        return delta;
    }

    private static void refusal() {
        rejects(NullPointerException.class, () -> M3File.of(SOURCE, "x").changesFrom(null));
        rejects(IllegalArgumentException.class, () -> new SourceDelta(true, -1, 0, 0, ByteChange.UNKNOWN));
        rejects(IllegalArgumentException.class, () -> new SourceDelta(true, 1, 0, 1, ByteChange.UNKNOWN));
        rejects(IllegalArgumentException.class, () -> new SourceDelta(true, 1, 1, 0, ByteChange.UNKNOWN));
        rejects(NullPointerException.class, () -> new SourceDelta(true, 0, 0, 0, null));
    }

    private static void rejects(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected " + type.getName());
        } catch (RuntimeException expected) {
            check(type.isInstance(expected), "expected refusal type");
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
