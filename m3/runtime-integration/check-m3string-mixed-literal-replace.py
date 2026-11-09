#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")

start = string.find("public String replace(CharSequence target, CharSequence replacement)")
end = string.find("/**", start + 16)
body = string[start:end] if start >= 0 and end >= 0 else ""
if not body:
    raise SystemExit("M3_MIXED_LITERAL_REPLACE_FAIL|method_missing")

required = [
    "M3String targetM3 = trgtStr.m3();",
    "M3String replacementM3 = replStr.m3();",
    "storage != null || targetM3 != null || replacementM3 != null",
    "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);",
    "if (targetM3 == null) targetM3 = M3String.canonicalize(trgtStr);",
    "if (replacementM3 == null) replacementM3 = M3String.canonicalize(replStr);",
    "return replaced == sourceM3 ? this : new String(replaced);",
    "storage != null || replacementM3 != null",
    "sourceM3.replaceEmptyTarget(replacementM3)",
]
for fragment in required:
    if fragment not in body:
        raise SystemExit(f"M3_MIXED_LITERAL_REPLACE_FAIL|missing={fragment}")

mixed_route = body.find("storage != null || targetM3 != null || replacementM3 != null")
fallback_value = body.find("trgtStr.value()")
if mixed_route < 0 or fallback_value < 0 or mixed_route > fallback_value:
    raise SystemExit("M3_MIXED_LITERAL_REPLACE_FAIL|M3_operands_materialized_before_route")

print("M3_STRING_MIXED_LITERAL_REPLACE_SOURCE_PASS|any_operand=true|canonical=true|flat_fallback=true")
