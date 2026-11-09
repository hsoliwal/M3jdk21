#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import argparse
import contextlib
import hashlib
import json
import os
import re
import stat
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

# An edited contract cannot silently remove a mandatory release gate.
EXPECTED_RELEASE_GATES = [
    "RIGHTS_AND_LICENSE_BOUNDARY",
    "SOURCE_TREE_AND_UPSTREAM_BASELINE",
    "OPENJDK_SANITY_MATRIX",
    "M3_FOUNDATION_TESTS",
    "PRECOMPUTE_JAVA_DIFFERENTIAL",
    "PRECOMPUTE_JNI_NATIVE_PARITY",
    "M3_MAVEN_CONTROL_REACTOR",
    "STRING_UTF16_CONTRACT",
    "RELEASE_CLAIMS_VALIDATION",
    "PROOF_PACK_SHA256",
]
TEST_GATES = frozenset(EXPECTED_RELEASE_GATES[2:8])
MAX_EVIDENCE_BYTES = 1024 * 1024
MAX_PROOF_BYTES = 64 * 1024 * 1024
MAX_IMAGE_BYTES = 16 * 1024 * 1024 * 1024


def check_evidence_file(directory: Path, record, maximum: int) -> None:
    """Check a bounded regular file under the proof directory, without executing it."""
    if not isinstance(record, dict) or set(record) != {"path", "sha256"}:
        raise ValueError("file receipt must contain exactly path and sha256")
    relative, expected = record["path"], record["sha256"]
    if (not isinstance(relative, str) or len(relative) > 1024
            or not re.fullmatch(r"[A-Za-z0-9._/-]+", relative)
            or any(part in {"", ".", ".."} for part in relative.split("/"))):
        raise ValueError("noncanonical proof path")
    if not isinstance(expected, str) or not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise ValueError("invalid proof SHA-256")
    directory = directory.resolve()
    path = directory
    for part in relative.split("/"):
        path = path / part
        if path.is_symlink():
            raise ValueError("symlink proof path")
    if not path.resolve().is_relative_to(directory):
        raise ValueError("proof path escapes its directory")
    before = path.stat()
    if not stat.S_ISREG(before.st_mode) or not 0 < before.st_size <= maximum:
        raise ValueError("proof file must be a nonempty, bounded regular file")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        opened = os.fstat(stream.fileno())
        if (opened.st_dev, opened.st_ino) != (before.st_dev, before.st_ino):
            raise ValueError("proof file changed before reading")
        total = 0
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            total += len(chunk)
            if total > maximum:
                raise ValueError("proof file exceeds its byte budget")
            digest.update(chunk)
        after = os.fstat(stream.fileno())
    snapshot = lambda s: (s.st_dev, s.st_ino, s.st_size, s.st_mtime_ns, s.st_ctime_ns)
    if snapshot(before) != snapshot(after) or total != before.st_size:
        raise ValueError("proof file changed while reading")
    if digest.hexdigest() != expected:
        raise ValueError("proof file SHA-256 mismatch")


def validate_evidence(path: Path, head: str, tree: str, platform: str, passed, block) -> None:
    """Validate local CI-produced evidence integrity; this is not remote attestation."""
    try:
        if not isinstance(platform, str) or not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+){1,4}", platform):
            raise ValueError("an explicit canonical platform is required")
        if path.is_symlink() or not path.is_file() or path.stat().st_size > MAX_EVIDENCE_BYTES:
            raise ValueError("evidence manifest must be a bounded regular file")
        with path.open("rb") as stream:
            raw = stream.read(MAX_EVIDENCE_BYTES + 1)
        if len(raw) > MAX_EVIDENCE_BYTES:
            raise ValueError("evidence manifest exceeds its byte budget")
        evidence = json.loads(raw.decode("utf-8"), object_pairs_hook=unique_object)
        if not isinstance(evidence, dict) or set(evidence) != {
            "schema", "source_commit", "source_tree", "platform", "artifacts", "gates"
        }:
            raise ValueError("unexpected evidence manifest fields")
        if evidence["schema"] != "M3_RELEASE_EVIDENCE_V1":
            raise ValueError("unexpected evidence schema")
        if not head or not tree or (evidence["source_commit"], evidence["source_tree"], evidence["platform"]) != (head, tree, platform):
            raise ValueError("evidence does not match the inspected commit, tree and requested platform")
        artifacts = evidence["artifacts"]
        if not isinstance(artifacts, list) or not 1 <= len(artifacts) <= 16:
            raise ValueError("one to sixteen product artifacts are required")
        for artifact in artifacts:
            check_evidence_file(path.parent, artifact, MAX_IMAGE_BYTES)
        if len({artifact["path"] for artifact in artifacts}) != len(artifacts):
            raise ValueError("duplicate product artifact")
        receipts = evidence["gates"]
        if not isinstance(receipts, list) or len(receipts) != len(EXPECTED_RELEASE_GATES):
            raise ValueError("exactly one receipt for every mandatory gate is required")
        names = [receipt.get("name") if isinstance(receipt, dict) else None for receipt in receipts]
        if any(not isinstance(name, str) for name in names) or sorted(names) != sorted(EXPECTED_RELEASE_GATES):
            raise ValueError("missing, unknown or duplicate mandatory gate")
        passed("evidence.identity_and_artifacts", f"{platform}: {head} {tree}")
        for receipt in receipts:
            name = receipt["name"]
            try:
                if set(receipt) != {"name", "status", "exit_code", "tests_run", "command", "environment", "proofs"}:
                    raise ValueError("unexpected gate receipt fields")
                if receipt["status"] != "PASS" or type(receipt["exit_code"]) is not int or receipt["exit_code"] != 0:
                    raise ValueError("gate did not execute successfully")
                tests = receipt["tests_run"]
                if type(tests) is not int or tests < (1 if name in TEST_GATES else 0):
                    raise ValueError("test gate needs a positive executed-test count")
                command = receipt["command"]
                if not isinstance(command, list) or not command or not all(isinstance(arg, str) and arg.strip() for arg in command):
                    raise ValueError("executed command is missing")
                environment = receipt["environment"]
                if not isinstance(environment, dict) or not environment or not all(
                    isinstance(k, str) and k.strip() and isinstance(v, str) and v.strip()
                    for k, v in environment.items()
                ):
                    raise ValueError("execution environment is missing")
                proofs = receipt["proofs"]
                if not isinstance(proofs, list) or not 1 <= len(proofs) <= 16:
                    raise ValueError("gate needs bounded raw proof-file receipts")
                for proof in proofs:
                    check_evidence_file(path.parent, proof, MAX_PROOF_BYTES)
                if len({proof["path"] for proof in proofs}) != len(proofs):
                    raise ValueError("duplicate gate proof file")
                passed(f"evidence.gate:{name}")
            except (OSError, UnicodeError, ValueError, TypeError) as exc:
                block(f"evidence.gate:{name}", str(exc))
    except (OSError, UnicodeError, ValueError, TypeError) as exc:
        block("evidence.manifest", str(exc))


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


def validate_release_source(root: Path, passed, block) -> None:
    """Require an isolated source checkout and exact regular boundary-input blobs."""
    for relative in REQUIRED_FILES + ["m3/docs/name-mapping.json", "m3/release/verify_release_admission.py"]:
        try:
            path = root
            for part in relative.split("/"):
                path = path / part
                if path.is_symlink():
                    raise ValueError("release source-boundary inputs cannot use symlinks")
            entry = git(root, "ls-tree", "HEAD", "--", relative)
            metadata, separator, recorded_path = entry.partition("\t")
            cells = metadata.split()
            if (not separator or recorded_path != relative or len(cells) != 3
                    or cells[0] not in {"100644", "100755"} or cells[1] != "blob"):
                raise ValueError("boundary input must be a tracked regular file in HEAD")
            before = path.stat()
            if not stat.S_ISREG(before.st_mode):
                raise ValueError("boundary input must be a regular file")
            digest = hashlib.sha1(b"blob " + str(before.st_size).encode("ascii") + b"\0")
            with path.open("rb") as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    digest.update(chunk)
            after = path.stat()
            if (before.st_size, before.st_mtime_ns, before.st_ctime_ns) != (
                    after.st_size, after.st_mtime_ns, after.st_ctime_ns) or digest.hexdigest() != cells[2]:
                raise ValueError("boundary input bytes differ from the inspected Git blob")
            passed(f"git.input:{relative}", cells[2])
        except (OSError, ValueError, subprocess.CalledProcessError) as exc:
            block(f"git.input:{relative}", str(exc))
    try:
        if git(root, "ls-files", "--others", "-z"):
            block("git.untracked_source", "release requires an isolated checkout without untracked or ignored files; keep builds and proof packs outside it")
        else:
            passed("git.untracked_source")
        if sha256(root / "m3/release/verify_release_admission.py") != sha256(Path(__file__)):
            block("git.verifier_identity", "executed verifier differs from the source-boundary verifier")
        else:
            passed("git.verifier_identity")
    except (OSError, subprocess.CalledProcessError) as exc:
        block("git.source_inspection", str(exc))


def output_errors(root: Path, release_mode: bool, outputs: list[Path]) -> list[dict]:
    errors = []
    seen = set()
    for path in outputs:
        resolved = path.resolve()
        if path.exists() or path.is_symlink():
            errors.append({"name": "output.existing", "detail": f"refusing to overwrite existing output or input: {path}"})
        if resolved in seen:
            errors.append({"name": "output.duplicate", "detail": "JSON and checksum outputs must be distinct"})
        seen.add(resolved)
        if release_mode and resolved.is_relative_to(root):
            errors.append({"name": "output.source_checkout", "detail": "release outputs must be outside the isolated source checkout"})
    return errors


def validate(root: Path, release_mode: bool = False, evidence: Path | None = None,
             platform: str | None = None) -> dict:
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
    try:
        contract = load_json(contract_path)
        if not isinstance(contract, dict):
            raise ValueError("release contract must be a JSON object")
    except (OSError, UnicodeError, ValueError) as exc:
        block("contract.read", str(exc))
        contract = {}
    if contract.get("schema") != "M3_RELEASE_CONTRACT_V1":
        block("contract.schema", "unexpected release contract schema")
    else:
        passed("contract.schema")

    if contract.get("mandatory_release_gates") == EXPECTED_RELEASE_GATES:
        passed("contract.mandatory_release_gates")
    else:
        block("contract.mandatory_release_gates", "mandatory gate inventory drifted")

    if contract.get("priority_order") == EXPECTED_PRIORITY:
        passed("contract.priority_order")
    else:
        block("contract.priority_order", "P0 order drifted")

    policy = contract.get("claim_policy")
    policy = policy if isinstance(policy, dict) else {}
    if (
        policy.get("evidence_before_claim") is True
        and policy.get("marketing_must_reference_proof") is True
        and policy.get("self_reported_completion_forbidden") is True
    ):
        passed("contract.claim_policy")
    else:
        block("contract.claim_policy", "release claim discipline weakened")

    claims = contract.get("current_claims")
    claims = claims if isinstance(claims, dict) else {}
    bad = {key: claims.get(key) for key in UNQUALIFIED_CLAIMS if claims.get(key) != "NOT_CLAIMED"}
    if bad:
        block("contract.unqualified_claims", f"unqualified claims promoted: {bad}")
    else:
        passed("contract.unqualified_claims")

    def read_text(relative):
        try:
            return (root / relative).read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            block(f"read:{relative}", str(exc))
            return ""

    notice = read_text("M3-SYNEXIA-NOTICE.md")
    rights = read_text("M3-SYNEXIA-RIGHTS.md")
    if "This notice does not relicense\nOpenJDK or JDK-derived source code as Apache-2.0." in notice:
        passed("rights.openjdk_boundary")
    else:
        block("rights.openjdk_boundary", "OpenJDK license boundary text missing")

    if "All ownership and all rights not expressly granted by the applicable license" in rights:
        passed("rights.reservation")
    else:
        block("rights.reservation", "first-party rights reservation missing")

    m3_readme = read_text("m3/README.md")
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
        try:
            mapping = load_json(mapping_path)
            seq = mapping.get("porting_invariant", {}).get("receiving_sequence")
        except (OSError, UnicodeError, ValueError, AttributeError) as exc:
            block("receiving_sequence.read", str(exc))
            seq = None
        if seq is None:
            detail = "receiving_sequence not yet merged; release requires String -> arrays -> collections -> AST/compiler -> remaining"
            if release_mode:
                block("receiving_sequence", detail)
            else:
                checks.append({"name": "receiving_sequence", "status": "PENDING", "detail": detail})
        elif isinstance(seq, dict) and seq.get("order") == EXPECTED_RECEIVING_ORDER and seq.get("active_phase") == "STRING":
            passed("receiving_sequence")
        else:
            block("receiving_sequence", "receiving order or active phase drifted")
    else:
        block("receiving_sequence", "name-mapping.json missing")

    release_contract = contract.get("release")
    release_contract = release_contract if isinstance(release_contract, dict) else {}
    upstream = release_contract.get("upstream_baseline")
    if (release_contract.get("append_only_history_required") is True
            and release_contract.get("single_fork_commit_required") is False):
        passed("contract.append_only_history")
    else:
        block("contract.append_only_history", "release must preserve upstream ancestry without requiring a squash")
    head = tree = ""
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
            if git(root, "status", "--porcelain=v1", "--untracked-files=no"):
                block("git.worktree", "tracked source differs from the inspected commit")
            else:
                passed("git.worktree")
        except (subprocess.CalledProcessError, FileNotFoundError) as exc:
            block("git.inspect", str(exc))

    if release_mode:
        validate_release_source(root, passed, block)

    source_admissible = not blockers and all(check["status"] == "PASS" for check in checks)
    if evidence is not None:
        validate_evidence(evidence, head, tree, platform, passed, block)
    elif release_mode:
        block("evidence.required", "release requires --evidence and --platform with every mandatory gate receipt")
    else:
        checks.append({"name": "evidence.required", "status": "PENDING",
                       "detail": "source inspection is not release admission"})

    return {
        "schema": "M3_RELEASE_ADMISSION_RESULT_V1",
        "mode": "release" if release_mode else "development",
        "source_admissible": source_admissible,
        "release_ready": release_mode and evidence is not None and source_admissible and not blockers,
        "platform": platform,
        "admission_scope": "LOCAL_PROOF_PACK_INTEGRITY_NOT_REMOTE_ATTESTATION",
        "blockers": blockers,
        "checks": checks,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--evidence", type=Path, help="CI-produced local proof-pack manifest")
    parser.add_argument("--platform", help="exact platform, for example linux-x64 or android-arm64")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sha256-out", type=Path)
    args = parser.parse_args()

    root = args.root.resolve()
    result = validate(root, release_mode=args.release, evidence=args.evidence, platform=args.platform)

    outputs = [path for path in (args.json_out, args.sha256_out) if path is not None]
    problems = output_errors(root, args.release, outputs)
    if not problems:
        created = []
        try:
            with contextlib.ExitStack() as stack:
                handles = {}
                # Reserve every new destination before writing either result. Exclusive
                # creation also refuses aliases or files introduced after the preflight.
                for path in outputs:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    handles[path] = stack.enter_context(path.open("x", encoding="utf-8"))
                    created.append(path)
                if args.sha256_out:
                    lines = []
                    for rel in REQUIRED_FILES + ["m3/release/verify_release_admission.py"]:
                        path = root / rel
                        if path.is_file():
                            lines.append(f"{sha256(path)}  {rel}")
                    handles[args.sha256_out].write("\n".join(lines) + "\n")
                if args.json_out:
                    handles[args.json_out].write(json.dumps(result, indent=2, sort_keys=True) + "\n")
        except OSError as exc:
            for path in created:
                path.unlink(missing_ok=True)
            problems.append({"name": "output.write", "detail": str(exc)})
    if problems:
        result["release_ready"] = False
        result["blockers"].extend(problems)
        result["checks"].extend({**problem, "status": "BLOCK"} for problem in problems)

    print(json.dumps(result, sort_keys=True))
    if result["blockers"] or (args.release and not result["release_ready"]):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
