// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Seeded differential of census3 P3-007 (m3/arrays): donor MIndexStringTypeBits / MIndexArgumentRoleBits against the
 * owner with the received atoms. Usage: java census3.Census3BitsDifferential donor|owner cases. Prints the case count
 * and a SHA-256 over the canonical log (names, has, hasAny, requireKnown results and refusal classes). Exception
 * messages are not compared: the owner message carries the M3 type name (M3String for MIndexString).
 */
public final class Census3BitsDifferential {
    private static final long SEED = 0x43334249545330L; // "C3BITS0"

    /** One implementation side. */
    public interface Side {
        int numeric();
        int typeKnown();
        List<String> typeNames(int bits);
        boolean typeHas(int bits, int required);
        boolean typeHasAny(int bits, int mask);
        int roleKnown();
        List<String> roleNames(int bits);
        boolean roleHas(int bits, int required);
        boolean roleHasAny(int bits, int mask);
        void roleRequireKnown(int bits);
    }

    private interface Call {
        Object run();
    }

    public static void main(String[] args) throws Exception {
        Side side = (Side) Class.forName(args[0].equals("donor") ? "census3.DonorBits" : "census3.OwnerBits")
                .getDeclaredConstructor().newInstance();
        int cases = Integer.parseInt(args[1]);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder log = new StringBuilder(1 << 16);
        SplittableRandom random = new SplittableRandom(SEED);
        log.append("NUMERIC ").append(side.numeric()).append(" TYPE_KNOWN ").append(side.typeKnown())
                .append(" ROLE_KNOWN ").append(side.roleKnown()).append('\n');
        for (int i = 0; i < cases; i++) {
            int a = bits(random, 13);
            int b = bits(random, 13);
            int c = bits(random, 11);
            int d = bits(random, 11);
            log.append(i).append(" t ").append(a).append(' ').append(b)
                    .append(' ').append(eval(() -> side.typeNames(a)))
                    .append(' ').append(eval(() -> side.typeHas(a, b)))
                    .append(' ').append(eval(() -> side.typeHasAny(a, b)))
                    .append(" r ").append(c).append(' ').append(d)
                    .append(' ').append(eval(() -> side.roleNames(c)))
                    .append(' ').append(eval(() -> side.roleHas(c, d)))
                    .append(' ').append(eval(() -> side.roleHasAny(c, d)))
                    .append(' ').append(eval(() -> {
                        side.roleRequireKnown(d);
                        return "known";
                    })).append('\n');
            if (log.length() > (1 << 15)) {
                digest.update(log.toString().getBytes(StandardCharsets.UTF_8));
                log.setLength(0);
            }
        }
        digest.update(log.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("census3-bits cases=" + cases + " sha256=" + HexFormat.of().formatHex(digest.digest()));
    }

    /** Mostly known bits; 1 in 8 values carries an unknown bit above the known mask or a negative sign. */
    private static int bits(SplittableRandom random, int width) {
        int value = random.nextInt(1 << width);
        return switch (random.nextInt(16)) {
            case 0 -> value | (1 << (width + random.nextInt(31 - width)));
            case 1 -> value | Integer.MIN_VALUE;
            case 2 -> 0;
            case 3 -> (1 << width) - 1;
            default -> value;
        };
    }

    private static String eval(Call call) {
        try {
            Object value = call.run();
            return String.valueOf(value);
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }
}
