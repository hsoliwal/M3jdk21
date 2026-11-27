/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 */

package gc.arguments;

/*
 * @test
 * @summary Preserve historical GC child-process overloads and limited-option semantics
 * @library /test/lib
 * @library /
 * @requires vm.gc.Serial & vm.gc.G1
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.M3GCArgumentsCompatibilityTest
 */

import java.util.List;
import jdk.test.lib.process.OutputAnalyzer;

public class M3GCArgumentsCompatibilityTest {
    public static void main(String[] args) throws Exception {
        List<String> flags = List.of("-XX:+UseSerialGC", "-XX:+PrintFlagsFinal", "-version");
        String[] array = flags.toArray(String[]::new);
        List<String> limited = GCArguments.createLimitedTestJavaProcessBuilder(flags).command();
        ProcessBuilder list = GCArguments.createJavaProcessBuilder(flags);
        ProcessBuilder varargs = GCArguments.createJavaProcessBuilder(array);
        if (!limited.equals(list.command()) || !limited.equals(varargs.command())) {
            throw new AssertionError("Historical overloads must use limited test VM options");
        }
        if (!flags.equals(List.of(array))) {
            throw new AssertionError("The caller's argument array must not be modified");
        }
        if (!limited.subList(limited.size() - flags.size(), limited.size()).equals(flags)) {
            throw new AssertionError("Explicit collector options must remain last and ordered");
        }
        OutputAnalyzer output = new OutputAnalyzer(list.start());
        output.shouldHaveExitValue(0);
        output.shouldMatch(" UseSerialGC .*=.* true ");
        output.shouldMatch(" UseG1GC .*=.* false ");
    }
}
