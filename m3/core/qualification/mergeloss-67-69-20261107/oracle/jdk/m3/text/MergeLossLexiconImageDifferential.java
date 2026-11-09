/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;
import java.util.TreeSet;

/**
 * Merge-loss leaves 67 and 69 differential for SharedLexiconImage (compareRecordTo, compareRecords).
 *
 * <p>Seeded images: create() V1 images and hand-written V1/V2 images whose record ranges overlap and are not
 * monotonic, plus unsorted and duplicate images that open() must refuse (the admission order check runs
 * compareRecords). Words mix ASCII, units around 0x7fff/0x8000, 0xffff, NUL and unpaired surrogates, with shared
 * prefixes across every lane boundary. Queries: every record, every record with one unit raised and lowered at each
 * position, prefixes and extensions, and random strings, through findRecord, lowerBound and startsWith.
 * REFERENCE counts results that disagree with String ordering over the sorted word list (0 on both sides). TRACE is a
 * sha256 over every result and refusal; it must be identical for the master and the restored class. TIMING (the
 * restored behaviour: fewer mapped reads per comparison) is reported, not compared.</p>
 *
 * usage: java -cp core-classes;oracle jdk.m3.text.MergeLossLexiconImageDifferential images seed [timing]
 */
public final class MergeLossLexiconImageDifferential {
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final int HEADER = 64;
    private static final char[] UNITS = {'a', 'b', 'c', 'd', 0, 0x7f, 0x7ffe, 0x7fff, 0x8000, 0x8001, 0xd800, 0xdbff,
        0xdc00, 0xdfff, 0xfffe, 0xffff, 0x00ff, 0x0100, 0x4e00};

    private final MessageDigest trace;
    private final SplittableRandom random;
    private long queries, mismatches, refusals, images, records;

    private MergeLossLexiconImageDifferential(SplittableRandom random) throws Exception {
        this.random = random;
        this.trace = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]);
        MergeLossLexiconImageDifferential run =
                new MergeLossLexiconImageDifferential(new SplittableRandom(Long.decode(args[1])));
        Path dir = Files.createTempDirectory("mergeloss-67-69-");
        for (int i = 0; i < count; i++) run.round(dir, i);
        System.out.printf("REFERENCE mismatches=%d%n", run.mismatches);
        System.out.printf("TRACE images=%d records=%d queries=%d refusals=%d sha256=%s%n", run.images, run.records,
                run.queries, run.refusals, HexFormat.of().formatHex(run.trace.digest()));
        if (args.length > 2 && args[2].equals("timing")) timing(dir);
    }

    private void round(Path dir, int index) throws Exception {
        List<String> words = words(1 + random.nextInt(index % 7 == 0 ? 300 : 40));
        Path created = dir.resolve("c" + index + ".m3lex");
        SharedLexiconImage.create(created, words);
        check(SharedLexiconImage.open(created), words);
        int version = 1 + random.nextInt(2);
        Path overlapped = dir.resolve("o" + index + ".m3lex");
        List<String> shared = overlapping(overlapped, version);
        check(SharedLexiconImage.open(overlapped), shared);
        if (words.size() > 1) {
            List<String> bad = new ArrayList<>(words);
            if (random.nextBoolean()) Collections.swap(bad, random.nextInt(bad.size() - 1), bad.size() - 1);
            else bad.set(bad.size() - 1, bad.get(bad.size() - 2));
            Path refused = dir.resolve("u" + index + ".m3lex");
            write(refused, version, String.join("", bad), ranges(bad));
            try {
                SharedLexiconImage.open(refused);
                feed("open-accepted");
            } catch (IOException expected) {
                refusals++;
                feed("refused:" + expected.getMessage());
            }
        }
    }

    private void check(SharedLexiconImage image, List<String> words) {
        images++;
        records += words.size();
        feed(image.size());
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            query(image, words, word);
            for (int at = 0; at < word.length(); at++) {
                query(image, words, replace(word, at, (char) (word.charAt(at) + 1)));
                query(image, words, replace(word, at, (char) (word.charAt(at) - 1)));
                feed(image.startsWith(i, word.substring(0, at)) ? 1 : 0);
            }
            query(image, words, word.substring(0, random.nextInt(word.length() + 1)));
            query(image, words, word + UNITS[random.nextInt(UNITS.length)]);
            String other = words.get(random.nextInt(words.size()));
            feed(image.startsWith(i, other) == word.startsWith(other) ? 1 : -1);
            if (!image.recordText(i).equals(word)) mismatches++;
        }
        for (int q = 0; q < 16; q++) query(image, words, word(random.nextInt(20)));
    }

    private void query(SharedLexiconImage image, List<String> words, String value) {
        queries++;
        int found = image.findRecord(value), lower = image.lowerBound(value);
        int expectedFound = Collections.binarySearch(words, value);
        int expectedLower = expectedFound >= 0 ? expectedFound : -expectedFound - 1;
        if (found != Math.max(expectedFound, -1) || lower != expectedLower) mismatches++;
        feed(found);
        feed(lower);
    }

    private static String replace(String word, int at, char unit) {
        char[] units = word.toCharArray();
        units[at] = unit;
        return new String(units);
    }

    private String word(int length) {
        char[] units = new char[length];
        for (int i = 0; i < length; i++) units[i] = UNITS[random.nextInt(UNITS.length)];
        return new String(units);
    }

    private List<String> words(int count) {
        TreeSet<String> set = new TreeSet<>();
        String stem = word(random.nextInt(12));
        for (int i = 0; i < count; i++) {
            String head = stem.substring(0, random.nextInt(stem.length() + 1));
            set.add(head + word(random.nextInt(10)));
        }
        return new ArrayList<>(set);
    }

    /** Records are distinct sorted substrings of one payload, so their ranges overlap and are not monotonic. */
    private List<String> overlapping(Path path, int version) throws IOException {
        String payload = word(8) + words(6).stream().reduce("", String::concat) + word(8);
        TreeSet<String> values = new TreeSet<>();
        List<int[]> chosen = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int from = random.nextInt(payload.length() + 1);
            int to = from + random.nextInt(Math.min(18, payload.length() - from) + 1);
            if (values.add(payload.substring(from, to))) chosen.add(new int[] {from, to - from});
        }
        chosen.sort((x, y) -> payload.substring(x[0], x[0] + x[1]).compareTo(payload.substring(y[0], y[0] + y[1])));
        write(path, version, payload, chosen);
        return new ArrayList<>(values);
    }

    private static List<int[]> ranges(List<String> words) {
        List<int[]> result = new ArrayList<>();
        int offset = 0;
        for (String word : words) {
            result.add(new int[] {offset, word.length()});
            offset += word.length();
        }
        return result;
    }

    /** Independent writer of the documented M3LEX001 layout (V1: offset/length; V2: plus the Java hash). */
    private static void write(Path path, int version, String payload, List<int[]> ranges) throws IOException {
        int directory = version == 1 ? 8 : 12;
        long payloadOffset = HEADER + (long) directory * ranges.size();
        ByteBuffer bytes = ByteBuffer.allocate((int) (payloadOffset + 2L * payload.length()))
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, MAGIC).putInt(8, version).putInt(12, ranges.size()).putLong(16, payloadOffset)
                .putLong(24, payload.length());
        for (int i = 0; i < ranges.size(); i++) {
            int[] range = ranges.get(i);
            bytes.putInt(HEADER + i * directory, range[0]).putInt(HEADER + i * directory + 4, range[1]);
            if (version == 2) bytes.putInt(HEADER + i * directory + 8,
                    payload.substring(range[0], range[0] + range[1]).hashCode());
        }
        for (int i = 0; i < payload.length(); i++) {
            char unit = payload.charAt(i);
            bytes.put((int) payloadOffset + 2 * i, (byte) unit).put((int) payloadOffset + 2 * i + 1, (byte) (unit >>> 8));
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes.array(), 0, 32);
            digest.update(bytes.array(), HEADER, bytes.capacity() - HEADER);
            bytes.put(32, digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        Files.write(path, bytes.array());
    }

    private void feed(int value) {
        trace.update((byte) (value >>> 24));
        trace.update((byte) (value >>> 16));
        trace.update((byte) (value >>> 8));
        trace.update((byte) value);
    }

    private void feed(String value) {
        trace.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 65,536 records with 24-unit shared prefixes: medians of lookup and admission (open) time. */
    private static void timing(Path dir) throws IOException {
        List<String> words = new ArrayList<>();
        String prefix = "m3-lexicon-shared-prefix";
        for (int i = 0; i < SharedLexiconImage.MAX_RECORDS; i++) words.add(prefix + String.format("%08x", i * 7919L));
        Collections.sort(words);
        Path path = dir.resolve("timing.m3lex");
        SharedLexiconImage.create(path, words);
        long[] lookups = new long[9], opens = new long[9];
        long sink = 0;
        for (int round = 0; round < 12; round++) {
            long start = System.nanoTime();
            SharedLexiconImage image = SharedLexiconImage.open(path);
            long opened = System.nanoTime();
            for (String word : words) sink += image.findRecord(word);
            long done = System.nanoTime();
            if (round >= 3) {
                opens[round - 3] = opened - start;
                lookups[round - 3] = (done - opened) / words.size();
            }
        }
        Arrays.sort(lookups);
        Arrays.sort(opens);
        System.out.printf("TIMING lookup_ns_median=%d open_us_median=%d records=%d sink=%d%n", lookups[4],
                opens[4] / 1000, words.size(), sink);
    }
}
