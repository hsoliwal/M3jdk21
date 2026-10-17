#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact historical-current regex DP Java source A/B on stock Java21.

Extracts original rolling() and patched rolling() verbatim, and extracts the
real M3String.CodeUnitCursor. A minimal immutable tuple fixture supplies their
dependencies. Proof covers the isolated DP operation, NOT product HotSpot/JNI.
"""
from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
REGEX = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
STRING = "src/java.base/share/classes/java/lang/M3String.java"
BASE = "970a34350ebdf6a3f1c5ff5acc8445d0f3515f55"
OLD = "9a5d9511b996d780f26672e2f5dc6ccc0a2747fe33f0c5ef1a9c62629564a41e"
NEW = "9536a6df2042fe047f02ef07834edca91ce7a863e5b3c1daafffe55fdad48e6e"
TEST = "971290714bb056831ad844288e66562f88fa87fbf0c4cd17fa0716f9df797ffc"

def call(cmd, **kwargs):
    result = subprocess.run(cmd, check=False, **kwargs)
    if result.returncode != 0:
        sys.exit(result.returncode)
    return result

def checked(content, expected, label):
    found = sha256(content).hexdigest()
    if found != expected:
        sys.exit(f"M3_REGEX_ROLLING_SOURCE_DRIFT name={label} found={found}")
    return content.decode("utf-8", "strict")

def excerpt(text, first, next_, name):
    start = text.find(first)
    end = text.find(next_, start + len(first))
    if start < 0 or end < start or text.count(first) != 1:
        sys.exit("M3_REGEX_ROLLING_EXTRACT_FAIL " + name)
    return text[start:end]

def build_kernel(source, class_name):
    rolling = excerpt(source,
            "    private static int rolling(M3String source, Plan plan) {",
            "    private static native long advance0(", class_name + ":rolling")
    terminator = excerpt(source,
            "    private static boolean isJdkLineTerminator(char value) {",
            "    private static final class Tables {", class_name + ":terminator")
    return """final class %s {
    private static final int STAR = 1 << 16;
    private static final int CODE_UNIT = 0xffff;
    private static final int UNKNOWN = -1;
    private static final int NO_MATCH = 0;
    private static final int MATCH = 1;
    static final class Plan {
        final int[] atoms;
        Plan(int[] atoms) { this.atoms = atoms; }
    }
    static int match(M3String source, Plan plan) { return rolling(source, plan); }
%s
%s
}
""" % (class_name, rolling, terminator)

def mock_owner(cursor):
    return """import java.util.Objects;

abstract class M3StringOwner {
    abstract int length();
    abstract char charAt(int position);
}
final class M3StringAtom extends M3StringOwner {
    private final char[] value;
    M3StringAtom(String value) { this.value = value.toCharArray(); }
    @Override int length() { return value.length; }
    @Override char charAt(int position) { return value[position]; }
}
final class M3StringTuple extends M3StringOwner {
    final M3String left, right;
    M3StringTuple(M3String left, M3String right) {
        this.left = left; this.right = right;
    }
    @Override int length() { return left.length() + right.length(); }
    @Override char charAt(int position) {
        int limit = left.length();
        return position < limit ? left.charAt(position)
                                : right.charAt(position - limit);
    }
}
final class M3String {
    private final M3StringOwner owner;
    private final int start, length;
    M3String(M3StringOwner owner, int start, int length) {
        this.owner = owner; this.start = start; this.length = length;
    }
    static M3String text(String text) {
        return new M3String(new M3StringAtom(text), 0, text.length());
    }
    static M3String join(M3String first, M3String second) {
        return new M3String(new M3StringTuple(first, second),
                0, first.length() + second.length());
    }
    M3String slice(int from, int to) {
        Objects.checkFromToIndex(from, to, length);
        return new M3String(owner, start + from, to - from);
    }
    int length() { return length; }
    int start() { return start; }
    M3StringOwner owner() { return owner; }
    char charAt(int position) {
        Objects.checkIndex(position, length);
        return owner.charAt(start + position);
    }

%s
}
""" % cursor

def main():
    baseline = checked(call(["git", "-C", str(ROOT), "show",
            BASE + ":" + REGEX], capture_output=True).stdout, OLD, "before")
    current = checked((ROOT / REGEX).read_bytes(), NEW, "after")
    checked((ROOT / "test/jdk/java/lang/String/M3StringRegexDpTest.java").read_bytes(),
            TEST, "jtreg")
    string_source = (ROOT / STRING).read_text(encoding="utf-8")
    cursor = excerpt(string_source,
            "    static final class CodeUnitCursor {",
            "    private static final class CharsSpliterator",
            "M3String.CodeUnitCursor")
    old_code = build_kernel(baseline, "LegacyRegexRolling")
    new_code = build_kernel(current, "CursorRegexRolling")
    if "source.charAt(i)" not in old_code or "units.at(i)" not in new_code:
        sys.exit("M3_REGEX_ROLLING_EXTRACT_FAIL unexpected edit")
    with TemporaryDirectory(prefix="m3-regex-rolling-") as directory:
        out = Path(directory)
        for name, content in [
            ("LegacyRegexRolling.java", old_code),
            ("CursorRegexRolling.java", new_code),
            ("M3String.java", mock_owner(cursor)),
        ]:
            (out / name).write_text(content, encoding="utf-8")
        runner = Path(__file__).with_name("RegexRollingBeforeAfter.java")
        call(["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
              "-d", directory, str(out / "LegacyRegexRolling.java"),
              str(out / "CursorRegexRolling.java"), str(out / "M3String.java"),
              str(runner)])
        call(["java", "-Xms256m", "-Xmx256m", "-ea", "-cp", directory,
              "RegexRollingBeforeAfter"])
    print("M3_REGEX_ROLLING_EXACT_JAVA_PASS source_extracted=true"
          " simulated_owner=true matched_m3jdk=false")
if __name__ == "__main__":
    main()
