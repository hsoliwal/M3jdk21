#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Collect per-gate execution receipts into the M3_RELEASE_EVIDENCE_V1 manifest.

Data-only receiver tooling for ``verify_release_admission.py``:

* ``run``      executes one product gate command that the release workflow
               already runs and records its exit status, environment and raw
               log as a receipt (``<GATE>.receipt.json`` beside its artifacts);
* ``receipt``  wraps an arbitrary argv the same way (used by the workflow jobs);
* ``check``    runs the release verifier in-process and fails unless the named
               checks PASS (the command behind the contract-derived gates);
* ``assemble`` binds existing receipts to the inspected HEAD commit/tree, copies
               their hash-verified artifacts and writes ``evidence.json`` plus a
               ``pending.tsv`` of gates without a current PASS receipt.

It never executes a Synexia recipe. The ``M3_MAVEN_CONTROL_REACTOR`` receipt is
produced in ``hsoliwal/com.synexia`` and imported from a receipts directory.
A second ``assemble`` over unchanged receipts writes byte-identical output.
Exit status 3 means the manifest is incomplete; the missing gates are listed.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import platform
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
_SPEC = importlib.util.spec_from_file_location(
    "m3_release_admission", HERE / "verify_release_admission.py")
release = importlib.util.module_from_spec(_SPEC)
assert _SPEC.loader is not None
_SPEC.loader.exec_module(release)

RECEIPT_SUFFIX = ".receipt.json"
DEFAULT_OUT = "m3/release/queue/current"
SYNEXIA_RECEIPTS = "m3/release/queue/synexia"
DEFAULT_REPO = "hsoliwal/M3jdk21"
SYNEXIA_REPO = "hsoliwal/com.synexia"
ADMISSION_WORKFLOW = "m3-first-release-admission.yml"
SANITY_WORKFLOW = "main.yml"
RECEIPT_ARTIFACT_PREFIX = "m3-gate-receipt-"
EXIT_INCOMPLETE = 3
EXIT_NO_PRODUCER = 2
VERIFY = "m3/release/verify_release_admission.py"
SELF = "m3/release/collect_release_evidence.py"
PY = "{python}"
# Gate table: id -> (producer, local command argv, extra artifacts).
# "{out}" is the receipt directory, "{python}" the running interpreter.
GATES = {
    "RIGHTS_AND_LICENSE_BOUNDARY": (
        ADMISSION_WORKFLOW + "#contract",
        [PY, SELF, "check", "--prefix", "file:", "--prefix", "rights."], []),
    "SOURCE_TREE_AND_UPSTREAM_BASELINE": (
        ADMISSION_WORKFLOW + "#contract",
        [PY, SELF, "check", "--release", "--prefix", "git.upstream_baseline",
         "--prefix", "git.merge_base", "--prefix", "git.clean_source",
         "--prefix", "git.single_release_commit"], []),
    "OPENJDK_SANITY_MATRIX": (
        SANITY_WORKFLOW + " (OpenJDK GHA Sanity Checks; receipt from the run record)", [], []),
    "M3_FOUNDATION_TESTS": (
        ADMISSION_WORKFLOW + "#foundation", ["bash", "./m3/build.sh"], []),
    "PRECOMPUTE_JAVA_DIFFERENTIAL": (
        ADMISSION_WORKFLOW + "#precompute",
        ["mvn", "-B", "-ntp", "-f", "m3/ports/precompute/pom.xml", "test"], []),
    "PRECOMPUTE_JNI_NATIVE_PARITY": (
        ADMISSION_WORKFLOW + "#precompute",
        ["mvn", "-B", "-ntp", "-f", "m3/ports/precompute/pom.xml", "-Pnative", "test"], []),
    "M3_MAVEN_CONTROL_REACTOR": (
        SYNEXIA_REPO + " (imported receipt; never executed here)", [], []),
    "STRING_UTF16_CONTRACT": (
        "UNBOUND: no workflow job or command is named for this gate", [], []),
    "RELEASE_CLAIMS_VALIDATION": (
        ADMISSION_WORKFLOW + "#contract",
        [PY, VERIFY, "--root", ".", "--json-out", "{out}/admission.json",
         "--queue-out", "{out}/README.md"], ["{out}/admission.json"]),
    "PROOF_PACK_SHA256": (
        ADMISSION_WORKFLOW + "#contract",
        [PY, VERIFY, "--root", ".", "--sha256-out", "{out}/SHA256SUMS"],
        ["{out}/SHA256SUMS"]),
}


def write_bytes_if_changed(path: Path, data: bytes) -> bool:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.is_file() and path.read_bytes() == data:
        return False
    path.write_bytes(data)
    return True


def write_json(path: Path, obj) -> bool:
    return write_bytes_if_changed(
        path, (json.dumps(obj, indent=2, sort_keys=True) + "\n").encode("utf-8"))


def head(root: Path) -> tuple[str, str]:
    return release.git(root, "rev-parse", "HEAD"), release.git(root, "rev-parse", "HEAD^{tree}")


def contract_gates(root: Path) -> tuple[list[str], dict]:
    contract = release.load_json(root / "m3/release/release-contract.json")
    gates = contract.get("mandatory_release_gates") or sorted(release.REQUIRED_GATES)
    owners = contract.get("execution_evidence", {}).get("gate_owners", {})
    return list(gates), dict(owners)


def default_toolchain() -> str:
    parts = ["python " + platform.python_version()]
    java = shutil.which("java")
    if java:
        probe = subprocess.run([java, "-version"], capture_output=True, text=True)
        parts.append((probe.stderr or probe.stdout).strip().splitlines()[0])
    return "; ".join(parts)


def environment(toolchain: str | None) -> dict:
    return {"os": platform.system() + " " + platform.release(),
            "architecture": platform.machine() or "unknown",
            "toolchain": toolchain or default_toolchain()}


def expand(argv: list[str], out: Path) -> list[str]:
    command = [a.replace("{out}", out.as_posix()).replace(PY, sys.executable) for a in argv]
    command[0] = shutil.which(command[0]) or command[0]
    return command


def copy_artifact(src: Path, dst: Path) -> dict:
    if src.resolve() != dst.resolve():
        write_bytes_if_changed(dst, src.read_bytes())
    return {"path": dst.name, "sha256": release.sha256(dst)}


def record_receipt(root: Path, out: Path, ident: str, command: list[str],
                   extra: list[str], meta: dict) -> int:
    out.mkdir(parents=True, exist_ok=True)
    log = out / (ident + ".log")
    with log.open("wb") as stream:
        code = subprocess.call(command, cwd=str(root), stdout=stream, stderr=subprocess.STDOUT)
    artifacts = [{"path": log.name, "sha256": release.sha256(log)}]
    artifacts += [copy_artifact(root / item, out / Path(item).name) for item in extra]
    commit, tree = head(root)
    record = {"id": ident, "status": "PASS" if code == 0 else "FAIL", "exit_code": code,
              "command": command, "artifacts": artifacts,
              "source_commit": commit, "source_tree": tree, **meta}
    write_json(out / (ident + RECEIPT_SUFFIX), record)
    print(json.dumps({"gate": ident, "status": record["status"], "exit_code": code,
                      "receipt": str(out / (ident + RECEIPT_SUFFIX))}, sort_keys=True))
    return code


def cmd_run(args) -> int:
    producer, argv, extra = GATES[args.gate]
    if not argv:
        print(f"{args.gate}: no local producer; {producer}", file=sys.stderr)
        return EXIT_NO_PRODUCER
    out = Path(args.out).resolve()
    meta = {"owner_repository": DEFAULT_REPO, "environment": environment(args.toolchain),
            "producer": producer}
    if args.run_url:
        meta["run_url"] = args.run_url
    return record_receipt(args.root.resolve(), out, args.gate, expand(argv, out),
                          [e.replace("{out}", out.as_posix()) for e in extra], meta)


def cmd_receipt(args) -> int:
    out = Path(args.out).resolve()
    meta = {"owner_repository": args.owner, "environment": environment(args.toolchain),
            "producer": args.producer or "receipt"}
    if args.run_url:
        meta["run_url"] = args.run_url
    command = list(args.command)
    command[0] = shutil.which(command[0]) or command[0]
    return record_receipt(args.root.resolve(), out, args.gate, command, args.artifact, meta)


def cmd_check(args) -> int:
    result = release.validate(args.root.resolve(), release_mode=args.release)
    rows = [c for c in result["checks"] if any(c["name"].startswith(p) for p in args.prefix)]
    for row in rows:
        print(json.dumps(row, sort_keys=True))
    bad = [r["name"] for r in rows if r["status"] != "PASS"]
    missing = [p for p in args.prefix if not any(r["name"].startswith(p) for r in rows)]
    if bad or missing:
        print(json.dumps({"not_pass": bad, "unmatched_prefixes": missing}, sort_keys=True), file=sys.stderr)
        return 1
    return 0


def discover(dirs: list[Path]) -> dict:
    found: dict = {}
    for base in dirs:
        for path in sorted(Path(base).rglob("*" + RECEIPT_SUFFIX)):
            found.setdefault(path.name[:-len(RECEIPT_SUFFIX)], []).append(path)
    return found


def pick(candidates: list[Path], commit: str) -> tuple[Path | None, str]:
    stale = []
    for path in candidates:
        bound = release.load_json(path).get("source_commit", commit)
        if bound == commit:
            return path, ""
        stale.append(f"{path.as_posix()}@{bound[:12]}")
    return None, ("stale receipt(s): " + ", ".join(stale)) if stale else "no receipt"


def import_gate(ident: str, path: Path, out: Path, owner: str) -> tuple[dict | None, str]:
    record = release.load_json(path)
    if record.get("id") != ident:
        raise ValueError(f"{path}: receipt id {record.get('id')!r} != {ident}")
    if record.get("owner_repository") != owner:
        raise ValueError(f"{path}: owner {record.get('owner_repository')!r}; expected {owner}")
    if record.get("status") != "PASS" or record.get("exit_code") != 0:
        return None, f"receipt {record.get('status')} exit_code={record.get('exit_code')}: {path.as_posix()}"
    artifacts = []
    for artifact in record.get("artifacts") or []:
        src = release.safe_artifact(path.parent, artifact["path"])
        if release.sha256(src) != artifact["sha256"]:
            raise ValueError(f"{path}: artifact SHA-256 mismatch: {artifact['path']}")
        write_bytes_if_changed(out / ident / artifact["path"], src.read_bytes())
        artifacts.append({"path": ident + "/" + artifact["path"], "sha256": artifact["sha256"]})
    return {**record, "artifacts": artifacts}, ""


def gh_json(args: list[str]):
    return json.loads(subprocess.check_output(["gh", *args], text=True))


def gh_receipts(repo: str, commit: str, target: Path) -> Path:
    """Newest successful admission run at this commit: download its receipt artifacts."""
    target.mkdir(parents=True, exist_ok=True)
    runs = gh_json(["run", "list", "-R", repo, "--workflow", ADMISSION_WORKFLOW, "--commit",
                    commit, "--status", "success", "--json", "databaseId,url", "-L", "1"])
    if runs:
        subprocess.call(["gh", "run", "download", str(runs[0]["databaseId"]), "-R", repo,
                         "-p", RECEIPT_ARTIFACT_PREFIX + "*", "-D", str(target)])
    sanity = gh_json(["run", "list", "-R", repo, "--workflow", SANITY_WORKFLOW, "--commit",
                      commit, "--status", "success", "--json", "databaseId,url", "-L", "1"])
    if sanity:
        gh_sanity_receipt(repo, sanity[0], target / "OPENJDK_SANITY_MATRIX", commit)
    return target


def gh_sanity_receipt(repo: str, run: dict, out: Path, commit: str) -> None:
    out.mkdir(parents=True, exist_ok=True)
    command = ["gh", "run", "view", str(run["databaseId"]), "-R", repo, "--json",
               "conclusion,headSha,jobs,url,workflowName"]
    data = subprocess.check_output(command, text=True).encode("utf-8")
    write_bytes_if_changed(out / "jobs.json", data)
    ok = json.loads(data).get("conclusion") == "success" and json.loads(data).get("headSha") == commit
    write_json(out / ("OPENJDK_SANITY_MATRIX" + RECEIPT_SUFFIX), {
        "id": "OPENJDK_SANITY_MATRIX", "status": "PASS" if ok else "FAIL",
        "exit_code": 0 if ok else 1, "owner_repository": DEFAULT_REPO, "command": command,
        "environment": {"os": "github-hosted matrix; per job in jobs.json",
                        "architecture": "per job in jobs.json",
                        "toolchain": ".github/workflows/" + SANITY_WORKFLOW},
        "artifacts": [{"path": "jobs.json", "sha256": release.sha256(out / "jobs.json")}],
        "producer": SANITY_WORKFLOW, "run_url": run["url"], "source_commit": commit})


def assemble(root: Path, out: Path, dirs: list[Path], repo: str, gh: bool) -> int:
    commit, tree = head(root)
    gates, owners = contract_gates(root)
    if gh:
        dirs = [*dirs, gh_receipts(repo, commit, out / "downloads")]
    found = discover([d for d in dirs if d.is_dir()])
    records, pending = [], []
    for ident in gates:
        chosen, reason = pick(found.get(ident, []), commit)
        if chosen is None:
            producer = GATES.get(ident, ("unknown producer",))[0]
            pending.append((ident, "PENDING", f"{reason}; producer: {producer}"))
            continue
        record, reason = import_gate(ident, chosen, out, owners.get(ident, DEFAULT_REPO))
        if record is None:
            pending.append((ident, "FAIL", reason))
        else:
            records.append(record)
    write_json(out / "evidence.json", {"schema": release.EVIDENCE_SCHEMA, "source_commit": commit,
                                       "source_tree": tree, "gates": records})
    rows = "".join(f"{g}\t{s}\t{r}\n" for g, s, r in pending)
    write_bytes_if_changed(out / "pending.tsv", ("gate\tstate\treason\n" + rows).encode("utf-8"))
    for row in pending:
        print("\t".join(row), file=sys.stderr)
    print(json.dumps({"manifest": (out / "evidence.json").as_posix(), "source_commit": commit,
                      "gates": [r["id"] for r in records], "pending": [g for g, _, _ in pending]},
                     sort_keys=True))
    return EXIT_INCOMPLETE if pending else 0


def cmd_assemble(args) -> int:
    root = args.root.resolve()
    out = Path(args.out) if Path(args.out).is_absolute() else root / args.out
    dirs = [Path(d) if Path(d).is_absolute() else root / d for d in args.receipts]
    if not args.receipts:
        dirs = [out / "receipts", root / SYNEXIA_RECEIPTS]
    return assemble(root, out, dirs, args.repo, args.gh)


def cmd_gates(_args) -> int:
    print("gate\tproducer\tlocal_command")
    for ident, (producer, argv, _extra) in GATES.items():
        print(f"{ident}\t{producer}\t{' '.join(argv)}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", type=Path, default=Path("."))
    sub = parser.add_subparsers(dest="action", required=True)
    run = sub.add_parser("run", help="execute one table gate and write its receipt")
    run.add_argument("--gate", required=True, choices=sorted(GATES))
    run.add_argument("--out", default=DEFAULT_OUT + "/receipts")
    run.add_argument("--toolchain")
    run.add_argument("--run-url")
    run.set_defaults(func=cmd_run)
    receipt = sub.add_parser("receipt", help="wrap an arbitrary argv as a gate receipt")
    receipt.add_argument("--gate", required=True)
    receipt.add_argument("--out", default=DEFAULT_OUT + "/receipts")
    receipt.add_argument("--owner", default=DEFAULT_REPO)
    receipt.add_argument("--toolchain")
    receipt.add_argument("--producer")
    receipt.add_argument("--run-url")
    receipt.add_argument("--artifact", action="append", default=[],
                         help="extra artifact path (relative to --root) copied beside the log")
    receipt.add_argument("command", nargs="+", help="argv to execute; put it after `--`")
    receipt.set_defaults(func=cmd_receipt)
    check = sub.add_parser("check", help="verifier checks that must PASS")
    check.add_argument("--release", action="store_true")
    check.add_argument("--prefix", action="append", required=True)
    check.set_defaults(func=cmd_check)
    asm = sub.add_parser("assemble", help="bind receipts into evidence.json")
    asm.add_argument("--out", default=DEFAULT_OUT)
    asm.add_argument("--receipts", action="append", default=[])
    asm.add_argument("--repo", default=DEFAULT_REPO)
    asm.add_argument("--gh", action="store_true",
                     help="also download receipt artifacts of the newest successful run at HEAD")
    asm.set_defaults(func=cmd_assemble)
    sub.add_parser("gates", help="print the gate table").set_defaults(func=cmd_gates)
    return parser


def main(argv=None) -> int:
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
