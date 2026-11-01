// SPDX-License-Identifier: Apache-2.0
package org.apache.lucene.analysis;

import java.util.Arrays;
import java.util.Random;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Same unmodified probe runs against frozen original and recipe output in separate JVMs. */
public final class M3ReaderBaselineCheck {
    private M3ReaderBaselineCheck() { }
    public static void main(String[] args) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        var random = new Random(20261014);
        var reader = new ReusableStringReader();
        int observations = 0;
        for (int round = 0; round < 10000; round++) {
            String value = round % 13 == 0 ? "" : "ab\uD83D\uDE00\uD800é\u0000\n".repeat(1 + round % 7);
            reader.setValue(value);
            for (int step = 0; step < 16; step++) {
                char[] target = new char[12];
                Arrays.fill(target, '_');
                Object result;
                try {
                    switch (random.nextInt(6)) {
                        case 0 -> result = reader.read();
                        case 1 -> result = reader.read(target, random.nextInt(18) - 3, random.nextInt(18) - 3);
                        case 2 -> { reader.close(); result = reader.read(); }
                        case 3 -> { reader.setValue((String) null); result = "unexpected"; }
                        case 4 -> { reader.setValue(value); result = reader.read(target, 2, 4); }
                        default -> result = reader.read(null, 0, 0);
                    }
                } catch (RuntimeException failure) {
                    result = failure.getClass().getName() + ":" + failure.getMessage();
                }
                digest.update((result + ":" + Arrays.toString(target) + "\n").getBytes(StandardCharsets.UTF_8));
                observations++;
            }
        }
        System.out.println("READER_BASELINE observations=" + observations + " sha256=" + HexFormat.of().formatHex(digest.digest()));
    }
}
