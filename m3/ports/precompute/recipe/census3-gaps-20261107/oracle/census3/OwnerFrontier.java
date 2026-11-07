// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import jdk.m3.precompute.M3EditDistancePlan;
import jdk.m3.precompute.M3Progress;

/** Owner side: jdk.m3.precompute M3EditDistancePlan with the restored flag and Frontier members. */
public final class OwnerFrontier implements Census3FrontierDifferential.Side {
    private M3EditDistancePlan plan;
    private final List<M3EditDistancePlan.Frontier> slots = new ArrayList<>();
    private CountMonitor last;

    public void compile(String pattern, boolean flag) {
        plan = flag ? M3EditDistancePlan.compile(pattern, true) : M3EditDistancePlan.compile(pattern, false);
        slots.clear();
    }

    // the plan's own primitivePayloadBytes() is PRESENT_EVOLVED (the owner adds its Myers lanes), not compared
    public String flags() {
        return plan.usesUtf16CodeUnits() + "/" + plan.patternLength();
    }

    public String frontier(int maxInput, long maxCells, long maxBytes, int cancelAtCheck) {
        try {
            M3EditDistancePlan.FrontierLimits limits = maxInput == -2 ? M3EditDistancePlan.FrontierLimits.DEFAULT
                    : new M3EditDistancePlan.FrontierLimits(maxInput, maxCells, maxBytes);
            M3EditDistancePlan.Frontier frontier = cancelAtCheck == -1 && maxInput == -2 ? plan.frontier()
                    : plan.frontier(limits, monitor(cancelAtCheck));
            slots.add(frontier);
            return "ok" + slots.size();
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String append(int from, String value, int cancelAtCheck) {
        try {
            M3EditDistancePlan.Frontier next = cancelAtCheck == -1 ? slots.get(from).append(value)
                    : slots.get(from).append(value, monitor(cancelAtCheck));
            slots.add(next);
            return "ok" + slots.size() + (next == slots.get(from) ? "=" : "");
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String appendRange(int from, String value, int begin, int end, int cancelAtCheck) {
        try {
            M3EditDistancePlan.Frontier next = slots.get(from).append(value, begin, end, monitor(cancelAtCheck));
            slots.add(next);
            return "ok" + slots.size() + (next == slots.get(from) ? "=" : "");
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public int slots() {
        return slots.size();
    }

    public String describe(int slot) {
        M3EditDistancePlan.Frontier f = slots.get(slot);
        return f.distance() + " " + f.inputUnits() + " " + f.cellUpdates() + " " + f.primitivePayloadBytes() + " "
                + (f.plan() == plan) + " " + f.limits() + " " + Arrays.toString(f.rowCopy());
    }

    public String minimum(int slot, int remaining) {
        try {
            return Long.toString(slots.get(slot).minimumPossibleDistance(remaining));
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String within(int slot, int maximum) {
        try {
            return Boolean.toString(slots.get(slot).within(maximum));
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String lastMonitor() {
        return last == null ? "-" : last.work + "/" + last.checks + "/" + last.lifecycle;
    }

    private CountMonitor monitor(int cancelAtCheck) {
        last = cancelAtCheck == -1 ? null : new CountMonitor(cancelAtCheck);
        return last;
    }

    /** Cancels at the n-th check; counts worked units and lifecycle calls (the frontier makes none). */
    static final class CountMonitor implements M3Progress {
        private long work;
        private int checks;
        private int lifecycle;
        private final int cancelAtCheck;

        CountMonitor(int cancelAtCheck) {
            this.cancelAtCheck = cancelAtCheck;
        }

        public void checkCanceled() {
            if (++checks >= cancelAtCheck) throw new CancellationException("fixture cancellation");
        }

        public void worked(long delta) {
            work += delta;
        }

        public void beginTask(String name, long total) {
            lifecycle++;
        }

        public void begin(String name, long total) {
            lifecycle++;
        }

        public void done() {
            lifecycle++;
        }

        public void setProgress(long worked, long total) {
            lifecycle++;
        }

        public void setTaskName(String name) {
            lifecycle++;
        }

        public void subTask(String name) {
            lifecycle++;
        }

        public boolean isCanceled() {
            return false;
        }

        public void setCanceled(boolean canceled) {
            lifecycle++;
        }

        public Object state() {
            return null;
        }
    }
}
