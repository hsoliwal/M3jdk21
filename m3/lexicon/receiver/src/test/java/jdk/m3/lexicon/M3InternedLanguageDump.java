// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.LongConsumer;

//FLLDCIM: DIFFERENTIAL - driver of recipe/interned-language-20261106/differential.sh; reflective, so one class drives the donor and the relocated substrate
/**
 * Reflective dump driver of the differential gate (not a test). For each label the same seeded operations run against
 * the named class and print one line per case: inputs, result or exception (simple class name and message). Nothing
 * identity-dependent is printed (no Object.toString of texts or ropes): packed texts and ropes are reduced to size,
 * hash code, token count, fingerprint and an FNV-1a digest of the forEach order.
 *
 * <p>usage: {@code M3InternedLanguageDump --cases N --seed S Label=fully.qualified.Name...} with the labels
 * {@code M3TokenId}, {@code M3PackedText}, {@code M3TokenRope}, {@code M3OverflowInterner}, {@code M3SharedSymbols}.
 */
public final class M3InternedLanguageDump {
    private static final int[] INT_EXTREMES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, 2, 0xfffd, 0xfffe, 0xffff, 0x10000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    private static final long[] LONG_EXTREMES = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, 0xfffe_0000_0000L, 0xfffe_0000_0015L, 0xfffe_0000_0016L,
        0xffff_0000_0000L, 0xffff_ffff_ffffL, 0x1_0000_0000_0000L, Long.MAX_VALUE - 1, Long.MAX_VALUE
    };
    private static final String[] SYMBOL_CANDIDATES = {
        " ", "\n", "\t", "\r", ".", ",", "!", "?", ";", ":", "-", "—", "(", ")", "[", "]", "{", "}", "\"", "'", "/", "\\",
        "", "  ", "a", "word", "résumé", "résumé", " ", "--"
    };
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final int MAX_LEAF = 6;
    private static final int MAX_WORD_LENGTH = 12;
    private static final long MAX_ROPE_TOKENS = 4_096L;

    private final PrintWriter out;
    private final long seed;
    private long cases;
    private long thrown;

    private M3InternedLanguageDump(PrintWriter out, long seed) {
        this.out = out;
        this.seed = seed;
    }

    public static void main(String[] args) throws Exception {
        int cases = 100_000;
        long seed = 20261106L;
        Map<String, String> targets = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cases" -> cases = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                default -> {
                    String[] pair = args[i].split("=", 2);
                    targets.put(pair[0], pair[1]);
                }
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)));
        M3InternedLanguageDump dump = new M3InternedLanguageDump(out, seed);
        for (Map.Entry<String, String> target : targets.entrySet()) {
            dump.run(target.getKey(), Class.forName(target.getValue()), targets, cases);
        }
        dump.emit(String.format("DUMP labels=%d cases=%d thrown=%d seed=%d", targets.size(), dump.cases, dump.thrown, seed));
        out.flush();
    }

    private void run(String label, Class<?> type, Map<String, String> targets, int count) throws Exception {
        SplittableRandom random = new SplittableRandom(seed ^ label.hashCode());
        switch (label) {
            case "M3TokenId" -> tokenIds(label, type, random, count);
            case "M3PackedText" -> packedTexts(label, type, random, count);
            case "M3TokenRope" -> ropes(label, type, Class.forName(targets.get("M3PackedText")), random, count);
            case "M3OverflowInterner" -> interner(label, type, random, count);
            case "M3SharedSymbols" -> symbols(label, type, random, count);
            default -> throw new IllegalArgumentException("unknown label " + label);
        }
    }

    private void tokenIds(String label, Class<?> type, SplittableRandom random, int count) throws Exception {
        Method pack = type.getMethod("pack", int.class, int.class);
        Method languageId = type.getMethod("languageId", long.class);
        Method wordId = type.getMethod("wordId", long.class);
        Method isSymbol = type.getMethod("isSymbol", long.class);
        Method isOverflow = type.getMethod("isOverflow", long.class);
        line(label, "SYMBOL_LANGUAGE_ID", type.getField("SYMBOL_LANGUAGE_ID").get(null));
        line(label, "OVERFLOW_LANGUAGE_ID", type.getField("OVERFLOW_LANGUAGE_ID").get(null));
        for (int language : INT_EXTREMES) {
            for (int word : INT_EXTREMES) {
                call(label, "pack", pack, null, language, word);
            }
        }
        for (long token : LONG_EXTREMES) {
            decode(label, languageId, wordId, isSymbol, isOverflow, token);
        }
        for (int i = 0; i < count; i++) {
            int language = random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(0x10000);
            int word = random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(Integer.MAX_VALUE);
            Object token = call(label, "pack", pack, null, language, word);
            if (token instanceof Long packed) {
                decode(label, languageId, wordId, isSymbol, isOverflow, packed);
            }
        }
    }

    private void decode(String label, Method languageId, Method wordId, Method isSymbol, Method isOverflow, long token)
            throws Exception {
        call(label, "languageId", languageId, null, token);
        call(label, "wordId", wordId, null, token);
        call(label, "isSymbol", isSymbol, null, token);
        call(label, "isOverflow", isOverflow, null, token);
    }

    private void packedTexts(String label, Class<?> type, SplittableRandom random, int count) throws Exception {
        Constructor<?> constructor = type.getConstructor(long[].class);
        Method size = type.getMethod("size");
        Method token = type.getMethod("token", int.class);
        Method toArray = type.getMethod("toArray");
        Method forEach = type.getMethod("forEach", LongConsumer.class);
        construct(label, constructor, (Object) null);
        for (int i = 0; i < count; i++) {
            long[] tokens = randomTokens(random, random.nextInt(0, MAX_LEAF));
            Object text = construct(label, constructor, (Object) tokens);
            if (text == null) {
                continue;
            }
            call(label, "size", size, text);
            call(label, "hashCode", type.getMethod("hashCode"), text);
            call(label, "token", token, text, tokens.length == 0 ? 0 : random.nextInt(tokens.length));
            call(label, "token.outOfBounds", token, text, random.nextBoolean() ? tokens.length : -1);
            long[] copy = (long[]) toArray.invoke(text);
            line(label, "toArray", copy.length + ":" + digest(copy) + " aliasing=" + (copy == tokens));
            Object same = construct(label, constructor, (Object) tokens.clone());
            line(label, "equals.same", text.equals(same) + " " + (text.hashCode() == same.hashCode()));
            long[] other = tokens.clone();
            if (other.length > 0) {
                other[random.nextInt(other.length)] ^= 1L << random.nextInt(64);
            } else {
                other = new long[] {random.nextLong()};
            }
            line(label, "equals.other", text.equals(construct(label, constructor, (Object) other)) + " " + text.equals(null) + " " + text.equals("x"));
            long[] seen = new long[1];
            int[] n = new int[1];
            forEach.invoke(text, (LongConsumer) value -> {
                seen[0] = fnv(seen[0] == 0 && n[0] == 0 ? FNV_OFFSET : seen[0], value);
                n[0]++;
            });
            line(label, "forEach", n[0] + ":" + Long.toHexString(seen[0]));
            call(label, "forEach.null", forEach, text, (Object) null);
        }
    }

    private void ropes(String label, Class<?> type, Class<?> textType, SplittableRandom random, int count) throws Exception {
        Constructor<?> constructor = textType.getConstructor(long[].class);
        Method leaf = type.getMethod("leaf", textType);
        Method concat = type.getMethod("concat", type, type);
        Method tokenCount = type.getMethod("tokenCount");
        Method fingerprint = type.getMethod("fingerprint");
        Method forEach = type.getMethod("forEach", LongConsumer.class);
        Class<?> leafType = Class.forName(type.getName() + "$Leaf");
        Class<?> concatType = Class.forName(type.getName() + "$Concat");
        Method leafText = leafType.getMethod("text");
        Method concatLeft = concatType.getMethod("left");
        Method concatRight = concatType.getMethod("right");
        call(label, "leaf.null", leaf, null, (Object) null);
        List<Object> pool = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Object rope;
            String op;
            Object left = pool.size() < 2 ? null : pool.get(random.nextInt(pool.size()));
            Object right = pool.size() < 2 ? null : pool.get(random.nextInt(pool.size()));
            // a pooled DAG doubles with every concat: a leaf replaces a concat whose flattened size would pass the bound
            boolean bounded = left != null && (Long) tokenCount.invoke(left) + (Long) tokenCount.invoke(right) <= MAX_ROPE_TOKENS;
            if (!bounded || random.nextInt(3) == 0) {
                long[] tokens = randomTokens(random, random.nextInt(0, MAX_LEAF));
                rope = leaf.invoke(null, constructor.newInstance((Object) tokens));
                op = "leaf " + tokens.length + ":" + digest(tokens);
                line(label, "leaf.text", leafType.isInstance(rope) + " " + leafText.invoke(rope).hashCode());
            } else {
                rope = concat.invoke(null, left, right);
                op = "concat";
                line(label, "concat.parts", concatType.isInstance(rope) + " " + (concatLeft.invoke(rope) == left) + " "
                        + (concatRight.invoke(rope) == right) + " " + fingerprint.invoke(left) + " " + fingerprint.invoke(right));
                if (random.nextBoolean()) {
                    call(label, "concat.null", concat, null, null, right);
                } else {
                    call(label, "concat.null", concat, null, left, null);
                }
            }
            long[] seen = {FNV_OFFSET};
            int[] n = new int[1];
            forEach.invoke(rope, (LongConsumer) value -> {
                seen[0] = fnv(seen[0], value);
                n[0]++;
            });
            line(label, op, tokenCount.invoke(rope) + " " + Long.toHexString((Long) fingerprint.invoke(rope)) + " " + n[0] + ":" + Long.toHexString(seen[0]));
            if (pool.size() < 64) {
                pool.add(rope);
            } else {
                pool.set(random.nextInt(pool.size()), rope);
            }
        }
    }

    private void interner(String label, Class<?> type, SplittableRandom random, int count) throws Exception {
        Object interner = type.getConstructor().newInstance();
        Method intern = type.getMethod("intern", int.class, String.class);
        Method entry = type.getMethod("entry", int.class);
        Method size = type.getMethod("size");
        Class<?> entryType = Class.forName(type.getName() + "$Entry");
        Method sourceLanguageId = entryType.getMethod("sourceLanguageId");
        Method text = entryType.getMethod("text");
        call(label, "intern.null", intern, interner, 0, null);
        for (int i = 0; i < count; i++) {
            int language = random.nextInt(4) == 0 ? INT_EXTREMES[random.nextInt(INT_EXTREMES.length)] : random.nextInt(3);
            String raw = randomWord(random);
            Object token = call(label, "intern " + language + " " + escape(raw), intern, interner, language, raw);
            int id = (int) (long) (Long) token;
            Object found = entry.invoke(interner, id);
            line(label, "entry", id + " " + sourceLanguageId.invoke(found) + " " + escape((String) text.invoke(found)) + " size=" + size.invoke(interner));
            if (random.nextInt(16) == 0) {
                call(label, "entry.outOfBounds", entry, interner, (Integer) size.invoke(interner));
            }
        }
        line(label, "size", size.invoke(interner));
    }

    private void symbols(String label, Class<?> type, SplittableRandom random, int count) throws Exception {
        Method find = type.getMethod("find", String.class);
        Method resolve = type.getMethod("resolve", long.class);
        Method size = type.getMethod("size");
        line(label, "SPACE", type.getField("SPACE").get(null));
        line(label, "NEWLINE", type.getField("NEWLINE").get(null));
        line(label, "TAB", type.getField("TAB").get(null));
        line(label, "size", size.invoke(null));
        for (String candidate : SYMBOL_CANDIDATES) {
            call(label, "find " + escape(candidate), find, null, candidate);
        }
        for (long token : LONG_EXTREMES) {
            call(label, "resolve", resolve, null, token);
        }
        int n = (Integer) size.invoke(null);
        for (int id = -1; id <= n; id++) {
            call(label, "resolve.symbol " + id, resolve, null, (0xfffeL << 32) | Integer.toUnsignedLong(id));
        }
        for (int i = 0; i < count; i++) {
            if (random.nextBoolean()) {
                String candidate = random.nextInt(4) == 0 ? randomWord(random) : SYMBOL_CANDIDATES[random.nextInt(SYMBOL_CANDIDATES.length)];
                call(label, "find " + escape(candidate), find, null, candidate);
            } else {
                long language = random.nextInt(4) == 0 ? random.nextInt(0x10000) : 0xfffeL;
                long token = (language << 32) | Integer.toUnsignedLong(random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(n + 2));
                Object text = call(label, "resolve", resolve, null, token);
                if (text instanceof String resolved) {
                    line(label, "resolve.text", escape(resolved) + " " + find.invoke(null, resolved));
                }
            }
        }
    }

    private Object construct(String label, Constructor<?> constructor, Object... args) {
        cases++;
        try {
            Object value = constructor.newInstance(args);
            emit(label + " new " + describeArgs(args) + " -> ok");
            return value;
        } catch (InvocationTargetException failure) {
            thrown++;
            emit(label + " new " + describeArgs(args) + " !! " + describe(failure.getCause()));
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private Object call(String label, String op, Method method, Object receiver, Object... args) {
        cases++;
        try {
            Object value = method.invoke(receiver, args);
            emit(label + " " + op + " " + describeArgs(args) + " -> " + describeValue(value));
            return value;
        } catch (InvocationTargetException failure) {
            thrown++;
            emit(label + " " + op + " " + describeArgs(args) + " !! " + describe(failure.getCause()));
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private void line(String label, String op, Object value) {
        cases++;
        emit(label + " " + op + " = " + describeValue(value));
    }

    /** One line, LF-terminated on every platform: the two dumps are compared byte for byte through a text-mode sed. */
    private void emit(String text) {
        out.print(text);
        out.print('\n');
    }

    private static String describeArgs(Object[] args) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(describeValue(args[i]));
        }
        return sb.append(')').toString();
    }

    private static String describeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof long[] array) {
            return array.length + ":" + digest(array);
        }
        if (value instanceof String text) {
            return escape(text);
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Boolean) {
            return value.toString();
        }
        return value.getClass().getSimpleName().replaceFirst("^M3", "") + "#" + value.hashCode();
    }

    private static String describe(Throwable failure) {
        return failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7e || c == '"' || c == '\\') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static long[] randomTokens(SplittableRandom random, int length) {
        long[] tokens = new long[length];
        for (int i = 0; i < length; i++) {
            tokens[i] = ((long) random.nextInt(0x10000) << 32) | Integer.toUnsignedLong(random.nextInt(Integer.MAX_VALUE));
        }
        return tokens;
    }

    private static String randomWord(SplittableRandom random) {
        int length = random.nextInt(0, MAX_WORD_LENGTH);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int kind = random.nextInt(10);
            if (kind < 7) {
                sb.append((char) ('a' + random.nextInt(26)));
            } else if (kind == 7) {
                sb.append((char) ('0' + random.nextInt(10)));
            } else if (kind == 8) {
                sb.append(random.nextBoolean() ? "é" : "é");
            } else {
                sb.append(SYMBOL_CANDIDATES[random.nextInt(SYMBOL_CANDIDATES.length)]);
            }
        }
        return sb.toString();
    }

    private static String digest(long[] tokens) {
        long h = FNV_OFFSET;
        for (long token : tokens) {
            h = fnv(h, token);
        }
        return Long.toHexString(h);
    }

    private static long fnv(long h, long value) {
        return (h ^ value) * FNV_PRIME;
    }
}
