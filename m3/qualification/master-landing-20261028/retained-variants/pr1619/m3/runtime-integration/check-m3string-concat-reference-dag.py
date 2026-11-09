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

required_body = [
    "static String m3Concat(String[] constants, String[] args)",
    "M3String result = M3String.empty();",
    "for (int index = 0; index < args.length; index++) {",
    "result = m3Append(result, constants[index]);",
    "if (result == null) return flatConcat(constants, args);",
    "result = m3Append(result, args[index]);",
    "if (result == null) return flatConcat(constants, args);",
    "}",
    "result = m3Append(result, constants[args.length]);",
    "return result == null ? flatConcat(constants, args) : new String(result);",
]
cursor = -1
for fragment in required_body:
    position = body.find(fragment, cursor + 1)
    if position < 0:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|missing_or_out_of_order={fragment}")
    cursor = position
if body.count("result = m3Append(result, constants[index]);") != 1:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|constant_append_count")
if body.count("result = m3Append(result, args[index]);") != 1:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|argument_append_count")
if body.count("return flatConcat(constants, args);") != 2:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|budget_fallback_count")

for fragment in [
    "Object[] args",
    "String[] pieces",
    "new String[args.length * 2 + 1]",
    'String.join("", "", "", pieces',
    "ArrayList<",
]:
    if fragment in body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|staging={fragment}")

append_start = helper.find("private static M3String m3Append(M3String result, String piece)")
if append_start < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|missing_m3Append")
append_end = helper.find("\n    /**", append_start)
if append_end < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|cannot_bound_m3Append")
append_body = helper[append_start:append_end]
for fragment in [
    "if (piece == null || piece.isEmpty()) return result;",
    "M3String admitted = M3String.canonicalize(piece);",
    "if (admitted == null) return null;",
    "return result.length() == 0 ? admitted : result.concat(admitted);",
]:
    if fragment not in append_body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|m3Append={fragment}")

flat_start = helper.find("private static String flatConcat(String[] constants, String[] args)")
if flat_start < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|missing_flatConcat")
flat_end = helper.find("\n    /**", flat_start)
if flat_end < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|cannot_bound_flatConcat")
flat_body = helper[flat_start:flat_end]
for fragment in [
    "if (constants[index] != null) indexCoder = mix(indexCoder, constants[index]);",
    "indexCoder = mix(indexCoder, args[index]);",
    "String suffix = constants[args.length];",
    "for (int index = args.length - 1; index >= 0; index--) {",
    "indexCoder = prepend(indexCoder, buf, args[index], constants[index]);",
    "return newString(buf, indexCoder);",
]:
    if fragment not in flat_body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|flat_fallback={fragment}")

for fragment in [
    "private static MethodHandle generateM3Concat(MethodType mt, String[] constants)",
    ".asCollector(String[].class, count)",
    "MethodHandle stringifier = m3Stringifier(mt.parameterType(i));",
    "mh = MethodHandles.filterArguments(mh, 0, stringifiers);",
    "private static MethodHandle m3Stringifier(Class<?> type)",
    "return stringValueOf(type);",
    "return stringValueOf(int.class).asType(methodType(String.class, type));",
    "return floatStringifier();",
    "return doubleStringifier();",
    "return objectStringifier();",
    'JLA.stringConcatHelper("stringOf",',
    "methodType(String.class, Object.class)",
    "methodType(String.class, String[].class, String[].class)",
]:
    if fragment not in factory:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|stringifier={fragment}")

stringifier_start = factory.find("private static MethodHandle m3Stringifier(Class<?> type)")
stringifier_end = factory.find("\n    // We need one prepender", stringifier_start)
if stringifier_end < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|cannot_bound_m3Stringifier")
stringifier_body = factory[stringifier_start:stringifier_end]
if "return null;" in stringifier_body:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|nullable_reference_conversion_bypass")
if "return objectStringifier();" not in stringifier_body:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|reference_stringifier_missing")

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

print("M3_STRING_CONCAT_REFERENCE_DAG_PASS|m3append=true|budget_fallback=true|typed_stringify=true|no_staging=true|balanced_owner=true")
