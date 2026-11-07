// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Seeded differential of census3 P3-010 (m3/ports/precompute): the donor MIndexStringEditDistancePlan against the
 * owner M3EditDistancePlan with the restored flag and Frontier members. Usage: java
 * census3.Census3FrontierDifferential donor|owner episodes [--trace]. Prints one SHA-256 over the canonical op log
 * (flag, frontier admission, appends and range appends with their refusal classes, distance / inputUnits /
 * cellUpdates / primitivePayloadBytes / limits / row of every checkpoint, minimumPossibleDistance, within, and the
 * monitor's worked / check counts, which pin the cancellation points). The two sides must print byte-identical output.
 * Only restored members and the flag are compared; the plan's own primitivePayloadBytes() is PRESENT_EVOLVED (the
 * owner counts its Myers lanes) and stays out of the log.
 */
public final class Census3FrontierDifferential {
    private static final long SEED = 0x4333465230L; // "C3FR0"
    private static final char[] ALPHABET = {'a', 'b', 'c', 'x', 'Ā', '\ud800', '\udc00', '￿'};

    /** One implementation side. */
    public interface Side {
        void compile(String pattern, boolean flag);
        String flags();
        String frontier(int maxInput, long maxCells, long maxBytes, int cancelAtCheck);
        String append(int from, String value, int cancelAtCheck);
        String appendRange(int from, String value, int begin, int end, int cancelAtCheck);
        int slots();
        String describe(int slot);
        String minimum(int slot, int remaining);
        String within(int slot, int maximum);
        String lastMonitor();
    }

    private final Side side;
    private final MessageDigest digest;
    private final StringBuilder log = new StringBuilder(1 << 16);
    private final boolean trace;

    private Census3FrontierDifferential(Side side, boolean trace) throws Exception {
        this.side = side;
        this.trace = trace;
        this.digest = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Exception {
        Side side = (Side) Class.forName(args[0].equals("donor") ? "census3.DonorFrontier" : "census3.OwnerFrontier")
                .getDeclaredConstructor().newInstance();
        int episodes = Integer.parseInt(args[1]);
        Census3FrontierDifferential run = new Census3FrontierDifferential(side, args.length > 2);
        SplittableRandom random = new SplittableRandom(SEED);
        for (int episode = 0; episode < episodes; episode++) run.episode(random.split(), episode);
        run.flush();
        System.out.println("census3-frontier episodes=" + episodes + " sha256="
                + HexFormat.of().formatHex(run.digest.digest()));
    }

    private void emit(String line) {
        log.append(line).append('\n');
        if (log.length() > (1 << 15)) flush();
    }

    private void flush() {
        digest.update(log.toString().getBytes(StandardCharsets.UTF_8));
        if (trace) System.out.print(log);
        log.setLength(0);
    }

    private static String text(SplittableRandom random, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        return new String(chars);
    }

    private static int cancel(SplittableRandom random) {
        return switch (random.nextInt(4)) {
            case 0, 1 -> -1;                       // null monitor
            case 2 -> Integer.MAX_VALUE;           // counting monitor, never cancels
            default -> random.nextInt(1, 400);     // cancels at the n-th check
        };
    }

    private void episode(SplittableRandom random, int episode) {
        int patternLength = random.nextInt(40) == 0 ? 1100 + random.nextInt(50) : random.nextInt(41);
        boolean flag = random.nextBoolean();
        side.compile(text(random, patternLength), flag);
        emit("E " + episode + " p=" + patternLength + " flag=" + flag + " " + side.flags());
        int frontiers = 1 + random.nextInt(2);
        for (int i = 0; i < frontiers; i++) {
            String admitted;
            if (random.nextInt(3) != 0) {
                admitted = side.frontier(-2, 0L, 0L, cancel(random));
            } else {
                int maxInput = random.nextInt(12) == 0 ? Integer.MAX_VALUE - random.nextInt(2) : random.nextInt(-1, 200);
                long maxCells = random.nextInt(12) == 0 ? -1L : random.nextLong(0L, 6000L);
                long maxBytes = random.nextInt(12) == 0 ? -1L : random.nextLong(0L, 3000L);
                admitted = side.frontier(maxInput, maxCells, maxBytes, cancel(random));
            }
            emit(" F " + admitted + " m=" + side.lastMonitor());
        }
        if (side.slots() == 0) return;
        int operations = 4 + random.nextInt(16);
        for (int op = 0; op < operations; op++) {
            int from = random.nextInt(side.slots());
            switch (random.nextInt(7)) {
                case 0, 1 -> {
                    int length = random.nextInt(60) == 0 ? 1030 + random.nextInt(40) : random.nextInt(50);
                    String value = random.nextInt(50) == 0 ? null : text(random, length);
                    emit(" a " + from + " " + length + " " + side.append(from, value, cancel(random)) + " m="
                            + side.lastMonitor());
                }
                case 2 -> {
                    int length = random.nextInt(50);
                    String value = text(random, length);
                    int begin = random.nextInt(-1, length + 2);
                    int end = random.nextInt(-1, length + 2);
                    emit(" r " + from + " " + begin + ".." + end + "/" + length + " "
                            + side.appendRange(from, value, begin, end, cancel(random)) + " m=" + side.lastMonitor());
                }
                case 3 -> emit(" d " + from + " " + side.describe(from));
                case 4 -> {
                    int remaining = random.nextInt(8) == 0 ? -1 : random.nextInt(0, 60);
                    emit(" n " + from + " " + remaining + " " + side.minimum(from, remaining));
                }
                default -> {
                    int maximum = random.nextInt(8) == 0 ? -1 : random.nextInt(0, 50);
                    emit(" w " + from + " " + maximum + " " + side.within(from, maximum));
                }
            }
        }
        StringBuilder all = new StringBuilder(" S");
        for (int slot = 0; slot < side.slots(); slot++) all.append(" [").append(side.describe(slot)).append(']');
        emit(all.toString());
    }
}
