/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/main/java/com/synexia/m3/cpugpu/Rv32iBulkExecutor.java; see PROVENANCE.md. */
package com.m3.vcpu;

import com.m3.parallel.Backend;
import com.m3.parallel.IntBuffer;
import com.m3.parallel.Plan;
import com.m3.parallel.Verified;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Bulk execution facade over the CPU oracle and the m3.parallel RV32IM kernel.
 *
 * <p>The same guest program/state is applied over many independent virtual cores. CPU mode is the
 * deterministic reference. Accelerated mode is admitted only after full registers/state/RAM parity
 * with an independently executed CPU copy.</p>
 *
 * <p>The receipt and CPU-only parity domain strings are kept byte-for-byte from cpu-gpu so that CPU receipts are
 * identical to the donor's.</p>
 */
public final class Rv32iBulkExecutor {
    private static final String RECEIPT_DOMAIN = "M3_RV32I_BULK_RECEIPT_V1";
    private static final String VERIFY_DOMAIN = "M3_RV32I_CPU_TORNADO_PARITY_V1";
    private static final String PARALLEL_VERIFY_DOMAIN = "M3_RV32I_CPU_PARALLEL_PARITY_V1";

    private final CpuRv32iEngine cpu = new CpuRv32iEngine();

    /** Creates an executor. */
    public Rv32iBulkExecutor() {
    }

    public Rv32iBulkReceipt executeCpu(
            Rv32iBatch batch,
            int instructionBudget) {
        Objects.requireNonNull(batch, "batch");
        String inputRoot = stateRoot(batch);
        cpu.runSlice(batch, instructionBudget);
        String outputRoot = stateRoot(batch);
        return new Rv32iBulkReceipt(
                Rv32iBulkReceipt.Mode.CPU,
                batch.coreCount(),
                instructionBudget,
                0,
                inputRoot,
                outputRoot,
                sha256(VERIFY_DOMAIN, "CPU_ONLY", outputRoot),
                null);
    }

    /**
     * Runs one slice on an m3.parallel backend and admits it only after full parity with both the CPU reference
     * backend ({@link Verified}) and the sequential CPU oracle ({@link CpuRv32iEngine}).
     *
     * @param batch             batch, updated in place with the verified result
     * @param instructionBudget per-core instruction budget, {@code >= 1}
     * @param backend           executing backend
     * @param localWork         work-group size, {@code >= 1}
     * @return a {@link Rv32iBulkReceipt.Mode#PARALLEL_VERIFIED} receipt
     * @throws IllegalStateException if any register, state word or RAM word differs
     */
    public Rv32iBulkReceipt executeVerified(
            Rv32iBatch batch,
            int instructionBudget,
            Backend backend,
            int localWork) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(backend, "backend");
        if (localWork < 1) {
            throw new IllegalArgumentException("localWork");
        }
        if (instructionBudget <= 0) {
            throw new IllegalArgumentException("instructionBudget must be > 0");
        }

        String inputRoot = stateRoot(batch);
        Rv32iBatch expected = batch.copy();
        cpu.runSlice(expected, instructionBudget);

        Verified parity = runVerified(batch, instructionBudget, backend, localWork);

        requireEqual("registers", expected.snapshotRegisters(), batch.snapshotRegisters());
        requireEqual("state", expected.snapshotState(), batch.snapshotState());
        requireEqual("memory", expected.snapshotMemory(), batch.snapshotMemory());

        String outputRoot = stateRoot(batch);
        String verificationRoot =
                sha256(
                        PARALLEL_VERIFY_DOMAIN,
                        backend.id(),
                        parity.root(),
                        stateRoot(expected),
                        outputRoot,
                        Integer.toString(batch.coreCount()),
                        Integer.toString(instructionBudget));
        return new Rv32iBulkReceipt(
                Rv32iBulkReceipt.Mode.PARALLEL_VERIFIED,
                batch.coreCount(),
                instructionBudget,
                localWork,
                inputRoot,
                outputRoot,
                verificationRoot,
                null);
    }

    private static Verified runVerified(Rv32iBatch batch, int instructionBudget, Backend backend, int localWork) {
        IntBuffer memory = IntBuffer.of(batch.rawMemory());
        IntBuffer registers = IntBuffer.of(batch.rawRegisters());
        IntBuffer state = IntBuffer.of(batch.rawState());
        IntBuffer config = IntBuffer.of(batch.coreCount(), batch.memoryWordsPerCore(), instructionBudget);
        Verified parity;
        try (Plan plan = Rv32iSession.plan(backend, batch.coreCount(), localWork, memory, registers, state, config)) {
            parity = Verified.run(plan);
        }
        batch.replaceMemory(memory.toArray());
        batch.replaceRegistersAndState(registers.toArray(), state.toArray());
        return parity;
    }

    static String stateRoot(Rv32iBatch batch) {
        MessageDigest digest = digest();
        frame(digest, "M3_RV32I_BATCH_STATE_V1");
        frame(digest, Integer.toString(batch.coreCount()));
        frame(digest, Integer.toString(batch.memoryBytesPerCore()));
        ints(digest, batch.snapshotRegisters());
        ints(digest, batch.snapshotState());
        ints(digest, batch.snapshotMemory());
        return HexFormat.of().formatHex(digest.digest());
    }

    static String receiptRoot(
            Rv32iBulkReceipt.Mode mode,
            int cores,
            int instructionBudget,
            int localWork,
            String inputRoot,
            String outputRoot,
            String verificationRoot) {
        return sha256(
                RECEIPT_DOMAIN,
                mode.name(),
                Integer.toString(cores),
                Integer.toString(instructionBudget),
                Integer.toString(localWork),
                inputRoot,
                outputRoot,
                verificationRoot);
    }

    static String requireSha256(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static void requireEqual(
            String name,
            int[] expected,
            int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new IllegalStateException("CPU/parallel mismatch in " + name);
        }
    }

    private static String sha256(String domain, String... values) {
        MessageDigest digest = digest();
        frame(digest, domain);
        for (String value : values) frame(digest, value);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static void ints(MessageDigest digest, int[] values) {
        frame(digest, Integer.toString(values.length));
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 4096);
        for (int value : values) {
            if (buffer.remaining() < Integer.BYTES) {
                digest.update(buffer.array(), 0, buffer.position());
                buffer.clear();
            }
            buffer.putInt(value);
        }
        if (buffer.position() > 0) {
            digest.update(buffer.array(), 0, buffer.position());
        }
    }
}
