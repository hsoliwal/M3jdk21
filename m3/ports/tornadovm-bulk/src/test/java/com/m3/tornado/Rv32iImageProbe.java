/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.tornado;

import com.synexia.m3.cpugpu.CpuRv32iEngine;
import com.synexia.m3.cpugpu.Rv32i;
import com.synexia.m3.cpugpu.Rv32iAssembler;
import com.synexia.m3.cpugpu.Rv32iBatch;
import com.synexia.m3.cpugpu.Rv32iBulkExecutor;
import com.synexia.m3.cpugpu.Rv32iBulkReceipt;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;
import uk.ac.manchester.tornado.api.KernelContext;

/** Executable receiver contracts for a named module and the installed product image. */
public final class Rv32iImageProbe {
    private static int checks;

    private Rv32iImageProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1 || !(arguments[0].equals("cpu") || arguments[0].equals("gpu"))) {
            throw new IllegalArgumentException("expected exactly one mode: cpu or gpu");
        }
        modules();
        cpuAdapter();
        guestMemory();
        boundedSlicesAndTraps();
        refusals();
        System.out.println("M3_RV32IM_IMAGE_CPU_PASS checks=" + checks);
        if (arguments[0].equals("gpu")) {
            requiredAccelerator();
            System.out.println("M3_RV32IM_IMAGE_GPU_PASS checks=" + checks);
        }
    }

    private static void modules() throws ClassNotFoundException {
        Module receiver = Rv32iBulkExecutorAdapter.class.getModule();
        check(receiver.isNamed() && "jdk.m3.tornado".equals(receiver.getName()), "receiver module");
        check(Rv32iBatch.class.getModule() == receiver, "compatibility type in receiver module");
        check(Class.forName("com.synexia.m3.cpugpu.Rv32iBatch") == Rv32iBatch.class,
                "legacy binary class identity");
        check("java.base".equals(BulkTask.class.getModule().getName()), "existing java.base contract");
        check("tornado.api".equals(KernelContext.class.getModule().getName()), "named Tornado API");
        check(receiver.canRead(KernelContext.class.getModule()), "Tornado API readability");
        check(receiver.isExported("com.m3.tornado"), "target compatibility export");
        check(receiver.isExported("com.synexia.m3.cpugpu"), "legacy compatibility export");
    }

    private static void cpuAdapter() {
        var cpu = new Rv32iBulkExecutorAdapter(Rv32iBulkExecutorAdapter.Mode.CPU);
        var tornado = new Rv32iBulkExecutorAdapter(Rv32iBulkExecutorAdapter.Mode.TORNADO_VERIFIED);
        BulkScheduler scheduler = new BulkScheduler(List.of(cpu, tornado));
        check(scheduler.providerIds().equals(List.of("tornado-rv32im", "cpu-rv32im")),
                "deterministic provider precedence");
        for (int cores : new int[] {1, 3, 4, 63, 64, 65}) {
            Rv32iBatch input = summation(cores);
            Rv32iBatch before = input.copy();
            check(scheduler.select(task(cores, 0)) == cpu, "CPU selection");
            check(scheduler.select(task(cores, Math.min(cores, 64))) == tornado, "Tornado selection");
            BulkExecution first = scheduler.execute(task(cores, 0), values(input, 1024));
            BulkExecution repeated = scheduler.execute(task(cores, 0), values(input, 1024));
            check(first.receiptRoot().equals(repeated.receiptRoot()), "deterministic adapter receipt");
            check(first.receiptRoot().matches("[0-9a-f]{64}"), "receipt encoding");
            check(first.provider().equals("cpu-rv32im"), "actual CPU provider identity");
            Rv32iBatch output = (Rv32iBatch) first.output("batch");
            check(output != input && output.getClass() == Rv32iBatch.class, "original output type and isolation");
            // BulkExecution's established non-array output contract preserves this reference.
            check(first.outputs().get("batch") == output, "non-array result reference contract");
            equal(before, input, "caller input preserved");
            for (int core = 0; core < cores; core++) {
                check(output.status(core) == Rv32i.STATUS_HALTED, "halted guest");
                check(output.exitCode(core) == 100 * 101 / 2, "summation oracle");
                check(output.register(core, 0) == 0, "hard-wired zero register");
                check(output.retiredInstructions(core) == 305, "retired instruction count");
            }
            Rv32iBatch direct = input.copy();
            Rv32iBulkReceipt receipt = new Rv32iBulkExecutor().executeCpu(direct, 1024);
            check(receipt.mode() == Rv32iBulkReceipt.Mode.CPU, "direct receipt mode");
            check(first.receiptRoot().equals(receipt.root()), "adapter/direct receipt identity");
            equal(direct, output, "adapter/direct full state");
        }
    }

    private static void guestMemory() {
        Rv32iBatch input = new Rv32iBatch(4, 256);
        input.loadWordsAllCores(0, Rv32iAssembler.sw(1, 0, 128),
                Rv32iAssembler.lw(10, 0, 128), Rv32iAssembler.halt());
        for (int core = 0; core < 4; core++) {
            input.setRegister(core, 1, 101 + core);
            input.setProgramCounter(core, 0);
        }
        Rv32iBatch before = input.copy();
        Rv32iBatch output = (Rv32iBatch) new Rv32iBulkExecutorAdapter(Rv32iBulkExecutorAdapter.Mode.CPU)
                .execute(task(4, 0), values(input, 16)).output("batch");
        equal(before, input, "memory-writing guest preserves caller");
        for (int core = 0; core < 4; core++) {
            check(output.readWord(core, 128) == 101 + core, "private RAM slice");
            check(output.exitCode(core) == 101 + core, "guest memory result");
            check(input.readWord(core, 128) == 0, "input RAM unchanged");
        }
    }

    private static void boundedSlicesAndTraps() {
        CpuRv32iEngine cpu = new CpuRv32iEngine();
        Rv32iBatch partial = summation(1);
        cpu.runSlice(partial, 7);
        check(partial.status(0) == Rv32i.STATUS_YIELDED, "bounded slice yields");
        check(partial.retiredInstructions(0) == 7, "exact slice budget");
        cpu.runSlice(partial, 1024);
        check(partial.exitCode(0) == 5050 && partial.retiredInstructions(0) == 305, "slice resume");
        Rv32iBatch halted = partial.copy();
        cpu.runSlice(partial, 1);
        equal(halted, partial, "halted core remains terminal");

        Rv32iBatch illegal = program(0xffff_ffff);
        cpu.runSlice(illegal, 10);
        check(illegal.status(0) == Rv32i.STATUS_TRAPPED, "illegal trap status");
        check(illegal.trapCause(0) == Rv32i.TRAP_ILLEGAL_INSTRUCTION, "illegal trap cause");
        check(illegal.retiredInstructions(0) == 0, "faulting instruction does not retire");
        Rv32iBatch trapped = illegal.copy();
        cpu.runSlice(illegal, 1);
        equal(trapped, illegal, "trapped core remains terminal");

        Rv32iBatch misaligned = program(Rv32iAssembler.addi(1, 0, 1), Rv32iAssembler.lw(2, 1, 0));
        cpu.runSlice(misaligned, 10);
        check(misaligned.trapCause(0) == Rv32i.TRAP_LOAD_ADDRESS_MISALIGNED, "unaligned load trap");
        check(misaligned.trapValue(0) == 1 && misaligned.retiredInstructions(0) == 1, "trap address and retirement");

        Rv32iBatch division = program(Rv32iAssembler.div(3, 1, 2), Rv32iAssembler.rem(4, 1, 2),
                Rv32iAssembler.div(5, 1, 0), Rv32iAssembler.rem(6, 1, 0), Rv32iAssembler.halt());
        division.setRegister(0, 1, Integer.MIN_VALUE);
        division.setRegister(0, 2, -1);
        cpu.runSlice(division, 10);
        check(division.register(0, 3) == Integer.MIN_VALUE && division.register(0, 4) == 0,
                "signed overflow ISA result");
        check(division.register(0, 5) == -1 && division.register(0, 6) == Integer.MIN_VALUE,
                "division by zero ISA result");
    }

    private static void refusals() {
        var adapter = new Rv32iBulkExecutorAdapter(Rv32iBulkExecutorAdapter.Mode.CPU);
        Rv32iBatch input = summation(1);
        Rv32iBatch before = input.copy();
        rejects(IllegalArgumentException.class, () -> adapter.execute(task(1, 0), values(input, 0)));
        rejects(IllegalArgumentException.class, () -> adapter.execute(task(2, 0), values(input, 1)));
        rejects(IllegalArgumentException.class, () -> adapter.execute(task(1, 1), values(input, 1)));
        rejects(IllegalArgumentException.class, () -> adapter.execute(task(1, 0), Map.of(
                "batch", new Object(), "instructionBudget", 1)));
        rejects(IllegalArgumentException.class, () -> adapter.execute(task(1, 0), Map.of(
                "batch", input, "instructionBudget", 1, "extra", 1)));
        rejects(IllegalArgumentException.class, () -> new BulkTask("other", 1, 2,
                List.of("batch", "instructionBudget"), List.of("batch")));
        rejects(IllegalArgumentException.class, () -> new BulkScheduler(List.of(adapter, adapter)));
        equal(before, input, "all rejected calls preserve caller input");
        Rv32iBulkReceipt receipt = new Rv32iBulkExecutor().executeCpu(input.copy(), 1024);
        rejects(IllegalArgumentException.class, () -> new Rv32iBulkReceipt(receipt.mode(), receipt.cores(),
                receipt.instructionBudget(), receipt.localWork(), receipt.inputRoot(), receipt.outputRoot(),
                receipt.verificationRoot(), "0".repeat(64)));
    }

    private static void requiredAccelerator() {
        var adapter = new Rv32iBulkExecutorAdapter(Rv32iBulkExecutorAdapter.Mode.TORNADO_VERIFIED);
        for (int cores : new int[] {1, 3, 64, 65}) {
            Rv32iBatch input = summation(cores);
            Rv32iBatch before = input.copy();
            Rv32iBatch expected = input.copy();
            new CpuRv32iEngine().runSlice(expected, 1024);
            BulkExecution execution = adapter.execute(task(cores, Math.min(cores, 64)), values(input, 1024));
            check("tornado-rv32im".equals(execution.provider()), "accelerator provider identity");
            check(execution.receiptRoot().matches("[0-9a-f]{64}"), "accelerator receipt");
            equal(expected, (Rv32iBatch) execution.output("batch"), "accelerator full CPU parity");
            equal(before, input, "accelerator preserves caller input");
        }
    }

    private static Rv32iBatch summation(int cores) {
        Rv32iBatch batch = new Rv32iBatch(cores, 256);
        batch.loadWordsAllCores(0, Rv32iAssembler.addi(1, 0, 0), Rv32iAssembler.addi(2, 0, 1),
                Rv32iAssembler.addi(3, 0, 101), Rv32iAssembler.add(1, 1, 2),
                Rv32iAssembler.addi(2, 2, 1), Rv32iAssembler.blt(2, 3, -8),
                Rv32iAssembler.addi(10, 1, 0), Rv32iAssembler.halt());
        for (int core = 0; core < cores; core++) batch.setProgramCounter(core, 0);
        return batch;
    }

    private static Rv32iBatch program(int... words) {
        Rv32iBatch batch = new Rv32iBatch(1, 256);
        batch.loadWords(0, 0, words);
        batch.setProgramCounter(0, 0);
        return batch;
    }

    private static BulkTask task(int cores, int localWork) {
        return new BulkTask("m3.rv32im.slice", cores, localWork,
                List.of("batch", "instructionBudget"), List.of("batch"));
    }

    private static Map<String, Object> values(Rv32iBatch batch, int budget) {
        return Map.of("batch", batch, "instructionBudget", budget);
    }

    private static void equal(Rv32iBatch expected, Rv32iBatch actual, String message) {
        check(expected.coreCount() == actual.coreCount(), message + " shape");
        check(Arrays.equals(expected.snapshotRegisters(), actual.snapshotRegisters()), message + " registers");
        check(Arrays.equals(expected.snapshotState(), actual.snapshotState()), message + " state");
        check(Arrays.equals(expected.snapshotMemory(), actual.snapshotMemory()), message + " memory");
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", observed " + failure);
            return;
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
