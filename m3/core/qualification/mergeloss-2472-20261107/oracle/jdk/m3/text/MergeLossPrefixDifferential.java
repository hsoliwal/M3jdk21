/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;
import java.util.TreeSet;

/**
 * Merge-loss #2472 differential for SharedLexiconImage.startsWith against String.startsWith.
 *
 * <p>Seeded images: create() V1 images and hand-written V1/V2 images whose record ranges overlap and are not
 * monotonic. Words mix ASCII, NUL, units around 0x7fff/0x8000, 0xffff and lone or paired surrogates, with lengths 0 to
 * 20 so the scalar tail after the four-unit lanes takes every length. Prefixes per record: empty, every proper prefix
 * (all odd and even lengths), the full word, the word plus one unit, every prefix with one unit raised or lowered at
 * each position (a mismatch in each lane and in the tail), prefixes of other records and random strings; plus index
 * and null refusals. REFERENCE counts results that disagree with String.startsWith on the source word (0 on both
 * sides). TRACE is a sha256 over every result and refusal; it must be identical for the master and restored class.
 * TIMING (the restored behaviour: one mapped long per four units) is reported, not compared.</p>
 *
 * usage: java -cp core-classes;oracle jdk.m3.text.MergeLossPrefixDifferential images seed [timing]
 */
public final class MergeLossPrefixDifferential {
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final int HEADER = 64;
    private static final char[] UNITS = {'a', 'b', 'c', 0, 0x7f, 0x7fff, 0x8000, 0xd800, 0xdbff, 0xdc00, 0xdfff,
        0xfffe, 0xffff, 0x00ff, 0x0100, 0x4e00};

    private final MessageDigest trace;
    private final SplittableRandom random;
    private long calls, mismatches, refusals, images, records;

    private MergeLossPrefixDifferential(SplittableRandom random) throws NoSuchAlgorithmException {
        this.random = random;
        this.trace = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Exception {
        MergeLossPrefixDifferential run =
                new MergeLossPrefixDifferential(new SplittableRandom(Long.decode(args[1])));
        Path dir = Files.createTempDirectory("mergeloss-2472-");
        for (int i = 0; i < Integer.parseInt(args[0]); i++) run.round(dir, i);
        System.out.printf("REFERENCE mismatches=%d%n", run.mismatches);
        System.out.printf("TRACE images=%d records=%d calls=%d refusals=%d sha256=%s%n", run.images, run.records,
                run.calls, run.refusals, HexFormat.of().formatHex(run.trace.digest()));
        if (args.length > 2 && args[2].equals("timing")) timing(dir);
    }

    private void round(Path dir, int index) throws IOException {
        List<String> words = words(1 + random.nextInt(index % 7 == 0 ? 200 : 30));
        Path created = dir.resolve("c" + index + ".m3lex");
        SharedLexiconImage.create(created, words);
        check(SharedLexiconImage.open(created), words);
        Path overlapped = dir.resolve("o" + index + ".m3lex");
        List<String> shared = overlapping(overlapped, 1 + random.nextInt(2));
        check(SharedLexiconImage.open(overlapped), shared);
    }

    private void check(SharedLexiconImage image, List<String> words) {
        images++;
        records += words.size();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            probe(image, i, word, "");
            probe(image, i, word, word);
            probe(image, i, word, word + UNITS[random.nextInt(UNITS.length)]);
            for (int k = 1; k <= word.length(); k++) {
                String prefix = word.substring(0, k);
                probe(image, i, word, prefix);
                for (int at = 0; at < k; at++) {
                    probe(image, i, word, replace(prefix, at, (char) (prefix.charAt(at) + 1)));
                    probe(image, i, word, replace(prefix, at, (char) (prefix.charAt(at) - 1)));
                }
            }
            String other = words.get(random.nextInt(words.size()));
            probe(image, i, word, other.substring(0, random.nextInt(other.length() + 1)));
            probe(image, i, word, word(random.nextInt(13)));
        }
        refuse(image, -1, "a");
        refuse(image, words.size(), "a");
        refuse(image, 0, null);
        refuse(image, -1, null);
    }

    private void probe(SharedLexiconImage image, int index, String word, String prefix) {
        calls++;
        boolean actual = image.startsWith(index, prefix);
        if (actual != word.startsWith(prefix)) mismatches++;
        trace.update((byte) (actual ? 1 : 0));
    }

    private void refuse(SharedLexiconImage image, int index, String prefix) {
        calls++;
        try {
            feed("accepted:" + image.startsWith(index, prefix));
        } catch (RuntimeException refused) {
            refusals++;
            feed(refused.getClass().getName() + ":" + refused.getMessage());
        }
    }

    private void feed(String value) {
        trace.update(value.getBytes(StandardCharsets.UTF_8));
        trace.update((byte) '\n');
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
            String word = head + word(random.nextInt(10));
            set.add(word.substring(0, Math.min(word.length(), 20)));
        }
        return new ArrayList<>(set);
    }

    /** Records are distinct sorted substrings of one payload, so their ranges overlap and are not monotonic. */
    private List<String> overlapping(Path path, int version) throws IOException {
        String payload = word(8) + String.join("", words(6)) + word(8);
        TreeSet<String> values = new TreeSet<>();
        List<int[]> chosen = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int from = random.nextInt(payload.length() + 1);
            int to = from + random.nextInt(Math.min(20, payload.length() - from) + 1);
            if (values.add(payload.substring(from, to))) chosen.add(new int[] {from, to - from});
        }
        chosen.sort((x, y) -> payload.substring(x[0], x[0] + x[1]).compareTo(payload.substring(y[0], y[0] + y[1])));
        write(path, version, payload, chosen);
        return new ArrayList<>(values);
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
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        Files.write(path, bytes.array());
    }

    /** 65,536 records of 32 units (24-unit shared prefix): median ns per startsWith for prefix lengths 4..32. */
    private static void timing(Path dir) throws IOException {
        List<String> words = new ArrayList<>();
        String stem = "m3-lexicon-shared-prefix";
        for (int i = 0; i < SharedLexiconImage.MAX_RECORDS; i++) words.add(stem + String.format("%08x", i * 7919L));
        Collections.sort(words);
        Path path = dir.resolve("timing.m3lex");
        SharedLexiconImage.create(path, words);
        SharedLexiconImage image = SharedLexiconImage.open(path);
        int[] lengths = {4, 9, 17, 24, 32};
        String[][] prefixes = new String[lengths.length][words.size()];
        for (int l = 0; l < lengths.length; l++)
            for (int i = 0; i < words.size(); i++) prefixes[l][i] = words.get(i).substring(0, lengths[l]);
        long[] samples = new long[9];
        long sink = 0;
        StringBuilder out = new StringBuilder("TIMING startsWith_ns_median");
        for (int l = 0; l < lengths.length; l++) {
            for (int round = 0; round < 12; round++) {
                long start = System.nanoTime();
                for (int i = 0; i < words.size(); i++) if (image.startsWith(i, prefixes[l][i])) sink++;
                long elapsed = System.nanoTime() - start;
                if (round >= 3) samples[round - 3] = elapsed * 1000 / words.size();
            }
            Arrays.sort(samples);
            out.append(" len").append(lengths[l]).append('=').append(samples[4] / 1000.0);
        }
        System.out.println(out.append(" sink=").append(sink));
    }
}
