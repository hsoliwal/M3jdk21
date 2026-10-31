/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.nio.file.*;

/** Offline builder and explicit mapped-image diagnostic; never used by String. */
public final class LexiconTool {
    private LexiconTool() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("build")) {
            SharedLexiconImage.create(Path.of(args[2]), Files.readAllLines(Path.of(args[1])));
        } else if (args.length == 3 && args[0].equals("map")) {
            SharedLexiconImage image = SharedLexiconImage.open(Path.of(args[1]));image.warm();
            System.out.println("pid=" + ProcessHandle.current().pid() + " image=" + image.imageIdentity()
                    + " records=" + image.size() + " bytes=" + image.mappedBytes());
            System.out.flush();Thread.sleep(Long.parseLong(args[2]));
            java.lang.ref.Reference.reachabilityFence(image);
        } else throw new IllegalArgumentException("build INPUT OUTPUT | map IMAGE HOLD_MILLIS");
    }
}
