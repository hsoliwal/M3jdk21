"""absorb_big.py <donor dir> <java.base classes dir> : NUMBER_PARSE_ABSORB v2 (big numbers) - the FastDoubleParser 1.0.90
classes behind JavaBigDecimalParser / JavaBigIntegerParser join java.base as jdk.internal.math.M3* next to the 13
classes absorbed on 2026-10-31 (TYPE_RENAME.tsv of make/m3/tooling/jdk-superset/number-parse): package
ch.randelshofer.fastdoubleparser -> jdk.internal.math, type T -> M3T for every absorbed type (old and new), a donor
facade named only in Javadoc ({@link JavaBigDecimalParser}) becomes {@code ...}. Prints one ABSORB row per class and
the TYPE_RENAME rows it adds."""
import re
import sys
from pathlib import Path

donor, target = Path(sys.argv[1]), Path(sys.argv[2]) / "jdk/internal/math"
OLD = ["AbstractFloatValueParser", "AbstractJavaFloatingPointBitsFromByteArray",
       "AbstractJavaFloatingPointBitsFromCharSequence", "AbstractNumberParser", "FastDoubleMath", "FastDoubleSwar",
       "FastFloatMath", "FastIntegerMath", "JavaDoubleBitsFromByteArray", "JavaDoubleBitsFromCharSequence",
       "JavaFloatBitsFromByteArray", "JavaFloatBitsFromCharSequence", "FftMultiplier"]
NEW = ["AbstractBigDecimalParser", "AbstractBigIntegerParser",
       "JavaBigDecimalFromCharArray", "JavaBigIntegerFromCharSequence", "BigSignificand",
       "ParseDigitsTaskCharSequence", "ParseDigitsTaskCharArray"]
FACADES = ["JavaBigDecimalParser", "JavaBigIntegerParser", "JavaDoubleParser", "JavaFloatParser"]
types = OLD + NEW
word = re.compile(r"(?<![\w$])(" + "|".join(sorted(types, key=len, reverse=True)) + r")(?![\w$])")
for name in NEW:
    src = (donor / f"{name}.java").read_text(encoding="utf-8")
    out = src.replace("package ch.randelshofer.fastdoubleparser;", "package jdk.internal.math;", 1)
    out = out.replace("ch.randelshofer.fastdoubleparser.", "jdk.internal.math.")
    out = word.sub(lambda m: "M3" + m.group(1), out)
    for facade in FACADES:
        out = re.sub(r"\{@link(plain)? " + facade + r"(#[^}]*)?\}", "{@code " + facade + "}", out)
    assert "ch.randelshofer" not in out, name
    (target / f"M3{name}.java").write_bytes(out.encode("utf-8"))
    print(f"ABSORB\t{name}.java\tjdk/internal/math/M3{name}.java")
for name in NEW:
    print(f"TYPE_RENAME\tch.randelshofer.fastdoubleparser.{name}\tjdk.internal.math.M3{name}")
