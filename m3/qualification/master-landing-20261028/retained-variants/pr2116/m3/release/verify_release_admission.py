#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Target-only release receipt verification; never executes a source recipe.

Metadata validation is a boundary check. Release admission additionally requires
source-bound, completed proof receipts for every mandatory gate. This verifier
checks supplied evidence; it does not manufacture, execute or attest those gates.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path, PurePosixPath

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
]
MANDATORY_GATES = frozenset({
    "RIGHTS_AND_LICENSE_BOUNDARY", "SOURCE_TREE_AND_UPSTREAM_BASELINE",
    "OPENJDK_SANITY_MATRIX", "M3_FOUNDATION_TESTS", "PRECOMPUTE_JAVA_DIFFERENTIAL",
    "PRECOMPUTE_JNI_NATIVE_PARITY", "M3_MAVEN_CONTROL_REACTOR",
    "STRING_UTF16_CONTRACT", "RELEASE_CLAIMS_VALIDATION", "PROOF_PACK_SHA256",
})
REPOSITORY = "hsoliwal/M3jdk21"
HEX40 = re.compile(r"[0-9a-f]{40}")
HEX64 = re.compile(r"[0-9a-f]{64}")
REPORT_PREFIX = "m3/release/queue/runs/"
QUEUE_FIELDS = ("id", "status", "source_sha", "source_tree", "evidence", "next_action", "detail")


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
    return subprocess.check_output(
        ["git", "-C", str(root), *args], text=True, stderr=subprocess.PIPE, timeout=60,
    ).strip()



def git_paths(root: Path, *args: str) -> list[str]:
    # Preserve leading/trailing whitespace in filenames; Git's -z is the delimiter.
    output = subprocess.check_output(
        ["git", "-C", str(root), args[0], "-z", *args[1:]], text=True,
        stderr=subprocess.PIPE, timeout=60,
    )
    return [path for path in output.split("\0") if path]


def source_lease(root: Path) -> dict:
    """Observe a complete, clean source checkout, including ignored input files.

    Product builds run elsewhere. Only this exact generated report subtree is
    allowed in an admission checkout; no global target/build wildcard is used.
    """
    require(Path(git(root, "rev-parse", "--show-toplevel")).resolve() == root.resolve(),
            "admission root must be the complete Git worktree")
    head = git(root, "rev-parse", "HEAD")
    tree = git(root, "rev-parse", head + "^{tree}")
    require(not git(root, "status", "--porcelain=v1", "--untracked-files=no"), "tracked source worktree is dirty")
    require(all(entry.startswith("H ") for entry in git_paths(root, "ls-files", "-v")),
            "assume-unchanged, skip-worktree or unresolved index entries prevent exact-source admission")
    # Without --exclude-standard Git also enumerates .gitignore, info/exclude
    # and globally ignored files. Ignored Java/native/config inputs must refuse.
    unknown = git_paths(root, "ls-files", "--others")
    require(all(path.startswith(REPORT_PREFIX) for path in unknown),
            "untracked or ignored inputs outside the exact visible report root require an isolated source checkout")
    require(git(root, "rev-parse", "HEAD") == head, "HEAD changed while observing the source lease")
    return {"repository": REPOSITORY, "commit": head, "tree": tree}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def text_value(value) -> bool:
    return isinstance(value, str) and bool(value.strip())


def positive_int(value) -> bool:
    return type(value) is int and value > 0


def zero_int(value) -> bool:
    return type(value) is int and value == 0


def object_value(value, label: str) -> dict:
    require(isinstance(value, dict), label + " must be an object")
    return value


def proof_path(directory: Path, relative: str) -> Path:
    require(isinstance(relative, str) and relative and "\\" not in relative,
            "invalid proof artifact path")
    raw = PurePosixPath(relative)
    require(not raw.is_absolute() and all(p not in ("", ".", "..") for p in relative.split("/")),
            "proof artifact path must stay below the proof directory")
    current = directory
    for part in raw.parts:
        current = current / part
        require(not current.is_symlink(), "symlink proof artifact is refused")
    require(current.resolve().is_relative_to(directory.resolve()), "proof artifact escapes its directory")
    return current


def checked_artifact(directory: Path, descriptor) -> Path:
    item = object_value(descriptor, "artifact")
    expected = item.get("sha256")
    require(isinstance(expected, str) and HEX64.fullmatch(expected), "artifact lacks exact SHA-256")
    path = proof_path(directory, item.get("path"))
    require(path.is_file() and path.stat().st_size > 0, "proof artifact is missing or empty: " + str(path))
    require(sha256(path) == expected, "proof artifact checksum drift: " + item["path"])
    return path


def source_matches(actual, expected: dict) -> None:
    source = object_value(actual, "source binding")
    require(all(source.get(key) == expected[key] for key in ("repository", "commit", "tree")),
            "evidence belongs to a different repository, commit or tree")


def check_github_execution(directory: Path, execution: dict, source: dict) -> None:
    run = object_value(load_json(checked_artifact(directory, execution.get("run"))), "GitHub run")
    jobs = object_value(load_json(checked_artifact(directory, execution.get("jobs"))), "GitHub jobs")
    check_github_data(run, jobs, execution, source)


def check_github_data(run: dict, jobs: dict, execution: dict, source: dict) -> None:
    require(positive_int(run.get("id")), "GitHub run ID is missing")
    require(run.get("head_sha") == source["commit"], "GitHub run is stale for this source commit")
    require(object_value(run.get("repository"), "GitHub repository").get("full_name") == REPOSITORY,
            "GitHub run belongs to a different repository")
    require(run.get("status") == "completed" and run.get("conclusion") == "success",
            "GitHub run is queued, unfinished or unsuccessful")
    rows = jobs.get("jobs")
    require(isinstance(rows, list) and positive_int(jobs.get("total_count"))
            and len(rows) == jobs["total_count"], "GitHub jobs are empty or incompletely paginated")
    by_id = {}
    for raw in rows:
        job = object_value(raw, "GitHub job")
        job_id = job.get("id")
        require(positive_int(job_id) and job_id not in by_id, "invalid or duplicate GitHub job ID")
        require(job.get("run_id") == run["id"] and job.get("head_sha") == source["commit"],
                "GitHub job has stale run/source identity")
        by_id[job_id] = job
    selected = execution.get("job_ids")
    require(isinstance(selected, list) and selected and all(positive_int(n) for n in selected)
            and len(selected) == len(set(selected)), "executed GitHub job selection is missing or duplicated")
    required_steps = execution.get("required_steps")
    require(isinstance(required_steps, list) and required_steps and all(text_value(s) for s in required_steps)
            and len(required_steps) == len(set(required_steps)), "actual gate steps must be named")
    succeeded_steps = set()
    for job_id in selected:
        require(job_id in by_id, "selected GitHub job is absent")
        job = by_id[job_id]
        require(job.get("status") == "completed" and job.get("conclusion") == "success",
                "selected GitHub job did not succeed")
        steps = job.get("steps")
        require(isinstance(steps, list) and steps, "selected GitHub job has zero executed steps")
        for raw in steps:
            step = object_value(raw, "GitHub step")
            if step.get("status") == "completed" and step.get("conclusion") == "success":
                require(text_value(step.get("name")), "successful GitHub step has no name")
                succeeded_steps.add(step["name"])
    require(set(required_steps).issubset(succeeded_steps), "required gate steps were skipped, missing or failed")


def check_receipt(directory: Path, entry: dict, source: dict) -> str:
    path = checked_artifact(directory, entry.get("receipt"))
    receipt = object_value(load_json(path), "gate receipt")
    require(receipt.get("schema") == "M3_RELEASE_GATE_RECEIPT_V1", "unexpected gate receipt schema")
    require(receipt.get("gate") == entry["id"], "gate receipt ID mismatch")
    source_matches(receipt.get("source"), source)
    require(receipt.get("status") == "PASS" and zero_int(receipt.get("exit_code")),
            "gate receipt is pending, failed or has no successful exit")
    command = receipt.get("command")
    require(isinstance(command, list) and command and all(text_value(arg) for arg in command),
            "gate lacks its actual command vector")
    environment = object_value(receipt.get("environment"), "gate environment")
    require(all(text_value(environment.get(key)) for key in ("os", "arch", "toolchain")),
            "gate environment is incomplete")
    checks = object_value(receipt.get("checks"), "executed checks")
    require(positive_int(checks.get("executed")) and zero_int(checks.get("failed")),
            "gate executed no checks or reports failures")
    artifacts = receipt.get("artifacts")
    require(isinstance(artifacts, list) and artifacts, "gate lacks raw result artifacts")
    seen = set()
    for artifact in artifacts:
        result = checked_artifact(directory, artifact)
        require(result not in seen and result != path, "duplicate or self-referential result artifact")
        seen.add(result)
    execution = object_value(receipt.get("execution"), "gate execution")
    require(execution.get("status") == "completed", "gate execution is unfinished")
    kind = execution.get("kind")
    if kind == "GITHUB_ACTIONS":
        check_github_execution(directory, execution, source)
    else:
        require(kind == "LOCAL", "unrecognized execution provider")
    return entry["receipt"]["path"]



def github_api(endpoint: str, paginated: bool = False):
    # Official GitHub CLI; read-only GET, fixed host and repository, no shell.
    command = ["gh", "api", "--hostname", "github.com", "--method", "GET", endpoint]
    if paginated:
        command.extend(["--paginate", "--slurp"])
    output = subprocess.check_output(command, text=True, stderr=subprocess.PIPE, timeout=60)
    return json.loads(output, object_pairs_hook=unique_object)


def check_trusted_producer(root: Path, directory: Path, entry: dict, source: dict,
                           contract: dict, cache: dict) -> None:
    policy = object_value(contract.get("trusted_gate_producers"), "trusted producer policy")
    approved = object_value(policy.get(entry["id"]), "approved gate producer")
    require(approved.get("status") == "QUALIFIED", "gate producer has not been independently qualified")
    workflow = approved.get("workflow")
    require(isinstance(workflow, str) and workflow.startswith(".github/workflows/")
            and ".." not in workflow and workflow.endswith((".yml", ".yaml")), "invalid approved workflow")
    require(git(root, "rev-parse", source["commit"] + ":" + workflow) == approved.get("workflow_blob"),
            "approved workflow source blob has drifted")
    receipt = object_value(load_json(checked_artifact(directory, entry["receipt"])), "gate receipt")
    execution = object_value(receipt.get("execution"), "gate execution")
    require(execution.get("kind") == "GITHUB_ACTIONS", "local/self-authored receipts cannot independently admit release")
    recorded = object_value(load_json(checked_artifact(directory, execution.get("run"))), "GitHub run")
    run_id = recorded.get("id")
    require(positive_int(run_id), "invalid live run ID")
    if run_id not in cache:
        endpoint = "repos/" + REPOSITORY + "/actions/runs/" + str(run_id)
        run = object_value(github_api(endpoint), "live GitHub run")
        pages = github_api(endpoint + "/jobs?filter=latest&per_page=100", paginated=True)
        require(isinstance(pages, list) and pages, "live jobs pagination is empty")
        counts = set()
        for page in pages:
            count = object_value(page, "live jobs page").get("total_count")
            require(positive_int(count), "live jobs count is invalid")
            counts.add(count)
        require(len(counts) == 1, "live jobs count changed during pagination")
        rows = []
        for page in pages:
            require(isinstance(page.get("jobs"), list), "live jobs page is incomplete")
            rows.extend(page["jobs"])
        cache[run_id] = (run, {"total_count": next(iter(counts)), "jobs": rows})
    run, jobs = cache[run_id]
    require(run.get("id") == run_id and run.get("path") == workflow, "live workflow identity disagrees with approved producer")
    require(run.get("run_attempt") == recorded.get("run_attempt") and positive_int(run.get("run_attempt")),
            "workflow attempt changed; refresh proof artifacts")
    check_github_data(run, jobs, execution, source)
    expected_jobs = approved.get("jobs")
    require(isinstance(expected_jobs, list) and expected_jobs, "approved producer lacks required job/step policy")
    selected = [job for job in jobs["jobs"] if job["id"] in execution["job_ids"]]
    by_name = {job.get("name"): job for job in selected}
    require(len(by_name) == len(selected), "ambiguous producer job names")
    expected_names = []
    for raw in expected_jobs:
        expected = object_value(raw, "approved job")
        name = expected.get("name")
        require(text_value(name) and name not in expected_names and name in by_name, "approved job is missing or duplicated")
        expected_names.append(name)
        steps = expected.get("required_steps")
        require(isinstance(steps, list) and steps and all(text_value(step) for step in steps), "approved job lacks real gate steps")
        succeeded = {step.get("name") for step in by_name[name]["steps"]
                     if step.get("status") == "completed" and step.get("conclusion") == "success"}
        require(set(steps).issubset(succeeded), "approved gate steps did not execute successfully")
    require(set(expected_names) == set(by_name), "unapproved GitHub job selection")


def custody_report(root: Path, scope: str) -> dict:
    """Refuse existing recipe execution wiring before a release job invokes it."""
    scopes = {
        "foundation": ["m3/recipes", "m3/runtime-integration/char-array-copy/recipe", "m3/runtime-integration/recipe"],
        "precompute": ["m3/runtime-integration/utf8-cursor-20261014/recipe"],
        "control-reactor": [],
    }
    require(scope in scopes, "unknown custody scope")
    source = {"repository": REPOSITORY, "commit": "UNKNOWN", "tree": "UNKNOWN"}
    errors, residue = [], []
    try:
        source.update(commit=git(root, "rev-parse", "HEAD"), tree=git(root, "rev-parse", "HEAD^{tree}"))
        for relative in scopes[scope]:
            directory = root / relative
            if directory.is_dir():
                residue.extend(str(path.relative_to(root)) for path in directory.rglob("*")
                               if path.is_file() and path.suffix in (".py", ".java", ".yml", ".yaml", ".xml", ".sh"))
        if scope == "control-reactor":
            pom = root / "m3/pom.xml"
            document = ET.parse(pom)
            legacy = {"synexia-import/pure-int-recipe-custody", "tooling/migration-recipes",
                      "tooling/synexia-jdk-a3-recipes", "tooling/recipe-agent-api"}
            for module in document.findall(".//{http://maven.apache.org/POM/4.0.0}module"):
                if module.text and module.text.strip() in legacy:
                    residue.append("m3/pom.xml -> " + module.text.strip())
    except (OSError, ValueError, ET.ParseError, subprocess.SubprocessError) as error:
        errors.append(str(error))
    residue = sorted(set(residue))
    held = bool(residue or errors)
    detail = ("Target-local recipe execution is blocked; " + str(len(residue)) + " residue paths/wirings found. "
              + "; ".join(errors)) if held else "No known recipe residue in this job scope; product gates remain independent."
    row = {"id": "CUSTODY_" + scope.upper().replace("-", "_"), "status": "BLOCKED" if held else "PASS",
           "source_sha": source["commit"], "source_tree": source["tree"],
           "evidence": "; ".join(residue), "detail": detail,
           "next_action": "Seal recipe bytes and callers in Synexia, execute there, and qualify replacement target-only job wiring before retrying."}
    return {"schema": "M3_RELEASE_ADMISSION_RESULT_V1", "mode": "custody-preflight", "source": source,
            "boundary_ready": not held, "evidence_integrity_ready": False, "release_ready": False,
            "blockers": [{"name": row["id"], "detail": detail}] if held else [], "checks": [], "queue": [row], "residue": residue}


def validate(root: Path, release_mode: bool = False, evidence_path: Path | None = None,
             verify_github: bool = False) -> dict:
    root = root.resolve()
    checks, blockers, queue = [], [], []
    source = {"repository": REPOSITORY, "commit": "UNKNOWN", "tree": "UNKNOWN"}

    def passed(name: str, detail: str = "", evidence: str = ""):
        checks.append({"name": name, "status": "PASS", "detail": detail})
        queue.append({"id": name, "status": "PASS", "evidence": evidence, "next_action": "Retain exact evidence.", "detail": detail})

    def block(name: str, detail: str, evidence: str = "", status: str = "BLOCKED"):
        checks.append({"name": name, "status": "BLOCK", "detail": detail})
        blockers.append({"name": name, "detail": detail})
        queue.append({"id": name, "status": status, "evidence": evidence,
                      "next_action": "Resolve this finding and rerun against the exact candidate source.", "detail": detail})

    initial_lease = None
    try:
        initial_lease = source_lease(root)
        source.update(initial_lease)
    except (OSError, UnicodeError, ValueError, subprocess.SubprocessError) as error:
        block("git.source_and_ancestry", str(error))

    def read_text(relative: str) -> str:
        try:
            if initial_lease is not None:
                # Policy bytes come from the immutable leased commit, not from
                # mutable files that could change between the two lease checks.
                return subprocess.check_output(
                    ["git", "-C", str(root), "show", source["commit"] + ":" + relative],
                    text=True, stderr=subprocess.PIPE, timeout=60,
                )
            return (root / relative).read_text(encoding="utf-8")
        except (OSError, UnicodeError, subprocess.SubprocessError) as error:
            block("read:" + relative, str(error))
            return ""

    for relative in REQUIRED_FILES:
        if (root / relative).is_file():
            passed("file:" + relative)
        else:
            block("file:" + relative, "required release-boundary file is missing")
    try:
        contract = object_value(json.loads(read_text("m3/release/release-contract.json"), object_pairs_hook=unique_object), "release contract")
    except (OSError, UnicodeError, ValueError) as error:
        block("contract.read", str(error))
        contract = {}
    if contract.get("schema") == "M3_RELEASE_CONTRACT_V1":
        passed("contract.schema")
    else:
        block("contract.schema", "unexpected release contract schema")
    if contract.get("priority_order") == EXPECTED_PRIORITY:
        passed("contract.priority_order")
    else:
        block("contract.priority_order", "P0 order drifted")
    policy = contract.get("claim_policy")
    if isinstance(policy, dict) and all(policy.get(key) is True for key in (
            "evidence_before_claim", "marketing_must_reference_proof", "self_reported_completion_forbidden",
            "catalogue_or_recipe_execution_is_not_runtime_acceptance")):
        passed("contract.claim_policy")
    else:
        block("contract.claim_policy", "release claim discipline weakened")
    claims = contract.get("current_claims")
    if isinstance(claims, dict) and all(claims.get(key) == "NOT_CLAIMED" for key in UNQUALIFIED_CLAIMS):
        passed("contract.unqualified_claims")
    else:
        block("contract.unqualified_claims", "unqualified claims promoted or omitted")
    release = contract.get("release")
    if not isinstance(release, dict):
        release = {}
    if (release.get("history_policy") == "APPEND_ONLY"
            and release.get("single_fork_commit_required") is False
            and release.get("development_history_immutable") is True
            and release.get("released_history_immutable_after_tag") is True):
        passed("contract.append_only_history")
    else:
        block("contract.append_only_history", "release must preserve development history and upstream ancestry")
    if "This notice does not relicense\nOpenJDK or JDK-derived source code as Apache-2.0." in read_text("M3-SYNEXIA-NOTICE.md"):
        passed("rights.openjdk_boundary")
    else:
        block("rights.openjdk_boundary", "OpenJDK license boundary text missing")
    if "All ownership and all rights not expressly granted by the applicable license" in read_text("M3-SYNEXIA-RIGHTS.md"):
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
    try:
        mapping = object_value(json.loads(read_text("m3/docs/name-mapping.json"), object_pairs_hook=unique_object), "name mapping")
        seq = object_value(object_value(mapping.get("porting_invariant"), "porting invariant").get("receiving_sequence"), "receiving sequence")
        require(seq.get("order") == EXPECTED_RECEIVING_ORDER and seq.get("active_phase") == "STRING",
                "receiving order or active phase drifted")
        passed("receiving_sequence")
    except (OSError, UnicodeError, ValueError) as error:
        block("receiving_sequence", str(error))
    upstream = release.get("upstream_baseline")
    try:
        require(isinstance(upstream, str) and HEX40.fullmatch(upstream), "invalid exact upstream SHA")
        require(initial_lease is not None, "initial exact-source lease was not established")
        require(git(root, "merge-base", source["commit"], upstream) == upstream, "upstream baseline is not an ancestor")
        passed("git.source_and_ancestry", source["commit"] + " tree=" + source["tree"])
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        block("git.source_and_ancestry", str(error))
    mandatory = contract.get("mandatory_release_gates")
    if (not isinstance(mandatory, list) or not mandatory or not all(text_value(n) for n in mandatory)
            or len(mandatory) != len(set(mandatory)) or not MANDATORY_GATES.issubset(mandatory)):
        block("contract.mandatory_release_gates", "mandatory gates are missing, duplicated or weakened")
        mandatory = sorted(MANDATORY_GATES)
    else:
        passed("contract.mandatory_release_gates")
    boundary_ready = not blockers
    entries = {}
    proof_pack_seal = None
    evidence_label = str(evidence_path) if evidence_path else ""
    if evidence_path is None:
        block("proof.pack", "no source-bound proof pack was supplied", status="NOT_RUN")
    else:
        try:
            evidence_path = evidence_path.absolute()
            require(not evidence_path.is_symlink(), "symlink proof pack is refused")
            proof_pack_seal = sha256(evidence_path)
            pack = object_value(load_json(evidence_path), "proof pack")
            require(pack.get("schema") == "M3_RELEASE_PROOF_PACK_V1", "unexpected proof pack schema")
            source_matches(pack.get("source"), source)
            rows = pack.get("gates")
            require(isinstance(rows, list), "proof gate list is absent")
            for raw in rows:
                entry = object_value(raw, "proof gate")
                ident = entry.get("id")
                require(isinstance(ident, str) and ident in mandatory and ident not in entries,
                        "unknown or duplicate proof gate")
                entries[ident] = entry
            passed("proof.pack", "exact source-bound proof pack", evidence_label)
        except (OSError, UnicodeError, ValueError) as error:
            block("proof.pack", str(error), evidence_label)
            entries = {}
    for gate in mandatory:
        entry = entries.get(gate)
        if entry is None:
            block("gate:" + gate, "mandatory executed receipt is missing", evidence_label, "NOT_RUN")
            continue
        try:
            receipt = check_receipt(evidence_path.parent, entry, source)
            passed("gate:" + gate, "completed checks and raw artifact hashes verified", receipt)
        except (OSError, UnicodeError, ValueError) as error:
            block("gate:" + gate, str(error), str(entry.get("receipt", "")))
    evidence_integrity_ready = not blockers
    if release_mode:
        if not verify_github:
            block("producer.verification", "hash checks establish integrity only; release needs fresh official GitHub verification of qualified gate producers", evidence_label)
        elif evidence_integrity_ready:
            cache = {}
            for gate in mandatory:
                try:
                    check_trusted_producer(root, evidence_path.parent, entries[gate], source, contract, cache)
                    passed("producer:" + gate, "qualified workflow blob and fresh GitHub run/job/step identity verified")
                except (OSError, UnicodeError, ValueError, subprocess.SubprocessError) as error:
                    block("producer:" + gate, str(error), evidence_label)
    # Network verification can be slow. No earlier observation leases later
    # source/proof bytes: recheck both before returning any admission decision.
    if evidence_integrity_ready:
        try:
            require(not evidence_path.is_symlink() and sha256(evidence_path) == proof_pack_seal,
                    "proof pack changed during verification")
            for gate in mandatory:
                check_receipt(evidence_path.parent, entries[gate], source)
            passed("proof.final_integrity", "proof pack, receipts and all raw artifacts retained their verified hashes")
        except (OSError, UnicodeError, ValueError) as error:
            evidence_integrity_ready = False
            block("proof.final_integrity", str(error), evidence_label)
    source_lease_verified = False
    if initial_lease is not None:
        try:
            require(source_lease(root) == initial_lease, "source commit/tree changed during verification")
            source_lease_verified = True
            passed("git.final_source_lease", "exact source identity and complete input cleanliness rechecked")
        except (OSError, UnicodeError, ValueError, subprocess.SubprocessError) as error:
            boundary_ready = False
            evidence_integrity_ready = False
            block("git.final_source_lease", str(error))
    for row in queue:
        row.update(source_sha=source["commit"], source_tree=source["tree"])
    return {
        "schema": "M3_RELEASE_ADMISSION_RESULT_V1", "mode": "release" if release_mode else "development",
        "source": source, "source_lease_verified": source_lease_verified,
        "proof_pack_sha256": proof_pack_seal,
        "boundary_ready": boundary_ready, "evidence_integrity_ready": evidence_integrity_ready,
        "release_ready": release_mode and not blockers, "blockers": blockers,
        "checks": checks, "queue": sorted(queue, key=lambda row: row["id"]),
    }


def queue_tsv(result: dict) -> str:
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=QUEUE_FIELDS, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(result["queue"])
    return output.getvalue()


def check_output_path(path: Path) -> Path:
    absolute = path.absolute()
    for item in (absolute, *absolute.parents):
        require(not item.is_symlink(), "symlink output path is refused")
    canonical = absolute.resolve()
    if canonical.exists():
        require(canonical.is_file() and canonical.stat().st_nlink == 1,
                "output must be a regular file without hard-link aliases")
    return canonical


def preflight_outputs(root: Path, evidence_path: Path | None, outputs: list[Path | None]) -> list[Path | None]:
    """No output may overwrite source, Git metadata, evidence, or another report."""
    result = []
    proof_root = evidence_path.absolute().parent.resolve() if evidence_path is not None else None
    git_roots = []
    for option in ("--absolute-git-dir", "--git-common-dir"):
        directory = Path(git(root, "rev-parse", option))
        git_roots.append((directory if directory.is_absolute() else root / directory).resolve())
    for output in outputs:
        if output is None:
            result.append(None)
            continue
        path = check_output_path(output)
        require(not any(path.is_relative_to(directory) for directory in git_roots), "output overlaps Git metadata")
        require(proof_root is None or not path.is_relative_to(proof_root), "output overlaps the proof input directory")
        if path.is_relative_to(root):
            relative = path.relative_to(root).as_posix()
            require(relative.startswith(REPORT_PREFIX), "in-worktree output must stay below the exact visible report root")
            require(not git_paths(root, "ls-files", "--", relative), "output overlaps a tracked source file")
        for other in result:
            if other is not None:
                require(not path.is_relative_to(other) and not other.is_relative_to(path),
                        "output destinations overlap each other")
        result.append(path)
    return result


def write_output(path: Path | None, content: str) -> None:
    if path is not None:
        # Recheck aliases and use replacement rather than truncating an existing
        # inode, so a hard-link alias cannot overwrite an input after preflight.
        path = check_output_path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent,
                                             prefix=".m3-report-", delete=False) as stream:
                temporary = Path(stream.name)
                stream.write(content)
            check_output_path(path)
            os.replace(temporary, path)
        finally:
            if temporary is not None and temporary.exists():
                temporary.unlink()


def output_refusal(error: Exception) -> dict:
    return {"schema": "M3_RELEASE_ADMISSION_RESULT_V1", "mode": "output-preflight",
            "boundary_ready": False, "evidence_integrity_ready": False, "release_ready": False,
            "blockers": [{"name": "output.preflight", "detail": str(error)}], "checks": [], "queue": []}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--evidence", type=Path, help="source-bound proof-pack.json; absence always holds release")
    parser.add_argument("--verify-github", action="store_true", help="verify approved gate producers with fresh official gh api GETs")
    parser.add_argument("--custody-check", choices=("foundation", "precompute", "control-reactor"))
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sha256-out", type=Path)
    parser.add_argument("--queue-out", type=Path, help="visible per-gate TSV including pending/blocked findings")
    args = parser.parse_args()
    root = args.root.resolve()
    outputs = [args.json_out, args.queue_out, args.sha256_out]
    try:
        outputs = preflight_outputs(root, args.evidence, outputs)
        result = (custody_report(root, args.custody_check) if args.custody_check else
                  validate(root, release_mode=args.release, evidence_path=args.evidence, verify_github=args.verify_github))
        # Reject aliases introduced while producer verification was in progress.
        outputs = preflight_outputs(root, args.evidence, outputs)
        lines = [f"{sha256(root / rel)}  {rel}" for rel in
                 REQUIRED_FILES + ["m3/release/verify_release_admission.py"] if (root / rel).is_file()]
        write_output(outputs[0], json.dumps(result, indent=2, sort_keys=True) + "\n")
        write_output(outputs[1], queue_tsv(result))
        write_output(outputs[2], "\n".join(lines) + "\n")
    except (OSError, UnicodeError, ValueError, subprocess.SubprocessError) as error:
        print(json.dumps(output_refusal(error), sort_keys=True))
        return 2
    print(json.dumps(result, sort_keys=True))
    return int(bool(result["blockers"]) if args.custody_check else args.release and not result["release_ready"])


if __name__ == "__main__":
    raise SystemExit(main())
