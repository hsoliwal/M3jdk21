// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * Differential dump (gate 3 of the relocation, recipe/csr-index-20261106/differential.sh). Replays the oracle
 * inputs of {@link M3CsrIndexContract} (the seven invalid inputs, the boundary rows 0..257 with filled owner
 * arrays, and the 1,600 seeded rounds of uniform, skewed and ordered owners) through the CSR class named by
 * args[0], reached by reflection so that the donor class and the relocated class run the same driver. For every
 * input it prints the exception class of a refused call, or the complete starts and references lanes of
 * fromOwners, the row-count, size and payload bytes, a checksum over every referenceAt(row, ordinal) and
 * referenceAtAbsolute(position), the exception classes of the three out-of-range queries, and whether the staged
 * Builder path emits identical lanes. Two CSR classes agree when their dumps are byte-identical. The final TOTAL
 * line counts the inputs and the references addressed.
 * Usage: {@code java -cp csr-classes;test-classes jdk.m3.arrays.M3CsrIndexDump FQN}
 */
//FLLDCIM: TFG - dump driver from the template m3/compiler/lifetime-slots M3LifetimeSlotPlanDump (reflective, serves donor and relocated class)
public final class M3CsrIndexDump {
    private static final long SEED = 0x4333525F44495245L;
    private static final int ROUNDS = 1600;
    private static final int[] BOUNDARY_ROWS = {0, 1, 2, 63, 64, 65, 257};
    private static final int[] BOUNDARY_LENGTHS = {1, 2, 3, 8, 33, 257};
    private static final long CHECKSUM_MULTIPLIER = 0x9E3779B97F4A7C15L;
    private final Method fromOwners;
    private final Method builder;
    private final Method rowCount;
    private final Method size;
    private final Method payloadBytes;
    private final Method start;
    private final Method end;
    private final Method referenceAt;
    private final Method referenceAtAbsolute;
    private final Method starts;
    private final Method references;
    private final PrintStream out;
    private long inputs;
    private long referencesAddressed;

    private M3CsrIndexDump(Class<?> csr, PrintStream out) throws ReflectiveOperationException {
        this.fromOwners = csr.getMethod("fromOwners", int.class, int[].class);
        this.builder = csr.getMethod("builder", int.class, int.class);
        this.rowCount = csr.getMethod("rowCount");
        this.size = csr.getMethod("size");
        this.payloadBytes = csr.getMethod("payloadBytes");
        this.start = csr.getMethod("start", int.class);
        this.end = csr.getMethod("end", int.class);
        this.referenceAt = csr.getMethod("referenceAt", int.class, int.class);
        this.referenceAtAbsolute = csr.getMethod("referenceAtAbsolute", int.class);
        this.starts = csr.getMethod("starts");
        this.references = csr.getMethod("references");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        M3CsrIndexDump dump = new M3CsrIndexDump(Class.forName(args[0]), out);
        dump.invalidInputs();
        dump.boundaryCases();
        dump.seededRounds();
        out.print("TOTAL inputs=" + dump.inputs + " references=" + dump.referencesAddressed + " rounds=" + ROUNDS + "\n");
        out.flush();
    }

    private void invalidInputs() throws ReflectiveOperationException {
        refused("null owner first", 0, null);
        refused("null outranks invalid rows", -1, null);
        refused("negative rows", -1, new int[0]);
        refused("zero rows nonempty", 0, new int[] {0});
        refused("negative row", 1, new int[] {-1});
        refused("row upper bound", 1, new int[] {1});
        refused("later invalid row", 4, new int[] {1, 2, 4});
    }

    private void boundaryCases() throws ReflectiveOperationException {
        for (int rows : BOUNDARY_ROWS) {
            dump("B", rows, new int[0]);
            if (rows > 0) {
                for (int owner : new int[] {0, rows / 2, rows - 1}) {
                    for (int length : BOUNDARY_LENGTHS) {
                        int[] owners = new int[length];
                        Arrays.fill(owners, owner);
                        dump("B", rows, owners);
                    }
                }
            }
        }
    }

    private void seededRounds() throws ReflectiveOperationException {
        Random rng = new Random(SEED);
        for (int round = 0; round < ROUNDS; round++) {
            int rows = rng.nextInt(2049);
            int length = rows == 0 ? 0 : rng.nextInt(1025);
            int[] owners = new int[length];
            for (int index = 0; index < owners.length; index++) {
                owners[index] = switch (round % 5) {
                    case 0 -> rng.nextInt(rows);
                    case 1 -> rng.nextInt(Math.min(3, rows));
                    case 2 -> (index % rows);
                    case 3 -> (rows - 1 - index % rows);
                    default -> rng.nextInt(rows) / 2;
                };
            }
            dump("S" + round, rows, owners);
        }
    }

    private void refused(String label, int rows, int[] owners) throws ReflectiveOperationException {
        inputs++;
        out.print("R " + label + " " + outcome(() -> fromOwners.invoke(null, rows, owners)) + "\n");
    }

    private void dump(String label, int rows, int[] owners) throws ReflectiveOperationException {
        inputs++;
        int[] original = owners.clone();
        Object csr = fromOwners.invoke(null, rows, owners);
        int[] startLane = (int[]) starts.invoke(csr);
        int[] referenceLane = (int[]) references.invoke(csr);
        referencesAddressed += referenceLane.length;
        long checksum = 0;
        for (int row = 0; row < rows; row++) {
            int from = (int) start.invoke(csr, row);
            int to = (int) end.invoke(csr, row);
            for (int ordinal = 0; ordinal < to - from; ordinal++) {
                checksum = checksum * CHECKSUM_MULTIPLIER + (int) referenceAt.invoke(csr, row, ordinal);
            }
        }
        for (int position = 0; position < referenceLane.length; position++) {
            checksum = checksum * CHECKSUM_MULTIPLIER + (int) referenceAtAbsolute.invoke(csr, position);
        }
        Object staged = builder.invoke(null, rows, original.length);
        Method add = staged.getClass().getMethod("add", int.class, int.class);
        Method build = staged.getClass().getMethod("build");
        for (int reference = 0; reference < original.length; reference++) {
            add.invoke(staged, original[reference], reference);
        }
        Object built = build.invoke(staged);
        boolean builderParity = Arrays.equals(startLane, (int[]) starts.invoke(built))
                && Arrays.equals(referenceLane, (int[]) references.invoke(built));
        out.print(label + " rows=" + rows + " n=" + original.length + " rowCount=" + rowCount.invoke(csr) + " size="
                + size.invoke(csr) + " payload=" + payloadBytes.invoke(csr) + " checksum=" + Long.toHexString(checksum)
                + " builder=" + builderParity + " start(-1)=" + outcome(() -> start.invoke(csr, -1)) + " end(rows)="
                + outcome(() -> end.invoke(csr, rows)) + " absolute(n)="
                + outcome(() -> referenceAtAbsolute.invoke(csr, original.length)) + "\n");
        out.print(" starts=" + Arrays.toString(startLane) + "\n");
        out.print(" references=" + Arrays.toString(referenceLane) + "\n");
    }

    private interface Call {
        Object run() throws ReflectiveOperationException;
    }

    private static String outcome(Call call) throws ReflectiveOperationException {
        try {
            call.run();
            return "ACCEPTED";
        } catch (InvocationTargetException refusal) {
            return refusal.getCause().getClass().getName();
        }
    }
}
