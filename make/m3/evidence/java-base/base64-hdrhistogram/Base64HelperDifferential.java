// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package org.HdrHistogram;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * BASE64_TO_JDK v1 differential: run with HdrHistogram before the overlay (reflection) and after (java.util.Base64).
 * Per sample: Base64Helper.printBase64Binary of random bytes; parseBase64Binary of that text and of a corrupted copy
 * (a character replaced, padding cut, whitespace or a URL-safe character inserted); a random histogram written by
 * HistogramLogWriter, read back by HistogramLogReader, and decoded by Histogram.fromString. One line per sample; the
 * before and after runs are compared line by line. Failures print the exception class, not its message.
 */
public final class Base64HelperDifferential {
    private static final String NOISE = "=-_ \n\t*%";

    public static void main(String[] args) throws Exception {
        SplittableRandom random = new SplittableRandom(Long.parseLong(args[0]));
        long count = Long.parseLong(args[1]);
        HexFormat hex = HexFormat.of();
        for (long i = 0; i < count; i++) {
            byte[] data = new byte[random.nextInt(8) == 0 ? random.nextInt(20_000) : random.nextInt(64)];
            random.nextBytes(data);
            String text = Base64Helper.printBase64Binary(data);
            StringBuilder line = new StringBuilder(i + " " + text.length() + " " + text.hashCode());
            line.append(" round=").append(java.util.Arrays.equals(Base64Helper.parseBase64Binary(text), data));
            line.append(" corrupt=").append(decode(corrupt(text, random), hex));
            if (i % 50 == 0) {
                line.append(" log=").append(histogramRoundTrip(random));
            }
            System.out.println(line);
        }
        System.out.println("null=" + decode(null, hex));
    }

    private static String corrupt(String text, SplittableRandom random) {
        if (text.isEmpty()) {
            return String.valueOf(NOISE.charAt(random.nextInt(NOISE.length())));
        }
        StringBuilder b = new StringBuilder(text);
        int at = random.nextInt(b.length());
        switch (random.nextInt(4)) {
            case 0 -> b.setCharAt(at, NOISE.charAt(random.nextInt(NOISE.length())));
            case 1 -> b.setLength(Math.max(0, b.length() - 1 - random.nextInt(2)));
            case 2 -> b.insert(at, NOISE.charAt(random.nextInt(NOISE.length())));
            default -> b.setCharAt(at, (char) ('A' + random.nextInt(26)));
        }
        return b.toString();
    }

    private static String decode(String text, HexFormat hex) {
        try {
            byte[] bytes = Base64Helper.parseBase64Binary(text);
            return "ok:" + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + ":" + bytes.length;
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private static String histogramRoundTrip(SplittableRandom random) throws Exception {
        Histogram histogram = new Histogram(3600L * 1000 * 1000, 3);
        for (int v = random.nextInt(2_000); v > 0; v--) {
            histogram.recordValue(1 + random.nextLong(3600L * 1000 * 1000));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream print = new PrintStream(out, true, StandardCharsets.UTF_8)) {
            HistogramLogWriter writer = new HistogramLogWriter(print);
            writer.outputLegend();
            writer.outputIntervalHistogram(1.0, 2.0, histogram, 1.0);
        }
        String log = out.toString(StandardCharsets.UTF_8);
        HistogramLogReader reader = new HistogramLogReader(new ByteArrayInputStream(out.toByteArray()));
        Histogram back = (Histogram) reader.nextIntervalHistogram();
        String payload = log.substring(log.lastIndexOf(',') + 1).trim();
        Histogram parsed = Histogram.fromString(payload);
        return log.hashCode() + ":" + back.equals(histogram) + ":" + parsed.equals(histogram) + ":"
                + histogram.getTotalCount();
    }
}
