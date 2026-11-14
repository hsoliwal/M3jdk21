#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Oracle 4 (source work-order half): SOURCE_WORK_QUEUE.tsv inputs for the donor and relocated M3OpenJdkWorkOrderCli.

From a `git ls-tree -r` listing of the M3JDK tree, writes one admissible queue (bounded to a few source families) in
the donor schema and the same queue in the relocated schema (the two differ only in the method_level_recipe_gate token
that the JDK naming stage renamed, a NAMING row of RELOCATION_MAP.tsv), plus forged variants (one refusal each). The
driver script runs both CLIs over every pair and requires identical outputs or identical refusal messages.
usage: python -I work_orders.py <ls-tree listing> <donor token> <relocated token> <out dir>
"""
import sys
from pathlib import Path

HEADER = "\t".join(("path", "git_blob_sha1", "mode", "file_kind", "owner_family", "phase", "candidate_category",
                    "challenge_id", "technique_hint", "source_evidence", "source_sha256", "api_abi_jni_gate",
                    "method_level_recipe_gate", "target_runtime_gate", "status"))
PREFIXES = ("src/java.base/share/classes/java/lang/", "src/java.base/share/classes/java/util/",
            "src/java.base/share/native/", "src/hotspot/share/opto/", "src/jdk.compiler/share/classes/com/sun/tools/javac/main/",
            "m3/collections/src/", "m3/tooling/atom-census/", "m3/vendor/", "test/jdk/java/util/Arrays/", "make/",
            ".m3/", "m3/qualification/superset-landing-20261101/")
KINDS = {".java": "JAVA", ".c": "NATIVE_C", ".cpp": "NATIVE_CPP", ".cc": "NATIVE_CPP", ".hpp": "NATIVE_HEADER",
         ".h": "NATIVE_HEADER", ".m": "NATIVE_OBJC", ".s": "ASSEMBLY", ".S": "ASSEMBLY", ".sh": "SCRIPT",
         ".py": "SCRIPT", ".template": "CODE_TEMPLATE"}
NATIVE = {"NATIVE_C", "NATIVE_CPP", "NATIVE_HEADER", "NATIVE_OBJC", "ASSEMBLY", "ACCEL_KERNEL"}


def kind_of(path: str) -> str:
    dot = path.rfind(".")
    return KINDS.get(path[dot:] if dot > path.rfind("/") else "", "OTHER_LANGUAGE")


def row(path: str, blob: str, mode: str, token: str, disposition: str = "NEEDS_AST_CONTRACT_REVIEW",
        kind: str | None = None) -> list:
    kind = kind or kind_of(path)
    gate = ("JAVAC_21_API_ABI_JTREG_REQUIRED" if kind == "JAVA" else "NATIVE_HEADERS_C17_CPP20_ABI_JNI_REQUIRED"
            if kind in NATIVE else "LANGUAGE_SPECIFIC_SOURCE_BUILD_REQUIRED")
    return [path, blob, mode, kind, path.split("/")[0], "INVENTORY", "UNCLASSIFIED", "NONE", "BY_EXTENSION",
            "FILE_NAME_HINT_ONLY_NOT_AST", "SOURCE_SHA256_NOT_READ", gate, token,
            "INSTALLED_M3JDK21_TEST_NOT_EXECUTED", disposition]


def text(rows: list) -> str:
    return HEADER + "\n" + "".join("\t".join(r) + "\n" for r in rows)


def main() -> int:
    listing, donor_token, relocated_token, out = sys.argv[1], sys.argv[2], sys.argv[3], Path(sys.argv[4])
    out.mkdir(parents=True, exist_ok=True)
    entries = []
    for line in Path(listing).read_text(encoding="utf-8").splitlines():
        meta, _tab, path = line.partition("\t")
        mode, kind, blob = meta.split()
        if kind != "blob" or mode not in ("100644", "100755") or path.startswith('"') or "\\" in path:
            continue
        if path.startswith(PREFIXES):
            entries.append((path, blob, mode))
    entries.sort()
    variants = {"admissible": lambda token: text([row(p, b, m, token) for p, b, m in entries])}
    java = next(e for e in entries if e[0].endswith(".java") and e[0].startswith("src/java.base/"))

    def forged(change):
        def build(token):
            rows = [row(p, b, m, token) for p, b, m in entries]
            return change(rows, token)
        return build

    def cell(index, value):
        def change(rows, token):
            rows[0][index] = value
            return text(rows)
        return change

    variants["forged_status_pass"] = forged(cell(14, "PASS"))
    variants["forged_recipe_executed"] = forged(cell(12, "EXECUTED"))
    variants["forged_runtime_pass"] = forged(cell(13, "PASS"))
    variants["wrong_source_gate"] = forged(cell(11, "NATIVE_HEADERS_C17_CPP20_ABI_JNI_REQUIRED"
                                                if kind_of(entries[0][0]) == "JAVA" else "JAVAC_21_API_ABI_JTREG_REQUIRED"))
    variants["symlink_mode"] = forged(cell(2, "120000"))
    variants["blank_field"] = forged(cell(7, " "))
    variants["duplicate_path"] = forged(lambda rows, token: text(rows + [rows[0]]))
    variants["schema_changed"] = forged(lambda rows, token: text(rows).replace("\tstatus\n", "\tstate\n", 1))
    variants["carriage_return"] = forged(lambda rows, token: text(rows).replace("\n", "\r\n", 1))
    variants["unsafe_segment"] = forged(lambda rows, token: text([row("src/../" + java[0], java[1], java[2], token)]
                                                                  + rows))
    variants["language_mismatch"] = forged(lambda rows, token: text(
        [row(java[0][:-5] + ".c", java[1], java[2], token, kind="JAVA")] + rows))
    variants["disposition_lane_mismatch"] = forged(lambda rows, token: text(
        [row(java[0], java[1], java[2], token, disposition="NATIVE_ABI_JNI_HOLD")]
        + [r for r in rows if r[0] != java[0]]))
    variants["empty_queue"] = lambda token: HEADER + "\n"
    for name, build in variants.items():
        for side, token in (("donor", donor_token), ("relocated", relocated_token)):
            data = build(token).encode("utf-8")
            (out / ("%s-%s.tsv" % (name, side))).write_bytes(data)
    (out / "malformed_utf8-donor.tsv").write_bytes(b"\xff" + variants["admissible"](donor_token).encode())
    (out / "malformed_utf8-relocated.tsv").write_bytes(b"\xff" + variants["admissible"](relocated_token).encode())
    print("WORK_ORDER_QUEUES rows=%d variants=%d" % (len(entries), len(variants) + 1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
