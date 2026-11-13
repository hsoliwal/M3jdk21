// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.apache.commons.codec.digest.PureJavaCrc32C;

/**
 * CHECKSUMS_TO_JDK v1 differential: run with commons-codec before the overlay (Hadoop's tables) and after (java.base).
 * Each sample drives PureJavaCrc32 and PureJavaCrc32C, and java.util.zip.CRC32 and CRC32C as the reference, through the
 * same random calls: update(byte[], off, len) over random chunks, update(int) with arbitrary ints, reset, getValue.
 * A quarter of the samples also make one invalid update(byte[], off, len) call. One line per sample; the before and
 * after runs are compared line by line. A line ends in "jdk=same" when the codec values equal the reference values.
 */
public final class CrcDifferential {
    public static void main(String[] args) {
        SplittableRandom random = new SplittableRandom(Long.parseLong(args[0]));
        long count = Long.parseLong(args[1]);
        StringBuilder line = new StringBuilder(256);
        for (long i = 0; i < count; i++) {
            long script = random.nextLong();
            String c32 = run(PureJavaCrc32::new, script), j32 = run(CRC32::new, script);
            String c32c = run(PureJavaCrc32C::new, script), j32c = run(CRC32C::new, script);
            line.append(i).append(" crc32=").append(c32).append(" crc32c=").append(c32c)
                    .append(c32.equals(j32) && c32c.equals(j32c) ? " jdk=same" : " jdk=DIFFERENT " + j32 + ' ' + j32c);
            System.out.println(line);
            line.setLength(0);
        }
    }

    /** Replays one random call script (seeded by {@code script}) on a fresh checksum; returns the observed values. */
    private static String run(Supplier<Checksum> factory, long script) {
        SplittableRandom random = new SplittableRandom(script);
        Checksum checksum = factory.get();
        StringBuilder seen = new StringBuilder(64);
        int length = random.nextInt(8) == 0 ? random.nextInt(70_000) : random.nextInt(300);
        byte[] data = new byte[length + 16];
        random.nextBytes(data);
        int position = random.nextInt(17);
        while (position < length) {
            int chunk = random.nextInt(4) == 0 ? 1 : 1 + random.nextInt(Math.max(1, Math.min(length - position, 9_000)));
            chunk = Math.min(chunk, length - position);
            if (chunk == 1 && random.nextBoolean()) {
                checksum.update(random.nextInt());  // only the low 8 bits count
            } else {
                checksum.update(data, position, chunk);
            }
            position += chunk;
            if (random.nextInt(64) == 0) {
                seen.append(Long.toHexString(checksum.getValue())).append('/');
                checksum.reset();
            }
        }
        if (random.nextInt(4) == 0) {
            int off = random.nextInt(-3, data.length + 3), len = random.nextInt(-3, data.length + 3);
            try {
                checksum.update(data, off, len);
                seen.append("bad(").append(off).append(',').append(len).append(")=ok/");
            } catch (RuntimeException e) {
                seen.append("bad(").append(off).append(',').append(len).append(")=")
                        .append(e.getClass().getSimpleName()).append('/');
            }
        }
        return seen.append(Long.toHexString(checksum.getValue())).toString();
    }
}
