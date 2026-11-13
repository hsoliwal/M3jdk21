#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the product's complete default Maven reactor and record current-source proof.

This is a product build driver, not a transformation runner. Recipe provenance is
read as committed JSON; neither its content nor a command in a receipt is run.
Known recipe roots and rewrite dependencies are refused before Maven starts.
Legacy custody records do not authorize executing those retained recipe sources.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import os
import platform
import shutil
import signal
import subprocess
import sys
from pathlib import Path
import xml.etree.ElementTree as ET


# Importing target helpers must not dirty the source snapshot being qualified.
sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location(
    "m3_release_admission", Path(__file__).with_name("verify_release_admission.py"))
release = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(release)
ORIGIN_PATH = release.MAVEN_ORIGIN_PATH
tracked_file = release.maven_tracked_file

def snapshot(root: Path) -> tuple[str, str]:
    """A receipt concerns this complete, clean worktree, never a sparse fixture."""
    if Path(release.git(root, "rev-parse", "--show-toplevel")).resolve() != root:
        raise ValueError("root must be the Git worktree root")
    sparse = subprocess.run(
        ["git", "-C", str(root), "config", "--bool", "core.sparseCheckout"],
        text=True, capture_output=True, check=False)
    if sparse.returncode not in (0, 1) or sparse.stdout.strip() == "true":
        raise ValueError("sparse or uninspectable checkout cannot qualify the full reactor")
    if release.git(root, "status", "--porcelain=v1", "--untracked-files=all"):
        raise ValueError("source worktree is dirty or has untracked files")
    return (release.git(root, "rev-parse", "HEAD"),
            release.git(root, "rev-parse", "HEAD^{tree}"))


def reactor_preflight(root: Path, tracked: set[str]) -> list[dict]:
    """Custody records alone never authorize running retained recipe sources."""
    residue = sorted(path for path in tracked
                     if release.under_recipe_roots(path) and release.recipe_candidate(path))
    custody_ok, detail, _ = release.recipe_custody(root, residue)
    if not custody_ok:
        raise ValueError("legacy recipe custody is unresolved: " + detail)
    if residue:
        raise ValueError("retained executable recipe sources require migration before product Maven: "
                         + ", ".join(residue[:8]) + (" ..." if len(residue) > 8 else ""))
    return release.maven_reactor_inputs(root, tracked)


def execution_environment(root: Path, tracked: set[str]) -> dict[str, str]:
    """Ambient CLI options may not narrow the committed reactor or skip gates."""
    if os.name != "posix":
        raise ValueError("this producer currently qualifies POSIX Maven process groups only")
    for name in ("MAVEN_ARGS", "MAVEN_OPTS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS"):
        if os.environ.get(name, "").strip():
            raise ValueError(name + " must be empty for the fixed full-reactor command")
    for relative in (".mvn/maven.config", "m3/.mvn/maven.config",
                     ".mvn/jvm.config", "m3/.mvn/jvm.config"):
        path = root / relative
        if path.exists():
            checked = tracked_file(root, relative, tracked)
            if any(line.strip() and not line.lstrip().startswith("#")
                   for line in checked.read_text(encoding="utf-8").splitlines()):
                raise ValueError("ambient Maven CLI/JVM configuration requires explicit review: " + relative)
    for relative in (".mvn/extensions.xml", "m3/.mvn/extensions.xml"):
        path = root / relative
        if path.exists():
            extension = tracked_file(root, relative, tracked)
            model = ET.fromstring(extension.read_bytes())
            for node in model.iter():
                if node.tag.rsplit("}", 1)[-1] not in ("groupId", "artifactId"):
                    continue
                value = (node.text or "").strip().lower()
                if "${" in value or "synexia" in value or "rewrite" in value or "recipe" in value:
                    raise ValueError("unqualified Maven core extension requires migration: " + relative)
    # Forced CLI flags take precedence for test/cache properties. No inherited
    # JVM option or Maven startup script may redirect the declared build scope.
    env = dict(os.environ)
    env["MAVEN_SKIP_RC"] = "true"
    return env


def run_logged(command: list[str], root: Path, env: dict[str, str], log: Path,
               timeout: int) -> int:
    """Bound the entire POSIX process group, including Maven's child JVMs."""
    with log.open("wb") as output:
        process = subprocess.Popen(command, cwd=root, env=env,
                                   stdout=output, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        try:
            return process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                pass
            # The group leader can exit while a child ignores SIGTERM. Kill the
            # remaining group even after wait() succeeds; do not leak that child.
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()
            output.write(b"\nM3_MAVEN_CONTROL_TIMEOUT\n")
            return 124


toolchain = release.maven_toolchain

def execute(root: Path, out: Path, expected_commit: str, maven: str = "mvn",
            timeout: int = 1800) -> int:
    root = Path(root).resolve()
    out = Path(out).absolute()
    if out.is_symlink() or out.resolve().is_relative_to(root):
        raise ValueError("evidence output must be outside the source tree and not a symlink")
    if any(parent.is_symlink() for parent in out.parents):
        raise ValueError("evidence output has a symlink ancestor")
    if out.exists() and (not out.is_dir() or any(out.iterdir())):
        raise ValueError("evidence directory must be new or empty; prior receipts are immutable")
    if not release.SHA1.fullmatch(expected_commit) or not 1 <= timeout <= 3600:
        raise ValueError("exact expected commit and a 1..3600 second budget are required")
    out.mkdir(parents=True, exist_ok=True)
    manifest = {"schema": release.EVIDENCE_SCHEMA, "source_commit": None,
                "source_tree": None, "gates": []}
    gate = {
        "id": release.MAVEN_GATE, "owner_repository": release.PRODUCT_REPOSITORY,
        "status": "HOLD", "exit_code": 2,
        "command": [maven, *release.MAVEN_CONTROL_ARGS],
        "environment": {"os": platform.platform(), "architecture": platform.machine(),
                        "toolchain": "NOT_EXECUTED"},
        "reactor": {"scope": release.MAVEN_REACTOR_SCOPE, "pom": "m3/pom.xml",
                    "maven_executed": False, "source_preserved": False},
        "artifacts": [],
    }
    manifest["gates"].append(gate)
    try:
        source = snapshot(root)
        manifest["source_commit"], manifest["source_tree"] = source
        if source[0] != expected_commit:
            raise ValueError("checkout differs from expected commit")
        tracked = set(release.git_paths(root, "ls-files", "-z"))
        # Check custody before provenance so the existing blocker remains visible
        # even before a newly executed migration origin has been attached.
        poms = reactor_preflight(root, tracked)
        (out / "reactor-inputs.json").write_text(
            json.dumps(poms, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        origin_path = tracked_file(root, ORIGIN_PATH, tracked)
        origin = release.load_json(origin_path)
        release.canonical_recipe_origin(origin)
        gate["canonical_recipe"] = {key: origin[key] for key in ("repository", "commit", "recipe_id")}
        shutil.copyfile(origin_path, out / "canonical-origin.json")
        env = execution_environment(root, tracked)
        executable = shutil.which(maven)
        if executable is None:
            raise ValueError("Maven executable is unavailable")
        gate["command"][0] = str(Path(executable).resolve())
        code = run_logged([gate["command"][0], "-version"], root, env,
                          out / "toolchain.log", min(timeout, 30))
        if code != 0:
            raise ValueError("Maven toolchain identification failed with exit " + str(code))
        gate["environment"]["toolchain"] = toolchain(out / "toolchain.log")
        if snapshot(root) != source:
            raise ValueError("source changed before Maven execution")
        gate["reactor"]["maven_executed"] = True
        code = run_logged(gate["command"], root, env, out / "maven.log", timeout)
        gate["exit_code"] = code
        gate["status"] = "FAIL"
        if snapshot(root) != source:
            raise ValueError("source changed during Maven execution")
        gate["reactor"]["source_preserved"] = True
        if code == 0:
            gate["status"] = "PASS"
        gate["detail"] = "full default product reactor executed; other release gates remain independent"
    except (ValueError, OSError, subprocess.SubprocessError, ET.ParseError) as exc:
        gate["detail"] = str(exc)
        gate["status"] = "FAIL" if gate["reactor"]["maven_executed"] else "HOLD"
        if gate["exit_code"] == 0:
            gate["exit_code"] = 2
    (out / "producer-result.json").write_text(
        json.dumps({key: value for key, value in gate.items() if key != "artifacts"},
                   indent=2, sort_keys=True) + "\n", encoding="utf-8")
    for path in sorted(out.iterdir()):
        if path.is_file():
            gate["artifacts"].append({"path": path.name, "sha256": release.sha256(path)})
    (out / "evidence.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"gate": release.MAVEN_GATE, "status": gate["status"],
                      "exit_code": gate["exit_code"], "detail": gate["detail"]}, sort_keys=True))
    return 0 if gate["status"] == "PASS" else (gate["exit_code"] or 2)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--timeout-seconds", type=int, default=1800)
    args = parser.parse_args(argv)
    try:
        return execute(args.root, args.out, args.expected_commit, args.maven, args.timeout_seconds)
    except (ValueError, OSError) as exc:
        print("M3_MAVEN_CONTROL_REFUSED: " + str(exc), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
