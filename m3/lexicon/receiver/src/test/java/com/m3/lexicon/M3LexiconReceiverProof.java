// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.zip.CRC32C;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * Differential proof of the M3Lexicons receiver (run by Maven {@code verify}). Oracles: Commons Lang
 * {@code splitByCharacterTypeCamelCase}, Commons CSV {@code CSVFormat.RFC4180}, {@code Character.getType},
 * {@code String.equalsIgnoreCase}, direct DFA runs and the M3StringPool M3LEX001/SYNARR01 admission rules.
 * Every composite case is evaluated on several random atom splits (split independence).
 */
public final class M3LexiconReceiverProof {
    private static final Random RANDOM = new Random(20261018L);
    private static final String ALPHABET = "aAbBzZ09 _-.,;:!?()éÉßΣσ—“”";
    private static final String[] SUPPLEMENTARY = {"😀", "𐄀", "𐐀", "𐐨"};
    private static int checks;

    private M3LexiconReceiverProof() {
    }

    public static void main(String[] args) throws Exception {
        formats();
        wordIds();
        lex001();
        sharedArena();
        foldIds();
        pascal();
        camel();
        csv();
        punct();
        wordIdSequence();
        regexRows();
        System.out.println("M3LexiconReceiverProof PASS checks=" + checks);
    }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    private static void refused(ThrowingRunnable action, String what) {
        try {
            action.run();
        } catch (M3LexiconFormatException expected) {
            checks++;
            return;
        } catch (Exception other) {
            throw new AssertionError(what + ": wrong refusal " + other, other);
        }
        throw new AssertionError(what + ": not refused");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void formats() throws Exception {
        for (M3LexiconFormat format : M3LexiconFormat.values()) {
            ByteBuffer header = ByteBuffer.allocate(Math.max(16, format.headerBytes()));
            header.order(ByteOrder.BIG_ENDIAN).putLong(0, format.magic());
            header.order(format.order()).putInt(8, versionFor(format));
            check(M3LexiconFormat.sniff(header).orElseThrow() == format, "sniff " + format);
            check(M3LexiconFormat.checkHeader(header) == format, "header " + format);
        }
        refused(() -> M3LexiconFormat.checkHeader(ByteBuffer.allocate(64)), "unknown magic");
        ByteBuffer bad = ByteBuffer.allocate(64).putLong(0, M3LexiconFormat.M3LEX001.magic()).putInt(8, 3);
        refused(() -> M3LexiconFormat.checkHeader(bad), "M3LEX001 version 3");
        ByteBuffer dict = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        dict.putLong(0, 0x31544349444e5953L).putInt(8, 1);
        check(M3LexiconFormat.checkHeader(dict) == M3LexiconFormat.SYNDICT1, "SYNDICT1 written by LexiconWriter");
    }

    private static int versionFor(M3LexiconFormat format) {
        return format == M3LexiconFormat.MIWROWS1 ? 3 : 1;
    }

    private static void wordIds() {
        long generation = 0x1234_5678_9abc_def0L;
        for (int cwid : new int[] {1, 2, 77, Integer.MAX_VALUE}) {
            long id = M3LexiconWordIds.canonicalId(generation, cwid);
            check(id < 0 && M3LexiconWordIds.wordId(id, generation) == cwid, "CWID round trip " + cwid);
            long expected = Long.MIN_VALUE | (generation & 0x3fff_ffff_0000_0000L) | Integer.toUnsignedLong(cwid);
            check(id == expected, "M3StringPool.Lexicon.atom id layout");
            check(M3LexiconWordIds.wordId(id, generation ^ (1L << 40)) == 0, "foreign generation refused");
        }
        check(M3LexiconWordIds.wordId(42L, generation) == 0, "non-lexicon atom has no CWID");
        int dict = M3LexiconWordIds.syndictId(3, 12345);
        check(M3LexiconWordIds.syndictLanguage(dict) == 3 && M3LexiconWordIds.syndictWord(dict) == 12345, "SYNDICT1 id");
        long indexId = (7L << 48) | (1L << 47) | 99L;
        check(M3LexiconWordIds.indexIdLanguage(indexId) == 7 && M3LexiconWordIds.indexIdOverlay(indexId)
                && M3LexiconWordIds.indexIdLocal(indexId) == 99L, "MIWROWS1 IndexId");
    }

    private static void lex001() throws Exception {
        List<String> words = randomLexicon(2000, 1, 9);
        byte[] image = M3Lex001Image.write(words);
        M3Lex001Image.Facts facts = M3Lex001Image.verify(ByteBuffer.wrap(image));
        long units = words.stream().mapToLong(String::length).sum();
        check(facts.version() == 2 && facts.count() == words.size() && facts.units() == units, "M3LEX001 facts");
        check(facts.generation64() == M3LexiconWordIds.generationOfDigest(facts.sha256()), "generation");
        byte[] flipped = image.clone();
        flipped[flipped.length - 1] ^= 1;
        refused(() -> M3Lex001Image.verify(ByteBuffer.wrap(flipped)), "payload flip");
        ByteBuffer swapped = ByteBuffer.wrap(image.clone());
        long first = swapped.getLong(64);
        swapped.putLong(64, swapped.getLong(76)).putLong(76, first);
        int hash = swapped.getInt(72);
        swapped.putInt(72, swapped.getInt(84)).putInt(84, hash);
        swapped.put(32, M3Lex001Image.digest(swapped));
        refused(() -> M3Lex001Image.verify(swapped), "unsorted rows");
        ByteBuffer badHash = ByteBuffer.wrap(image.clone());
        badHash.putInt(72, badHash.getInt(72) + 1);
        badHash.put(32, M3Lex001Image.digest(badHash));
        refused(() -> M3Lex001Image.verify(badHash), "Java hash mismatch");
        refused(() -> M3Lex001Image.verify(ByteBuffer.allocate(63)), "short image");
        String row = M3Lex001Image.shardRow(0, "synexia-000.m3lex", words, image);
        String[] cells = row.split("\t", -1);
        check(cells.length == 7 && cells[4].equals(Integer.toString(words.size()))
                && cells[5].equals(Long.toString(units)), "shard row columns");
        check(M3Lex001Image.escape("a\\b\tc\ud83d").equals("a\\\\b\\u0009c\\ud83d"), "sidecar escaping");
    }

    private static void sharedArena() throws Exception {
        ArenaFixture fixture = new ArenaFixture();
        int latin = fixture.scalar(1, 0, "plain");
        fixture.scalar(2, 0, "wordé");
        fixture.scalar(2, 5, "foreign");
        int pair = fixture.scalar(2, 0, "pair");
        fixture.tuple(2, pair, 1, 3);
        M3SharedArenaVerifier.Facts facts = M3SharedArenaVerifier.verify(fixture.image(16, false));
        check(facts.records() == 5 && facts.stringRows() == 2, "SYNARR01 records " + facts);
        check(facts.generation64() == (facts.namespaceHigh() ^ facts.namespaceLow()), "arena generation");
        check(latin == 64, "first record position");
        refused(() -> M3SharedArenaVerifier.verify(fixture.image(16, true)), "record CRC flip");
        refused(() -> M3SharedArenaVerifier.verify(fixture.image(3, false)), "record count bound");
        ArenaFixture broken = new ArenaFixture();
        int atom = broken.scalar(2, 0, "abc");
        broken.tuple(2, atom, 2, 5);
        refused(() -> M3SharedArenaVerifier.verify(broken.image(16, false)), "tuple out of bounds");
    }

    private static void foldIds() {
        List<String> words = randomLexicon(3000, 1, 4);
        int[] unicode = M3LexiconFoldIds.build(words, M3LexiconFoldIds.Kind.UNICODE_SIMPLE);
        int[] ascii = M3LexiconFoldIds.build(words, M3LexiconFoldIds.Kind.ASCII_LOWER);
        for (int trial = 0; trial < 40_000; trial++) {
            int a = RANDOM.nextInt(words.size()) + 1;
            int b = trial % 4 == 0 ? a : RANDOM.nextInt(words.size()) + 1;
            String left = words.get(a - 1);
            String right = words.get(b - 1);
            check(M3LexiconFoldIds.foldEquals(unicode, a, b) == left.equalsIgnoreCase(right), "fold " + left + "/" + right);
            check(M3LexiconFoldIds.foldEquals(ascii, a, b) == asciiLower(left).equals(asciiLower(right)), "ascii fold");
        }
        int folded = Arrays.binarySearch(words.toArray(new String[0]), "a");
        if (folded >= 0) {
            int upper = Arrays.binarySearch(words.toArray(new String[0]), "A");
            check(upper < 0 || unicode[upper] == folded + 1, "fold to existing row");
        }
    }

    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder();
        value.chars().forEach(c -> out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : (char) c));
        return out.toString();
    }

    private static void pascal() {
        String[][] fixed = {{"HTMLParser", "HTML|Parser"}, {"ASFRules", "ASF|Rules"}, {"fooBar", "foo|Bar"},
            {"number5", "number|5"}, {"aBC", "a|BC"}, {"ABc", "A|Bc"}, {"foo200Bar", "foo|200|Bar"}};
        for (String[] pair : fixed) {
            check(String.join("|", splitTokens(M3SeparatorKind.PASCAL, pair[0], 2)).equals(pair[1]), "PASCAL " + pair[0]);
        }
        for (int trial = 0; trial < 20_000; trial++) {
            String text = randomText(24);
            String[] expected = StringUtils.splitByCharacterTypeCamelCase(text);
            for (int split = 0; split < 3; split++) {
                check(Arrays.equals(splitTokens(M3SeparatorKind.PASCAL, text, 1 + split * 3), expected),
                        "PASCAL differential " + text);
            }
        }
    }

    private static void camel() {
        for (int trial = 0; trial < 20_000; trial++) {
            String text = randomText(24);
            long[] bits = M3SeparatorLexicon.bits(M3SeparatorKind.CAMEL, randomAtoms(text, 4));
            long[] pascal = M3SeparatorLexicon.bits(M3SeparatorKind.PASCAL, randomAtoms(text, 3));
            for (int j = 0; j < text.length(); j++) {
                int at = Character.getType(text.charAt(j));
                int before = j == 0 ? -1 : Character.getType(text.charAt(j - 1));
                boolean oracle = j > 0 && at == Character.UPPERCASE_LETTER
                        && (before == Character.LOWERCASE_LETTER || before == Character.DECIMAL_DIGIT_NUMBER);
                check(M3BitLanes.get(bits, j) == oracle, "CAMEL oracle " + text + "@" + j);
                check(!oracle || M3BitLanes.get(pascal, j), "CAMEL subset of PASCAL " + text);
            }
        }
    }

    private static void csv() throws IOException {
        for (int trial = 0; trial < 6_000; trial++) {
            String text = randomCsv();
            List<List<String>> expected = new ArrayList<>();
            try (CSVParser parser = CSVFormat.RFC4180.parse(new StringReader(text))) {
                for (CSVRecord record : parser) {
                    expected.add(record.toList());
                }
            }
            for (int split = 0; split < 3; split++) {
                List<CharSequence> atoms = randomAtoms(text, 2 + split * 4);
                M3CsvBoundarySummary root = M3SeparatorLexicon.csv(atoms);
                check(!root.refused(), "CSV accepted " + text);
                check(csvRecords(text, root).equals(expected), "CSV differential " + text.replace("\r", "\\r"));
            }
        }
        for (String invalid : new String[] {"\"a\"b,c\r\n", "x,\"abc"}) {
            boolean commonsRefused = false;
            try (CSVParser parser = CSVFormat.RFC4180.parse(new StringReader(invalid))) {
                parser.getRecords();
            } catch (IOException | RuntimeException expected) {
                commonsRefused = true;
            }
            check(commonsRefused && M3SeparatorLexicon.csv(randomAtoms(invalid, 3)).refused(), "CSV refusal " + invalid);
        }
    }

    private static List<List<String>> csvRecords(String text, M3CsvBoundarySummary root) {
        long[] separators = root.separators();
        long[] records = root.recordEnds();
        List<List<String>> out = new ArrayList<>();
        List<String> record = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!M3BitLanes.get(separators, i)) {
                continue;
            }
            boolean recordEnd = M3BitLanes.get(records, i);
            if (recordEnd || text.charAt(i) == ',') {
                record.add(unquote(text.substring(start, i)));
            }
            if (recordEnd) {
                out.add(record);
                record = new ArrayList<>();
            }
            start = i + 1;
        }
        if (start < text.length() || !record.isEmpty()) {
            record.add(unquote(text.substring(start)));
            out.add(record);
        }
        return out;
    }

    private static String unquote(String raw) {
        if (!raw.startsWith("\"")) {
            return raw;
        }
        return raw.substring(1, raw.lastIndexOf('"')).replace("\"\"", "\"");
    }

    private static void punct() {
        for (int trial = 0; trial < 20_000; trial++) {
            String text = randomText(20);
            long[] oracle = M3BitLanes.lanes(text.length());
            for (int i = 0; i < text.length(); i += Character.charCount(text.codePointAt(i))) {
                int cp = text.codePointAt(i);
                if (M3PunctBoundarySummary.member(M3SeparatorKind.PUNCTUATION_CLASSES, cp)) {
                    M3BitLanes.set(oracle, i);
                    if (Character.charCount(cp) == 2) {
                        M3BitLanes.set(oracle, i + 1);
                    }
                }
            }
            for (int split = 0; split < 3; split++) {
                long[] bits = M3SeparatorLexicon.bits(M3SeparatorKind.PUNCT, randomAtoms(text, 2 + split * 5));
                check(Arrays.equals(bits, oracle), "PUNCT differential " + text);
            }
        }
    }

    private static void wordIdSequence() {
        List<String> lexicon = randomLexicon(500, 1, 6).stream().filter(w -> w.chars().allMatch(Character::isLetter)).toList();
        M3WordIdSequence.Resolver resolver = M3WordIdSequence.sortedLexicon(lexicon);
        for (int trial = 0; trial < 3_000; trial++) {
            StringBuilder text = new StringBuilder();
            List<Integer> expected = new ArrayList<>();
            int tokens = 1 + RANDOM.nextInt(8);
            for (int t = 0; t < tokens; t++) {
                boolean known = RANDOM.nextInt(5) != 0;
                String word = known ? lexicon.get(RANDOM.nextInt(lexicon.size())) : "zzzq" + t;
                expected.add(known ? lexicon.indexOf(word) + 1 : 0);
                text.append(word).append(",.;-".charAt(RANDOM.nextInt(4)));
            }
            M3WordIdSequence sequence = M3WordIdSequence.of(M3SeparatorKind.PUNCT, randomAtoms(text.toString(), 4), resolver);
            check(Arrays.equals(sequence.wordIds(), expected.stream().mapToInt(Integer::intValue).toArray()),
                    "word-id sequence " + text);
        }
    }

    private static void regexRows() throws Exception {
        M3RegexWordTransferRows.Dfa dfa = new AbThenC();
        List<String> words = new ArrayList<>(new TreeSet<>(randomWords(400, "abcx", 1, 5)));
        M3RegexWordTransferRows rows = M3RegexWordTransferRows.build(dfa, words);
        M3RegexWordTransferRows read = M3RegexWordTransferRows.read(ByteBuffer.wrap(rows.image(7L, new byte[32])));
        for (int trial = 0; trial < 20_000; trial++) {
            int[] cwids = RANDOM.ints(1 + RANDOM.nextInt(10), 1, words.size() + 1).toArray();
            StringBuilder joined = new StringBuilder();
            Arrays.stream(cwids).forEach(id -> joined.append(words.get(id - 1)));
            int direct = 0;
            for (int i = 0; i < joined.length(); i++) {
                direct = dfa.next(direct, joined.charAt(i));
            }
            check(rows.run(cwids, 0) == direct && read.run(cwids, 0) == direct, "row composition " + joined);
            int cwid = cwids[0];
            int state = RANDOM.nextInt(dfa.stateCount());
            check(rows.acceptsInside(cwid, state) == acceptsInside(dfa, words.get(cwid - 1), state), "accept bits");
        }
        byte[] image = rows.image(7L, new byte[32]);
        image[image.length - 1] ^= 1;
        refused(() -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image)), "M3RXW001 digest");
    }

    private static boolean acceptsInside(M3RegexWordTransferRows.Dfa dfa, String word, int state) {
        int current = state;
        boolean accepted = false;
        for (int i = 0; i < word.length(); i++) {
            current = dfa.next(current, word.charAt(i));
            accepted |= dfa.accepting(current);
        }
        return accepted;
    }

    /** DFA for "ab" followed later by "c": 0 start, 1 saw a, 2 saw ab, 3 accept (absorbing). */
    private static final class AbThenC implements M3RegexWordTransferRows.Dfa {
        @Override
        public int stateCount() {
            return 4;
        }

        @Override
        public int next(int state, char unit) {
            if (state == 3) {
                return 3;
            }
            if (state == 2) {
                return unit == 'c' ? 3 : 2;
            }
            if (unit == 'a') {
                return 1;
            }
            return state == 1 && unit == 'b' ? 2 : 0;
        }

        @Override
        public boolean accepting(int state) {
            return state == 3;
        }
    }

    private static String[] splitTokens(M3SeparatorKind kind, String text, int pieces) {
        M3SeparatorLexicon.Spans spans = M3SeparatorLexicon.tokens(kind, randomAtoms(text, pieces));
        String[] out = new String[spans.count()];
        for (int k = 0; k < out.length; k++) {
            out[k] = text.substring(spans.starts()[k], spans.ends()[k]);
        }
        return out;
    }

    private static List<CharSequence> randomAtoms(String text, int pieces) {
        TreeSet<Integer> cuts = new TreeSet<>();
        for (int i = 0; i < pieces && text.length() > 1; i++) {
            cuts.add(1 + RANDOM.nextInt(text.length() - 1));
        }
        List<CharSequence> atoms = new ArrayList<>();
        int from = 0;
        for (int cut : cuts) {
            atoms.add(text.substring(from, cut));
            if (RANDOM.nextInt(8) == 0) {
                atoms.add("");
            }
            from = cut;
        }
        atoms.add(text.substring(from));
        return atoms;
    }

    private static String randomText(int maxLength) {
        StringBuilder out = new StringBuilder();
        int length = RANDOM.nextInt(maxLength + 1);
        for (int i = 0; i < length; i++) {
            if (RANDOM.nextInt(10) == 0) {
                out.append(SUPPLEMENTARY[RANDOM.nextInt(SUPPLEMENTARY.length)]);
            } else {
                out.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return out.toString();
    }

    private static List<String> randomLexicon(int count, int minLength, int maxLength) {
        TreeSet<String> words = new TreeSet<>();
        String letters = "aAbBcCéÉßΣσς";
        words.addAll(randomWords(count, letters, minLength, maxLength));
        for (int i = 0; i < count / 20; i++) {
            words.add(SUPPLEMENTARY[RANDOM.nextInt(SUPPLEMENTARY.length)] + "a");
        }
        words.add("a");
        words.add("A");
        return new ArrayList<>(words);
    }

    private static List<String> randomWords(int count, String letters, int minLength, int maxLength) {
        List<String> words = new ArrayList<>();
        for (int w = 0; w < count; w++) {
            StringBuilder word = new StringBuilder();
            int length = minLength + RANDOM.nextInt(maxLength - minLength + 1);
            for (int i = 0; i < length; i++) {
                word.append(letters.charAt(RANDOM.nextInt(letters.length())));
            }
            words.add(word.toString());
        }
        return words;
    }

    private static String randomCsv() {
        StringBuilder out = new StringBuilder();
        int records = 1 + RANDOM.nextInt(4);
        for (int r = 0; r < records; r++) {
            int fields = 2 + RANDOM.nextInt(3);
            for (int f = 0; f < fields; f++) {
                out.append(f == 0 ? "" : ",").append(randomField());
            }
            boolean last = r == records - 1;
            int ending = RANDOM.nextInt(3);
            if (!last || ending != 2) {
                out.append(ending == 0 ? "\r\n" : "\n");
            }
        }
        return out.toString();
    }

    private static String randomField() {
        StringBuilder field = new StringBuilder();
        int length = RANDOM.nextInt(5);
        if (RANDOM.nextBoolean()) {
            for (int i = 0; i < length; i++) {
                field.append("ab9 x".charAt(RANDOM.nextInt(5)));
            }
            return field.toString();
        }
        field.append('"');
        for (int i = 0; i < length; i++) {
            String[] parts = {"a", ",", "\r\n", "\n", "\"\"", " ", "b"};
            field.append(parts[RANDOM.nextInt(parts.length)]);
        }
        field.append('"');
        if (RANDOM.nextInt(4) == 0) {
            field.append("  ");
        }
        return field.toString();
    }

    /** Builds SYNARR01 snapshots exactly as SharedArrayPool lays them out. */
    private static final class ArenaFixture {
        private final List<byte[]> records = new ArrayList<>();
        private int position = 64;

        int scalar(int width, int language, String text) {
            ByteBuffer payload = ByteBuffer.allocate(width * text.length()).order(ByteOrder.BIG_ENDIAN);
            text.chars().forEach(unit -> {
                if (width == 2) {
                    payload.putChar((char) unit);
                } else {
                    payload.put((byte) unit);
                }
            });
            return add(width, language, text.length(), -1, text.hashCode(), payload.array());
        }

        int tuple(int width, int atomPosition, int start, int size) {
            ByteBuffer payload = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
            payload.putLong(atomPosition).putInt(start).putInt(size);
            return add(width, 0, size, 1, 0, payload.array());
        }

        private int add(int width, int language, int units, int parts, int hash, byte[] payload) {
            int total = 80 + payload.length + 8;
            ByteBuffer record = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
            record.putInt(0, 0x41525231).putInt(4, total).putInt(8, width).putInt(12, language)
                    .putInt(16, units).putInt(20, parts).putInt(24, hash).putInt(28, payload.length);
            record.put(80, payload);
            CRC32C crc = new CRC32C();
            crc.update(record.array(), 0, 64);
            crc.update(payload, 0, payload.length);
            record.putLong(64, crc.getValue()).putLong(total - 8, 0x434f4d4d49543031L ^ position ^ total);
            records.add(record.array());
            int at = position;
            position += total;
            return at;
        }

        ByteBuffer image(int maxRecords, boolean corrupt) {
            ByteBuffer out = ByteBuffer.allocate(position).order(ByteOrder.BIG_ENDIAN);
            out.putLong(0, 0x53594e4152523031L).putInt(8, 1).putLong(16, 0x0123_4567_89ab_cdefL)
                    .putLong(24, 0x0fed_cba9_8765_4321L).putLong(32, 1L << 20).putInt(40, maxRecords);
            CRC32C crc = new CRC32C();
            crc.update(out.array(), 0, 56);
            out.putLong(56, crc.getValue());
            int at = 64;
            for (byte[] record : records) {
                out.put(at, record);
                at += record.length;
            }
            if (corrupt) {
                out.put(64 + 80, (byte) (out.get(64 + 80) ^ 1));
            }
            return out;
        }
    }
}
