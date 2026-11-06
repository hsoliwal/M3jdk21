// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * Differential dump (gate 3 of the relocation, recipe/bitmatrix-20261106/differential.sh). Replays the oracle inputs
 * of {@link M3BitMatrixContract} (the 0 x Integer.MAX_VALUE boundaries and the 10 x 10 boundary shapes x 3 densities
 * with their seeds) and of {@link M3BitMatrixTileContract} (the 15 x 15 sizes x 6 densities with their seeds), then
 * {@code args[1]} (default 100,000) seeded random matrices with shapes up to 4,096 x 4,096 bits (empty, dense tiles,
 * sparse, stride, first-tile probe boundary and mixed fills, every fill bounded in set calls), through the bit matrix
 * class named by {@code args[0]}, reached by reflection so that the donor class and the relocated class run the same
 * driver. For every input it prints the shape, payload bytes, the word lane (complete for small matrices and the
 * first 256 random rounds, FNV-1a fingerprint otherwise), the row cardinalities, the transposed shape and word lane,
 * the double transpose, the set-column lanes of the first rows and eight coordinate probes. The system property
 * {@code m3.bitmatrix.dumpBackend=NATIVE_REQUIRED} transposes through {@code transpose(Backend)} instead of
 * {@code transpose()}. Two bit matrix classes agree when their dumps are byte-identical.
 * Usage: {@code java -cp matrix-classes;test-classes jdk.m3.arrays.M3BitMatrixDump FQN [rounds]}
 */
//FLLDCIM: TFG - dump driver from the template M3IntPostingIndexDump (reflective, serves donor and relocated class); recipe m3/arrays/recipe/bitmatrix-20261106
public final class M3BitMatrixDump {
    private static final long RANDOM_SEED = 0x4249544D41545258L;
    private static final int DEFAULT_RANDOM_ROUNDS = 100_000;
    private static final int FULL_LANE_ROUNDS = 256;
    private static final int FULL_LANE_WORDS = 64;
    private static final int CARDINALITY_ROWS = 4096;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final int[] CONVERGENCE_BOUNDARIES = {0, 1, 2, 7, 31, 63, 64, 65, 95, 127};
    private static final int[] TILE_SIZES = {0, 1, 31, 63, 64, 65, 95, 127, 128, 129, 191, 192, 193, 256, 257};
    private final Method builder;
    private final Method set;
    private final Method build;
    private final Method transpose;
    private final Object backend;
    private final Method rows;
    private final Method columns;
    private final Method payloadBytes;
    private final Method get;
    private final Method rowCardinality;
    private final Method setColumns;
    private final Field words;
    private final PrintStream out;
    private long inputs;
    private long setCalls;
    private long transposes;

    private M3BitMatrixDump(Class<?> matrix, String backendName, PrintStream out) throws ReflectiveOperationException {
        this.builder = matrix.getMethod("builder", int.class, int.class);
        Class<?> builderType = builder.getReturnType();
        this.set = builderType.getMethod("set", int.class, int.class);
        this.build = builderType.getMethod("build");
        this.rows = matrix.getMethod("rows");
        this.columns = matrix.getMethod("columns");
        this.payloadBytes = matrix.getMethod("primitivePayloadBytes");
        this.get = matrix.getMethod("get", int.class, int.class);
        this.rowCardinality = matrix.getMethod("rowCardinality", int.class);
        this.setColumns = matrix.getMethod("setColumns", int.class);
        this.words = matrix.getDeclaredField("words");
        this.words.setAccessible(true);
        if (backendName == null || backendName.isBlank()) {
            this.transpose = matrix.getMethod("transpose");
            this.backend = null;
        } else {
            Class<?> backendType = Class.forName(matrix.getName() + "$Backend");
            Object constant = null;
            for (Object candidate : backendType.getEnumConstants()) {
                if (((Enum<?>) candidate).name().equals(backendName)) constant = candidate;
            }
            if (constant == null) throw new IllegalArgumentException("unknown backend " + backendName);
            this.transpose = matrix.getMethod("transpose", backendType);
            this.backend = constant;
        }
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        int randomRounds = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_RANDOM_ROUNDS;
        String backendName = System.getProperty("m3.bitmatrix.dumpBackend", "");
        M3BitMatrixDump dump = new M3BitMatrixDump(Class.forName(args[0]), backendName, out);
        dump.universeBoundaries();
        dump.convergenceFixtures();
        dump.tileFixtures();
        dump.randomRounds(randomRounds);
        out.print("TOTAL inputs=" + dump.inputs + " setCalls=" + dump.setCalls + " transposes=" + dump.transposes
                + " randomRounds=" + randomRounds + " backend=" + (backendName.isBlank() ? "JAVA" : backendName) + "\n");
        out.flush();
    }

    /** The zero-area universes of the convergence oracle: no allocation of the declared universe. */
    private void universeBoundaries() throws ReflectiveOperationException {
        emit("U0xMAX", builder.invoke(null, 0, Integer.MAX_VALUE), 0, Integer.MAX_VALUE, true);
        emit("UMAXx0", builder.invoke(null, Integer.MAX_VALUE, 0), Integer.MAX_VALUE, 0, true);
    }

    /** The 10 x 10 boundary shapes x 3 densities of the convergence oracle, same seeds and bit functions. */
    private void convergenceFixtures() throws ReflectiveOperationException {
        for (int r : CONVERGENCE_BOUNDARIES) {
            for (int c : CONVERGENCE_BOUNDARIES) {
                for (int density = 0; density < 3; density++) {
                    long seed = density == 0 ? r * 1024L + c : density == 1 ? r * 2048L + c : r * 4096L + c;
                    Random random = new Random(seed);
                    Object b = builder.invoke(null, r, c);
                    for (int row = 0; row < r; row++) {
                        for (int col = 0; col < c; col++) {
                            boolean value = switch (density) {
                                case 0 -> false;
                                case 1 -> true;
                                default -> random.nextInt(16) == 0;
                            };
                            if (value) setBit(b, row, col);
                        }
                    }
                    emit("F" + r + "x" + c + "d" + density, b, r, c, true);
                }
            }
        }
    }

    /** The 15 x 15 sizes x 6 densities of the tile oracle, same seeds and bit functions. */
    private void tileFixtures() throws ReflectiveOperationException {
        for (int r : TILE_SIZES) {
            for (int c : TILE_SIZES) {
                for (int density = 0; density < 6; density++) {
                    Random random = new Random(0x4d3354494c453634L ^ (r * 131L + c * 17L + density));
                    Object b = builder.invoke(null, r, c);
                    for (int row = 0; row < r; row++) {
                        for (int col = 0; col < c; col++) {
                            if (tileBit(density, random, row, col)) setBit(b, row, col);
                        }
                    }
                    emit("T" + r + "x" + c + "d" + density, b, r, c, (long) r * c <= FULL_LANE_WORDS * 64L);
                }
            }
        }
    }

    private static boolean tileBit(int density, Random random, int row, int col) {
        return switch (density) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> random.nextInt(128) == 0;
            case 3 -> random.nextInt(16) == 0;
            case 4 -> random.nextBoolean();
            default -> ((row + col) & 63) == 0;
        };
    }

    /** Seeded random shapes: 70% up to 200, 25% up to 1,100, 5% up to 4,096 bits a side; six bounded fill modes. */
    private void randomRounds(int randomRounds) throws ReflectiveOperationException {
        Random shapes = new Random(RANDOM_SEED);
        for (int round = 0; round < randomRounds; round++) {
            int klass = round % 100;
            int limit = klass < 70 ? 200 : klass < 95 ? 1100 : 4096;
            int r = shapes.nextInt(limit + 1);
            int c = shapes.nextInt(limit + 1);
            int mode = round % 6;
            Random fill = new Random(shapes.nextLong());
            Object b = builder.invoke(null, r, c);
            fill(b, r, c, mode, fill);
            emit("R" + round + "m" + mode, b, r, c, round < FULL_LANE_ROUNDS);
        }
    }

    private void fill(Object b, int r, int c, int mode, Random random) throws ReflectiveOperationException {
        if (r == 0 || c == 0) return;
        switch (mode) {
            case 0 -> { }
            case 1 -> denseTiles(b, r, c, 1 + random.nextInt(64), random.nextBoolean(), random);
            case 2 -> sparse(b, r, c, random);
            case 3 -> stride(b, r, c);
            case 4 -> probeBoundary(b, r, c, random);
            default -> {
                denseTiles(b, r, c, 1 + random.nextInt(16), random.nextBoolean(), random);
                sparse(b, r, c, random);
                for (int full = random.nextInt(4); full > 0; full--) {
                    int row = random.nextInt(r);
                    for (int col = 0; col < c; col++) setBit(b, row, col);
                }
            }
        }
    }

    /** Whole 64 x 64 tiles (clipped at the shape); every bit when the matrix has at most 65,536 bits. */
    private void denseTiles(Object b, int r, int c, int tiles, boolean firstTile, Random random)
            throws ReflectiveOperationException {
        if ((long) r * c <= 65_536L) {
            for (int row = 0; row < r; row++) for (int col = 0; col < c; col++) setBit(b, row, col);
            return;
        }
        int tileRows = (r + 63) >>> 6;
        int tileColumns = (c + 63) >>> 6;
        for (int tile = 0; tile < tiles; tile++) {
            int tr = tile == 0 && firstTile ? 0 : random.nextInt(tileRows);
            int tc = tile == 0 && firstTile ? 0 : random.nextInt(tileColumns);
            fillTile(b, r, c, tr, tc);
        }
    }

    private void fillTile(Object b, int r, int c, int tr, int tc) throws ReflectiveOperationException {
        int rowEnd = Math.min(r, (tr << 6) + 64);
        int colEnd = Math.min(c, (tc << 6) + 64);
        for (int row = tr << 6; row < rowEnd; row++) {
            for (int col = tc << 6; col < colEnd; col++) setBit(b, row, col);
        }
    }

    /** Up to 4,096 random cells. */
    private void sparse(Object b, int r, int c, Random random) throws ReflectiveOperationException {
        int count = random.nextInt(1 + (int) Math.min((long) r * c, 4096L));
        for (int i = 0; i < count; i++) setBit(b, random.nextInt(r), random.nextInt(c));
    }

    /** The anti-diagonal stride ((row + col) & 63) == 0 of the tile oracle, at most rows x ceil(columns / 64) cells. */
    private void stride(Object b, int r, int c) throws ReflectiveOperationException {
        for (int row = 0; row < r; row++) {
            for (int col = (64 - (row & 63)) & 63; col < c; col += 64) setBit(b, row, col);
        }
    }

    /** First tile at exactly 255 or 256 bits (the probe gate of the dense path), plus random dense tiles. */
    private void probeBoundary(Object b, int r, int c, Random random) throws ReflectiveOperationException {
        if (r < 64 || c < 64) {
            sparse(b, r, c, random);
            return;
        }
        int population = random.nextBoolean() ? 255 : 256;
        for (int i = 0; i < population; i++) setBit(b, i >>> 6, i & 63);
        int tileRows = (r + 63) >>> 6;
        int tileColumns = (c + 63) >>> 6;
        for (int tile = random.nextInt(8); tile > 0; tile--) {
            int tr = random.nextInt(tileRows);
            int tc = random.nextInt(tileColumns);
            if (tr != 0 || tc != 0) fillTile(b, r, c, tr, tc);
        }
    }

    private void setBit(Object b, int row, int col) throws ReflectiveOperationException {
        set.invoke(b, row, col);
        setCalls++;
    }

    private void emit(String label, Object b, int r, int c, boolean fullLanes) throws ReflectiveOperationException {
        inputs++;
        Object m = build.invoke(b);
        long[] source = (long[]) words.get(m);
        Object t = transposeOf(m);
        long[] target = (long[]) words.get(t);
        Object twice = transposeOf(t);
        long[] again = (long[]) words.get(twice);
        out.print(label + " rows=" + rows.invoke(m) + " cols=" + columns.invoke(m) + " payload=" + payloadBytes.invoke(m)
                + " words=" + lane(source, fullLanes) + " card=" + cardinalities(m, r)
                + " t.rows=" + rows.invoke(t) + " t.cols=" + columns.invoke(t) + " t.payload=" + payloadBytes.invoke(t)
                + " t.words=" + lane(target, fullLanes) + " t.card=" + cardinalities(t, c)
                + " tt.words=" + lane(again, false) + " tt.equal=" + Arrays.equals(source, again)
                + " cols0=" + columnsOf(m, r, 0, fullLanes) + " colsLast=" + columnsOf(m, r, r - 1, fullLanes)
                + " t.cols0=" + columnsOf(t, c, 0, fullLanes) + " probes=" + probes(m, t, r, c) + "\n");
    }

    private Object transposeOf(Object m) throws ReflectiveOperationException {
        transposes++;
        return backend == null ? transpose.invoke(m) : transpose.invoke(m, backend);
    }

    private String cardinalities(Object m, int rowCount) throws ReflectiveOperationException {
        if (rowCount > CARDINALITY_ROWS) return "-";
        long hash = FNV_OFFSET;
        long total = 0;
        for (int row = 0; row < rowCount; row++) {
            int cardinality = (Integer) rowCardinality.invoke(m, row);
            total += cardinality;
            hash = (hash ^ (cardinality & 0xffffffffL)) * FNV_PRIME;
        }
        return total + ":" + Long.toHexString(hash);
    }

    private String columnsOf(Object m, int rowCount, int row, boolean full) throws ReflectiveOperationException {
        if (rowCount == 0 || rowCount > CARDINALITY_ROWS) return "-";
        Object list = setColumns.invoke(m, row);
        int[] selected = (int[]) list.getClass().getMethod("toArray").invoke(list);
        return lane(selected, full);
    }

    private String probes(Object m, Object t, int r, int c) throws ReflectiveOperationException {
        if (r == 0 || c == 0 || r > CARDINALITY_ROWS || c > CARDINALITY_ROWS) return "-";
        Random random = new Random(r * 8191L + c);
        StringBuilder bits = new StringBuilder(8);
        for (int probe = 0; probe < 8; probe++) {
            int row = random.nextInt(r);
            int col = random.nextInt(c);
            boolean direct = (Boolean) get.invoke(m, row, col);
            boolean mirrored = (Boolean) get.invoke(t, col, row);
            bits.append(direct ? '1' : '0').append(mirrored == direct ? '=' : '!');
        }
        return bits.toString();
    }

    private static String lane(long[] values, boolean full) {
        if (full && values.length <= FULL_LANE_WORDS) return values.length + ":" + Arrays.toString(values);
        long hash = FNV_OFFSET;
        for (long value : values) {
            hash = (hash ^ value) * FNV_PRIME;
        }
        return values.length + ":" + Long.toHexString(hash);
    }

    private static String lane(int[] values, boolean full) {
        if (full && values.length <= FULL_LANE_WORDS * 64) return values.length + ":" + Arrays.toString(values);
        long hash = FNV_OFFSET;
        for (int value : values) {
            hash = (hash ^ (value & 0xffffffffL)) * FNV_PRIME;
        }
        return values.length + ":" + Long.toHexString(hash);
    }
}
