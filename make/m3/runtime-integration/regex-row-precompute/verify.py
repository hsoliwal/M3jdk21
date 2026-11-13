#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Thin M3JDK21 receiver: independently seal source and test java.base patch-module proof."""
from __future__ import annotations

import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
STUB_M3 = """package java.lang;
final class M3String implements CharSequence {
  private final String text;
  M3String(String text){this.text=text;}
  public int length(){return text.length();}
  public char charAt(int index){return text.charAt(index);}
  public CharSequence subSequence(int a,int b){return text.subSequence(a,b);}
  M3StringFacts facts(){return new M3StringFacts(text);}
}
"""
STUB_FACTS = """package java.lang;
final class M3StringFacts {
 final long bitSignal64;
 M3StringFacts(CharSequence input){long signal=0;for(int i=0;i<input.length();i++)signal|=codeUnitSignal(input.charAt(i));bitSignal64=signal;}
 static long codeUnitSignal(char code){int unit=code>='A'&&code<='Z'?code+32:code;int x=unit*0x9e3779b9;return(1L<<(x&63))|(1L<<((x^(x>>>13))&63));}
}
"""

def seal():
    entries = [line.split("\t") for line in (HERE / "source-seals.tsv").read_text("utf8").splitlines()
               if line and not line.startswith("#")]
    expected = [
        "src/java.base/share/classes/java/lang/M3StringRegexRowPrecompute.java",
        "test/jdk/java/lang/String/M3StringRegexRowPrecomputeTest.java"
    ]
    if len(entries) != 2 or [row[0] for row in entries] != expected:
        raise AssertionError("receiver expected file set changed")
    for path, sha in entries:
        actual = ROOT / path
        if not actual.is_file() or hashlib.sha256(actual.read_bytes()).hexdigest() != sha:
            raise AssertionError("M3JDK21 receiver source drift: " + path)
    return [ROOT / path for path in expected]

def main():
    target, test = seal()
    javac = shutil.which("javac")
    java = shutil.which("java")
    if not javac or not java:
        raise RuntimeError("Java21 compiler/runtime required")
    with tempfile.TemporaryDirectory(prefix="m3jdk21-regex-receiver-") as temp:
        root = Path(temp)
        stubs = root / "stubs/java/lang"
        stubs.mkdir(parents=True)
        source1 = stubs / "M3String.java"
        source2 = stubs / "M3StringFacts.java"
        source1.write_text(STUB_M3, encoding="utf8")
        source2.write_text(STUB_FACTS, encoding="utf8")
        classes = root / "classes"
        tests = root / "testclasses"
        classes.mkdir()
        tests.mkdir()
        subprocess.run([
            javac, "--patch-module", "java.base=" + str(root / "stubs") + ":" + str(target.parent),
            "-proc:none", "-Xlint:all", "-Werror", "-d", str(classes),
            str(source1), str(source2), str(target)
        ], check=True)
        subprocess.run([
            javac, "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
            "-d", str(tests), str(test)
        ], check=True)
        subprocess.run([
            java, "--patch-module", "java.base=" + str(classes),
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-Xmx96m", "-cp", str(tests), "M3StringRegexRowPrecomputeTest"
        ], check=True)
    print("M3JDK21_REGEX_ROW_RECEIVER_PASS sources_sha256=true patch_module=true")

if __name__ == "__main__":
    main()
