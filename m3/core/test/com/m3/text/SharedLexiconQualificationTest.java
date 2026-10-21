/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Independent ordered-content seal oracle and two-machine ID composition contract. */
public final class SharedLexiconQualificationTest {
    static int checks;
    static final String REV = "32bd9b17239c0fb1cdea3f4749496144c5b8c0f0";
    static final List<String> ORDER = List.of("z", "a");
    static final String SOURCE = "unicodex-test@" + REV;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    interface Action { void run() throws Exception; }
    static void refuse(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); throw new AssertionError("missing " + type); }
        catch (Exception failure) { if (!type.isInstance(failure)) throw failure; checks++; }
    }
    static void text(DataOutputStream out, String value) throws IOException {
        out.writeInt(value.length());
        for (int i = 0; i < value.length(); i++) out.writeChar(value.charAt(i));
    }
    static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String orderedSeal(List<String> order, String revision, String version) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            text(out, "M3-UNICODEX-RANK-MAP-1");
            text(out, "synexia-unicodex"); text(out, version); text(out, revision);
            text(out, SOURCE); text(out, "en"); text(out, "main");
            out.writeInt(order.size());
            for (int rank = 0; rank < order.size(); rank++) {
                out.writeInt(rank); text(out, Integer.toString(rank)); text(out, order.get(rank));
            }
        }
        return hash(bytes.toByteArray());
    }
    static String projectionSeal(Path root) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            text(out, "M3-UNICODEX-PROJECTION-1"); out.writeInt(1);
            out.writeInt(0); out.writeInt(2); out.writeLong(2);
            out.write(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(root.resolve("one.m3lex"))));
        }
        return hash(bytes.toByteArray());
    }
    static SharedLexiconCatalog.Qualification qualification(Path root, List<String> words,
            String revision, String version) throws Exception {
        return new SharedLexiconCatalog.Qualification("synexia-unicodex", version, revision,
                SOURCE, "en", "main", 2, orderedSeal(words, revision, version), projectionSeal(root));
    }
    static void writeV2(Path path, List<String> words) throws Exception {
        int units = words.stream().mapToInt(String::length).sum();
        int payload = 64 + 12 * words.size();
        var bytes = java.nio.ByteBuffer.allocate(payload + 2 * units);
        bytes.putLong(0, 0x4d334c4558303031L).putInt(8, 2).putInt(12, words.size())
                .putLong(16, payload).putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row); int at = 64 + 12 * row;
            bytes.putInt(at, offset).putInt(at + 4, word.length()).putInt(at + 8, word.hashCode());
            for (int i = 0; i < word.length(); i++) {
                char unit = word.charAt(i); int dest = payload + 2 * (offset + i);
                bytes.put(dest, (byte) unit).put(dest + 1, (byte) (unit >>> 8));
            }
            offset += word.length();
        }
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.asReadOnlyBuffer().slice(0, 32));
        digest.update(bytes.asReadOnlyBuffer().slice(64, bytes.limit() - 64));
        bytes.put(32, digest.digest()); Files.write(path, bytes.array());
    }

    static void write(Path root) throws Exception {
        writeV2(root.resolve("one.m3lex"), List.of("a", "z"));
        Files.writeString(root.resolve("synexia.shards.tsv"),
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                + "0\tone.m3lex\ta\tz\t2\t2\t" + hash(Files.readAllBytes(root.resolve("one.m3lex"))) + "\n");
        Files.writeString(root.resolve("synexia.records.tsv"),
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                + SOURCE + "\tfixture\tdictionary\ten\t1\ta\t0\t0\t-\t-\t-\tfixture\t{}\n"
                + SOURCE + "\tfixture\tdictionary\ten\t0\tz\t0\t1\t-\t-\t-\tfixture\t{}\n");
        Files.writeString(root.resolve("synexia.precompute.tsv"),
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                + "0\t0\t1\t" + "a".hashCode() + "\t1\t0\t0\tTrue\tTrue\tFalse\tfixture\n"
                + "0\t1\t1\t" + "z".hashCode() + "\t1\t0\t0\tTrue\tTrue\tFalse\tfixture\n");
        Files.writeString(root.resolve("synexia.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                + "fixture\t2\t2\t" + hash("fixture\t2\t2\n".getBytes(StandardCharsets.UTF_8)) + "\n");
    }
    static void delete(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
    public static void main(String[] args) throws Exception {
        Path sender = Files.createTempDirectory("m3lex-sender-");
        Path receiver = Files.createTempDirectory("m3lex-receiver-");
        try {
            write(sender);
            try (var paths = Files.list(sender)) {
                for (Path file : paths.toList()) Files.copy(file, receiver.resolve(file.getFileName()));
            }
            var pin = qualification(sender, ORDER, REV, "ordering-test-v1");
            try (var a = SharedLexiconCatalog.openQualified(sender, pin);
                 var b = SharedLexiconCatalog.openQualified(receiver, pin)) {
                a.requireCompatible(b.qualification());
                check(a.wordCount() == 2);
                check(a.coordinate(0).imageRow() == 1);
                check(a.coordinate(1).imageRow() == 0);
                check(a.textAtRank(0).equals("z"));
                if (args.length == 1 && args[0].equals("--cold")) {
                    // Optional integration lane has both independent modules on its test classpath.
                    // Production modules retain their existing dependency boundaries.
                    Class.forName("com.m3.fs.M3QualifiedColdFileTest").getMethod("main", String[].class)
                            .invoke(null, (Object) new String[] {a.qualification().sha256(), b.qualification().sha256()});
                }
                // Only occurrence ranks cross this test stream. Exact local unknown/whitespace
                // use existing String-piece ownership; this is not a production wire codec.
                var wire = new ByteArrayOutputStream();
                try (var out = new DataOutputStream(wire)) { out.writeInt(0); out.writeInt(1); out.writeInt(0); }
                var local = new LocalM3Arena();
                String unknown = " \t\r\nnew\uD800\u0000";
                try (var in = new DataInputStream(new ByteArrayInputStream(wire.toByteArray()))) {
                    M3StringPiece joined = M3StringPiece.join(
                            b.copyRank(in.readInt(), local), local.copyUtf16(unknown.toCharArray()),
                            b.copyRank(in.readInt(), local), b.copyRank(in.readInt(), local));
                    check(joined.flatten().equals("z" + unknown + "az"));
                }
                refuse(IndexOutOfBoundsException.class, () -> b.coordinate(-1));
                refuse(IndexOutOfBoundsException.class, () -> b.coordinate(2));
                var differentSpec = qualification(sender, ORDER, REV, "ordering-test-v2");
                refuse(IllegalArgumentException.class, () -> b.requireCompatible(differentSpec));
                var differentRevision = qualification(sender, ORDER, "a".repeat(40), "ordering-test-v1");
                refuse(IllegalArgumentException.class, () -> b.requireCompatible(differentRevision));
                byte[] modified = Files.readAllBytes(receiver.resolve("one.m3lex"));
                modified[88] ^= 1; // first payload unit; existing mapped record checksum must refuse
                Files.write(receiver.resolve("one.m3lex"), modified);
                refuse(IOException.class, () -> b.copyRank(1, local));
            }
            var missing = new SharedLexiconCatalog.Qualification(pin.specId(), pin.specVersion(),
                    pin.sourceRevision(), "foreign-source", pin.languageTag(), pin.lexiconId(),
                    pin.wordCount(), pin.rankMapSha256(), pin.projectionSha256());
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, missing));
            var oversized = new SharedLexiconCatalog.Qualification(pin.specId(), pin.specVersion(),
                    pin.sourceRevision(), pin.sourceId(), pin.languageTag(), pin.lexiconId(),
                    Integer.MAX_VALUE, pin.rankMapSha256(), pin.projectionSha256());
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, oversized));
            var foreignLanguage = new SharedLexiconCatalog.Qualification(pin.specId(), pin.specVersion(),
                    pin.sourceRevision(), pin.sourceId(), "xx", pin.lexiconId(),
                    pin.wordCount(), pin.rankMapSha256(), pin.projectionSha256());
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, foreignLanguage));
            var closed = SharedLexiconCatalog.openQualified(sender, pin);
            closed.close(); closed.close();
            refuse(IllegalStateException.class, () -> closed.coordinate(0));
            refuse(IllegalStateException.class, () -> closed.requireCompatible(pin));
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender,
                    qualification(sender, List.of("a", "z"), REV, "ordering-test-v1")));
            var badImage = new SharedLexiconCatalog.Qualification(pin.specId(), pin.specVersion(),
                    pin.sourceRevision(), pin.sourceId(), pin.languageTag(), pin.lexiconId(),
                    pin.wordCount(), pin.rankMapSha256(), "0".repeat(64));
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, badImage));
            String records = Files.readString(sender.resolve("synexia.records.tsv"));
            for (String malformed : List.of("-1", "2", "00", "0")) {
                Files.writeString(sender.resolve("synexia.records.tsv"),
                        records.replace("\ten\t1\ta", "\ten\t" + malformed + "\ta"));
                refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, pin));
            }
            Files.writeString(sender.resolve("synexia.records.tsv"), records);
            // Legacy open remains valid with arbitrary nonnumeric source identities.
            Files.writeString(sender.resolve("synexia.records.tsv"),
                    records.replace("\ten\t1\ta", "\ten\tarbitrary\ta"));
            var legacy = SharedLexiconCatalog.open(sender); legacy.close();
            refuse(IOException.class, () -> SharedLexiconCatalog.openQualified(sender, pin));
            refuse(IllegalArgumentException.class, () -> new SharedLexiconCatalog.Qualification(
                    pin.specId(), pin.specVersion(), pin.sourceRevision(), pin.sourceId(),
                    pin.languageTag(), pin.lexiconId(), -1, pin.rankMapSha256(), pin.projectionSha256()));
            System.out.println("LEXICON_QUALIFICATION_PASS checks=" + checks);
        } finally { delete(sender); delete(receiver); }
    }
}

