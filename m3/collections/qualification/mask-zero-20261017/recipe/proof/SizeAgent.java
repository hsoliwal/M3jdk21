// SPDX-License-Identifier: Apache-2.0
import java.lang.instrument.Instrumentation;

/** Test-only shallow sizes of existing owners and their retained primitive arrays. */
public final class SizeAgent {
    public static Instrumentation instrumentation;
    private SizeAgent() { }
    public static void premain(String arguments, Instrumentation supplied) { instrumentation = supplied; }
}
