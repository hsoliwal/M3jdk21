// SPDX-FileCopyrightText: 2026 Synexia, Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package __PKG__;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Seeded differential driver of retained-port-20261107: the same source is compiled once in the donor package
 * (the pre-rename donor package, classes from the donor blobs) and once in the head package (jdk.m3.file, restored classes), fed the same
 * seeded corpus through the same observer stack with the same block splits, and every observable of every layer is
 * printed. differential.sh requires the four outputs (donor/head, JIT/-Xint) to be byte-identical.
 */
public final class ShadowDifferential {
    private ShadowDifferential() {
    }

    public static void main(String[] args) throws Exception {
        long seed = Long.parseLong(args[0]);
        int episodes = Integer.parseInt(args[1]);
        try (PrintStream out = new PrintStream(new FileOutputStream(args[2]), false, StandardCharsets.UTF_8)) {
            Random random = new Random(seed);
            for (int episode = 0; episode < episodes; episode++) {
                episode(out, random, episode);
            }
            out.println("END episodes=" + episodes);
        }
    }

    private static void episode(PrintStream out, Random random, int episode) {
        byte[] data = corpus(random, episode);
        M3FileDigestShadow sha = M3FileShadow.sha256();
        M3FileDigestShadow md5 = M3FileShadow.digest("MD5");
        M3FileCrcShadow crc = M3FileShadow.crc32c();
        M3FileLineShadow lines = M3FileShadow.lines();
        M3FileWcShadow wc = M3FileShadow.wc();
        M3FileTokenShadow tokens = M3FileCharShadow.tokens();
        M3FileTextShadow text = M3FileCharShadow.text();
        int maxParagraphs = 1 + random.nextInt(48);
        M3Grammar grammar = random.nextInt(3) == 0
                ? M3Grammar.paragraphs(maxParagraphs, 1 + random.nextInt(3000))
                : M3FileCharShadow.grammar(maxParagraphs);
        StructHash struct = new StructHash();
        M3FileCharShadow charLayers = tokens.andThen(text).andThen(struct).andThen(grammar);
        M3FileUtf8Shadow utf8 = M3FileShadow.utf8(charLayers);
        M3FileShadow stack = sha.andThen(md5).andThen(crc).andThen(lines).andThen(wc).andThen(utf8);
        out.println("E " + episode + " bytes=" + data.length + " maxParagraphs=" + maxParagraphs
                + " finishedBefore=" + sha.finished());
        String failure = "none";
        int offset = 0;
        int blocks = 0;
        try {
            while (offset < data.length) {
                int bound = random.nextInt(8) == 0 ? 1 : 1 + random.nextInt(97);
                int length = Math.min(data.length - offset, 1 + random.nextInt(bound));
                stack.update(data, offset, length);
                offset += length;
                blocks++;
            }
            if (random.nextInt(8) == 0) {
                stack.update(data, 0, 0);
            }
            stack.finish();
        } catch (RuntimeException failed) {
            failure = failed.getClass().getSimpleName() + ":" + failed.getMessage();
        }
        out.println("F blocks=" + blocks + " fed=" + offset + " failure=" + failure);
        out.println("D sha=" + v(sha::hex) + " words=" + v(() -> sha.word(0)) + "," + v(() -> sha.word(1)) + ","
                + v(() -> sha.word(2)) + "," + v(() -> sha.word(3)) + " len=" + v(() -> sha.bytes().length) + " md5="
                + v(md5::hex) + " finished=" + sha.finished() + " crc=" + crc.value());
        out.println("L length=" + lines.length() + " count=" + lines.lineCount() + " first="
                + (lines.lineCount() > 0 ? lines.lineStart(0) : -1) + " last="
                + (lines.lineCount() > 0 ? lines.lineStart(lines.lineCount() - 1) : -1));
        for (int probe = 0; probe < 3; probe++) {
            long at = data.length == 0 ? 0 : random.nextInt(data.length + 1);
            String lineOf;
            try {
                lineOf = Integer.toString(lines.lineOf(at));
            } catch (RuntimeException failed) {
                lineOf = failed.getClass().getSimpleName();
            }
            out.println("P at=" + at + " lineOf=" + lineOf);
        }
        out.println("W bytes=" + wc.bytes() + " lines=" + wc.lines() + " words=" + wc.words());
        out.println("C chars=" + utf8.charCount() + " tokens=" + v(tokens::tokens) + " tokenHash=" + v(tokens::hash)
                + " textLength=" + v(() -> text.text().length()) + " textHash=" + v(() -> text.text().hashCode())
                + " struct=" + struct.structHash() + " sameAsJdk="
                + v(() -> text.text().equals(new String(data, StandardCharsets.UTF_8))));
        if ("none".equals(failure)) {
            M3Paragraphs paragraphs = grammar.details();
            out.println("G size=" + paragraphs.size() + " units=" + paragraphs.observedUtf16Units() + " rowIdSum="
                    + paragraphs.rowIds().asLongStream().sum());
            int shown = Math.min(paragraphs.size(), 12);
            for (int ordinal = 0; ordinal < shown; ordinal++) {
                out.println("R " + ordinal + " row=" + paragraphs.physicalRow(ordinal) + " start=" + paragraphs.start(ordinal)
                        + " end=" + paragraphs.end(ordinal) + " length=" + paragraphs.length(ordinal) + " lines="
                        + paragraphs.firstLine(ordinal) + ".." + paragraphs.lastLine(ordinal) + " tokens="
                        + paragraphs.whitespaceTokens(ordinal) + " flags=" + paragraphs.flags(ordinal));
            }
        }
    }

    /** An observable, or the exception class it refuses with (a layer whose stream never finished). */
    private static String v(java.util.function.Supplier<Object> observable) {
        try {
            return String.valueOf(observable.get());
        } catch (RuntimeException refused) {
            return refused.getClass().getSimpleName();
        }
    }

    /** Char layer implementing the struct-hash interface: FNV-1a over the decoded chars. */
    private static final class StructHash implements M3FileStructHash {
        private long hash = 0xcbf29ce484222325L;
        private boolean finished;

        @Override
        public void update(char[] chars, int offset, int length) {
            for (int index = offset; index < offset + length; index++) {
                hash = (hash ^ chars[index]) * 0x100000001b3L;
            }
        }

        @Override
        public void finish() {
            finished = true;
        }

        @Override
        public long structHash() {
            return finished ? hash : ~hash;
        }
    }

    private static final String WORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$";
    private static final String SYMBOLS = "(){}[];,.=+-*/<>!&|%^~?:#@\"'\\";
    private static final String[] SEPARATORS = {" ", " ", "  ", "\t", "\n", "\n", "\r\n", "\r", "\n\n", "\n\n\n", "\t\t"};

    private static byte[] corpus(Random random, int episode) {
        switch (episode % 4) {
            case 0:
                return ascii(random, random.nextInt(4000));
            case 1:
                return unicode(random, random.nextInt(2500));
            case 2:
                return raw(random, random.nextInt(2000));
            default:
                byte[] a = ascii(random, random.nextInt(600));
                byte[] b = unicode(random, random.nextInt(600));
                byte[] c = raw(random, random.nextInt(300));
                byte[] d = unicode(random, random.nextInt(600));
                byte[] all = new byte[a.length + b.length + c.length + d.length];
                int at = 0;
                for (byte[] part : new byte[][] {a, b, c, d}) {
                    System.arraycopy(part, 0, all, at, part.length);
                    at += part.length;
                }
                return all;
        }
    }

    private static byte[] ascii(Random random, int target) {
        StringBuilder text = new StringBuilder(target + 16);
        while (text.length() < target) {
            int wordLength = 1 + random.nextInt(12);
            for (int index = 0; index < wordLength; index++) {
                text.append(WORD_CHARS.charAt(random.nextInt(WORD_CHARS.length())));
            }
            if (random.nextInt(3) == 0) {
                text.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
            }
            text.append(SEPARATORS[random.nextInt(SEPARATORS.length)]);
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static final int[] SPECIALS = {0x85, 0x2028, 0x2029, '\t', '\n', '\r', ' ', 0xA0, 0xFEFF, 0xFFFD};

    private static byte[] unicode(Random random, int scalars) {
        StringBuilder text = new StringBuilder(scalars * 2);
        for (int index = 0; index < scalars; index++) {
            int pick = random.nextInt(16);
            int codePoint;
            if (pick < 6) {
                codePoint = 0x20 + random.nextInt(0x5f);
            } else if (pick < 8) {
                codePoint = SPECIALS[random.nextInt(SPECIALS.length)];
            } else if (pick < 10) {
                codePoint = 0x80 + random.nextInt(0x780);
            } else if (pick < 13) {
                codePoint = 0x800 + random.nextInt(0xd800 - 0x800);
            } else if (pick < 15) {
                codePoint = 0xe000 + random.nextInt(0x2000);
            } else {
                codePoint = 0x10000 + random.nextInt(0x100000);
            }
            text.appendCodePoint(codePoint);
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] raw(Random random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        for (int index = 0; index < length; index++) {
            if (random.nextInt(5) == 0) {
                bytes[index] = (byte) (random.nextInt(3) == 0 ? '\n' : 0x20 + random.nextInt(0x5f));
            }
        }
        return bytes;
    }
}
