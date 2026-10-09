#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
helper = (ROOT / "src/java.base/share/classes/java/lang/StringConcatHelper.java").read_text(encoding="utf-8")
factory = (ROOT / "src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java").read_text(encoding="utf-8")
pool = (ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java").read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")

start = helper.find("static String m3Concat(String[] constants, String[] args)")
if start < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|missing m3Concat")
end = helper.find("\n    /**", start)
if end < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|cannot bound m3Concat")
body = helper[start:end]

for fragment in [
    "M3String result = M3String.empty();",
    "String argument = args[index];",
    "M3String.canonicalize(constant)",
    "M3String.canonicalize(argument)",
    "result.concat(piece)",
    "return new String(result);",
]:
    if fragment not in body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|missing={fragment}")

for fragment in [
    "Object[] args",
    "String[] pieces",
    "new String[args.length * 2 + 1]",
    'String.join("", "", "", pieces',
    "ArrayList<",
]:
    if fragment in body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|staging={fragment}")

for fragment in [
    "private static MethodHandle generateM3Concat(MethodType mt, String[] constants)",
    ".asCollector(String[].class, count)",
    "MethodHandle stringifier = m3Stringifier(mt.parameterType(i));",
    "mh = MethodHandles.filterArguments(mh, 0, stringifiers);",
    "private static MethodHandle m3Stringifier(Class<?> type)",
    "if (type == String.class)",
    "return stringValueOf(type);",
    "return stringValueOf(int.class).asType(methodType(String.class, type));",
    "return floatStringifier();",
    "return doubleStringifier();",
    "return objectStringifier();",
    'JLA.stringConcatHelper("stringOf",',
    'methodType(String.class, Object.class)',
    'methodType(String.class, String[].class, String[].class)',
]:
    if fragment not in factory:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|stringifier={fragment}")

if "static String stringOf(Object value)" not in helper:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|stringOf authority missing")
if '(value == null || (s = value.toString()) == null) ? "null" : s' not in helper:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|stringOf null/toString contract drift")

if 'return M3StringPool.concat(this, Objects.requireNonNull(other, "other"));' not in m3:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|M3String concat owner drift")
for fragment in [
    "private static M3String concatBalanced(M3String left, M3String right)",
    "return balance(branch.left, concatBalanced(branch.right, right));",
    "return balance(concatBalanced(left, branch.left), branch.right);",
]:
    if fragment not in pool:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|balancing={fragment}")

print("M3_STRING_CONCAT_REFERENCE_DAG_PASS|typed_stringify=true|no_staging=true|balanced_owner=true")
