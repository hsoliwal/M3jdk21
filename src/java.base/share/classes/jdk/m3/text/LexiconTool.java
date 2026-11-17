/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.nio.file.*;

/**
 * Offline builder and explicit mapped-image diagnostic; never used by String.
 * {@code build INPUT OUTPUT} writes one v1 image from sorted lines. {@code build --frequency}
 * writes a v2 catalog export directory from {@code WORD TAB COUNT} lines, ranked by count.
 */
public final class LexiconTool {
    private LexiconTool() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("build")) {
            SharedLexiconImage.create(Path.of(args[2]), Files.readAllLines(Path.of(args[1])));
        } else if (args.length == 6 && args[0].equals("build") && args[1].equals("--frequency")) {
            SharedLexiconExport.writeFrequency(args[2], args[3], Path.of(args[4]),
                    Path.of(args[5]));
        } else if (args.length == 3 && args[0].equals("map")) {
            SharedLexiconImage image = SharedLexiconImage.open(Path.of(args[1]));image.warm();
            System.out.println("pid=" + ProcessHandle.current().pid() + " image=" + image.imageIdentity()
                    + " records=" + image.size() + " bytes=" + image.mappedBytes());
            System.out.flush();Thread.sleep(Long.parseLong(args[2]));
            java.lang.ref.Reference.reachabilityFence(image);
        } else throw new IllegalArgumentException("build INPUT OUTPUT | build --frequency"
                + " SOURCE_ID LANGUAGE_TAG INPUT DIRECTORY | map IMAGE HOLD_MILLIS");
    }
}
