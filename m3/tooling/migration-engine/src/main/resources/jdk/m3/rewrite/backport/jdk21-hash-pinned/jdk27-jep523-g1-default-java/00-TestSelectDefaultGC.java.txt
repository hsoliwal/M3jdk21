/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gc.arguments;

/*
 * @test TestSelectDefaultGC
 * @summary Test selection of GC when no GC option is specified
 * @bug 8068582 8383856
 * @library /test/lib
 * @library /
 * @requires vm.gc.Serial & vm.gc.G1
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestSelectDefaultGC
 */

import jdk.test.lib.process.OutputAnalyzer;

public class TestSelectDefaultGC {
    public static void assertVMOption(OutputAnalyzer output, String option, boolean value) {
        output.shouldMatch(" " + option + " .*=.* " + value + " ");
    }

    public static void testDefaultGC(boolean actAsServer) throws Exception {
        // Start VM without specifying GC
        ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
            "-XX:" + (actAsServer ? "+" : "-") + "AlwaysActAsServerClassMachine",
            "-XX:" + (actAsServer ? "-" : "+") + "NeverActAsServerClassMachine",
            "-XX:+PrintFlagsFinal",
            "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        // G1 is the default whenever it is included, independent of server-class ergonomics.
        assertVMOption(output, "UseG1GC",            true);
        assertVMOption(output, "UseSerialGC",        false);
    }

    public static void testExplicitSerialGC() throws Exception {
        ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
            "-XX:+UseSerialGC",
            "-XX:+PrintFlagsFinal",
            "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        assertVMOption(output, "UseG1GC",     false);
        assertVMOption(output, "UseSerialGC", true);
    }

    public static void main(String[] args) throws Exception {
        // G1 stays default for both constrained/non-server and server-class ergonomics.
        testDefaultGC(false);
        testDefaultGC(true);

        // Explicit collector choice still overrides the ergonomic default.
        testExplicitSerialGC();
    }
}
