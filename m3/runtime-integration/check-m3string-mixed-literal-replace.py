#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")


def bounded(signature: str, label: str) -> str:
    start = string.find(signature)
    end = string.find("/**", start + 16)
    if start < 0 or end < 0:
        raise SystemExit(f"M3_MIXED_LITERAL_REPLACE_FAIL|{label}_missing")
    return string[start:end]


body = bounded("public String replace(CharSequence target, CharSequence replacement)", "method")
# A6 (aec42c5a02c3): the non-empty-target M3 lane is owned by m3Replace; a null result means the
# pool refused an admission (budget) and the flat compatibility path computes the same result.
lane = bounded("private String m3Replace(M3String storage, String trgtStr, M3String targetM3,", "m3Replace")

required = [
    "M3String targetM3 = trgtStr.m3();",
    "M3String replacementM3 = replStr.m3();",
    "storage != null || targetM3 != null || replacementM3 != null",
    "String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);",
    "if (replaced != null) return replaced;",
    "storage != null || replacementM3 != null",
    "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);",
    "if (replacementM3 == null) replacementM3 = M3String.canonicalize(replStr);",
    "if (sourceM3 != null && replacementM3 != null) {",
    "sourceM3.replaceEmptyTarget(replacementM3)",
]
for fragment in required:
    if fragment not in body:
        raise SystemExit(f"M3_MIXED_LITERAL_REPLACE_FAIL|missing={fragment}")

# Ordered: the match is located before anything is admitted, so an absent target keeps this
# String's identity without admitting the target or the replacement.
lane_required = [
    "int found = indexOf(trgtStr);",
    "if (found < 0) return this;",
    "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);",
    "if (sourceM3 == null) return null;",
    "M3String replacement = replacementM3 != null ? replacementM3 : M3String.canonicalize(replStr);",
    "if (replacement == null) return null;",
    "sourceM3.replaceAt(targetM3, replacement, found)",
    "sourceM3.replaceFlatTarget(this, trgtStr, replacement, found)",
]
cursor = -1
for fragment in lane_required:
    position = lane.find(fragment, cursor + 1)
    if position < 0:
        raise SystemExit(f"M3_MIXED_LITERAL_REPLACE_FAIL|m3Replace_missing_or_out_of_order={fragment}")
    cursor = position

# A flat target only drives the search; it is never admitted into the pool.
for scope, text in (("replace", body), ("m3Replace", lane)):
    if "M3String.canonicalize(trgtStr)" in text:
        raise SystemExit(f"M3_MIXED_LITERAL_REPLACE_FAIL|flat_target_admitted={scope}")

mixed_route = body.find("storage != null || targetM3 != null || replacementM3 != null")
fallback_value = body.find("trgtStr.value()")
if mixed_route < 0 or fallback_value < 0 or mixed_route > fallback_value:
    raise SystemExit("M3_MIXED_LITERAL_REPLACE_FAIL|M3_operands_materialized_before_route")

print("M3_STRING_MIXED_LITERAL_REPLACE_SOURCE_PASS|any_operand=true|canonical=true|flat_fallback=true")
