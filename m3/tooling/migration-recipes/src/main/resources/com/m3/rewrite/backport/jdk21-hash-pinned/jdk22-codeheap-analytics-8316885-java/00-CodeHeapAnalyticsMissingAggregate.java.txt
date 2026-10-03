/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test CodeHeapAnalyticsMissingAggregate
 * @bug 8316885
 * @summary Compiler.CodeHeap_Analytics detail functions explain that aggregate must run first
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver CodeHeapAnalyticsMissingAggregate
 */
public class CodeHeapAnalyticsMissingAggregate {
    private static final String MISSING =
            "No aggregated code heap data available. Run function aggregate first.";

    public static void main(String[] args) throws Exception {
        PidJcmdExecutor executor = new PidJcmdExecutor();

        for (String function : new String[] {
                "UsedSpace",
                "FreeSpace",
                "MethodCount",
                "MethodSpace",
                "MethodAge",
                "MethodNames"
        }) {
            executor.execute("Compiler.CodeHeap_Analytics " + function)
                    .shouldHaveExitValue(0)
                    .shouldContain(MISSING);
        }

        executor.execute("Compiler.CodeHeap_Analytics aggregate")
                .shouldHaveExitValue(0);

        OutputAnalyzer afterAggregate =
                executor.execute("Compiler.CodeHeap_Analytics UsedSpace")
                        .shouldHaveExitValue(0);
        afterAggregate.shouldNotContain(MISSING);
    }
}
