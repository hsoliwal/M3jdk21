// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import jdk.internal.mindex.M3TokenRegexPlan;

public class M3TokenResourceProbe {
    private static volatile boolean sink;
    public static void main(String[] args) {
        Object domain = new Object();
        var atoms = new M3TokenRegexPlan.Predicate[63];
        Arrays.fill(atoms, new M3TokenRegexPlan.Predicate(0, 0, 0, 0, 0));
        var plan = M3TokenRegexPlan.compile(domain, atoms, Long.MAX_VALUE);
        var input = M3TokenRegexPlan.Input.of(domain, new int[256], new long[256], new long[256]);
        for (int i = 0; i < 2000; i++) sink = plan.matches(input);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported() || !bean.isCurrentThreadCpuTimeSupported()) throw new AssertionError("counters");
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        long thread = Thread.currentThread().threadId();
        bean.getThreadAllocatedBytes(thread);
        long allocation = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime();
        for (int i = 0; i < 10000; i++) sink = plan.matches(input);
        cpu = bean.getCurrentThreadCpuTime() - cpu;
        allocation = bean.getThreadAllocatedBytes(thread) - allocation;
        if (!sink) throw new AssertionError("matching result");
        System.out.println("matches=10000 tokensPerInput=256 predicates=63 cpuNs=" + cpu
                + " allocatedBytes=" + allocation + " inputPrimitivePayloadBytes=5120");
    }
}
