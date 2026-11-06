// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.SplittableRandom;

//FLLDCIM: DIFFERENTIAL - owner check 1 of recipe/graph-trie-20261106 (unionfind_differential.sh): the donor union-find is verified against the M3JDK owner, never ported
/**
 * Partition differential of the donor {@code IntUnionFind} (a class given by name, loaded from the donor compile tree,
 * every method taking a progress monitor) against the owner {@code jdk.m3.collections.M3IntDisjointSet}: both replay
 * the same seeded sequences of union / connected / find / componentSize / componentCount operations over sizes 1 to
 * 100,000 (and the empty set), and each side writes one dump line per operation to its own file with every observation
 * that belongs to the partition (union result, connected result, component size and count, out-of-range refusals) but
 * never a representative; at the end of every sequence each side prints the sha256 of its canonical labelling (the
 * label of an element is the first element of its set in index order). The shell compares the two dumps byte for byte.
 * Representatives ({@code find}) are compared in process and only counted: the two structures compress paths
 * differently (full compression against path halving), so they may name different roots for the same set.
 *
 * <p>usage: {@code UnionFindPartitionDump --ops N --seed S --out DIR Donor=fqn.IntUnionFind Monitor=fqn.M3ProgressMonitor}
 */
public final class UnionFindPartitionDump {
    private static final long SEED = 20261106L;
    private static final int MAX_SIZE = 100_000;
    private static final int SMALL_SIZE = 64;
    private static final int MEDIUM_SIZE = 5_000;
    private static final int MAX_SEQUENCE_OPS = 20_000;

    private UnionFindPartitionDump() {
    }

    public static void main(String[] args) throws Exception {
        long ops = 1_000_000L;
        long seed = SEED;
        Path out = Path.of(".");
        String donorName = null;
        String monitorName = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--ops" -> ops = Long.parseLong(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                default -> {
                    String[] pair = args[i].split("=", 2);
                    if (pair[0].equals("Donor")) {
                        donorName = pair[1];
                    } else if (pair[0].equals("Monitor")) {
                        monitorName = pair[1];
                    }
                }
            }
        }
        Files.createDirectories(out);
        try (PrintWriter donorOut = writer(out.resolve("donor.txt")); PrintWriter ownerOut = writer(out.resolve("owner.txt"))) {
            Donor donor = new Donor(Class.forName(donorName), Class.forName(monitorName));
            Replay replay = new Replay(donor, donorOut, ownerOut, seed);
            replay.run(ops);
            System.out.println(replay.summary());
        }
    }

    private static PrintWriter writer(Path path) throws IOException {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path),
                StandardCharsets.UTF_8), 1 << 20));
    }

    /** The donor class behind reflection: every method takes the donor's progress monitor (none()). */
    private static final class Donor {
        final Class<?> type;
        final Object none;
        final Method find;
        final Method union;
        final Method connected;
        final Method componentSize;
        final Method componentCount;

        Donor(Class<?> type, Class<?> monitor) throws Exception {
            this.type = type;
            this.none = monitor.getMethod("none").invoke(null);
            this.find = type.getMethod("find", int.class, monitor);
            this.union = type.getMethod("union", int.class, int.class, monitor);
            this.connected = type.getMethod("connected", int.class, int.class, monitor);
            this.componentSize = type.getMethod("componentSize", int.class, monitor);
            this.componentCount = type.getMethod("componentCount", monitor);
        }

        Object create(int size) throws Exception {
            return type.getConstructor(int.class).newInstance(size);
        }

        /** The result, or "throws Type: message" when the donor refuses. */
        Object call(Method method, Object receiver, Object... argv) throws Exception {
            try {
                return method.invoke(receiver, argv);
            } catch (InvocationTargetException refused) {
                Throwable cause = refused.getCause();
                return "throws " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            }
        }
    }

    private static final class Replay {
        private final Donor donor;
        private final PrintWriter donorOut;
        private final PrintWriter ownerOut;
        private final long seed;
        private long ops;
        private long unions;
        private long finds;
        private long repMismatch;
        private long refusals;
        private int sequences;
        private int maxSize;
        private String firstDifference;

        Replay(Donor donor, PrintWriter donorOut, PrintWriter ownerOut, long seed) {
            this.donor = donor;
            this.donorOut = donorOut;
            this.ownerOut = ownerOut;
            this.seed = seed;
        }

        void run(long budget) throws Exception {
            while (ops < budget) {
                SplittableRandom random = new SplittableRandom(seed * 1_000_003L + 104_729L * (sequences + 1));
                int tier = random.nextInt(100);
                int size = tier < 1 ? 0 : tier < 51 ? random.nextInt(1, SMALL_SIZE + 1)
                        : tier < 81 ? random.nextInt(SMALL_SIZE + 1, MEDIUM_SIZE + 1)
                        : random.nextInt(MEDIUM_SIZE + 1, MAX_SIZE + 1);
                long count = Math.min(budget - ops, random.nextInt(1, MAX_SEQUENCE_OPS + 1));
                sequence(size, count, random);
                sequences++;
            }
        }

        private void sequence(int size, long count, SplittableRandom random) throws Exception {
            Object donorSet = donor.create(size);
            jdk.m3.collections.M3IntDisjointSet owner = new jdk.m3.collections.M3IntDisjointSet(size);
            maxSize = Math.max(maxSize, size);
            String head = "seq " + sequences + " n=" + size + " ";
            donorOut.print(head + "begin\n");
            ownerOut.print(head + "begin\n");
            for (long op = 0; op < count; op++) {
                int kind = random.nextInt(100);
                int a = element(size, random);
                int b = element(size, random);
                String donorLine;
                String ownerLine;
                if (kind < 50) {
                    donorLine = "union " + a + " " + b + " -> " + donor.call(donor.union, donorSet, a, b, donor.none);
                    ownerLine = "union " + a + " " + b + " -> " + owner(() -> owner.union(a, b));
                    unions++;
                } else if (kind < 70) {
                    donorLine = "connected " + a + " " + b + " -> " + donor.call(donor.connected, donorSet, a, b, donor.none);
                    ownerLine = "connected " + a + " " + b + " -> " + owner(() -> owner.connected(a, b));
                } else if (kind < 85) {
                    Object donorRoot = donor.call(donor.find, donorSet, a, donor.none);
                    Object ownerRoot = owner(() -> owner.find(a));
                    finds++;
                    boolean refused = donorRoot instanceof String;
                    if (!refused && !donorRoot.equals(ownerRoot)) {
                        repMismatch++;
                    }
                    donorLine = "find " + a + " -> " + (refused ? donorRoot : "representative");
                    ownerLine = "find " + a + " -> " + (ownerRoot instanceof String ? ownerRoot : "representative");
                } else if (kind < 95) {
                    donorLine = "componentSize " + a + " -> " + donor.call(donor.componentSize, donorSet, a, donor.none);
                    ownerLine = "componentSize " + a + " -> " + owner(() -> owner.componentSize(a));
                } else {
                    donorLine = "componentCount -> " + donor.call(donor.componentCount, donorSet, donor.none);
                    ownerLine = "componentCount -> " + owner.components();
                }
                if (donorLine.contains("throws ")) {
                    refusals++;
                }
                ops++;
                donorOut.print(head + op + " " + donorLine + "\n");
                ownerOut.print(head + op + " " + ownerLine + "\n");
                if (firstDifference == null && !donorLine.equals(ownerLine)) {
                    firstDifference = "seq " + sequences + " op " + op + ": donor '" + donorLine + "' owner '" + ownerLine + "'";
                }
            }
            String donorLabels = canonical(size, index -> (Integer) donor.call(donor.find, donorSet, index, donor.none));
            String ownerLabels = canonical(size, owner::find);
            donorOut.print(head + "partition sha256=" + donorLabels + "\n");
            ownerOut.print(head + "partition sha256=" + ownerLabels + "\n");
            if (firstDifference == null && !donorLabels.equals(ownerLabels)) {
                firstDifference = "seq " + sequences + " partition digests differ";
            }
        }

        /** Mostly in range; one in a hundred probes is out of range (size, -1 or MAX), which both sides refuse. */
        private static int element(int size, SplittableRandom random) {
            if (size == 0 || random.nextInt(100) == 0) {
                int kind = random.nextInt(3);
                return kind == 0 ? size : kind == 1 ? -1 : Integer.MAX_VALUE;
            }
            return random.nextInt(size);
        }

        private interface Probe {
            Object get();
        }

        private interface Root {
            int find(int index) throws Exception;
        }

        private static Object owner(Probe probe) {
            try {
                return probe.get();
            } catch (RuntimeException refused) {
                return "throws " + refused.getClass().getSimpleName() + ": " + refused.getMessage();
            }
        }

        /** sha256 of the canonical labelling: label(i) = the smallest index of i's set, computed in one pass. */
        private static String canonical(int size, Root root) throws Exception {
            int[] rootOf = new int[size];
            int[] labelOfRoot = new int[size];
            java.util.Arrays.fill(labelOfRoot, -1);
            byte[] bytes = new byte[size * Integer.BYTES];
            for (int i = 0; i < size; i++) {
                rootOf[i] = root.find(i);
                if (labelOfRoot[rootOf[i]] < 0) {
                    labelOfRoot[rootOf[i]] = i;
                }
                int label = labelOfRoot[rootOf[i]];
                bytes[i * 4] = (byte) (label >>> 24);
                bytes[i * 4 + 1] = (byte) (label >>> 16);
                bytes[i * 4 + 2] = (byte) (label >>> 8);
                bytes[i * 4 + 3] = (byte) label;
            }
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            } catch (NoSuchAlgorithmException missing) {
                throw new IllegalStateException(missing);
            }
        }

        String summary() {
            return "UNIONFIND ops=" + ops + " sequences=" + sequences + " max_size=" + maxSize + " unions=" + unions
                    + " finds=" + finds + " representative_mismatches=" + repMismatch + " refusals=" + refusals
                    + " first_partition_difference=" + (firstDifference == null ? "none" : firstDifference);
        }
    }
}
