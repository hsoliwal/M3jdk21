#!/usr/bin/env python3
"""Proof that the M3 String concat path composes canonical owners directly.

This checker intentionally follows the current two-operand implementation:
String.concat -> String.m3Concat(first, second) -> M3String.join -> M3StringPool.concat.
It must not resurrect the older StringConcatHelper N-ary staging contract.
"""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def fail(detail: str) -> None:
    raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|{detail}")


def balanced_method_body(source: str, signature: str) -> str:
    start = source.find(signature)
    if start < 0:
        fail(f"missing={signature}")
    opening = source.find("{", start)
    if opening < 0:
        fail(f"cannot_bound={signature}")
    depth = 0
    for index in range(opening, len(source)):
        unit = source[index]
        if unit == "{":
            depth += 1
        elif unit == "}":
            depth -= 1
            if depth == 0:
                return source[start : index + 1]
    fail(f"cannot_bound={signature}")
    raise AssertionError("unreachable")


string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(
    encoding="utf-8"
)
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(
    encoding="utf-8"
)
pool = (ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java").read_text(
    encoding="utf-8"
)

receiver = balanced_method_body(string, "static String m3Concat(String first, String second)")
for fragment in (
    "if (!m3JoinedStringsEnabled())",
    "return null;",
    "M3String storage = M3String.join(first, second);",
    "return storage == null ? null : new String(storage);",
):
    if fragment not in receiver:
        fail(f"missing_receiver={fragment}")

concat = balanced_method_body(string, "public String concat(String str)")
for fragment in (
    "if (M3_JOINED_STRINGS)",
    "String joined = m3Concat(this, str);",
    "if (joined != null)",
    "return joined;",
    "return StringConcatHelper.simpleConcat(this, str);",
):
    if fragment not in concat:
        fail(f"missing_concat={fragment}")

join = balanced_method_body(m3, "static M3String join(String first, String second)")
if "M3StringPool.concat(canonicalize(first), canonicalize(second))" not in join:
    fail("missing_join_owner")

m3_concat = balanced_method_body(m3, "M3String concat(M3String other)")
if 'M3StringPool.concat(this, Objects.requireNonNull(other, "other"))' not in m3_concat:
    fail("missing_m3string_concat_owner")

for fragment in (
    "private static M3String concatBalanced(M3String left, M3String right)",
    "return balance(branch.left, concatBalanced(branch.right, right));",
    "return balance(concatBalanced(left, branch.left), branch.right);",
):
    if fragment not in pool:
        fail(f"missing_balancing={fragment}")

# These belong to the retired N-ary StringConcatHelper design. Keep this
# assertion scoped to the actual binary receiver so M3String's independent
# compatibility oracle and designated join adapter remain valid.
for fragment in (
    "String[] constants",
    "Object[] args",
    "new String[args.length * 2 + 1]",
    "String argument = stringOf(args[index]);",
    "result.concat(piece)",
):
    if fragment in receiver:
        fail(f"staging={fragment}")

if "static String stringOf(Object value)" in receiver:
    fail("staging=stringOf authority in binary receiver")

print("M3_STRING_CONCAT_REFERENCE_DAG_PASS|two_operand=true|no_staging=true|balanced_owner=true")
