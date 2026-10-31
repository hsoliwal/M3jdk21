/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Full-content M3StringPool direct-address admission and resource fallback
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3StringLexiconHashIndexTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:-CompactStrings M3StringLexiconHashIndexTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint M3StringLexiconHashIndexTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class M3StringLexiconHashIndexTest {
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final Class<?> LEXICON;
    private static final Method OPEN, FIND_CHAR, FIND_UNIT, FIND_LATIN, FIND_COMPACT, FIND_POINTS;
    private static final Field SLOTS;
    private static long checks;

    static {
        try {
            LEXICON = Class.forName("java.lang.M3StringPool$Lexicon");
            OPEN = method("open", Path.class);
            FIND_CHAR = method("find", char[].class, int.class, int.class);
            FIND_UNIT = method("findUnit", char.class);
            FIND_LATIN = method("findLatin1Bytes", byte[].class, int.class, int.class);
            FIND_COMPACT = method("findCompactBytes",
                    byte[].class, int.class, int.class, byte.class);
            FIND_POINTS = method("findCodePoints",
                    int[].class, int.class, int.class, int.class);
            SLOTS = LEXICON.getDeclaredField("exactRows");
            SLOTS.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Method method(String name, Class<?>... args)
            throws ReflectiveOperationException {
        Method method = LEXICON.getDeclaredMethod(name, args);
        method.setAccessible(true);
        return method;
    }

    private static int call(Method method, Object owner, Object... args) throws Exception {
        return (Integer) method.invoke(owner, args);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static List<String> spellings() {
        TreeSet<String> result = new TreeSet<>(List.of(
                "", " ", "A", "Aa", "BB", "AaAa", "BBBB", "AaBB", "BBAa",
                "z", "the", "bank", "bAnk", "\u0000", "\u00e9", "e\u0301",
                "\u4f60\u597d", "\ud800", "\udc00", "\ud83d\ude80"));
        char[] alphabet = {'a', 'B', '0', 0, '\u00e9', '\u4f60',
                '\ud800', '\udc00', '\ud83d', '\ude80', '\u200d'};
        Random random = new Random(20261018L);
        for (int i = 0; i < 4096; i++) {
            int n = random.nextInt(22);
            char[] units = new char[n];
            for (int k = 0; k < n; k++) {
                units[k] = alphabet[random.nextInt(alphabet.length)];
            }
            result.add(new String(units));
        }
        return new ArrayList<>(result);
    }

    private static Path emit(List<String> strings) throws Exception {
        int units = 0;
        for (String string : strings) units = Math.addExact(units, string.length());
        int payload = Math.addExact(64, Math.multiplyExact(strings.size(), 12));
        ByteBuffer image = ByteBuffer.allocate(Math.addExact(payload, 2 * units))
                .order(ByteOrder.BIG_ENDIAN);
        image.putLong(0, MAGIC);
        image.putInt(8, 2);
        image.putInt(12, strings.size());
        image.putLong(16, payload);
        image.putLong(24, units);
        int offset = 0;
        for (int row = 0; row < strings.size(); row++) {
            String spelling = strings.get(row);
            int at = 64 + 12 * row;
            image.putInt(at, offset);
            image.putInt(at + 4, spelling.length());
            image.putInt(at + 8, spelling.hashCode());
            for (int i = 0; i < spelling.length(); i++) {
                char unit = spelling.charAt(i);
                int pos = payload + ((offset + i) << 1);
                image.put(pos, (byte) unit);
                image.put(pos + 1, (byte) (unit >>> 8));
            }
            offset += spelling.length();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(image.array(), 0, 32);
        digest.update(image.array(), 64, image.capacity() - 64);
        byte[] sum = digest.digest();
        for (int i = 0; i < sum.length; i++) image.put(32 + i, sum[i]);
        Path file = Files.createTempFile("m3-string-lexicon-hash-", ".m3lex");
        Files.write(file, image.array());
        return file;
    }

    private static void oracle(Object lexicon, List<String> words, boolean indexed) throws Exception {
        check(((int[]) SLOTS.get(lexicon)) != null, "bounded primitive table prepared");
        int[] rows = (int[]) SLOTS.get(lexicon);
        check(indexed ? rows.length > 0 : rows.length == 0,
                indexed ? "prepared exact metadata" : "optional exact metadata unavailable");
        if (indexed) {
            for (int encoded : rows) {
                check(encoded >= 0, "production direct rows have no ambiguity marker");
            }
        }
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            check(call(FIND_CHAR, lexicon, word.toCharArray(), 0, word.length()) == row,
                    "char[] mapped identity row=" + row);
            int[] points = word.codePoints().toArray();
            check(call(FIND_POINTS, lexicon, points, 0, points.length, word.length()) == row,
                    "codepoint mapped identity row=" + row);
            boolean latin1 = word.chars().allMatch(v -> v <= 0xff);
            if (latin1) {
                byte[] bytes = new byte[word.length()];
                for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) word.charAt(i);
                check(call(FIND_LATIN, lexicon, bytes, 0, bytes.length) == row,
                        "Latin1 mapped identity row=" + row);
                check(call(FIND_COMPACT, lexicon, bytes, 0, bytes.length, (byte) 0) == row,
                        "compact Latin1 mapped identity row=" + row);
            }
            if (word.length() == 1) {
                check(call(FIND_UNIT, lexicon, word.charAt(0)) == row, "single unit");
            }
        }
        check("Aa".hashCode() == "BB".hashCode(), "collision fixture authority");
        for (int miss = 0; miss < 2048; miss++) {
            String missing = "M3_NOT_PRESENT_" + miss;
            check(call(FIND_CHAR, lexicon, missing.toCharArray(), 0, missing.length()) == -1,
                    "absent char[]");
            int[] points = missing.codePoints().toArray();
            check(call(FIND_POINTS, lexicon, points, 0, points.length, missing.length()) == -1,
                    "absent codepoints");
        }
    }

    private static Object withoutExactIndex(Path image) throws Exception {
        // Use a fresh parsed owner before any lookup. The actual EMPTY metadata is the
        // same optional state that a refused index build returns; spelling, facts and
        // generation stay bound to their validated mapped image.
        Object lexicon = OPEN.invoke(null, image);
        Class<?> index = Class.forName("java.lang.M3FrozenLexiconIndex");
        Method emptyMethod = index.getDeclaredMethod("empty");
        Method rowsMethod = index.getDeclaredMethod("rows");
        emptyMethod.setAccessible(true);
        rowsMethod.setAccessible(true);
        Object empty = emptyMethod.invoke(null);
        int[] emptyRows = (int[]) rowsMethod.invoke(empty);
        Field exactIndex = LEXICON.getDeclaredField("exactIndex");
        exactIndex.setAccessible(true);
        exactIndex.set(lexicon, empty);
        SLOTS.set(lexicon, emptyRows);
        check(exactIndex.get(lexicon) == empty, "fallback retains actual EMPTY index singleton");
        check(SLOTS.get(lexicon) == emptyRows && emptyRows.length == 0,
                "fallback row view aliases actual EMPTY rows");
        return lexicon;
    }

    public static void main(String[] args) throws Exception {
        List<String> words = spellings();
        Path image = emit(words);
        try {
            Object lexicon = OPEN.invoke(null, image);
            oracle(lexicon, words, true);
            oracle(withoutExactIndex(image), words, false);
            Path shortImage = emit(words.subList(0, 3));
            try {
                Object small = withoutExactIndex(shortImage);
                check(((int[]) SLOTS.get(small)).length == 0,
                        "small lexicon retains binary fallback without optional metadata");
                for (int i = 0; i < 3; i++) {
                    String w = words.get(i);
                    check(call(FIND_CHAR, small, w.toCharArray(), 0, w.length()) == i,
                            "binary fallback row " + i);
                }
            } finally {
                Files.deleteIfExists(shortImage);
            }
            System.out.println("M3_STRING_LEXICON_HASH_PASS checks=" + checks
                    + " words=" + words.size());
        } finally {
            Files.deleteIfExists(image);
        }
    }
}
