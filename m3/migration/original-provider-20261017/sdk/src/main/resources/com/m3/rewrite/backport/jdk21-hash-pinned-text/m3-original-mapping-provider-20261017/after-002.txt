/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.algorithm.M3PrefixZ;
import com.m3.text.LocalM3Arena;
import com.m3.text.LocalM3StringPiece;
import com.m3.text.M3StringPiece;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Differential tests use real, hash-pinned M3 owners and an independent String oracle. */
public final class PrefixZContractTest {
    private static long checks;
    private static long cases;

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static M3StringPiece view(String value, Random random) {
        LocalM3StringPiece atom = new LocalM3Arena().copyUtf16(value.toCharArray());
        var ranges = new ArrayList<M3StringPiece>();
        ranges.add(atom.subSequence(0, 0));
        for (int start = 0; start < value.length();) {
            int end = Math.min(value.length(), start + 1 + random.nextInt(7));
            ranges.add(atom.subSequence(start, end));
            start = end;
        }
        ranges.add(atom.subSequence(value.length(), value.length()));
        return M3StringPiece.join(M3StringPiece.join(ranges.toArray(M3StringPiece[]::new)));
    }

    private static void compare(String expected, M3StringPiece value) {
        M3PrefixZ actual = M3PrefixZ.analyze(value, 4L * value.length(), () -> false);
        require(actual.length() == expected.length(), "length");
        require(actual.metadataPayloadBytes() == 4L * expected.length(), "primitive payload accounting");
        long sum = 0;
        int border = 0;
        for (int i = 0; i < expected.length(); i++) {
            int prefix = 0;
            while (prefix < expected.length() - i
                    && expected.charAt(prefix) == expected.charAt(i + prefix)) prefix++;
            require(actual.prefixLengthAt(i) == prefix, "prefix at " + i);
            sum += prefix;
            if (i != 0 && prefix == expected.length() - i) border = Math.max(border, prefix);
        }
        require(actual.similaritySum() == sum, "long similarity");
        require(actual.longestProperBorderLength() == border, "proper border");
        rejects(IndexOutOfBoundsException.class, () -> actual.prefixLengthAt(-1));
        rejects(IndexOutOfBoundsException.class, () -> actual.prefixLengthAt(expected.length()));
        cases++;
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4D335A21L);
        for (String value : new String[] {"", "a", "aaaaa", "abababa", "banana", "\0a\0a",
                "\ud83d\ude00\ud83d\ude00", "\ud800x\udc00\ud800", "\uffff\u0000\uffff"}) {
            compare(value, view(value, random));
            // Every UTF-16 cut, including cuts through surrogate pairs.
            M3StringPiece atom = new LocalM3Arena().copyUtf16(value.toCharArray());
            for (int a = 0; a <= value.length(); a++) {
                compare(value, M3StringPiece.join(atom.subSequence(0, a), atom.subSequence(a, value.length())));
                for (int b = a; b <= value.length(); b++) {
                    compare(value.substring(a, b), view(value, random).subSequence(a, b));
                }
            }
        }
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String value = new String(new char[] {(char) unit, '\0', (char) unit, '\0', (char) unit});
            compare(value, view(value, random));
        }
        for (int sample = 0; sample < 12000; sample++) {
            char[] units = new char[random.nextInt(129)];
            for (int i = 0; i < units.length; i++) units[i] = switch (sample % 4) {
                case 0 -> (char) random.nextInt(65536);
                case 1 -> (char) ('a' + random.nextInt(3));
                case 2 -> (char) (0xD800 + random.nextInt(2048));
                default -> (char) (i % (1 + sample % 9));
            };
            String value = new String(units);
            compare(value, view(value, random));
        }
        byte[] latin = {(byte) 255, 0, (byte) 255, 0};
        M3StringPiece latinView = new LocalM3Arena().copyLatin1(latin);
        compare("\u00ff\0\u00ff\0", latinView);
        Arrays.fill(latin, (byte) 1);
        compare("\u00ff\0\u00ff\0", latinView);
        char[] mutable = {'a', 'b', 'a'};
        LocalM3StringPiece copied = new LocalM3Arena().copyUtf16(mutable);
        var identity = copied.storageIdentity();
        M3PrefixZ saved = M3PrefixZ.analyze(copied);
        Arrays.fill(mutable, 'x');
        compare("aba", copied);
        require(copied.storageIdentity().equals(identity), "owner identity unchanged");
        require(saved.similaritySum() == 4, "facts detached from mutable admission source");

        require(M3PrefixZ.requiredMetadataPayloadBytes(Integer.MAX_VALUE) == 8589934588L, "size overflow");
        rejects(IllegalArgumentException.class, () -> M3PrefixZ.requiredMetadataPayloadBytes(-1));
        rejects(IllegalArgumentException.class, () -> M3PrefixZ.analyze(copied, 11, () -> false));
        rejects(IllegalArgumentException.class, () -> M3PrefixZ.analyze(copied, -1, () -> false));
        rejects(NullPointerException.class, () -> M3PrefixZ.analyze(null));
        rejects(NullPointerException.class, () -> M3PrefixZ.analyze(copied, 12, null));
        rejects(CancellationException.class, () -> M3PrefixZ.analyze(copied, 12, () -> true));

        M3StringPiece longRun = new LocalM3Arena().copyUtf16("a".repeat(100000).toCharArray());
        M3PrefixZ longFacts = M3PrefixZ.analyze(longRun);
        require(longFacts.similaritySum() == 5000050000L, "similarity must exceed int safely");
        require(longFacts.longestProperBorderLength() == 99999, "long border");
        AtomicInteger polls = new AtomicInteger();
        rejects(CancellationException.class,
                () -> M3PrefixZ.analyze(longRun, 400000, () -> polls.incrementAndGet() == 3));
        require(polls.get() == 3, "cancellation inside long prefix scan");
        Thread.currentThread().interrupt();
        rejects(CancellationException.class, () -> M3PrefixZ.analyze(copied));
        require(Thread.interrupted(), "interrupt status preserved");

        // Instances retain only the primitive fact lane, not text owners or duplicate payloads.
        for (var field : M3PrefixZ.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                require(field.getType().isPrimitive() || field.getType() == int[].class,
                        "unexpected retained field " + field.getName());
                require(Modifier.isFinal(field.getModifiers()), "facts must be final");
            }
        }
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Callable<Long>>();
            for (int i = 0; i < 32; i++) tasks.add(() -> M3PrefixZ.analyze(copied).similaritySum());
            for (var result : executor.invokeAll(tasks)) require(result.get() == 4L, "parallel facts");
        }
        System.out.println("PREFIX_Z_PASS cases=" + cases + " checks=" + checks + " seed=0x4D335A21");
    }
}
