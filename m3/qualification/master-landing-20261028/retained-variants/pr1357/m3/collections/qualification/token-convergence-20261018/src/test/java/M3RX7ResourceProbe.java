// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import jdk.internal.mindex.M3RX7;

public class M3RX7ResourceProbe {
    private static volatile M3RX7.TokenIndex sink;

    public static void main(String[] args) {
        int states = 64, tokens = 1025;
        short[] transitions = new short[states * 128];
        byte[] accepting = new byte[states];
        Arrays.fill(accepting, (byte) 1);
        for (int i = 0; i < transitions.length; i++) transitions[i] = (short) ((i + 1) % states);
        M3RX7 owner = M3RX7.fromAsciiDfa("0123456789abcdef".repeat(4), 0, transitions, accepting);
        byte[] payload = new byte[]{'a'};
        int[] offsets = new int[tokens], lengths = new int[tokens];
        Arrays.fill(lengths, 1);
        for (int i = 0; i < 200; i++) sink = owner.prepareTokenIndex(payload, offsets, lengths);
        com.sun.management.ThreadMXBean thread = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!thread.isThreadAllocatedMemorySupported() || !thread.isCurrentThreadCpuTimeSupported()) {
            throw new AssertionError("required resource counters unavailable");
        }
        thread.setThreadAllocatedMemoryEnabled(true);
        thread.setThreadCpuTimeEnabled(true);
        long id = Thread.currentThread().threadId();
        long bytes = thread.getThreadAllocatedBytes(id), cpu = thread.getCurrentThreadCpuTime();
        long wall = System.nanoTime();
        for (int i = 0; i < 1000; i++) sink = owner.prepareTokenIndex(payload, offsets, lengths);
        wall = System.nanoTime() - wall;
        cpu = thread.getCurrentThreadCpuTime() - cpu;
        bytes = thread.getThreadAllocatedBytes(id) - bytes;
        if (sink == null || sink.allowedMaskWord(0, 16) != 1L) throw new AssertionError("result");
        System.out.println("preparations=1000 states=" + states + " tokens=" + tokens
                + " cpuNs=" + cpu + " wallNs=" + wall + " allocatedBytes=" + bytes
                + " retainedPrimitiveBytes=" + sink.retainedPrimitiveBytes());
    }
}
