// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.standard;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Added-line filtering and M3 source-scope tests for the changed-name gate. */
public final class M3ChangedNameGateSelfTest {
    private M3ChangedNameGateSelfTest() {
    }

    /**
     * Runs deterministic diff and filtering fixtures.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        String diff = """
                diff --git a/m3/demo/M3Widget.java b/m3/demo/M3Widget.java
                index 1111111..2222222 100644
                --- a/m3/demo/M3Widget.java
                +++ b/m3/demo/M3Widget.java
                @@ -2,0 +3,2 @@ package com.m3.demo;
                +public final class Helper {
                +}
                @@ -8 +10 @@ public final class M3Widget {
                +    void doStuff() {
                diff --git a/m3/demo/M3Old.java b/m3/demo/M3Old.java
                deleted file mode 100644
                --- a/m3/demo/M3Old.java
                +++ /dev/null
                @@ -1,3 +0,0 @@
                """;
        Map<Path, List<M3ChangedNameGate.LineRange>> ranges =
                M3ChangedNameGate.addedLines(diff);
        Path widget = Path.of("m3/demo/M3Widget.java");
        check(ranges.size() == 1 && ranges.containsKey(widget), "wrong paths " + ranges);
        check(ranges.get(widget).equals(List.of(
                new M3ChangedNameGate.LineRange(3, 4),
                new M3ChangedNameGate.LineRange(10, 10))),
                "wrong ranges " + ranges.get(widget));

        List<String> findings = List.of(
                "m3/demo/M3Widget.java\t3\t1\tNAME_QUALITY\ttype Helper: placeholder",
                "m3/demo/M3Widget.java\t7\t5\tNAME_QUALITY\told finding",
                "m3/demo/M3Widget.java\t10\t5\tNAME_QUALITY\tmethod doStuff: placeholder");
        List<String> introduced = M3ChangedNameGate.filterAddedFindings(findings, ranges);
        check(introduced.size() == 2 && introduced.get(0).contains("\t3\t")
                && introduced.get(1).contains("\t10\t"),
                "wrong introduced findings " + introduced);

        check(M3JdkStandardCensus.isM3OwnedSource(Path.of(
                "m3/demo/src/main/java/com/m3/demo/M3Widget.java")),
                "M3 owner was excluded");
        check(!M3JdkStandardCensus.isM3OwnedSource(Path.of(
                "m3/vendor/third_party/src/main/java/com/m3/vendor/M3Widget.java")),
                "vendor source was included");
        check(!M3JdkStandardCensus.isM3OwnedSource(Path.of(
                "src/java.base/share/classes/java/lang/String.java")),
                "ordinary JDK owner was included");
        System.out.println("M3_CHANGED_NAME_GATE_SELFTEST PASS");
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError("M3_CHANGED_NAME_GATE_SELFTEST FAIL " + failure);
        }
    }
}
