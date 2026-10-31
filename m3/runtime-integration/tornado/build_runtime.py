#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Ordered, source-sealed TornadoVM build and installed M3JDK image proof.

This is the target acceptance consumer. Canonical source materialization and its
append-only correction memory remain in the Synexia recipe owner.
"""
from __future__ import annotations

import argparse
import ast
import hashlib
import importlib.util
import json
import os
from pathlib import Path, PurePosixPath
import re
import resource
import signal
import shlex
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
import zipfile

PHASES = ("lint", "compile", "test", "image-runtime", "receipt")
PIN = "87a465f9185eb8e822f9532076fb4a5bc44fdf2a"
BACKENDS = ("opencl", "cuda", "metal")
STAMP = "2026-01-01T00:00:00Z"
BULK = ("BulkTask", "BulkExecutor", "BulkExecution", "BulkExecutorProvider", "BulkScheduler")
BASE = Path(__file__).resolve().parent
HEX256 = re.compile(r"[0-9a-f]{64}\Z")


class Refusal(RuntimeError):
    """An unqualified input or failed evidence gate; never a successful fallback."""


def require(condition, message):
    if not condition:
        raise Refusal(message)


def sha(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(path)


def relative_path(value):
    require(isinstance(value, str) and value and "\\" not in value, "invalid relative path")
    require(not any(ord(ch) < 32 for ch in value), "control character in path")
    parsed = PurePosixPath(value)
    require(not parsed.is_absolute() and not any(p in ("", ".", "..") for p in value.split("/")),
            "unsafe relative path: " + value)
    require(":" not in parsed.parts[0], "drive-qualified relative path")
    return Path(*parsed.parts)


def checked_file(root, name):
    part = relative_path(name)
    current = Path(root)
    require(not current.is_symlink(), "symlink root refused: " + str(current))
    for component in part.parts:
        current = current / component
        require(not current.is_symlink(), "symlink input refused: " + str(current))
    require(current.is_file(), "missing regular file: " + str(current))
    return current


def file_inventory(root):
    root = Path(root)
    require(root.is_dir() and not root.is_symlink(), "regular directory required: " + str(root))
    result = {}
    for directory, dirs, files in os.walk(root, followlinks=False):
        for name in dirs + files:
            candidate = Path(directory) / name
            require(not candidate.is_symlink(), "symlink in sealed tree: " + str(candidate))
        for name in files:
            candidate = Path(directory) / name
            require(candidate.is_file(), "non-regular sealed entry")
            result[candidate.relative_to(root).as_posix()] = candidate
    return result


def verify_source(lock, root):
    require(lock.get("schema") == "m3-tornado-source-lock/1", "unsupported source lock schema")
    require(lock.get("source", {}).get("commit") == PIN, "unqualified Tornado source revision")
    require(lock["source"].get("version") == "7.2.0", "unexpected Tornado source version")
    entries = lock.get("files", [])
    require(0 < len(entries) <= 10000, "bounded nonempty source inventory required")
    seen = set()
    total = 0
    for row in entries:
        name = row["path"]
        require(name not in seen, "duplicate locked source path: " + name)
        seen.add(name)
        require(HEX256.fullmatch(row["sha256"]), "invalid source SHA-256")
        require(row["mode"] in ("100644", "100755"), "unsupported source mode")
        item = checked_file(root, name)
        total += item.stat().st_size
        require(total <= 128 * 1024 * 1024, "source inventory exceeds 128 MiB bound")
        require(sha(item) == row["sha256"], "source hash mismatch: " + name)
        require(bool(item.stat().st_mode & 0o100) == (row["mode"] == "100755"),
                "source executable-mode mismatch: " + name)
    actual = set(file_inventory(root))
    require(actual == seen, "source file set differs: missing=" + str(sorted(seen - actual)[:8])
            + " extra=" + str(sorted(actual - seen)[:8]))
    return {"files": len(seen), "bytes": total, "commit": PIN}


def verify_dependencies(lock, repository):
    rows = lock.get("runtime_dependencies", [])
    require(rows, "runtime dependency inventory absent")
    result = {}
    modules = set()
    for row in rows:
        require(row.get("role") in ("runtime", "graal-bootstrap", "test"), "invalid dependency role")
        expected = "/".join((row["group"].replace(".", "/"), row["artifact"], row["version"],
                             row["artifact"] + "-" + row["version"] + ".jar"))
        require(row["maven_path"] == expected, "dependency coordinate/path mismatch")
        require(expected not in result, "duplicate dependency path")
        path = checked_file(repository, expected)
        require(HEX256.fullmatch(row["sha256"]) and sha(path) == row["sha256"],
                "dependency hash mismatch: " + expected)
        if row.get("module"):
            require(row["module"] not in modules, "duplicate dependency module")
            modules.add(row["module"])
        result[expected] = path
    require({"org.graalvm.collections", "org.graalvm.word", "org.graalvm.truffle.compiler",
             "org.objectweb.asm", "commons.math3"} <= modules, "incomplete runtime dependency closure")
    return result


def verify_staged(lock, root, expected_generated=None):
    """Check every staged preimage and permit only bounded build-output lanes."""
    actual = file_inventory(root)
    originals = {row["path"]: row for row in lock["files"]}
    for name, row in originals.items():
        require(name in actual and sha(actual[name]) == row["sha256"], "staged source changed: " + name)
        require(bool(actual[name].stat().st_mode & 0o100) == (row["mode"] == "100755"), "staged source mode changed: " + name)
    generated = {}
    flattened = {str(PurePosixPath(name).with_name(".flattened-pom.xml")) for name in originals if PurePosixPath(name).name == "pom.xml"}
    for name in sorted(set(actual) - set(originals)):
        parts = PurePosixPath(name).parts
        require("target" in parts[:-1] or parts[0] == "graalJars" or name in flattened,
                "unlocked staged source/build input: " + name)
        generated[name] = sha(actual[name])
    if expected_generated is not None:
        require(generated == expected_generated, "staged build outputs changed between phases")
    return generated


def verify_junit_lock(path):
    lock = json.loads(Path(path).read_text(encoding="utf-8"))
    require(lock.get("schema") == "m3-source-junit-support/1" and lock.get("status") == "PASS", "source-owned JUnit support receipt required")
    require(lock.get("artifacts") and lock.get("sources") and lock.get("build_receipts"), "incomplete source JUnit custody")
    for lane in ("artifacts", "sources", "build_receipts"):
        seen = set()
        for row in lock[lane]:
            candidate = Path(row["path"])
            require(candidate.is_absolute() and str(candidate) not in seen and not candidate.is_symlink(), "invalid JUnit sealed path")
            seen.add(str(candidate))
            require(sha(candidate) == row["sha256"], "JUnit " + lane + " seal mismatch: " + str(candidate))
    return lock


def checkstyle_report(path, source_root):
    report = ET.parse(path).getroot()
    require(report.tag == "checkstyle", "Checkstyle XML report required")
    files = []
    diagnostics = []
    for item in report.findall("file"):
        original = Path(item.attrib["name"])
        require(original.is_absolute() and source_root in original.parents, "Checkstyle path outside selected source")
        name = original.relative_to(source_root).as_posix()
        files.append(name)
        for error in item.findall("error"):
            diagnostics.append({"path": name, **error.attrib})
    return {"checker_version": report.attrib.get("version"), "reported_files": sorted(files),
            "diagnostics": sorted(diagnostics, key=canonical)}


def verify_checkstyle_baseline(baseline, report, lock, config_sha256, backend):
    require(baseline.get("schema") == "m3-tornado-checkstyle-baseline/1", "unsupported lint baseline")
    require(backend == baseline.get("backend") == "opencl", "UNQUALIFIED_CHECKSTYLE_BASELINE: " + backend)
    require(baseline.get("source_commit") == lock["source"]["commit"] == PIN, "stale baseline source revision")
    require(baseline.get("source_files_sha256") == hashlib.sha256(canonical(lock["files"])).hexdigest(), "stale baseline source inventory")
    require(baseline.get("config_sha256") == config_sha256, "stale baseline Checkstyle configuration")
    require(baseline.get("plugin_version") == "3.3.0", "unqualified Checkstyle plugin")
    require(baseline.get("checker_version") == report["checker_version"], "unqualified Checkstyle engine")
    require(baseline.get("reported_files") == report["reported_files"], "Checkstyle audited file set changed")
    require(baseline.get("diagnostics") == report["diagnostics"], "Checkstyle diagnostic multiset changed; canonical recipe review required")
    return {"status": "BASELINE_MATCH", "existing_diagnostics": len(report["diagnostics"]),
            "reported_files": len(report["reported_files"]), "checker_version": report["checker_version"], "clean_style": False}


def reactor_coordinates(trees):
    coordinates = set()
    def visit(node):
        coordinates.add((node["groupId"], node["artifactId"], node["version"], node.get("type", "jar"), node.get("scope", "")))
        for child in node.get("children", []):
            visit(child)
    for tree in trees:
        visit(tree)
    return sorted(coordinates)


def verify_runtime_coordinates(lock, coordinates):
    for dependency in lock["runtime_dependencies"]:
        versions = {version for group, artifact, version, kind, scope in coordinates
                    if group == dependency["group"] and artifact == dependency["artifact"] and kind == "jar"}
        require(versions == {dependency["version"]}, "source/runtime dependency mismatch for " + dependency["group"] + ":" +
                dependency["artifact"] + ": source=" + str(sorted(versions)) + " lock=" + dependency["version"])


def parse_export_list(path):
    tokens = shlex.split(Path(path).read_text(encoding="utf-8"), comments=True)
    result = []
    i = 0
    while i < len(tokens):
        token = tokens[i]
        if token in ("--add-exports", "--add-opens"):
            require(i + 1 < len(tokens), "truncated export option")
            key, value = token, tokens[i + 1]
            i += 2
        else:
            require(token.startswith(("--add-exports=", "--add-opens=")), "unapproved launcher option: " + token)
            key, value = token.split("=", 1)
            i += 1
        require(re.fullmatch(r"[A-Za-z0-9_.]+/[A-Za-z0-9_.]+=[A-Za-z0-9_.,-]+", value),
                "invalid export target: " + value)
        result.append(key + "=" + value)
    return result


def validate_phase_chain(state, phase):
    index = PHASES.index(phase)
    events = state.get("phases", [])
    require(len(events) == index, "phase sequence requires " + str(PHASES[:index]))
    for expected, event in zip(PHASES[:index], events):
        require(event.get("phase") == expected and event.get("status") == "PASS",
                "predecessor phase did not pass: " + expected)


def verify_m3_receipt(path, jdk):
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    require(value.get("schema") == "m3-jdk-source-build/1" and value.get("status") == "PASS",
            "successful M3 source-build receipt required")
    require(Path(value["jdk_home"]).resolve() == jdk.resolve(), "M3 receipt identifies a different image")
    require(re.fullmatch(r"[0-9a-f]{40}", value["source"]["commit"]), "source commit absent")
    require(HEX256.fullmatch(value["source"]["tree_sha256"]), "source tree seal absent")
    build = value["build"]
    require(build.get("exit_code") == 0 and isinstance(build.get("argv"), list) and build["argv"],
            "successful source build command absent")
    require(Path(build["cwd"]).is_absolute(), "absolute build working directory required")
    require(sha(Path(build["log"]["path"])) == build["log"]["sha256"], "source-build log seal mismatch")
    artifacts = value.get("artifacts", [])
    names = set()
    for item in artifacts:
        require(item["path"] not in names, "duplicate M3 image artifact")
        names.add(item["path"])
        require(sha(checked_file(jdk, item["path"])) == item["sha256"],
                "source-built image mismatch: " + item["path"])
    required = {"release", "bin/java", "lib/server/libjvm.so"}
    required.update("jmods/" + item.name for item in (jdk / "jmods").glob("*.jmod"))
    require("jmods/java.base.jmod" in required and required <= names, "incomplete source-built JDK artifact seal")
    require(value.get("vm", {}).get("UseM3StringStorage") is False, "M3 String admission must remain disabled")
    return {"receipt_sha256": sha(path), "source": value["source"], "artifacts": len(artifacts)}


class Build:
    def __init__(self, args):
        self.a = args
        self.repo = args.repo_root.resolve()
        self.lock_path = (args.source_lock or self.repo / "m3/runtime-integration/tornado/source-lock.json").resolve()
        self.lock = json.loads(self.lock_path.read_text(encoding="utf-8"))
        self.source = (args.source_root or self.repo / relative_path(self.lock["source"]["root"])).resolve()
        self.receiver = (args.receiver_source or self.repo / "m3/ports/tornadovm-bulk").resolve()
        self.jdk = args.jdk_home.resolve()
        self.maven = args.maven.resolve()
        self.m2 = args.maven_repo.resolve()
        self.out = args.out.resolve()
        require(self.out != self.repo and self.out not in self.repo.parents,
                "output must not replace or contain the repository")
        require(self.source != self.out and self.source not in self.out.parents and self.out not in self.source.parents,
                "output and vendored source must be disjoint")
        for protected in (self.jdk, self.receiver, self.m2, BASE):
            require(self.out != protected and protected not in self.out.parents and self.out not in protected.parents,
                    "output overlaps a protected input root: " + str(protected))
        self.stage = self.out / "staged/tornadovm"
        self.jars = self.out / "jars"
        self.image = self.out / "image"
        self.state_path = self.out / "STATE.json"
        self.state = {}
        self.dependencies = {}
        self.junit = verify_junit_lock(args.junit_lock) if args.junit_lock else None
        self.env = dict(os.environ)
        for key in ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_ARGS", "MAVEN_OPTS"):
            require(not self.env.get(key), "untracked tool injection refused: " + key)
        self.env.update(JAVA_HOME=str(self.jdk), PATH=str(self.jdk / "bin") + os.pathsep + self.env.get("PATH", ""))
        self.env["PYTHONDONTWRITEBYTECODE"] = "1"
        self.env["MAVEN_OPTS"] = "-Xmx2g -XX:ActiveProcessorCount=4"
        if args.maven_truststore:
            require(args.maven_truststore.is_file(), "Maven TLS truststore absent")
            self.env["MAVEN_OPTS"] += " -Djavax.net.ssl.trustStore=" + str(args.maven_truststore.resolve())
        self.modules = {
            "tornado.api": "tornado-api", "tornado.runtime": "tornado-runtime",
            "tornado.annotation": "tornado-annotation", "tornado.drivers.common": "tornado-drivers/drivers-common",
            "tornado.drivers." + args.backend: "tornado-drivers/" + args.backend,
        }

    def save(self):
        write_json(self.state_path, self.state)

    def tool(self, name):
        return str(self.jdk / "bin" / name)

    def fingerprint(self):
        source_proof = verify_source(self.lock, self.source)
        self.dependencies = verify_dependencies(self.lock, self.m2)
        owners = {}
        for row in self.lock["packaging_owners"]:
            value = sha(checked_file(self.repo, row["path"]))
            require(value == row["sha256"], "packager owner changed: " + row["path"])
            owners[row["path"]] = value
        for name in ("descriptor_gen.py", "split_packages.py"):
            path = self.repo / "m3/tooling/jmod-recipes/tools" / name
            owners[str(path.relative_to(self.repo))] = sha(path)
        notices = {}
        for row in self.lock.get("legal_notices", []):
            # Explicit candidate source-lock location supports development before
            # recipe materialization; final paths resolve inside repo-root.
            path = self.repo / relative_path(row["path"])
            if not path.is_file() and self.a.source_root:
                path = self.lock_path.parent / "legal" / Path(row["path"]).name
            require(path.is_file() and sha(path) == row["sha256"], "locked dependency notice missing or changed: " + row["path"])
            notices[row["path"]] = row["sha256"]
        receiver = {}
        for folder in ("src/main", "src/test", "source-custody"):
            for name, path in file_inventory(self.receiver / folder).items():
                receiver[folder + "/" + name] = sha(path)
        for name in ("LICENSE", "NOTICE", "pom.xml"):
            path = self.receiver / name
            if path.is_file():
                receiver[name] = sha(path)
        for name in BULK:
            path = self.repo / "src/java.base/share/classes/jdk/internal/vm/parallel" / (name + ".java")
            owners[str(path.relative_to(self.repo))] = sha(path)
        jdk_files = {name: sha(checked_file(self.jdk, name)) for name in ("release", "bin/java", "bin/javac", "bin/jlink")}
        for path in sorted((self.jdk / "jmods").glob("*.jmod")):
            jdk_files["jmods/" + path.name] = sha(path)
        require("jmods/java.base.jmod" in jdk_files, "complete JDK with JMODs required")
        release = (self.jdk / "release").read_text(encoding="utf-8")
        require(re.search(r'^JAVA_VERSION="21(?:[.+\-"]|$)', release, re.M), "this admission requires JDK 21")
        own = {name: sha(BASE / name) for name in ("build_runtime.py", "test_build_runtime.py", "checkstyle-baseline.json",
                                                   "fixtures/M3TornadoImageProbe.java", "fixtures/Rv32iJUnitLauncher.java")}
        junit_proof = None
        if self.a.junit_lock:
            self.junit = verify_junit_lock(self.a.junit_lock)
            junit_proof = {"lock_sha256": sha(self.a.junit_lock), "artifacts": self.junit["artifacts"],
                           "source_files": len(self.junit["sources"]), "build_receipts": self.junit["build_receipts"]}
        m3 = verify_m3_receipt(self.a.m3_jdk_receipt, self.jdk) if self.a.m3_jdk_receipt else None
        require(m3 is not None or self.a.allow_bootstrap_jdk, "M3 source-built receipt required; bootstrap must be explicit")
        require(not (m3 and self.a.allow_bootstrap_jdk), "choose final M3 receipt or provisional bootstrap")
        identity = {
            "source_lock_sha256": sha(self.lock_path), "source": source_proof, "source_root": str(self.source),
            "legal_notices": notices, "source_junit": junit_proof,
            "receiver": receiver, "owners": owners, "harness": own, "jdk": jdk_files,
            "m3_jdk": m3, "maven": sha(self.maven), "maven_repo": str(self.m2), "jdk_home": str(self.jdk),
            "backend": self.a.backend, "require_device": self.a.require_device,
            "require_gpu": self.a.require_gpu,
            "allow_bootstrap_jdk": self.a.allow_bootstrap_jdk, "resolve_build_tools": self.a.resolve_build_tools,
            "maven_settings_path": str(self.a.maven_settings.resolve()) if self.a.maven_settings else None,
            "maven_truststore_path": str(self.a.maven_truststore.resolve()) if self.a.maven_truststore else None,
            "runtime_environment": {key: self.env[key] for key in (
                "LD_LIBRARY_PATH", "OCL_ICD_VENDORS", "POCL_KERNEL_CACHE", "POCL_MAX_PTHREAD_COUNT", "POCL_CACHE_DIR") if key in self.env},
        }
        return identity, hashlib.sha256(canonical(identity)).hexdigest()

    def run(self, name, argv, cwd=None, acceptable=(0,), extra_env=None, timeout=None):
        require(re.fullmatch(r"[a-z0-9-]+", name), "invalid command evidence label")
        number = len(self.state.setdefault("commands", []))
        log = self.out / "logs" / (f"{number:03d}-" + name + ".log")
        log.parent.mkdir(parents=True, exist_ok=True)
        arguments = [str(part) for part in argv]
        work = Path(cwd or self.out)
        environment = dict(self.env)
        if extra_env:
            environment.update(extra_env)
        before = resource.getrusage(resource.RUSAGE_CHILDREN)
        start = time.monotonic()
        error = None
        with log.open("wb") as stream:
            try:
                process = subprocess.Popen(arguments, cwd=work, env=environment, stdout=stream,
                                           stderr=subprocess.STDOUT, start_new_session=True)
                try:
                    code = process.wait(timeout=timeout or self.a.command_timeout)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                    raise
            except (OSError, subprocess.TimeoutExpired) as failure:
                error = str(failure)
                stream.write(("\nHARNESS_EXECUTION_FAILURE: " + error + "\n").encode())
                code = -1
        after = resource.getrusage(resource.RUSAGE_CHILDREN)
        event = {"name": name, "argv": arguments, "cwd": str(work), "exit_code": code,
                 "elapsed_seconds": round(time.monotonic() - start, 6),
                 "child_cpu_user_seconds": max(0, after.ru_utime - before.ru_utime),
                 "child_cpu_system_seconds": max(0, after.ru_stime - before.ru_stime),
                 "children_cumulative_max_rss_kib": after.ru_maxrss,
                 "log": str(log.relative_to(self.out)), "log_sha256": sha(log)}
        if error:
            event["execution_error"] = error
        self.state["commands"].append(event)
        self.save()
        print(f"{name}: exit={code} log={log}", flush=True)
        require(code in acceptable, f"{name} failed with exit {code}; see {log}")
        return code, log.read_text(encoding="utf-8", errors="replace")

    def maven_base(self):
        result = [str(self.maven), "--batch-mode", "--no-transfer-progress", "-T1", "-Dstyle.color=never",
                  "-Dmaven.repo.local=" + str(self.m2)]
        if self.a.maven_settings:
            require(self.a.maven_settings.is_file(), "Maven settings path absent")
            result += ["--settings", str(self.a.maven_settings.resolve())]
        if not self.a.resolve_build_tools:
            result.append("--offline")
        else:
            result.append("--update-snapshots")
        return result

    def resolve_dependencies(self):
        for row in self.lock["runtime_dependencies"]:
            path = self.m2 / relative_path(row["maven_path"])
            if path.exists():
                require(not path.is_symlink() and sha(path) == row["sha256"], "existing dependency does not match lock: " + str(path))
                continue
            require(self.a.resolve_build_tools, "locked artifact missing; prefetch it or use --resolve-build-tools: " + str(path))
            coordinate = ":".join((row["group"], row["artifact"], row["version"]))
            self.run("resolve-" + row["artifact"], self.maven_base() + [
                "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get", "-Dartifact=" + coordinate, "-Dtransitive=false"])
            require(path.is_file() and sha(path) == row["sha256"], "downloaded dependency differs from lock: " + coordinate)

    def reactor(self):
        return self.maven_base() + ["-Pjdk21," + self.a.backend + "-backend", "-Dtornado.backend=" + self.a.backend,
                                    "-Dmaven.buildNumber.skip=true", "-DbuildNumber=" + PIN[:7],
                                    "-Dcheckstyle.config.location=" + str(self.stage / "tornado-assembly/src/etc/checkstyle.xml"),
                                    "-pl", ",".join(self.modules.values()), "-am"]

    def seal_outputs(self):
        result = {}
        for folder in ("jars", "probe-classes", "receiver-classes", "bootstrap-patch", "image", "packed/jmods", "junit-test-classes", "junit-runner-classes"):
            root = self.out / folder
            if root.is_dir():
                for directory, dirs, files in os.walk(root, followlinks=False):
                    for name in dirs:
                        require(not (Path(directory) / name).is_symlink(), "directory symlink in output refused")
                    for name in files:
                        path = Path(directory) / name
                        relative = folder + "/" + path.relative_to(root).as_posix()
                        result[relative] = self.output_seal(path)
        return result

    def output_seal(self, path):
        if path.is_symlink():
            require(self.image in path.resolve().parents, "installed-image symlink escapes the image")
            return {"sha256": sha(path), "symlink": os.readlink(path)}
        require(path.is_file(), "regular output file required")
        return {"sha256": sha(path), "executable": bool(path.stat().st_mode & 0o100)}

    def verify_outputs(self):
        require(self.seal_outputs() == self.state.get("output_seals", {}),
                "phase output inventory changed: added, removed or altered artifact")
        for command in self.state.get("commands", []):
            require(sha(checked_file(self.out, command["log"])) == command["log_sha256"], "command log changed")
        for artifact in self.state.get("reactor_artifacts", []):
            require(sha(checked_file(self.m2, artifact["maven_path"])) == artifact["sha256"], "reactor artifact changed between phases")

    def lint(self):
        require(self.a.backend == "opencl", "UNQUALIFIED_CHECKSTYLE_BASELINE: " + self.a.backend)
        self.stage.mkdir(parents=True)
        for row in self.lock["files"]:
            target = self.stage / relative_path(row["path"])
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(self.source / row["path"], target)
            target.chmod(0o755 if row["mode"] == "100755" else 0o644)
            if target.suffix == ".py":
                ast.parse(target.read_text(encoding="utf-8"), filename=row["path"])
            elif target.name == "pom.xml":
                ET.parse(target)
        for path in BASE.glob("*.py"):
            ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        parse_export_list(self.stage / "tornado-assembly/src/etc/exportLists/common-exports")
        parse_export_list(self.stage / "tornado-assembly/src/etc/exportLists" / (self.a.backend + "-exports"))
        self.run("jdk-version", [self.tool("java"), "-version"])
        self.run("maven-version", self.maven_base() + ["--version"])
        # A separate lint-only POM avoids resolving/building Tornado dependencies
        # before lint. The unchanged donor Checkstyle configuration is the policy.
        directories = [self.stage / module / "src/main/java" for module in self.modules.values()]
        directories += [self.stage / "tornado-runtime/src/test/java"]
        sources = "".join("<sourceDirectory>" + escape(str(path)) + "</sourceDirectory>" for path in directories if path.is_dir())
        pom = self.out / "lint-pom.xml"
        pom.write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
<groupId>com.m3.acceptance</groupId><artifactId>tornado-source-lint</artifactId><version>1</version>
<build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-checkstyle-plugin</artifactId>
<version>3.3.0</version><configuration><consoleOutput>true</consoleOutput><failsOnError>true</failsOnError>
<includeResources>false</includeResources><includeTestResources>false</includeTestResources><linkXRef>false</linkXRef>
<configLocation>''' + escape(str(self.stage / "tornado-assembly/src/etc/checkstyle.xml")) + '''</configLocation>
<sourceDirectories>''' + sources + '''</sourceDirectories></configuration></plugin></plugins></build></project>\n''', encoding="utf-8")
        code, _ = self.run("source-checkstyle", self.maven_base() + ["-f", pom, "checkstyle:check"], acceptable=(0, 1))
        report = checkstyle_report(self.out / "target/checkstyle-result.xml", self.stage)
        baseline = json.loads((BASE / "checkstyle-baseline.json").read_text(encoding="utf-8"))
        self.state["lint"] = verify_checkstyle_baseline(baseline, report, self.lock,
            sha(self.stage / "tornado-assembly/src/etc/checkstyle.xml"), self.a.backend)
        require(code == baseline["upstream_exit_code"], "Checkstyle exit disposition changed")
        self.state["lint"]["upstream_exit_code"] = code
        write_json(self.out / "LINT_BASELINE_RESULT.json", self.state["lint"])

    def compile(self):
        graal = self.stage / "graalJars"
        graal.mkdir()
        for row in self.lock["runtime_dependencies"]:
            if row["group"].startswith("org.graalvm."):
                shutil.copyfile(self.m2 / row["maven_path"], graal / Path(row["maven_path"]).name)
        bootstrap = self.out / "invoke-graal-bootstrap.py"
        bootstrap.write_text("import importlib.util\n" +
            "s=importlib.util.spec_from_file_location('donor_graal', " + repr(str(self.stage / "bin/build_graal_module.py")) + ")\n" +
            "m=importlib.util.module_from_spec(s); s.loader.exec_module(m)\n" +
            "m._mvn=lambda: " + repr(self.maven_base()) + "\n" +
            "m.build(jdk='jdk21')\n", encoding="utf-8")
        self.run("graal-bootstrap", [sys.executable, bootstrap], cwd=self.stage)
        self.run("source-dependency-tree", self.reactor() + ["org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
                 "-DoutputType=json", "-DoutputFile=target/m3-dependency-tree.json"], cwd=self.stage)
        trees = [json.loads(path.read_text(encoding="utf-8")) for path in sorted(self.stage.rglob("m3-dependency-tree.json"))]
        require(len(trees) == len(self.modules) + 2, "incomplete selected reactor dependency audit")
        coordinates = reactor_coordinates(trees)
        verify_runtime_coordinates(self.lock, coordinates)
        self.state["reactor_coordinates"] = coordinates
        artifacts = {}
        for group, artifact, version, kind, scope in coordinates:
            if kind != "jar" or group == "io.github.beehive-lab":
                continue
            name = "/".join((group.replace(".", "/"), artifact, version, artifact + "-" + version + ".jar"))
            path = checked_file(self.m2, name)
            artifacts[name] = {"maven_path": name, "sha256": sha(path)}
        self.state["reactor_artifacts"] = list(artifacts.values())
        self.save()
        self.run("tornado-compile", self.reactor() + ["compile", "-DskipTests=true"], cwd=self.stage)
        self.jars.mkdir()
        for module, path in self.modules.items():
            classes = self.stage / path / "target/classes"
            require((classes / "module-info.class").is_file(), "source module descriptor absent: " + module)
            self.run("jar-" + module.replace(".", "-"), [self.tool("jar"), "--create", "--file", self.jars / (module + ".jar"),
                     "--module-version", "7.2.0", "--date", STAMP, "-C", classes, "."])
        shutil.copyfile(graal / "tornado-graal-23.1.0.jar", self.jars / "tornado.graal.jar")
        for row in self.lock["runtime_dependencies"]:
            if row["role"] == "runtime":
                shutil.copyfile(self.m2 / row["maven_path"], self.jars / (row["module"] + ".jar"))
        self.compile_receiver()

    def bootstrap_args(self):
        return ["--patch-module", "java.base=" + str(self.out / "bootstrap-patch")] if self.a.allow_bootstrap_jdk else []

    def compile_receiver(self):
        if self.a.allow_bootstrap_jdk:
            root = self.out / "bootstrap-src/jdk/internal/vm/parallel"
            root.mkdir(parents=True)
            for name in BULK:
                shutil.copyfile(self.repo / "src/java.base/share/classes/jdk/internal/vm/parallel" / (name + ".java"), root / (name + ".java"))
            patch = self.out / "bootstrap-patch"
            patch.mkdir()
            self.run("bootstrap-bulk-contract", [self.tool("javac"), "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
                     "--patch-module", "java.base=" + str(root.parents[3]), "-d", patch] + sorted(root.glob("*.java")))
        production = sorted((self.receiver / "src/main/java").rglob("*.java"))
        require(production and any(path.name == "module-info.java" for path in production), "named receiver source required")
        classes = self.out / "receiver-classes"
        classes.mkdir()
        common = [self.tool("javac"), "--enable-preview", "-source", "21", "-target", "21", "-proc:none",
                  "-Xlint:all,-preview,-module,-exports,-missing-explicit-ctor", "-Werror",
                  "--module-path", self.jars,
                  "--add-exports", "java.base/jdk.internal.vm.parallel=jdk.m3.tornado"] + self.bootstrap_args()
        self.run("receiver-compile", common + ["-d", classes] + production)
        resources = classes / "META-INF"
        resources.mkdir(exist_ok=True)
        for name in ("LICENSE", "NOTICE"):
            require((self.receiver / name).is_file(), "receiver license absent: " + name)
            shutil.copyfile(self.receiver / name, resources / name)
        shutil.copytree(self.receiver / "source-custody", resources / "m3/source-custody")
        self.run("receiver-jar", [self.tool("jar"), "--create", "--file", self.jars / "jdk.m3.tornado.jar",
                 "--module-version", "7.2.0", "--date", STAMP, "-C", classes, "."])
        probes = [self.receiver / "src/test/java/com/m3/tornado/Rv32iImageProbe.java", BASE / "fixtures/M3TornadoImageProbe.java"]
        require(all(path.is_file() for path in probes), "executable receiver/runtime probe absent")
        target = self.out / "probe-classes"
        target.mkdir()
        patch = os.pathsep.join(str(path.parent) for path in probes)
        self.run("probe-compile", common + ["--patch-module", "jdk.m3.tornado=" + patch, "-d", target] + probes)
        if self.junit:
            junit_classes = self.out / "junit-test-classes"
            junit_classes.mkdir()
            tests = sorted((self.receiver / "src/test/java").rglob("*Test.java"))
            require(len(tests) == 6, "expected five original donor tests and unchanged existing adapter test")
            self.run("original-junit-compile", common + ["--add-reads", "jdk.m3.tornado=ALL-UNNAMED",
                     "--patch-module", "jdk.m3.tornado=" + str(self.receiver / "src/test/java"),
                     "-classpath", self.junit_classpath(), "-d", junit_classes] + tests)
            runner = self.out / "junit-runner-classes"
            runner.mkdir()
            self.run("junit-launcher-compile", [self.tool("javac"), "--release", "21", "-Xlint:all", "-Werror",
                     "-classpath", self.junit_classpath(), "-d", runner, BASE / "fixtures/Rv32iJUnitLauncher.java"])

    def junit_classpath(self):
        return os.pathsep.join(row["path"] for row in self.junit["artifacts"])

    def junit_arguments(self, mode="cpu", device=None):
        require(mode in ("cpu", "device"), "invalid original-suite execution mode")
        selection = []
        if mode == "device":
            require(device is not None, "original device suite needs actual selected-device evidence")
            selection = ["-Dm3rv32i.execute.device=" + str(device["backend_index"]) + ":" + str(device["device_index"])]
        return self.bootstrap_args() + selection + ["--add-modules", "jdk.m3.tornado", "--patch-module",
                "jdk.m3.tornado=" + str(self.out / "junit-test-classes"),
                "--add-exports", "java.base/jdk.internal.vm.parallel=jdk.m3.tornado,ALL-UNNAMED",
                "--add-reads", "jdk.m3.tornado=ALL-UNNAMED",
                "--add-opens", "jdk.m3.tornado/com.m3.tornado=ALL-UNNAMED",
                "--add-opens", "jdk.m3.tornado/com.synexia.m3.cpugpu=ALL-UNNAMED", "-Dm3.gpu=" + str(mode == "device").lower(),
                "-classpath", str(self.out / "junit-runner-classes") + os.pathsep + self.junit_classpath(), "Rv32iJUnitLauncher", mode]

    def test(self):
        self.run("harness-guard-tests", [sys.executable, "-m", "unittest", "-v", "test_build_runtime.py"], cwd=BASE)
        self.run("tornado-jvmci-tests", self.reactor() + ["test", "-DskipTests=false"], cwd=self.stage)
        reports = sorted(self.stage.glob("*/target/surefire-reports/TEST-*.xml"))
        require(reports, "no upstream JUnit test reports were produced")
        executed = failures = errors = skipped = 0
        for report in reports:
            suite = ET.parse(report).getroot()
            executed += int(suite.get("tests", "0")) - int(suite.get("skipped", "0"))
            failures += int(suite.get("failures", "0"))
            errors += int(suite.get("errors", "0"))
            skipped += int(suite.get("skipped", "0"))
        require(executed > 0 and failures == errors == 0, "upstream test execution incomplete or failed")
        self.state["upstream_tests"] = {"executed": executed, "skipped": skipped, "failures": failures, "errors": errors}
        args = [self.tool("java"), "--enable-preview", "--module-path", self.jars,
                "--add-exports", "java.base/jdk.internal.vm.parallel=jdk.m3.tornado"] + self.bootstrap_args()
        args += ["--patch-module", "jdk.m3.tornado=" + str(self.out / "probe-classes"),
                 "-m", "jdk.m3.tornado/com.m3.tornado.Rv32iImageProbe", "cpu"]
        _, output = self.run("receiver-cpu-contract", args)
        require("M3_RV32IM_IMAGE_CPU_PASS" in output, "receiver CPU completion marker absent")
        if self.junit:
            _, original = self.run("original-junit-tests", [self.tool("java"), "--enable-preview", "-Xcheck:jni",
                                  "--module-path", self.jars] + self.junit_arguments())
            require("M3_RV32IM_ORIGINAL_JUNIT_PASS" in original, "original receiver JUnit CPU suite did not satisfy exact counts")
            self.state["receiver_original_tests"] = {"found": 19, "successful": 16, "aborted_gpu_assumptions": 2,
                                                      "skipped_gpu_condition": 1, "failed": 0, "scope": "CPU-only original tests"}

    def pack(self):
        legal = self.out / "legal-notices"
        legal.mkdir()
        for path in sorted(self.stage.glob("LICENSE*")):
            shutil.copyfile(path, legal / path.name)
        shutil.copyfile(self.receiver / "LICENSE", legal / "RECEIVER_LICENSE")
        shutil.copyfile(self.receiver / "NOTICE", legal / "RECEIVER_NOTICE")
        for row in self.lock.get("legal_notices", []):
            path = self.repo / relative_path(row["path"])
            if not path.is_file() and self.a.source_root:
                path = self.lock_path.parent / "legal" / Path(row["path"]).name
            require(sha(path) == row["sha256"], "dependency notice seal mismatch")
            shutil.copyfile(path, legal / Path(row["path"]).name)
        # Preserve embedded third-party license/notice files without flattening names.
        for jar in self.jars.glob("*.jar"):
            with zipfile.ZipFile(jar) as archive:
                for entry in archive.infolist():
                    if not entry.is_dir() and re.search(r"(^|/)(license|notice|copying)([./_-]|$)", entry.filename, re.I):
                        name = relative_path(entry.filename)
                        target = legal / "dependencies" / jar.stem / name
                        target.parent.mkdir(parents=True, exist_ok=True)
                        target.write_bytes(archive.read(entry))
        descriptors = self.out / "descriptors"
        math = self.jars / "commons.math3.jar"
        self.run("math-descriptor", [self.tool("jdeps"), "--multi-release", "21", "--generate-module-info", descriptors, math])
        descriptor = descriptors / "commons.math3/module-info.java"
        versioned = descriptors / "commons.math3/versions/21/module-info.java"
        candidates = sorted(descriptors.rglob("module-info.java"))
        require(len(candidates) == 1 and candidates[0] in (descriptor, versioned),
                "unexpected commons-math descriptor module/release layout")
        if candidates[0] == versioned:
            shutil.copyfile(versioned, descriptor)
        # Reuse the target's descriptor normalizer, not a second descriptor owner.
        owner = self.repo / "m3/tooling/jmod-recipes/tools/descriptor_gen.py"
        spec = importlib.util.spec_from_file_location("m3_descriptor_owner", owner)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        normalized, dropped = module.normalize(descriptor.read_text(), math, module.class_names(math), release=21)
        descriptor.write_text(normalized, encoding="utf-8")
        self.state["descriptor_normalization"] = {"owner_sha256": sha(owner), "dropped_clauses": dropped}
        named = self.out / "JMOD_IMAGE.tsv"
        named.write_text("module\tjar\tdisposition\n" + "".join(
            path.stem + "\t" + str(path) + "\tMODULAR_JAR\n" for path in sorted(self.jars.glob("*.jar")) if path != math), encoding="utf-8")
        manifest = self.out / "DESCRIPTOR_MANIFEST.tsv"
        manifest.write_text("module\tunit_jars\ttarget\ncommons.math3\t" + str(math) + "\tcommons.math3/module-info.java\n", encoding="utf-8")
        self.run("jmod-pack", [sys.executable, self.repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py",
                 "--image-tsv", named, "--manifest", manifest, "--layer-root", descriptors,
                 "--out", self.out / "packed", "--jdk-home", self.jdk, "--release", "21", "--date", STAMP,
                 "--jmods-only", "--legal-notices", legal])
        expected = {path.stem for path in self.jars.glob("*.jar")}
        require({path.stem for path in (self.out / "packed/jmods").glob("*.jmod")} == expected, "incomplete runtime JMOD closure")
        roots = {path.stem for path in (self.jdk / "jmods").glob("*.jmod") if path.stem.startswith(("java.", "jdk."))}
        roots.update(expected)
        # Pin custom Graal support ahead of any vendor modules in a bootstrap JDK.
        self.run("jdk-image-link", [self.tool("jlink"), "--module-path", os.pathsep.join((str(self.out / "packed/jmods"), str(self.jdk / "jmods"))),
                 "--add-modules", ",".join(sorted(roots)), "--output", self.image])
        require(sha(self.image / "lib/server/libjvm.so") == sha(self.jdk / "lib/server/libjvm.so"),
                "linked native VM differs from supplied image authority")
        extracted = self.out / "image-module-audit"
        pattern = ",".join("glob:/" + name + "/**" for name in sorted(expected))
        self.run("image-module-content", [self.tool("jimage"), "extract", "--dir", extracted, "--include", pattern, self.image / "lib/modules"])
        verified = {}
        for name in sorted(expected):
            with zipfile.ZipFile(self.out / "packed/jmods" / (name + ".jmod")) as archive:
                input_classes = {entry.filename[len("classes/"):]: hashlib.sha256(archive.read(entry)).hexdigest()
                                 for entry in archive.infolist() if entry.filename.startswith("classes/") and
                                 entry.filename.endswith(".class") and entry.filename != "classes/module-info.class"}
            image_classes = {path.relative_to(extracted / name).as_posix(): sha(path)
                             for path in (extracted / name).rglob("*.class") if path.name != "module-info.class"}
            require(input_classes and input_classes == image_classes, "installed module differs from exact input JMOD: " + name)
            verified[name] = {"classes": len(input_classes), "class_inventory_sha256": hashlib.sha256(canonical(input_classes)).hexdigest()}
        self.state["installed_module_contents"] = verified
        write_json(self.out / "IMAGE_MODULE_CONTENTS.json", verified)

    def launcher(self):
        flags = ["--enable-preview", "-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI", "-XX:-UseJVMCICompiler",
                 "--add-modules=" + ",".join(sorted(set(self.modules) | {"tornado.graal", "jdk.m3.tornado"})),
                 "--add-exports=java.base/jdk.internal.vm.parallel=jdk.m3.tornado",
                 "--enable-native-access=tornado.runtime,tornado.drivers.common,tornado.drivers." + self.a.backend]
        for name in ("common", self.a.backend):
            flags += parse_export_list(self.stage / "tornado-assembly/src/etc/exportLists" / (name + "-exports"))
        flags += ["-Dtornado.load.api.implementation=uk.ac.manchester.tornado.runtime.tasks.TornadoTaskGraph",
                  "-Dtornado.load.runtime.implementation=uk.ac.manchester.tornado.runtime.TornadoCoreRuntime",
                  "-Dtornado.load.tornado.implementation=uk.ac.manchester.tornado.runtime.common.Tornado",
                  "-Dtornado.load.annotation.implementation=uk.ac.manchester.tornado.annotation.ASMClassVisitor",
                  "-Dtornado.load.annotation.parallel=uk.ac.manchester.tornado.api.annotations.Parallel",
                  "-Dtornado.recover.bailout=false"]
        conf = self.image / "conf/m3-tornado"
        conf.mkdir(parents=True)
        (conf / "options").write_text("\n".join(flags) + "\n", encoding="utf-8")
        shutil.copyfile(self.lock_path, conf / "source-lock.json")
        launcher = self.image / "bin/m3-tornado"
        launcher.write_text('#!/bin/sh\nset -eu\nM3_IMAGE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)\n'
                            'exec "$M3_IMAGE_DIR/bin/java" "@$M3_IMAGE_DIR/conf/m3-tornado/options" "$@"\n', encoding="utf-8")
        launcher.chmod(0o755)
        return launcher

    def image_runtime(self):
        self.pack()
        launcher = self.launcher()
        _, listed = self.run("image-modules", [self.image / "bin/java", "--list-modules"])
        installed = {line.split("@", 1)[0] for line in listed.splitlines()}
        require(set(self.modules) | {"jdk.m3.tornado", "tornado.graal", "jdk.compiler", "jdk.jlink"} <= installed,
                "installed product module closure incomplete")
        self.run("image-javac", [self.image / "bin/javac", "-version"])
        if self.junit:
            _, original = self.run("image-original-junit", [launcher, "-Xcheck:jni"] + self.junit_arguments())
            require("M3_RV32IM_ORIGINAL_JUNIT_PASS" in original, "installed-image original JUnit suite incomplete")
        if self.a.m3_jdk_receipt:
            _, flags = self.run("m3-vm-flags", [self.image / "bin/java", "-XX:+UnlockExperimentalVMOptions", "-XX:+PrintFlagsFinal", "-version"])
            require(re.search(r"\bUseM3StringStorage\s*=\s*false\b", flags), "installed image lacks disabled M3 String flag")
            code, rejected = self.run("m3-jvmci-string-refusal", [self.image / "bin/java", "-XX:+UnlockExperimentalVMOptions",
                     "-XX:+UseM3StringStorage", "-XX:+EnableJVMCI", "-version"], acceptable=(1,))
            require(code != 0 and "UseM3StringStorage does not support JVMCI" in rejected, "String/JVMCI guard changed")
        patch = self.bootstrap_args() + ["--patch-module", "jdk.m3.tornado=" + str(self.out / "probe-classes")]
        receiver = [launcher] + patch + ["-m", "jdk.m3.tornado/com.m3.tornado.Rv32iImageProbe"]
        _, cpu = self.run("image-receiver-cpu", receiver + ["cpu"])
        require("M3_RV32IM_IMAGE_CPU_PASS" in cpu, "installed receiver CPU proof incomplete")
        probe = [launcher] + patch + ["--add-opens", "jdk.m3.tornado/com.m3.tornado=tornado.runtime",
                                      "-m", "jdk.m3.tornado/com.m3.tornado.M3TornadoImageProbe"]
        _, identity = self.run("image-runtime-identity", probe + ["identity", self.a.backend])
        require("M3_TORNADO_IMAGE_IDENTITY_PASS" in identity, "Tornado classes did not load from installed image")
        # Deterministic explicit absence is tested separately from host discovery.
        _, absent = self.run("image-no-device-refusal", probe + ["no-device", self.a.backend], acceptable=(3,))
        require("M3_TORNADO_NO_DEVICE" in absent, "no-device refusal marker absent")
        code, device = self.run("image-device-kernel", probe + ["kernel", self.a.backend] + (["gpu"] if self.a.require_gpu else []), acceptable=(0, 3))
        if code == 3:
            require("M3_TORNADO_NO_DEVICE" in device, "unrecognized no-device outcome")
            self.state["device"] = {"status": "NO_DEVICE", "backend": self.a.backend}
            require(not self.a.require_device, "selected device unavailable; required-device qualification refused")
        else:
            rows = [line[len("M3_TORNADO_KERNEL_PASS "):] for line in device.splitlines() if line.startswith("M3_TORNADO_KERNEL_PASS ")]
            require(len(rows) == 1, "actual kernel evidence absent or ambiguous")
            detail = json.loads(rows[0])
            require(detail.get("elements") == 257 and detail.get("backend", "").lower() == self.a.backend,
                    "kernel output does not identify selected backend")
            require(not self.a.require_gpu or detail.get("device_type") == "GPU", "physical GPU lane did not execute on a GPU")
            self.state["device"] = {"status": "PASS", **detail}
            device_property = "-Dm3rv32i.execute.device=" + str(detail["backend_index"]) + ":" + str(detail["device_index"])
            _, accelerated = self.run("image-receiver-accelerated", [launcher] + patch + [device_property,
                "-m", "jdk.m3.tornado/com.m3.tornado.Rv32iImageProbe", "gpu"])
            require("M3_RV32IM_IMAGE_GPU_PASS" in accelerated, "receiver accelerator parity incomplete")
            if self.junit:
                _, original = self.run("image-original-junit-device", [launcher, "-Xcheck:jni"] + self.junit_arguments("device", detail))
                require("M3_RV32IM_ORIGINAL_JUNIT_DEVICE_PASS" in original, "original device suite did not complete all19 tests")
                self.state["receiver_original_device_tests"] = {"found": 19, "successful": 19, "aborted": 0, "skipped": 0,
                                                              "failed": 0, "backend": detail["backend"], "device_type": detail["device_type"]}
        self.state["image_modules"] = sorted(installed)
        self.state["runtime_scope"] = "SOURCE_BUILT_M3JDK" if self.a.m3_jdk_receipt else "PROVISIONAL_BOOTSTRAP_JDK"

    def receipt(self):
        self.verify_outputs()
        identity, fingerprint = self.fingerprint()
        require(fingerprint == self.state["input_fingerprint"], "inputs changed during qualification")
        device = self.state.get("device", {"status": "NOT_EXECUTED"})
        status = "RUNTIME_QUALIFIED" if self.a.m3_jdk_receipt and device["status"] == "PASS" and self.junit else "PROVISIONAL"
        pending = ["Full JDK jtreg/native/platform admission remains independently required"]
        if not self.junit:
            pending.append("Original receiver JUnit 5 suite requires the source-owned JUnit toolchain")
        if self.a.allow_bootstrap_jdk:
            pending.append("Source-built M3JDK installed-image replay required; bootstrap patch is not product java.base evidence")
        if device["status"] != "PASS":
            pending.append("Actual selected-backend device kernel and accelerated receiver parity required")
        write_json(self.out / "RESULT.json", {"schema": "m3-tornado-runtime-receipt/1", "status": status,
                   "scope": "selected Tornado backend and receiver runtime", "input_fingerprint": fingerprint,
                   "inputs": identity, "phases": self.state["phases"], "commands": self.state["commands"],
                   "output_seals": self.state["output_seals"], "device": device,
                   "upstream_tests": self.state.get("upstream_tests"), "pending_gates": pending,
                   "lint": self.state.get("lint"),
                   "reactor_coordinates": self.state.get("reactor_coordinates"),
                   "reactor_artifacts": self.state.get("reactor_artifacts"),
                   "installed_module_contents": self.state.get("installed_module_contents"),
                   "receiver_original_tests": self.state.get("receiver_original_tests"),
                   "receiver_original_device_tests": self.state.get("receiver_original_device_tests"),
                   "performance_claim": False, "memory_scope": "child-process cumulative max RSS, not retained heap"})

    def execute(self):
        self.out.mkdir(parents=True, exist_ok=True)
        guard = self.out / "RUNNING"
        try:
            descriptor = os.open(guard, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        except FileExistsError as failure:
            raise Refusal("qualification directory has an active or interrupted writer: " + str(guard)) from failure
        os.close(descriptor)
        try:
            if self.state_path.exists():
                self.state = json.loads(self.state_path.read_text(encoding="utf-8"))
                require(not self.state.get("terminal_refusal"), "an earlier refusal sealed this directory; start a fresh run")
                identity, fingerprint = self.fingerprint()
                require(self.state["input_fingerprint"] == fingerprint, "inputs changed; use a fresh evidence directory")
            else:
                require(set(path.name for path in self.out.iterdir()) == {"RUNNING"}, "fresh evidence directory required")
                self.state = {"commands": [], "phases": [], "output_seals": {}}
                self.resolve_dependencies()
                identity, fingerprint = self.fingerprint()
                self.state = {"schema": "m3-tornado-build-state/1", "input_fingerprint": fingerprint,
                              "inputs": identity, "phases": [], "commands": self.state["commands"], "output_seals": {}}
                self.save()
            phases = PHASES if self.a.phase == "all" else (self.a.phase,)
            for phase in phases:
                validate_phase_chain(self.state, phase)
                self.verify_outputs()
                if self.stage.exists():
                    verify_staged(self.lock, self.stage, self.state.get("stage_generated_seals", {}))
                start = time.monotonic()
                try:
                    getattr(self, phase.replace("-", "_"))()
                    verify_source(self.lock, self.source)
                    self.state["stage_generated_seals"] = verify_staged(self.lock, self.stage)
                    self.state["output_seals"] = self.seal_outputs()
                    self.state["phases"].append({"phase": phase, "status": "PASS", "elapsed_seconds": time.monotonic() - start})
                    self.save()
                    if phase == "receipt":
                        result = json.loads((self.out / "RESULT.json").read_text())
                        result["phases"] = self.state["phases"]
                        write_json(self.out / "RESULT.json", result)
                except Exception as failure:
                    self.state["phases"].append({"phase": phase, "status": "FAIL", "error": str(failure),
                                                 "elapsed_seconds": time.monotonic() - start})
                    self.save()
                    write_json(self.out / "RESULT.json", {"schema": "m3-tornado-runtime-receipt/1", "status": "FAILED",
                               "input_fingerprint": fingerprint, "phases": self.state["phases"], "commands": self.state["commands"],
                               "failure": str(failure), "performance_claim": False})
                    raise
            print("M3_TORNADO_PHASE_PASS " + self.a.phase + " " + str(self.out), flush=True)
        except Exception as failure:
            self.state["terminal_refusal"] = str(failure)
            self.save()
            write_json(self.out / "RESULT.json", {"schema": "m3-tornado-runtime-receipt/1", "status": "FAILED",
                       "input_fingerprint": self.state.get("input_fingerprint"), "phases": self.state.get("phases", []),
                       "commands": self.state.get("commands", []), "failure": str(failure), "performance_claim": False})
            raise
        finally:
            guard.unlink(missing_ok=True)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--source-lock", type=Path)
    parser.add_argument("--source-root", type=Path, help="explicit isolated candidate override; default is locked target source")
    parser.add_argument("--receiver-source", type=Path)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--maven", type=Path, required=True)
    parser.add_argument("--maven-repo", type=Path, required=True)
    parser.add_argument("--maven-settings", type=Path, help="private Maven settings; only its path is recorded")
    parser.add_argument("--maven-truststore", type=Path, help="optional trusted CA store; TLS verification remains enabled")
    parser.add_argument("--junit-lock", type=Path, help="source-owned JUnit support seals; test-only, never packaged")
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--backend", choices=BACKENDS, default="opencl")
    parser.add_argument("--phase", choices=PHASES + ("all",), required=True)
    parser.add_argument("--allow-bootstrap-jdk", action="store_true")
    parser.add_argument("--m3-jdk-receipt", type=Path)
    parser.add_argument("--require-device", action="store_true")
    parser.add_argument("--require-gpu", action="store_true", help="require a reported GPU device; implies --require-device")
    parser.add_argument("--resolve-build-tools", action="store_true", help="permit explicit Maven build-tool resolution; runtime artifacts remain hash-locked")
    parser.add_argument("--command-timeout", type=int, default=1800)
    arguments = parser.parse_args(argv)
    arguments.require_device = arguments.require_device or arguments.require_gpu
    require(1 <= arguments.command_timeout <= 7200, "command timeout must be between 1 and 7200 seconds")
    return arguments


def main(argv=None):
    try:
        Build(parse_args(argv)).execute()
        return 0
    except (Refusal, OSError, ValueError, KeyError, ET.ParseError) as failure:
        print("M3_TORNADO_REFUSED: " + str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
