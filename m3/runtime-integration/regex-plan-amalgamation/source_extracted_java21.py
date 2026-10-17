#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Extract complete actual M3JDK regex engines before/after, compile Java21 with
a small immutable M3 owner fixture and actual source-extracted cursor/fact mixer.

This is *not* a source-built M3JDK21 HotSpot/JNI or a JMH suite. We extract
the complete class and only replace its java.lang package and class name.
"""
from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
SOURCE = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
STRING = "src/java.base/share/classes/java/lang/M3String.java"
FACTS = "src/java.base/share/classes/java/lang/M3StringFacts.java"
BEFORE = "e8320c0669740b7d3a579a9c1e408f6291b3d01f"
BEFORE_SHA = "f875210dfb605799b6c8dbee53b28ad6383e000d90da775f7fd285464da7b2d3"
AFTER_SHA = "3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4"
TEST_SHA = "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a"

def run(command, **kwargs):
    outcome = subprocess.run(command, check=False, **kwargs)
    if outcome.returncode != 0:
        sys.exit(outcome.returncode)
    return outcome

def checked(raw, expected, label):
    actual = sha256(raw).hexdigest()
    if actual != expected:
        sys.exit(f"M3_REGEX_AMALGAMATION_SOURCE_FAIL {label} {actual}")
    return raw.decode("utf-8", "strict")

def extract(source, anchor, end, label):
    start = source.find(anchor)
    finish = source.find(end, start + len(anchor))
    if start < 0 or finish <= start or source.count(anchor) != 1:
        sys.exit("M3_REGEX_AMALGAMATION_SOURCE_FAIL extract=" + label)
    return source[start:finish]

def fixture_owner():
    current = (ROOT / STRING).read_text(encoding="utf-8")
    facts = (ROOT / FACTS).read_text(encoding="utf-8")
    cursor = extract(current, "    static final class CodeUnitCursor {",
                     "    private static final class CharsSpliterator", "M3String.CodeUnitCursor")
    add_signal = extract(facts, "    private static long addSignal(long signal, char value) {",
                         "    static long bigramSignal(", "M3StringFacts.addSignal")
    ngram = extract(facts, "    static long bigramSignal(char first, char second) {",
                    "    private static int mix32(int value) {", "M3StringFacts.ngram")
    mix32 = extract(facts, "    private static int mix32(int value) {",
                    "    private static char asciiUpper", "M3StringFacts.mix32")
    return """import java.util.Objects;
abstract class M3StringOwner {
    abstract int length();
    abstract char charAt(int index);
}
final class M3StringAtom extends M3StringOwner {
    final long address = 0L;
    final int storageWidth = 2;
    final boolean bigEndian = false;
    final int length;
    private final char[] value;
    M3StringAtom(String data) {
        value = data.toCharArray(); length = value.length;
    }
    @Override int length() { return length; }
    @Override char charAt(int index) { return value[index]; }
}
final class M3StringTuple extends M3StringOwner {
    final M3String left, right;
    M3StringTuple(M3String left, M3String right) {
        this.left = left; this.right = right;
    }
    @Override int length() { return left.length() + right.length(); }
    @Override char charAt(int index) {
        int mid = left.length();
        return index < mid ? left.charAt(index) : right.charAt(index - mid);
    }
}
final class M3StringFacts {
    final long bitSignal64;
    final long bigramSignal64;
    final long trigramSignal64;
    M3StringFacts(long bits, long bigrams, long trigrams) {
        bitSignal64 = bits;
        bigramSignal64 = bigrams;
        trigramSignal64 = trigrams;
    }
    static long codeUnitSignal(char unit) { return addSignal(0L, unit); }
%s
%s
%s
}
final class M3String {
    private final M3StringOwner owner;
    private final int start, length;
    private M3StringFacts facts;
    M3String(M3StringOwner owner, int start, int length) {
        this.owner = owner; this.start = start; this.length = length;
    }
    static M3String text(String text) {
        return new M3String(new M3StringAtom(text), 0, text.length());
    }
    static M3String join(M3String left, M3String right) {
        return new M3String(new M3StringTuple(left, right),
                0, left.length() + right.length());
    }
    M3String slice(int begin, int end) {
        Objects.checkFromToIndex(begin, end, length);
        return new M3String(owner, start + begin, end - begin);
    }
    int length() { return length; }
    int start() { return start; }
    M3StringOwner owner() { return owner; }
    char charAt(int index) {
        Objects.checkIndex(index, length);
        return owner.charAt(start + index);
    }
    M3StringFacts factsIfPrepared() { return facts; }
    void prepareFacts() {
        long bits = 0, bigrams = 0, trigrams = 0;
        for (int i = 0; i < length; i++) {
            char unit = charAt(i);
            bits |= M3StringFacts.codeUnitSignal(unit);
            if (i > 0) bigrams |= M3StringFacts.bigramSignal(charAt(i - 1), unit);
            if (i > 1) trigrams |= M3StringFacts.trigramSignal(
                    charAt(i - 2), charAt(i - 1), unit);
        }
        facts = new M3StringFacts(bits, bigrams, trigrams);
    }
    @Override public String toString() {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append(charAt(i));
        return text.toString();
    }
%s
}
""" % (add_signal, ngram, mix32, cursor)

def rewrite_engine(source, class_name):
    if "package java.lang;" not in source:
        sys.exit("M3_REGEX_AMALGAMATION_SOURCE_FAIL java.lang package")
    if source.count("final class M3StringRegexDp {") != 1:
        sys.exit("M3_REGEX_AMALGAMATION_SOURCE_FAIL class owner")
    return (source.replace("package java.lang;", "")
                  .replace("final class M3StringRegexDp {",
                           "final class " + class_name + " {")
                  .replace("M3StringRegexDp.class", class_name + ".class")
                  .replace("private M3StringRegexDp() { }",
                           "private " + class_name + "() { }"))

def main():
    previous = checked(Path(__file__).with_name("M3StringRegexDpBefore.java.txt").read_bytes(),
                       BEFORE_SHA, "before")
    updated = checked((ROOT / SOURCE).read_bytes(), AFTER_SHA, "after")
    checked((ROOT / "test/jdk/java/lang/String/M3StringRegexDpTest.java").read_bytes(),
            TEST_SHA, "jtreg")
    if "rejectsByPrecompute" in previous or "source.factsIfPrepared()" not in updated:
        sys.exit("M3_REGEX_AMALGAMATION_SOURCE_FAIL preflight delta")
    with TemporaryDirectory(prefix="m3-regex-amalgamation-") as directory:
        work = Path(directory)
        for filename, content in [
            ("RegexBefore.java", rewrite_engine(previous, "RegexBefore")),
            ("RegexAfter.java", rewrite_engine(updated, "RegexAfter")),
            ("M3String.java", fixture_owner()),
        ]:
            (work / filename).write_text(content, encoding="utf-8")
        oracle = Path(__file__).parent / "src/test/java/RegexAmalgamationBeforeAfter.java"
        run(["javac", "--release", "21", "-proc:none", "-Xlint:all,-auxiliaryclass", "-Werror",
             "-d", directory, str(work / "RegexBefore.java"),
             str(work / "RegexAfter.java"), str(work / "M3String.java"), str(oracle)])
        run(["java", "-Xms256m", "-Xmx256m", "-ea", "-cp", directory,
             "RegexAmalgamationBeforeAfter"])
    print("M3_REGEX_AMALGAMATION_EXTRACTED_JAVA_PASS before_after_exact=true"
          " mock_owner=true matched_jdk=false")
if __name__ == "__main__":
    main()
