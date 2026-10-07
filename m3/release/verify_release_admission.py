#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path

EXPECTED_PRIORITY = [
    "M3_STRING",
    "REGEX_PRECOMPUTE",
    "GENERAL_PRECOMPUTE",
    "ARRAYS",
    "COLLECTIONS",
    "SWT_ECLIPSE_IDE",
]

EXPECTED_RECEIVING_ORDER = [
    "STRING",
    "ARRAYS",
    "COLLECTIONS",
    "AST_COMPILER",
    "REMAINING_FAMILIES",
]

UNQUALIFIED_CLAIMS = {
    "java_lang_string_backed_by_m3_string",
    "full_regex_precompute",
    "full_jtreg_compatibility",
    "performance_superiority_over_openjdk21",
    "arrays_qualified",
    "collections_qualified",
    "swt_eclipse_ide_qualified",
}

REQUIRED_FILES = [
    "M3-SYNEXIA-NOTICE.md",
    "M3-SYNEXIA-RIGHTS.md",
    "M3-SYNEXIA-AUTHORS.md",
    "LICENSE-M3-APACHE-2.0.txt",
    "m3/README.md",
    "m3/release/release-contract.json",
    "m3/release/README.md",
    "m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md",
]


def unique_object(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError(f"duplicate JSON key: {key}")
        out[key] = value
    return out


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()


def validate(root: Path, release_mode: bool = False) -> dict:
    checks = []
    blockers = []

    def passed(name: str, detail: str = ""):
        checks.append({"name": name, "status": "PASS", "detail": detail})

    def block(name: str, detail: str):
        checks.append({"name": name, "status": "BLOCK", "detail": detail})
        blockers.append({"name": name, "detail": detail})

    for rel in REQUIRED_FILES:
        path = root / rel
        if path.is_file():
            passed(f"file:{rel}")
        else:
            block(f"file:{rel}", "required release-boundary file is missing")

    contract_path = root / "m3/release/release-contract.json"
    contract = load_json(contract_path)
    if contract.get("schema") != "M3_RELEASE_CONTRACT_V1":
        block("contract.schema", "unexpected release contract schema")
    else:
        passed("contract.schema")

    if contract.get("priority_order") == EXPECTED_PRIORITY:
        passed("contract.priority_order")
    else:
        block("contract.priority_order", "P0 order drifted")

    policy = contract.get("claim_policy", {})
    if (
        policy.get("evidence_before_claim") is True
        and policy.get("marketing_must_reference_proof") is True
        and policy.get("self_reported_completion_forbidden") is True
    ):
        passed("contract.claim_policy")
    else:
        block("contract.claim_policy", "release claim discipline weakened")

    claims = contract.get("current_claims", {})
    bad = {key: claims.get(key) for key in UNQUALIFIED_CLAIMS if claims.get(key) != "NOT_CLAIMED"}
    if bad:
        block("contract.unqualified_claims", f"unqualified claims promoted: {bad}")
    else:
        passed("contract.unqualified_claims")

    notice = (root / "M3-SYNEXIA-NOTICE.md").read_text(encoding="utf-8")
    rights = (root / "M3-SYNEXIA-RIGHTS.md").read_text(encoding="utf-8")
    if "This notice does not relicense\nOpenJDK or JDK-derived source code as Apache-2.0." in notice:
        passed("rights.openjdk_boundary")
    else:
        block("rights.openjdk_boundary", "OpenJDK license boundary text missing")

    if "All ownership and all rights not expressly granted by the applicable license" in rights:
        passed("rights.reservation")
    else:
        block("rights.reservation", "first-party rights reservation missing")

    m3_readme = (root / "m3/README.md").read_text(encoding="utf-8")
    if "does **not** modify `java.lang.String`" in m3_readme:
        passed("claims.current_string_boundary")
    else:
        block("claims.current_string_boundary", "current P0 String boundary changed; update proof contract")

    if "No speedup, complete compatibility, or full jtreg pass is claimed." in m3_readme:
        passed("claims.no_unproved_performance")
    else:
        block("claims.no_unproved_performance", "existing no-overclaim sentence missing")

    mapping_path = root / "m3/docs/name-mapping.json"
    if mapping_path.is_file():
        mapping = load_json(mapping_path)
        seq = mapping.get("porting_invariant", {}).get("receiving_sequence")
        if seq is None:
            detail = "receiving_sequence not yet merged; release requires String -> arrays -> collections -> AST/compiler -> remaining"
            if release_mode:
                block("receiving_sequence", detail)
            else:
                checks.append({"name": "receiving_sequence", "status": "PENDING", "detail": detail})
        elif seq.get("order") == EXPECTED_RECEIVING_ORDER and seq.get("active_phase") == "STRING":
            passed("receiving_sequence")
        else:
            block("receiving_sequence", "receiving order or active phase drifted")
    else:
        block("receiving_sequence", "name-mapping.json missing")

    upstream = contract.get("release", {}).get("upstream_baseline")
    if not isinstance(upstream, str) or len(upstream) != 40 or any(c not in "0123456789abcdef" for c in upstream):
        block("git.upstream_baseline", "invalid exact upstream SHA")
    else:
        passed("git.upstream_baseline", upstream)
        try:
            head = git(root, "rev-parse", "HEAD")
            tree = git(root, "rev-parse", "HEAD^{tree}")
            merge_base = git(root, "merge-base", "HEAD", upstream)
            passed("git.head", head)
            passed("git.tree", tree)
            if merge_base == upstream:
                passed("git.merge_base", upstream)
            else:
                block("git.merge_base", f"expected {upstream}, got {merge_base}")
            if release_mode:
                ahead = int(git(root, "rev-list", "--count", f"{upstream}..HEAD"))
                parent = git(root, "rev-parse", "HEAD^")
                if ahead == 1 and parent == upstream:
                    passed("git.single_release_commit", head)
                else:
                    block("git.single_release_commit", f"ahead={ahead} parent={parent}")
        except (subprocess.CalledProcessError, FileNotFoundError) as exc:
            block("git.inspect", str(exc))

    return {
        "schema": "M3_RELEASE_ADMISSION_RESULT_V1",
        "mode": "release" if release_mode else "development",
        "release_ready": not blockers,
        "blockers": blockers,
        "checks": checks,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sha256-out", type=Path)
    args = parser.parse_args()

    root = args.root.resolve()
    result = validate(root, release_mode=args.release)

    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    if args.sha256_out:
        lines = []
        for rel in REQUIRED_FILES + ["m3/release/verify_release_admission.py"]:
            path = root / rel
            if path.is_file():
                lines.append(f"{sha256(path)}  {rel}")
        args.sha256_out.parent.mkdir(parents=True, exist_ok=True)
        args.sha256_out.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(json.dumps(result, sort_keys=True))
    if args.release and not result["release_ready"]:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
