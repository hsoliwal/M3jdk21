/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Raw identities for all eight primitives and compiler-prepared mapped images
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3PrimitiveIdentityTest
 */
import java.nio.file.Path;
import java.util.Arrays;
import jdk.internal.mindex.M3PrimitiveIdentity;
import jdk.internal.mindex.M3PrimitiveImage;

public class M3PrimitiveIdentityTest {
    private static long checks;
    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        var p = M3PrimitiveIdentity.class;
        check(M3PrimitiveIdentity.bits(true) == 1, "boolean true");
        check(M3PrimitiveIdentity.bits(false) == 0, "boolean false");
        for (int v = Byte.MIN_VALUE; v <= Byte.MAX_VALUE; ++v) {
            check(M3PrimitiveIdentity.valid(M3PrimitiveIdentity.BYTE,
                    M3PrimitiveIdentity.bits((byte) v)), "full byte domain");
        }
        for (int v = Character.MIN_VALUE; v <= Character.MAX_VALUE; ++v) {
            check(M3PrimitiveIdentity.valid(M3PrimitiveIdentity.CHAR,
                    M3PrimitiveIdentity.bits((char) v)), "full char domain");
        }
        for (int v = Short.MIN_VALUE; v <= Short.MAX_VALUE; ++v) {
            check(M3PrimitiveIdentity.valid(M3PrimitiveIdentity.SHORT,
                    M3PrimitiveIdentity.bits((short) v)), "full short domain");
        }
        check(M3PrimitiveIdentity.bits(Integer.MIN_VALUE) == 0x80000000L, "negative int");
        check(M3PrimitiveIdentity.bits(Long.MIN_VALUE) == Long.MIN_VALUE, "min long");
        check(M3PrimitiveIdentity.bits(-0.0f) != M3PrimitiveIdentity.bits(+0.0f), "float zero identity");
        check(M3PrimitiveIdentity.floatEquals(M3PrimitiveIdentity.bits(-0.0f),
                M3PrimitiveIdentity.bits(+0.0f)), "float zero ==");
        check(M3PrimitiveIdentity.bits(-0.0d) != M3PrimitiveIdentity.bits(+0.0d), "double zero identity");
        check(M3PrimitiveIdentity.doubleEquals(M3PrimitiveIdentity.bits(-0.0d),
                M3PrimitiveIdentity.bits(+0.0d)), "double zero ==");
        float nanA = Float.intBitsToFloat(0x7f800001);
        float nanB = Float.intBitsToFloat(0x7fc00001);
        check(!M3PrimitiveIdentity.floatEquals(M3PrimitiveIdentity.bits(nanA),
                M3PrimitiveIdentity.bits(nanB)), "NaN != NaN");
        check(M3PrimitiveIdentity.bits(nanA) != M3PrimitiveIdentity.bits(nanB), "preserve NaN payload");
        check(!M3PrimitiveIdentity.sameRaw(M3PrimitiveIdentity.FLOAT,
                M3PrimitiveIdentity.bits(1f), M3PrimitiveIdentity.INT, M3PrimitiveIdentity.bits(1)),
                "typed identities");
        check(!M3PrimitiveIdentity.valid(0, 0), "invalid kind");
        check(!M3PrimitiveIdentity.valid(M3PrimitiveIdentity.BYTE, 256), "invalid canonical bits");
        check(!M3PrimitiveIdentity.valid(M3PrimitiveIdentity.INT, -1L), "unsigned-32 canonical range");
        if (args.length > 0) {
            M3PrimitiveImage image = M3PrimitiveImage.open(Path.of(args[0]));
            M3PrimitiveImage second = M3PrimitiveImage.open(Path.of(args[0]));
            check(image.size() > 8, "compiled types");
            for (int index = 0; index < image.size(); index++) {
                check(image.find(image.kind(index), image.bits(index)) == index, "slot lookup");
                check(image.offset(index) == 64L + 16L * index, "stable offset");
                check(image.sameSlot(second, index, index), "equal namespace slots");
                if (index > 0) check(!image.sameSlot(second, index, index - 1), "different slot");
            }
            check(image.find(M3PrimitiveIdentity.BYTE, M3PrimitiveIdentity.bits((byte) -1)) >= 0,
                    "attributed byte constant");
            check(image.find(M3PrimitiveIdentity.SHORT, M3PrimitiveIdentity.bits((short) -2)) >= 0,
                    "attributed short constant");
            check(image.find(M3PrimitiveIdentity.CHAR, M3PrimitiveIdentity.bits('Z')) >= 0,
                    "attributed char constant");
            check(image.find(M3PrimitiveIdentity.FLOAT, M3PrimitiveIdentity.bits(-0.0f)) >= 0,
                    "literal float -0.0");
            check(image.find(M3PrimitiveIdentity.DOUBLE, M3PrimitiveIdentity.bits(-0.0d)) >= 0,
                    "literal double -0.0");
            System.out.println("M3_NAMESPACE=" + java.util.HexFormat.of().formatHex(image.namespace()));
        }
        System.out.println("M3_PRIMITIVE_TEST checks=" + checks);
    }
}
