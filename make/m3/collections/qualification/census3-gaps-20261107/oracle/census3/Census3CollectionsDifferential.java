// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Seeded differential of the census3 m3/collections RESTORE members: donor (com.synexia develop
 * MIndexMonotoneLongDeque / MIndexPersistentLongSumTree) against the owner with the restored members.
 * Usage: java census3.Census3CollectionsDifferential donor|owner episodes [--trace]. Prints one SHA-256 over the
 * canonical op log (and the log with --trace). The two sides must print byte-identical output.
 *
 * <p>Deque episodes compare only the restored tail accessors (lastIndex/lastValue): distinct-value episodes use a
 * small capacity (capacity refusals coincide), duplicate-value episodes use capacity = offers (the equal-value
 * policy is the NEC part of offer and never reaches the tail). Tree episodes compare forkAdd results and refusals,
 * rootNode ids, length, versionCount, created node count (sentinel excluded) and the total of every version;
 * extreme-delta episodes (Long.MIN/MAX deltas, overflow refusals) skip rangeSum, an owner member whose partial
 * sums are unchecked (not a census3 restore).
 */
public final class Census3CollectionsDifferential {
    private static final long SEED = 0x43334741505330L; // "C3GAPS0"
    private final Side side;
    private final StringBuilder log = new StringBuilder(1 << 16);
    private final MessageDigest digest;
    private final boolean trace;

    private Census3CollectionsDifferential(Side side, boolean trace) throws Exception {
        this.side = side;
        this.trace = trace;
        this.digest = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Exception {
        Side side = switch (args[0]) {
            case "donor" -> (Side) Class.forName("census3.DonorSide").getDeclaredConstructor().newInstance();
            case "owner" -> (Side) Class.forName("census3.OwnerSide").getDeclaredConstructor().newInstance();
            default -> throw new IllegalArgumentException(args[0]);
        };
        int episodes = Integer.parseInt(args[1]);
        Census3CollectionsDifferential run = new Census3CollectionsDifferential(side, args.length > 2);
        SplittableRandom random = new SplittableRandom(SEED);
        for (int episode = 0; episode < episodes; episode++) {
            run.dequeEpisode(random.split(), episode);
            run.treeEpisode(random.split(), episode);
        }
        run.flush();
        System.out.println("census3-collections episodes=" + episodes + " sha256="
                + HexFormat.of().formatHex(run.digest.digest()));
    }

    private void emit(String line) {
        log.append(line).append('\n');
        if (log.length() > (1 << 15)) flush();
    }

    private void flush() {
        byte[] bytes = log.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        digest.update(bytes);
        if (trace) System.out.print(log);
        log.setLength(0);
    }

    private static String failure(RuntimeException failure) {
        return "!" + failure.getClass().getSimpleName();
    }

    private void dequeEpisode(SplittableRandom random, int episode) {
        boolean duplicates = (episode & 1) == 1;
        boolean max = random.nextBoolean();
        int offers = 1 + random.nextInt(40);
        int capacity = duplicates ? offers : 1 + random.nextInt(6);
        side.newDeque(capacity, max);
        emit("D " + episode + " cap=" + capacity + " max=" + max + " dup=" + duplicates);
        int index = random.nextInt(3);
        long[] used = new long[offers];
        for (int step = 0; step < offers; step++) {
            long value = duplicates ? random.nextInt(4) : distinct(random, used, step);
            String offered;
            try {
                side.offer(index, value);
                offered = "ok";
            } catch (RuntimeException failure) {
                offered = failure(failure);
            }
            String evicted = "";
            if (random.nextInt(4) == 0) {
                int minimum = index - random.nextInt(4) + 1;
                side.evictBefore(Math.max(0, minimum));
                evicted = " evict=" + Math.max(0, minimum);
            }
            emit(" o " + index + " " + value + " " + offered + evicted + " tail=" + tail());
            index += 1 + random.nextInt(3);
        }
    }

    private static long distinct(SplittableRandom random, long[] used, int step) {
        while (true) {
            long candidate = random.nextLong(-1_000_000L, 1_000_000L);
            boolean seen = false;
            for (int i = 0; i < step; i++) seen |= used[i] == candidate;
            if (!seen) {
                used[step] = candidate;
                return candidate;
            }
        }
    }

    private String tail() {
        String index;
        String value;
        try {
            index = Long.toString(side.lastIndex());
        } catch (RuntimeException failure) {
            index = failure(failure);
        }
        try {
            value = Long.toString(side.lastValue());
        } catch (RuntimeException failure) {
            value = failure(failure);
        }
        return index + "/" + value;
    }

    private void treeEpisode(SplittableRandom random, int episode) {
        int length = random.nextInt(8) == 0 ? -1 + random.nextInt(2) : 1 + random.nextInt(70);
        int expected = random.nextInt(10) == 0 ? -1 : random.nextInt(12);
        boolean presized = random.nextInt(3) != 0;
        try {
            if (presized) side.newTree(length, expected);
            else if (length > 0) side.newTreeDefault(length);
            else {
                emit("T " + episode + " skip");
                return;
            }
        } catch (RuntimeException failure) {
            emit("T " + episode + " len=" + length + " exp=" + expected + " " + failure(failure));
            return;
        }
        emit("T " + episode + " len=" + side.length() + " presized=" + presized + " exp=" + expected);
        int operations = 1 + random.nextInt(60);
        boolean extreme = random.nextBoolean();
        for (int op = 0; op < operations; op++) {
            int versions = side.versionCount();
            int base = random.nextInt(16) == 0 ? versions : random.nextInt(versions);
            int index = random.nextInt(16) == 0 ? (random.nextBoolean() ? -1 : length) : random.nextInt(length);
            long delta = switch (extreme ? random.nextInt(8) : 3) {
                case 0 -> 0L;
                case 1 -> Long.MAX_VALUE - random.nextInt(3);
                case 2 -> Long.MIN_VALUE + random.nextInt(3);
                default -> random.nextLong(-1_000L, 1_000L);
            };
            String result;
            try {
                result = Integer.toString(side.forkAdd(base, index, delta));
            } catch (RuntimeException failure) {
                result = failure(failure);
            }
            emit(" f " + base + " " + index + " " + delta + " -> " + result + " vc=" + side.versionCount()
                    + " nodes=" + side.createdNodes());
        }
        StringBuilder state = new StringBuilder(" s");
        for (int version = 0; version <= side.versionCount(); version++) {
            try {
                state.append(' ').append(side.rootNode(version)).append(':').append(side.total(version));
                int from = random.nextInt(length + 1);
                int to = from + random.nextInt(length - from + 1);
                // rangeSum is an owner member (unchecked partial sums); compared on non-extreme episodes only
                if (!extreme) state.append(':').append(side.rangeSum(version, from, to));
            } catch (RuntimeException failure) {
                state.append(' ').append(failure(failure));
            }
        }
        emit(state.toString());
    }
}
