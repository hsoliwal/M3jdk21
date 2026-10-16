/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/* @test
 * @summary Optional fixed-virtual-address M3 image mapping smoke probe
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3PrimitiveFixedArenaProbe
 */
import java.nio.file.Path;
import jdk.internal.mindex.M3PrimitiveFixedArena;

public class M3PrimitiveFixedArenaProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("M3_FIXED_ARENA_SKIP requires IMAGE LIBRARY");
            return;
        }
        M3PrimitiveFixedArena.loadForTest(Path.of(args[1]));
        try (M3PrimitiveFixedArena.Mapping mapping = M3PrimitiveFixedArena.open(
                Path.of(args[0]), 0x6000_0000_0000L)) {
            if (mapping == null) {
                System.out.println("M3_FIXED_ARENA_UNAVAILABLE");
            } else {
                if (mapping.addressOf(0) != mapping.baseAddress() + 64L)
                    throw new AssertionError("slot address mismatch");
                if (M3PrimitiveFixedArena.open(Path.of(args[0]), 0x6000_0000_0000L) != null)
                    throw new AssertionError("must not overwrite occupied virtual address");
                System.out.printf("M3_FIXED_ARENA_OK base=0x%x namespace=%s%n",
                        mapping.baseAddress(),
                        java.util.HexFormat.of().formatHex(mapping.namespace()));
            }
        }
    }
}
