#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Fail closed when general M3 join reintroduces linear reference staging."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[2]
SOURCE = "src/java.base/share/classes/java/lang/M3String.java"
WORKFLOW = ".github/workflows/mindex-string-backing.yml"
JTREG = "test/jdk/java/lang/String/M3StringGeneralJoinCarryTest.java"


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def region(source, start, end):
    left = source.find(start)
    right = source.find(end, left + len(start))
    require(left >= 0 and right > left, "missing or moved owner boundary: " + start)
    require(source.find(start, left + len(start)) == -1,
            "duplicate owner method: " + start)
    return source[left:right]


def verify_source(source):
    general = region(source, "    static M3String join(String[] parts) {",
                     "    /**\n     * Designated String.join adapter")
    values = region(source,
                    "    private static M3String joinValues(ArrayList<M3String> values) {",
                    "    static M3String sliceOf(")
    for part, name in ((general, "array"), (values, "values")):
        require("new ArrayList<" not in part and
                "ArrayList<M3String> level" not in part,
                "linear staging reintroduced in " + name)
        require("new M3String[Integer.SIZE - Integer.numberOfLeadingZeros(" in part,
                "logarithmic bound removed in " + name)
        require("return foldJoinLevels(levels);" in part,
                "pairwise folding removed in " + name)

    require("Objects.requireNonNull(parts, \"parts\")" in general,
            "general array null-check removed")
    require("if (parts.length == 0) return EMPTY;" in general,
            "general empty join contract removed")
    require("if (!joinAddDesignated(levels, part)) return null;" in general,
            "pool budget refusal or empty-piece admission changed")
    require("Objects.requireNonNull(value, \"M3 join value\")" in values,
            "internal canonical value check removed")
    require("carry = M3StringPool.concat(previous, carry);" in values,
            "adjacent pairwise reduction changed")
    require("if (!placed) throw new InternalError" in values,
            "carry capacity refusal removed")

    fold_marker = "    private static M3String foldJoinLevels(M3String[] levels) {"
    fold_at = values.find(fold_marker)
    require(fold_at >= 0, "general pairwise fold owner missing")
    fold = values[fold_at:]
    require("for (M3String value : levels)" in fold
            and "M3StringPool.concat(value, result)" in fold,
            "high-group over low-remainder fold order changed")
    require("static M3String joinDesignated(" in source
            and "private static boolean joinAddDesignated(" in source,
            "existing designated String.join owner lost")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    try:
        java = (root / SOURCE).read_text(encoding="utf-8")
        workflow = (root / WORKFLOW).read_text(encoding="utf-8")
        require((root / JTREG).is_file(), "jtreg source missing")
        verify_source(java)
        require("check-m3string-general-join-carry.py" in workflow,
                "join source guard not wired to String workflow")
        require("M3StringGeneralJoinCarryTest.java" in workflow,
                "join jtreg not wired to String workflow")
        require("make images" in workflow and "Run M3 String backing jtreg" in workflow,
                "JDK image or jtreg acceptance gate removed")
        if args.self_test:
            hostile = [
                java.replace("if (!joinAddDesignated(levels, part)) return null;",
                             "joinAddDesignated(levels, part);", 1),
                java.replace(
                    "                carry = M3StringPool.concat(previous, carry);\n"
                    "            }\n            if (!placed)",
                    "                carry = previous;\n"
                    "            }\n            if (!placed)", 1),
                java.replace("return foldJoinLevels(levels);",
                             "return EMPTY;", 1),
            ]
            for index, mutant in enumerate(hostile):
                try:
                    verify_source(mutant)
                except ValueError:
                    continue
                raise ValueError("hostile mutant admitted: " + str(index))
    except (OSError, ValueError) as error:
        raise SystemExit("M3_GENERAL_JOIN_CARRY_FAIL|" + str(error)) from error
    print("M3_GENERAL_JOIN_CARRY_PASS|source=1|jitreg=1|mutants=" +
          ("3" if args.self_test else "0"))


if __name__ == "__main__":
    main()
