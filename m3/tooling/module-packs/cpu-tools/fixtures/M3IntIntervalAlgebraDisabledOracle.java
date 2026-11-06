// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalAlgebraNative;
import com.m3.tools.cpu.M3IntIntervalSet;

/** Native is opt-in and must refuse without an explicit absolute path. */
public final class M3IntIntervalAlgebraDisabledOracle {
    public static void main(String[] args) {
        M3IntIntervalSet empty = M3IntIntervalSet.copyOf(new int[0]);
        try {
            M3IntIntervalAlgebraNative.intersection(empty, empty);
            throw new AssertionError("native loaded without opt-in");
        } catch (IllegalStateException expected) {
            if (!"native interval algebra backend unavailable".equals(expected.getMessage())) {
                throw new AssertionError("unexpected exception contract", expected);
            }
        }
        System.out.println("M3_INTERVAL_ALGEBRA_DISABLED_PASS checks=1");
    }
}
