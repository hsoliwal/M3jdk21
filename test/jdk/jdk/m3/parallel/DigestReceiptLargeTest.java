/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Verify streaming parallel receipt SHA-256 against an independent bounded oracle
 * @modules m3.parallel/jdk.m3.parallel
 * @run main/othervm -Xmx24m -XX:MaxDirectMemorySize=256m DigestReceiptLargeTest
 */

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.JavaKernel;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.Plan;
import jdk.m3.parallel.Transfer;
import jdk.m3.parallel.Verified;

/** External public API receipt oracle; never calls package-private Digests. */
public final class DigestReceiptLargeTest {
    private static final int HUGE = 8_000_000;
    private static final JavaKernel NOOP = (index, buffers) -> { };
    private static final KernelId KERNEL = JavaKernels.register(
            KernelId.of("test.m3.parallel.digest.receipt.bounded.v1"), NOOP);

    private DigestReceiptLargeTest() {
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(valueBytes.length).array());
        digest.update(valueBytes);
    }

    /** Independent 2048-integer scalar big-endian cursor, without toArray or a full heap snapshot. */
    private static String referenceRoot(IntBuffer source, Transfer transfer) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        frame(digest, "M3_PARALLEL_BINDINGS_V1");
        frame(digest, "1");
        frame(digest, transfer.name());
        frame(digest, Integer.toString(source.length()));
        byte[] chunk = new byte[2048 * Integer.BYTES];
        for (int start = 0; start < source.length();) {
            int count = Math.min(chunk.length / Integer.BYTES, source.length() - start);
            for (int index = 0; index < count; index++) {
                int value = source.get(start + index);
                int offset = index * Integer.BYTES;
                chunk[offset] = (byte) (value >>> 24);
                chunk[offset + 1] = (byte) (value >>> 16);
                chunk[offset + 2] = (byte) (value >>> 8);
                chunk[offset + 3] = (byte) value;
            }
            digest.update(chunk, 0, count * Integer.BYTES);
            start += count;
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void exercise(int count, Transfer transfer) throws Exception {
        IntBuffer source = IntBuffer.allocate(count);
        for (int i = 0; i < count; i++) {
            source.set(i, (i * 31) ^ (i >>> 7) ^ 0x80000001);
        }
        String expectedInput = referenceRoot(source, transfer);
        try (Plan plan = Plan.builder(Backend.reference()).kernel(KERNEL).grid(Grid1D.of(1, 1))
                .bind(source, transfer).build()) {
            Verified receipt = Verified.run(plan);
            require(expectedInput.equals(receipt.inputRoot()), "input digest " + count + "/" + transfer);
            require(referenceRoot(source, transfer).equals(receipt.outputRoot()),
                    "output digest " + count + "/" + transfer);
            require(receipt.backend().equals(Backend.reference().id()), "CPU reference changed");
            require(receipt.root().length() == 64, "invalid verified receipt root");
            require(plan.session().launches() == 1, "not exactly one kernel launch");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments accepted");
        int scenarios = 0;
        for (int size : List.of(0, 1, 4095, 4096, 4097, 8193)) {
            for (Transfer transfer : Transfer.values()) {
                exercise(size, transfer);
                scenarios++;
            }
        }
        exercise(HUGE, Transfer.ONCE);
        scenarios++;
        require(scenarios == 19, "missing digest scenarios");
        System.out.println("M3_PARALLEL_RECEIPT_STREAM_PASS scenarios=19 large=" + HUGE);
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(Objects.requireNonNull(message, "message"));
    }
}
