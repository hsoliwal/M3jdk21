// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.management.ManagementFactory;
import jdk.internal.mindex.M3TokenRegexPlan;

/** Fixed long-miss witness; comparative evidence, not a general benchmark claim. */
public final class M3TokenFindResourceProbe {
    public static void main(String[] args) {
        Object domain = new Object();
        var any = new M3TokenRegexPlan.Predicate(0, 0, 0, 0, 0);
        var absent = new M3TokenRegexPlan.Predicate(1, 0, 0, 0, M3TokenRegexPlan.KIND);
        var plan = M3TokenRegexPlan.compile(domain, new M3TokenRegexPlan.Predicate[]{any, absent}, 1);
        int count = 8000;
        var input = M3TokenRegexPlan.Input.of(domain, new int[count], new long[count], new long[count]);
        var small = input.slice(0, 40);
        for (int i = 0; i < 2000; i++) if (plan.find(small) != null) throw new AssertionError("warmup");
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        long allocated = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime();
        for (int i = 0; i < 3; i++) if (plan.find(input) != null) throw new AssertionError("unexpected match");
        cpu = bean.getCurrentThreadCpuTime() - cpu;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        System.out.println("FIND_MISS_PASS tokens=" + count + " repetitions=3 cpu_ns=" + cpu + " allocated_bytes=" + allocated);
    }
}
