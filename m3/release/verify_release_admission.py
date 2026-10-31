#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Validate product release policy and source-bound execution receipts.

The development command is a policy/reporting check, not release acceptance.
Only a complete, current evidence manifest can make release_ready true. This
consumer verifies receipt structure and bytes; reviewers remain responsible for
whether each recorded command actually covers its declared product contract.
It never executes commands contained in a receipt or runs a transformation.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import subprocess
from pathlib import Path

EXPECTED_PRIORITY = [
    "M3_STRING", "REGEX_PRECOMPUTE", "GENERAL_PRECOMPUTE", "ARRAYS",
    "COLLECTIONS", "SWT_ECLIPSE_IDE",
]
EXPECTED_RECEIVING_ORDER = [
    "STRING", "ARRAYS", "COLLECTIONS", "AST_COMPILER", "REMAINING_FAMILIES",
]
UNQUALIFIED_CLAIMS = {
    "java_lang_string_backed_by_m3_string", "full_regex_precompute",
    "full_jtreg_compatibility", "performance_superiority_over_openjdk21",
    "arrays_qualified", "collections_qualified", "swt_eclipse_ide_qualified",
}
REQUIRED_FILES = [
    "M3-SYNEXIA-NOTICE.md", "M3-SYNEXIA-RIGHTS.md", "M3-SYNEXIA-AUTHORS.md",
    "LICENSE-M3-APACHE-2.0.txt", "m3/README.md",
    "m3/release/release-contract.json", "m3/release/README.md",
    "m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md",
    "m3/docs/name-mapping.json", "m3/tooling/name-contract/name-plan.json",
]
REQUIRED_GATES = frozenset({
    "RIGHTS_AND_LICENSE_BOUNDARY", "SOURCE_TREE_AND_UPSTREAM_BASELINE",
    "OPENJDK_SANITY_MATRIX", "M3_FOUNDATION_TESTS",
    "PRECOMPUTE_JAVA_DIFFERENTIAL", "PRECOMPUTE_JNI_NATIVE_PARITY",
    "M3_MAVEN_CONTROL_REACTOR", "STRING_UTF16_CONTRACT",
    "RELEASE_CLAIMS_VALIDATION", "PROOF_PACK_SHA256",
})
RECIPE_ROOTS = (
    ".m3/openrewrite-recipes", ".m3/task-crates", "m3/recipes",
    "m3/tooling/migration-recipes", "m3/tooling/m3-jdk-a3-recipes",
    "m3/m3-import/pure-int-recipe-custody",
    "m3/runtime-integration/recipe",
    "m3/runtime-integration/char-array-copy/recipe",
    "m3/runtime-integration/utf8-cursor-20261014/recipe/canonical",
)
NAME_MAP = "m3/docs/name-mapping.json"
NAME_PLAN = "m3/tooling/name-contract/name-plan.json"
EVIDENCE_SCHEMA = "M3_RELEASE_EVIDENCE_V1"
DEFAULT_EVIDENCE = "m3/release/queue/current/evidence.json"
SHA1 = re.compile(r"[0-9a-f]{40}\Z")
SHA256 = re.compile(r"[0-9a-f]{64}\Z")
GATE_ID = re.compile(r"[A-Z][A-Z0-9_]*\Z")
SOURCE_SUFFIXES = frozenset({
    ".java", ".c", ".cc", ".cpp", ".cxx", ".h", ".hpp", ".py",
    ".sh", ".xml", ".yaml", ".yml", ".properties", ".json", ".tsv",
})


def unique_object(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError(f"duplicate JSON key: {key}")
        out[key] = value
    return out


def reject_constant(value):
    raise ValueError("non-finite JSON constant: " + value)


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"),
                      object_pairs_hook=unique_object,
                      parse_constant=reject_constant)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(root), *args], text=True,
        stderr=subprocess.PIPE).strip()


def git_paths(root: Path, *args: str) -> list[str]:
    data = subprocess.check_output(
        ["git", "-C", str(root), *args], text=True, stderr=subprocess.PIPE)
    return [path for path in data.split("\0") if path]


def untracked_source(root: Path) -> list[str]:
    paths = git_paths(root, "ls-files", "--others", "--exclude-standard", "-z",
                      "--", "src", "m3", ".github")
    return [path for path in paths if Path(path).suffix in SOURCE_SUFFIXES]


def safe_artifact(base: Path, relative: str) -> Path:
    """Reject aliases, traversal and every symlink component of an artifact."""
    if (not isinstance(relative, str) or not relative or "\\" in relative
            or any(ord(c) < 32 for c in relative)):
        raise ValueError("invalid evidence artifact path")
    parts = relative.split("/")
    if Path(relative).is_absolute() or any(p in ("", ".", "..") for p in parts):
        raise ValueError("evidence artifact must be a safe relative path")
    current = base.resolve()
    for part in parts:
        current = current / part
        if current.is_symlink():
            raise ValueError("evidence artifact contains a symlink: " + relative)
    if not current.resolve().is_relative_to(base.resolve()):
        raise ValueError("evidence artifact escapes manifest directory")
    if not current.is_file():
        raise ValueError("evidence artifact missing: " + relative)
    return current


def recipe_candidate(path: str) -> bool:
    name = Path(path).name
    return (Path(path).suffix in {".java", ".py", ".sh", ".yaml", ".yml"}
            or name == "pom.xml" or name.startswith("pom-") and name.endswith(".xml"))


def validate(root: Path, release_mode: bool = False,
             evidence_path: Path | None = None) -> dict:
    root = Path(root).resolve()
    checks, blockers, queue = [], [], []
    policy_failures = []
    source = {"commit": None, "tree": None}

    def passed(name: str, detail: str = ""):
        checks.append({"name": name, "status": "PASS", "detail": detail})

    def block(name: str, detail: str, *, policy: bool = True,
              status: str = "BLOCK"):
        checks.append({"name": name, "status": status, "detail": detail})
        blockers.append({"name": name, "detail": detail})
        queue.append({"id": name, "state": status, "blocking": True,
                      "detail": detail})
        if policy:
            policy_failures.append(name)

    def read_text(rel: str) -> str:
        try:
            return (root / rel).read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            block("read:" + rel, str(exc))
            return ""

    def read_object(path: Path, name: str) -> dict:
        try:
            obj = load_json(path)
            if not isinstance(obj, dict):
                raise ValueError("expected a JSON object")
            return obj
        except (ValueError, OSError, UnicodeError) as exc:
            block(name, str(exc))
            return {}

    for rel in REQUIRED_FILES:
        if (root / rel).is_file():
            passed("file:" + rel)
        else:
            block("file:" + rel, "required release-boundary file is missing")

    contract = read_object(root / "m3/release/release-contract.json", "contract.read")
    if contract.get("schema") == "M3_RELEASE_CONTRACT_V1":
        passed("contract.schema")
    else:
        block("contract.schema", "unexpected release contract schema")
    if contract.get("priority_order") == EXPECTED_PRIORITY:
        passed("contract.priority_order")
    else:
        block("contract.priority_order", "P0 order drifted")
    claim_policy = contract.get("claim_policy")
    if (isinstance(claim_policy, dict) and all(claim_policy.get(key) is True for key in (
            "evidence_before_claim", "marketing_must_reference_proof",
            "self_reported_completion_forbidden",
            "catalogue_or_recipe_execution_is_not_runtime_acceptance"))):
        passed("contract.claim_policy")
    else:
        block("contract.claim_policy", "release claim discipline weakened")
    claims = contract.get("current_claims")
    if not isinstance(claims, dict):
        claims = {}
    bad = {key: claims.get(key) for key in sorted(UNQUALIFIED_CLAIMS)
           if claims.get(key) != "NOT_CLAIMED"}
    if bad:
        block("contract.unqualified_claims", f"unqualified claims promoted: {bad}")
    else:
        passed("contract.unqualified_claims")

    gates = contract.get("mandatory_release_gates")
    if (not isinstance(gates, list) or not gates
            or any(not isinstance(g, str) or not GATE_ID.fullmatch(g) for g in gates)
            or len(gates) != len(set(gates))
            or not REQUIRED_GATES.issubset(gates)):
        block("contract.mandatory_gates", "missing, duplicate or invalid required gate IDs")
        gates = sorted(REQUIRED_GATES)
    else:
        passed("contract.mandatory_gates")

    evidence_policy = contract.get("execution_evidence")
    if (not isinstance(evidence_policy, dict)
            or evidence_policy.get("schema") != EVIDENCE_SCHEMA
            or evidence_policy.get("require_exact_commit_and_tree") is not True
            or evidence_policy.get("require_clean_source") is not True
            or evidence_policy.get("artifact_paths_relative_to") != "manifest_directory"
            or evidence_policy.get("legacy_recipe_sources_block_release") is not True):
        block("contract.execution_evidence", "source-bound execution evidence policy missing or weakened")
        evidence_policy = {}
    else:
        passed("contract.execution_evidence")
    gate_owners = evidence_policy.get("gate_owners", {})
    if (not isinstance(gate_owners, dict)
            or gate_owners.get("M3_MAVEN_CONTROL_REACTOR") != "hsoliwal/com.synexia"):
        block("contract.recipe_owner", "Maven/OpenRewrite execution must be Synexia-owned")
        gate_owners = {"M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"}

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

    mapping = read_object(root / NAME_MAP, "mapping.read")
    invariant = mapping.get("porting_invariant", {})
    sequence = invariant.get("receiving_sequence") if isinstance(invariant, dict) else None
    if (isinstance(sequence, dict) and sequence.get("order") == EXPECTED_RECEIVING_ORDER
            and sequence.get("active_phase") == "STRING"):
        passed("receiving_sequence")
    else:
        block("receiving_sequence", "receiving order or active phase missing or drifted")

    plan = read_object(root / NAME_PLAN, "name_queue.read")
    try:
        raw_mapping = (root / NAME_MAP).read_bytes()
        map_blob = hashlib.sha1(b"blob " + str(len(raw_mapping)).encode("ascii")
                                + b"\0" + raw_mapping).hexdigest()
        if (plan.get("schema") != "M3JDK21_NAME_CONTRACT_PLAN_V1"
                or plan.get("authority") != NAME_MAP
                or plan.get("authority_blob") != map_blob):
            raise ValueError("naming queue must pin the existing canonical map")
        migration = mapping.get("migration", {})
        records = migration.get("records") if isinstance(migration, dict) else None
        if not isinstance(records, list) or any(not isinstance(r, dict) for r in records):
            raise ValueError("canonical mapping records missing")
        ids = [r.get("id") for r in records]
        if any(not isinstance(i, str) or not i for i in ids) or len(ids) != len(set(ids)):
            raise ValueError("invalid or duplicate canonical mapping IDs")
        operations = plan.get("operations")
        if not isinstance(operations, list):
            raise ValueError("naming operations missing")
        for index, operation in enumerate(operations):
            if (not isinstance(operation, dict) or operation.get("mapping_id") not in ids
                    or operation.get("state") != "QUEUED"
                    or not isinstance(operation.get("reason"), str)
                    or not operation["reason"].strip()):
                raise ValueError("invalid or self-promoted naming operation")
            queue.append({"id": "naming:" + str(index) + ":" + operation["mapping_id"],
                          "state": "QUEUED", "blocking": False,
                          "mapping_id": operation["mapping_id"],
                          "kind": operation.get("kind"),
                          "before_type": operation.get("before_type"),
                          "after_type": operation.get("after_type"),
                          "authority": NAME_PLAN, "detail": operation["reason"]})
        passed("name_queue", f"{len(operations)} queued candidates; no naming promotion")
    except (ValueError, OSError, UnicodeError) as exc:
        block("name_queue", str(exc))

    release = contract.get("release", {})
    if not isinstance(release, dict):
        release = {}
    upstream = release.get("upstream_baseline")
    if not isinstance(upstream, str) or not SHA1.fullmatch(upstream):
        block("git.upstream_baseline", "invalid exact upstream SHA")
    else:
        passed("git.upstream_baseline", upstream)
    if (release.get("single_fork_commit_required") is not True
            or release.get("released_history_immutable_after_tag") is not True):
        block("contract.release_history", "existing release-history requirements must remain explicit")
    try:
        source["commit"] = git(root, "rev-parse", "HEAD")
        source["tree"] = git(root, "rev-parse", "HEAD^{tree}")
        passed("git.head", source["commit"])
        passed("git.tree", source["tree"])
        dirty = git(root, "status", "--porcelain=v1", "--untracked-files=no")
        extra_sources = untracked_source(root)
        if dirty or extra_sources:
            block("git.clean_source", "tracked modifications or untracked source: "
                  + (dirty + "\n" + "\n".join(extra_sources)).strip())
        else:
            passed("git.clean_source", "tracked tree and untracked source inputs are clean")
        if isinstance(upstream, str) and SHA1.fullmatch(upstream):
            merge_base = git(root, "merge-base", "HEAD", upstream)
            if merge_base == upstream:
                passed("git.merge_base", upstream)
            else:
                block("git.merge_base", f"expected {upstream}, got {merge_base}")
            ahead = int(git(root, "rev-list", "--count", f"{upstream}..HEAD"))
            parent = git(root, "rev-parse", "HEAD^")
            if ahead == 1 and parent == upstream:
                passed("git.single_release_commit", source["commit"])
            else:
                block("git.single_release_commit", f"ahead={ahead} parent={parent}; no history rewrite performed",
                      policy=release_mode, status="HOLD")
        tracked = git_paths(root, "ls-tree", "-r", "--name-only", "-z", "HEAD", "--", *RECIPE_ROOTS)
        residue = [p for p in tracked if recipe_candidate(p)]
        if residue:
            block("legacy_recipe_custody", f"{len(residue)} executable candidates remain under legacy recipe roots; "
                  "seal Synexia custody and callers before a reviewed migration",
                  policy=False, status="HOLD")
            queue[-1]["paths"] = residue
        else:
            passed("legacy_recipe_custody", "no executable candidates in the enumerated legacy roots; not a full-tree ownership proof")
    except (subprocess.CalledProcessError, FileNotFoundError, ValueError, OSError) as exc:
        block("git.inspect", str(exc))

    # Standalone rule (2026-10-31): M3JDK21 needs no other repository to build, test, gate or release
    # (check_standalone.py). Development mode is a ratchet against STANDALONE_BASELINE.tsv; release mode refuses any
    # cross-repository dependency when the contract's standalone.required_for_release is true. A tree without the
    # baseline, outside an enforced release, is reported without blocking.
    section = contract.get("standalone")
    required = isinstance(section, dict) and section.get("required_for_release") is True
    enforced = release_mode and required
    try:
        spec = importlib.util.spec_from_file_location("check_standalone", Path(__file__).with_name("check_standalone.py"))
        standalone = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(standalone)
        scanned = standalone.scan(root)
        baseline = standalone.read_baseline(root)
        status, detail = standalone.verdict(scanned, baseline, enforced)
        if status == "PASS":
            passed("standalone.no_external_repo_dependency", detail)
        elif baseline is None and not enforced:
            checks.append({"name": "standalone.no_external_repo_dependency", "status": "INFO", "detail": detail})
        else:
            block("standalone.no_external_repo_dependency", detail, policy=status == "BLOCK", status=status)
    except (subprocess.CalledProcessError, FileNotFoundError, OSError, ValueError, ImportError) as exc:
        block("standalone.no_external_repo_dependency", str(exc))

    manifest_path = Path(evidence_path) if evidence_path is not None else root / DEFAULT_EVIDENCE
    if not manifest_path.is_absolute():
        manifest_path = root / manifest_path
    manifest, entries = {}, {}
    if not manifest_path.is_file():
        block("evidence.manifest", "execution evidence manifest missing: " + str(manifest_path),
              policy=False, status="PENDING")
    else:
        try:
            if manifest_path.is_symlink():
                raise ValueError("evidence manifest must not be a symlink")
            manifest = load_json(manifest_path)
            if not isinstance(manifest, dict) or manifest.get("schema") != EVIDENCE_SCHEMA:
                raise ValueError("unexpected execution evidence schema")
            if (manifest.get("source_commit") != source["commit"]
                    or manifest.get("source_tree") != source["tree"]
                    or source["commit"] is None or source["tree"] is None):
                raise ValueError("stale execution evidence: commit/tree does not match inspected source")
            passed("evidence.source", source["commit"] + " " + source["tree"])
            data = manifest.get("gates")
            if not isinstance(data, list):
                raise ValueError("evidence gates must be a list")
            for entry in data:
                if not isinstance(entry, dict):
                    raise ValueError("invalid execution gate record")
                ident = entry.get("id")
                if not isinstance(ident, str) or ident not in gates or ident in entries:
                    raise ValueError("unknown or duplicate execution gate: " + str(ident))
                entries[ident] = entry
        except (ValueError, OSError, UnicodeError) as exc:
            entries = {}
            block("evidence.manifest", str(exc), policy=False)

    for ident in gates:
        entry = entries.get(ident)
        if entry is None:
            block("gate:" + ident, "no current execution receipt", policy=False, status="PENDING")
            continue
        try:
            if entry.get("status") != "PASS" or type(entry.get("exit_code")) is not int or entry["exit_code"] != 0:
                raise ValueError("gate did not record PASS with integer exit_code=0")
            expected_owner = gate_owners.get(ident, "hsoliwal/M3jdk21")
            if entry.get("owner_repository") != expected_owner:
                raise ValueError("wrong gate owner; expected " + str(expected_owner))
            command = entry.get("command")
            if (not isinstance(command, list) or not command
                    or any(not isinstance(x, str) or not x.strip() for x in command)):
                raise ValueError("exact command argv missing")
            environment = entry.get("environment")
            if (not isinstance(environment, dict) or any(
                    not isinstance(environment.get(key), str) or not environment[key].strip()
                    for key in ("os", "architecture", "toolchain"))):
                raise ValueError("OS, architecture and toolchain identity missing")
            if ident == "M3_MAVEN_CONTROL_REACTOR":
                canonical = entry.get("canonical_recipe")
                if (not isinstance(canonical, dict)
                        or canonical.get("repository") != "hsoliwal/com.synexia"
                        or not isinstance(canonical.get("commit"), str)
                        or not SHA1.fullmatch(canonical["commit"])
                        or not isinstance(canonical.get("recipe_id"), str)
                        or not canonical["recipe_id"].strip()):
                    raise ValueError("exact Synexia recipe revision and identity missing")
            artifacts = entry.get("artifacts")
            if not isinstance(artifacts, list) or not artifacts:
                raise ValueError("raw execution artifacts missing")
            seen = set()
            for artifact in artifacts:
                if not isinstance(artifact, dict):
                    raise ValueError("invalid execution artifact")
                path, digest = artifact.get("path"), artifact.get("sha256")
                if not isinstance(path, str) or path in seen:
                    raise ValueError("invalid or duplicate artifact path")
                seen.add(path)
                if not isinstance(digest, str) or not SHA256.fullmatch(digest):
                    raise ValueError("invalid artifact SHA-256")
                file = safe_artifact(manifest_path.parent, path)
                if file.resolve() == manifest_path.resolve():
                    raise ValueError("manifest cannot serve as its own execution artifact")
                if sha256(file) != digest:
                    raise ValueError("execution artifact SHA-256 mismatch: " + path)
            passed("gate:" + ident, f"{len(artifacts)} hash-verified artifacts; command provenance requires review")
        except (ValueError, OSError, UnicodeError) as exc:
            block("gate:" + ident, str(exc), policy=False)

    # A source-changing concurrent process invalidates the snapshot inspected above.
    try:
        if (git(root, "rev-parse", "HEAD") != source["commit"]
                or git(root, "rev-parse", "HEAD^{tree}") != source["tree"]
                or git(root, "status", "--porcelain=v1", "--untracked-files=no")
                or untracked_source(root)):
            block("git.stable_snapshot", "source changed during release validation")
    except (subprocess.CalledProcessError, FileNotFoundError, OSError) as exc:
        block("git.stable_snapshot", str(exc))

    return {
        "schema": "M3_RELEASE_ADMISSION_RESULT_V1",
        "mode": "release" if release_mode else "development",
        "policy_valid": not policy_failures,
        "release_ready": bool(entries) and not blockers,
        "source_commit": source["commit"], "source_tree": source["tree"],
        "evidence_manifest": str(manifest_path),
        "blockers": blockers, "checks": checks, "queue": queue,
        "scope": "Receipt integrity and explicit release policy only; no unrecorded runtime, performance or full-source claims.",
    }


def render_queue(result: dict) -> str:
    def cell(value):
        return str(value).replace("|", "\\|").replace("\r", " ").replace("\n", " ")
    lines = ["# M3JDK21 release queue", "",
             "Source commit: `" + str(result["source_commit"]) + "`  ",
             "Source tree: `" + str(result["source_tree"]) + "`", "",
             "Release ready: **" + str(result["release_ready"]).lower() + "**. "
             "Queued names remain candidates in the existing naming authority.", "",
             "| Item | State | Blocks release | Required work |",
             "| --- | --- | --- | --- |"]
    for row in result["queue"]:
        detail = row["detail"]
        if row.get("before_type") and row.get("after_type"):
            detail = str(row["before_type"]) + " -> " + str(row["after_type"]) + ": " + detail
        lines.append("| " + " | ".join(cell(v) for v in (
            row["id"], row["state"], "yes" if row["blocking"] else "no", detail)) + " |")
    if not result["queue"]:
        lines.append("| No pending items | VERIFIED_RECEIPTS | no | Review the exact proof pack before publication. |")
    lines.extend(["", "This is a generated view of gate evidence and `m3/tooling/name-contract/name-plan.json`. "
                  "It creates no second naming registry, recipe authority, or release claim.", ""])
    return "\n".join(lines)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--evidence", type=Path,
                        help="source-bound JSON manifest; artifact paths are relative to its directory")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sha256-out", type=Path)
    parser.add_argument("--queue-out", type=Path, help="write the visible pending-work Markdown queue")
    args = parser.parse_args(argv)
    root = args.root.resolve()
    result = validate(root, release_mode=args.release, evidence_path=args.evidence)

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
    if args.queue_out:
        args.queue_out.parent.mkdir(parents=True, exist_ok=True)
        args.queue_out.write_text(render_queue(result), encoding="utf-8")
    print(json.dumps(result, sort_keys=True))
    if not result["policy_valid"] or args.release and not result["release_ready"]:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
