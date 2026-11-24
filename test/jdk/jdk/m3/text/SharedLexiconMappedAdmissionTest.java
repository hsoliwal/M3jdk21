// Copyright 2026 Hitesh Soliwal and Contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Exact UTF-16 and hash parity for direct mapped lexicon row admission
 * @modules java.base/jdk.m3.text
 * @run main/othervm SharedLexiconMappedAdmissionTest
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import jdk.m3.text.SharedLexiconImage;

public final class SharedLexiconMappedAdmissionTest {
    private static long checks;
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final int HEADER = 64;

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3lex-mapped-validation-");
        try {
            TreeSet<String> items = new TreeSet<>();
            items.add("");
            items.add("\u0000");
            items.add("\ud800");
            items.add("\udc00");
            items.add("\ud800z\udc00");
            items.add("\ud83d\ude03");
            items.add("alpha");
            items.add("alpha\u0000beta");
            items.add("alpha\ud83d\ude03z");
            items.add("\uffff");
            Random random = new Random(0x517a2dL);
            char[] alphabet = {'\u0000', 'a', 'b', '\ud800', '\udc00', '\uffff'};
            for (int i = 0; i < 384; i++) {
                int length = 1 + random.nextInt(48);
                StringBuilder text = new StringBuilder(length);
                for (int j = 0; j < length; j++) text.append(alphabet[random.nextInt(alphabet.length)]);
                items.add(text.toString());
            }
            List<String> words = new ArrayList<>(items);
            Path v1 = dir.resolve("words-v1.m3lex");
            SharedLexiconImage.create(v1, words);
            exercise(v1, words, 1);
            Path v2 = dir.resolve("words-v2.m3lex");
            byte[] v2Image = encodeV2(words);
            Files.write(v2, v2Image);
            exercise(v2, words, 2);

            // Full payload checksum must still reject a modified published image.
            byte[] tampered = v2Image.clone();
            tampered[tampered.length - 1] ^= 0x01;
            Path invalid = dir.resolve("tampered.m3lex");
            Files.write(invalid, tampered);
            boolean rejected = false;
            try {
                SharedLexiconImage.open(invalid);
            } catch (IOException expected) {
                rejected = true;
            }
            check(rejected, "modified image must fail checksum");
            System.out.println("M3LEX_MAPPED_ADMISSION_PASS|checks=" + checks);
        } finally {
            try (var paths = Files.list(dir)) {
                for (Path path : paths.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(dir);
        }
    }

    private static void exercise(Path file, List<String> words, int version) throws Exception {
        SharedLexiconImage image = SharedLexiconImage.open(file);
        check(image.version() == version, "image version");
        check(image.size() == words.size(), "record count");
        for (int i = 0; i < words.size(); i++) {
            String text = words.get(i);
            check(image.recordEquals(i, text), "self equality " + i);
            check(image.recordUtf16Length(i) == text.length(), "length " + i);
            check(image.recordJavaHash(i) == text.hashCode(), "hash " + i);
            for (int candidate = 0; candidate < words.size(); candidate += 11) {
                String other = words.get(candidate);
                check(image.recordEquals(i, other) == text.equals(other), "differential equals");
            }
            String prefix = text.isEmpty() ? "a" : text.substring(0, text.length() - 1);
            check(image.recordEquals(i, prefix) == text.equals(prefix), "prefix mismatch");
            String suffix = text + "\ud800";
            check(image.recordEquals(i, suffix) == text.equals(suffix), "suffix mismatch");
        }
        boolean outOfBounds = false;
        try { image.recordEquals(words.size(), "x"); }
        catch (IndexOutOfBoundsException expected) { outOfBounds = true; }
        check(outOfBounds, "record index must be checked");
        boolean nullRejected = false;
        try { image.recordEquals(0, null); }
        catch (NullPointerException expected) { nullRejected = true; }
        check(nullRejected, "null candidate must be rejected");
    }

    private static byte[] encodeV2(List<String> words) throws Exception {
        int units = 0;
        for (String text : words) units = Math.addExact(units, text.length());
        int payload = HEADER + 12 * words.size();
        ByteBuffer bytes = ByteBuffer.allocate(Math.addExact(payload, Math.multiplyExact(2, units)))
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, MAGIC);
        bytes.putInt(8, 2);
        bytes.putInt(12, words.size());
        bytes.putLong(16, payload);
        bytes.putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String text = words.get(row);
            int dir = HEADER + row * 12;
            bytes.putInt(dir, offset);
            bytes.putInt(dir + 4, text.length());
            bytes.putInt(dir + 8, text.hashCode());
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                int at = payload + (offset + i) * 2;
                bytes.put(at, (byte)ch);
                bytes.put(at + 1, (byte)(ch >>> 8));
            }
            offset += text.length();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.asReadOnlyBuffer().slice(0, 32));
        digest.update(bytes.asReadOnlyBuffer().slice(HEADER, bytes.limit() - HEADER));
        bytes.put(32, digest.digest());
        return bytes.array();
    }
}
