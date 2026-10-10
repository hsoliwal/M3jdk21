#!/usr/bin/env python3
"""Focused source proof for the M3 CharSequence replacement owner."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
STRING_SOURCE = ROOT / "src/java.base/share/classes/java/lang/String.java"
SIGNATURE = "public String replace(CharSequence target, CharSequence replacement)"


def fail(message: str) -> None:
    raise SystemExit(f"M3_STRING_REPLACE_OWNER_FAIL: {message}")


def method_body(source: str) -> str:
    start = source.find(SIGNATURE)
    if start < 0:
        fail("CharSequence replacement method is absent")
    opening = source.find("{", start)
    if opening < 0:
        fail("CharSequence replacement method has no body")
    depth = 0
    for index in range(opening, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    fail("CharSequence replacement method body is unbalanced")
    return ""


def require(body: str, marker: str, label: str) -> None:
    if marker not in body:
        fail(f"missing {label}: {marker}")


def main() -> int:
    body = method_body(STRING_SOURCE.read_text(encoding="utf-8"))
    require(
        body,
        "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);",
        "canonical source fallback",
    )
    require(
        body,
        "M3String targetM3 = trgtStr.m3();",
        "canonical target owner",
    )
    require(
        body,
        "M3String replacementM3 = replStr.m3();",
        "canonical replacement owner",
    )
    require(
        body,
        "M3String replaced = sourceM3.replace(targetM3, replacementM3);",
        "canonical replacement call",
    )
    require(
        body,
        "return replaced == sourceM3 ? this : new String(replaced);",
        "identity-preserving unchanged result",
    )
    if "M3String replaced = storage.replace(targetM3, replacementM3);" in body:
        fail("nullable storage was used as the replacement owner")
    print(
        "M3_STRING_REPLACE_OWNER_PASS"
        "|canonicalSourceFallback=true"
        "|canonicalTarget=true"
        "|canonicalReplacement=true"
        "|identityPreserving=true"
        "|nullableStorageCall=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
