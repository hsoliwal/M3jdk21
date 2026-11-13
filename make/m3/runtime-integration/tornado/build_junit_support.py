#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Build existing source-owned JUnit support and emit a test-only runtime lock."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
PREFIX = "m3/vendor/third_party/"
SELECTED = {
    "apiguardian-api": ("org.apiguardian", "apiguardian-api", "1.1.2-m3-source-1"),
    "opentest4j": ("org.opentest4j", "opentest4j", "1.3.0-m3-source-1"),
    "univocity-parsers": ("com.univocity", "univocity-parsers", "2.9.1-m3-source-1"),
    "junit-platform-commons-1.11.4": ("org.junit.platform", "junit-platform-commons", "1.11.4-m3-source-1"),
    "junit-jupiter-api-5.11.4": ("org.junit.jupiter", "junit-jupiter-api", "5.11.4-m3-source-1"),
    "junit-platform-engine-1.11.4": ("org.junit.platform", "junit-platform-engine", "1.11.4-m3-source-1"),
    "junit-jupiter-engine-5.11.4": ("org.junit.jupiter", "junit-jupiter-engine", "5.11.4-m3-source-1"),
    "junit-jupiter-params-5.11.4": ("org.junit.jupiter", "junit-jupiter-params", "5.11.4-m3-source-1"),
    "junit-jupiter-5.11.4": ("org.junit.jupiter", "junit-jupiter", "5.11.4-m3-source-1"),
    "junit-platform-launcher-1.11.4": ("org.junit.platform", "junit-platform-launcher", "1.11.4-m3-source-1"),
}
ARTIFACT_CLASSES = {
    "apiguardian-api": "org/apiguardian/api/API.class",
    "opentest4j": "org/opentest4j/TestAbortedException.class",
    "junit-jupiter-api-5.11.4": "org/junit/jupiter/api/Test.class",
    "junit-jupiter-engine-5.11.4": "org/junit/jupiter/engine/JupiterTestEngine.class",
    "junit-platform-commons-1.11.4": "org/junit/platform/commons/util/StringUtils.class",
    "junit-platform-engine-1.11.4": "org/junit/platform/engine/TestEngine.class",
    "junit-platform-launcher-1.11.4": "org/junit/platform/launcher/Launcher.class",
}


class Refusal(RuntimeError):
    """Missing source or failed build support cannot produce an admission lock."""


def require(condition, message):
    if not condition:
        raise Refusal(message)


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def seal(path):
    return {"path": str(Path(path).resolve()), "sha256": sha(path)}


def write_json(path, value):
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def checked(root, relative):
    require(isinstance(relative, str) and relative and "\\" not in relative,
            "invalid source path")
    parsed = PurePosixPath(relative)
    require(not parsed.is_absolute() and ":" not in parsed.parts[0]
            and all(part not in ("", ".", "..") for part in relative.split("/"))
            and not any(ord(char) < 32 for char in relative), "noncanonical source path")
    path = root
    for part in parsed.parts:
        path = path / part
        require(not path.is_symlink(), "source symlink refused: " + str(path))
    require(path.is_file(), "source file absent: " + str(path))
    return path


def source_contract(repo):
    """Validate existing owners and return the exact unchanged staging inputs."""
    vendor = repo / PREFIX
    root_pom = checked(repo, PREFIX + "pom.xml")
    model = ET.parse(root_pom).getroot()
    modules = [item.text for item in model.findall("m:modules/m:module", NS)]
    require(0 < len(modules) <= 512 and len(set(modules)) == len(modules),
            "bounded unique reactor module model required")
    require(set(SELECTED) <= set(modules), "source JUnit reactor closure missing")
    inputs = {"pom.xml": root_pom}
    for name in modules:
        require(isinstance(name, str) and "/" not in name, "direct module model required")
        path = checked(vendor, name + "/pom.xml")
        child = ET.parse(path).getroot()
        require(child.find("m:parent", NS) is None and not child.findall("m:modules/m:module", NS),
                "parent/nested reactor model requires separate qualification: " + name)
        inputs[name + "/pom.xml"] = path
        if name in SELECTED:
            coordinate = tuple(child.findtext("m:" + key, namespaces=NS)
                               for key in ("groupId", "artifactId", "version"))
            require(coordinate == SELECTED[name], "selected source coordinate drift: " + name)
    provenance_count = 0
    for name in SELECTED:
        root = vendor / name
        require(root.is_dir() and not root.is_symlink(), "regular module directory required")
        for directory, dirs, files in os.walk(root, followlinks=False):
            for item in dirs + files:
                require(not (Path(directory) / item).is_symlink(), "source symlink refused")
            dirs[:] = [item for item in dirs if item not in ("target", ".git")]
            for item in files:
                path = Path(directory) / item
                if path == root / "dependency-reduced-pom.xml":
                    continue  # Existing shade build output, never a source input.
                relative = path.relative_to(vendor).as_posix()
                inputs[relative] = checked(vendor, relative)
        provenance = json.loads(checked(root, "PROVENANCE.json").read_text(encoding="utf-8"))
        require(provenance.get("coordinate") == ":".join(SELECTED[name]), "provenance coordinate drift")
        require(provenance.get("files"), "empty source provenance")
        seen = set()
        for row in provenance["files"]:
            path = row["repository"]
            require(path.startswith(PREFIX + name + "/") and path not in seen,
                    "provenance path escapes owner or is duplicated")
            seen.add(path)
            require(re.fullmatch(r"[0-9a-f]{64}", row["sha256"])
                    and sha(checked(repo, path)) == row["sha256"], "source provenance drift: " + path)
            provenance_count += 1
    require(len(inputs) <= 10000 and sum(path.stat().st_size for path in inputs.values()) <= 64 * 1024 * 1024,
            "source support inventory exceeds bound")
    return inputs, {"reactor_models": len(modules), "selected_modules": len(SELECTED),
                    "source_files": len(inputs), "provenance_files_checked": provenance_count}


def run(argv, cwd, env, log, timeout):
    start = time.monotonic()
    code = -1
    with log.open("wb") as stream:
        process = subprocess.Popen([str(value) for value in argv], cwd=cwd, env=env,
                                   stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
            stream.write(b"\nM3_JUNIT_SUPPORT_TIMEOUT\n")
    return {"argv": [str(value) for value in argv], "cwd": str(cwd), "exit_code": code,
            "elapsed_seconds": time.monotonic() - start, "log": seal(log)}


def prepare(args):
    repo, jdk, maven, m2, out = (path.resolve() for path in
                                (args.repo_root, args.jdk_home, args.maven, args.maven_repo, args.out))
    for protected in (repo, jdk, m2, maven.parent):
        require(out != protected and out not in protected.parents and protected not in out.parents,
                "output overlaps an input root")
    require(not out.exists(), "fresh output directory required")
    require(maven.is_file() and (jdk / "bin/java").is_file() and (jdk / "bin/javac").is_file(),
            "Maven and complete Java toolchain required")
    require(re.search(r'^JAVA_VERSION="21(?:[.+\-"]|$)', (jdk / "release").read_text(), re.M),
            "source support build requires Java21")
    env = dict(os.environ)
    for name in ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_ARGS", "MAVEN_OPTS"):
        require(not env.get(name), "untracked tool injection refused: " + name)
    env.update(JAVA_HOME=str(jdk), PATH=str(jdk / "bin") + os.pathsep + env.get("PATH", ""),
               MAVEN_OPTS="-Xmx2g -XX:ActiveProcessorCount=4")
    if args.maven_truststore:
        require(args.maven_truststore.is_file(), "Maven CA store absent")
        env["MAVEN_OPTS"] += " -Djavax.net.ssl.trustStore=" + str(args.maven_truststore.resolve())
    if args.maven_settings:
        require(args.maven_settings.is_file(), "Maven settings absent")
    out.mkdir(parents=True)
    result = {"schema": "m3-source-junit-build/1", "status": "FAILED",
              "scope": "Source-built test support only; skipTests packaging is not test execution",
              "commands": [], "preparer": seal(Path(__file__)),
              "toolchain": [seal(jdk / name) for name in ("release", "bin/java", "bin/javac")]
                           + [seal(maven)]}
    try:
        inputs, contract = source_contract(repo)
        before = {name: sha(path) for name, path in inputs.items()}
        write_json(out / "SOURCE_CONTRACT.json", {"status": "PASS", **contract,
                   "inputs": [{"relative": name, **seal(path)} for name, path in sorted(inputs.items())]})
        stage = out / "staged/third_party"
        for name, source in inputs.items():
            target = stage / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            target.chmod(0o755 if source.stat().st_mode & 0o111 else 0o644)
        command = [maven, "-B", "-ntp", "-T1", "-Dstyle.color=never", "-Dmaven.repo.local=" + str(m2)]
        command += ["-U"] if args.resolve_build_tools else ["--offline"]
        if args.maven_settings:
            command += ["-s", args.maven_settings.resolve()]
        command += ["-f", stage / "pom.xml", "-pl", "junit-jupiter-5.11.4,junit-platform-launcher-1.11.4",
                    "-am", "-DskipTests", "install"]
        write_json(out / "COMMAND.json", [str(value) for value in command])
        event = run(command, stage, env, out / "maven-build.log", args.command_timeout)
        result["commands"].append(event)
        require(event["exit_code"] == 0, "source JUnit Maven build failed; see maven-build.log")
        after, _ = source_contract(repo)
        require({name: sha(path) for name, path in after.items()} == before,
                "original source inputs changed during support build")
        for name, digest in before.items():
            require(sha(checked(stage, name)) == digest, "staged source input changed: " + name)
        artifacts = out / "artifacts"
        artifacts.mkdir()
        jars = []
        for name, required_class in ARTIFACT_CLASSES.items():
            _, artifact, version = SELECTED[name]
            filename = artifact + "-" + version + ".jar"
            produced = checked(stage, name + "/target/" + filename)
            with zipfile.ZipFile(produced) as archive:
                require(archive.testzip() is None and required_class in archive.namelist(),
                        "invalid or incomplete source-built support jar: " + filename)
            target = artifacts / filename
            shutil.copyfile(produced, target)
            jars.append(seal(target))
        result.update(status="PASS", contract=contract, artifacts=jars,
                      upstream_source_bytes_changed=False, tests_executed=False,
                      selected_source_coordinates=[":".join(value) for value in SELECTED.values()])
        write_json(out / "BUILD_RESULT.json", result)
        sources = [seal(path) for _, path in sorted(inputs.items())]
        sources += [seal(stage / name) for name in sorted(inputs)]
        lock = {"schema": "m3-source-junit-support/1", "status": "PASS", "scope": result["scope"],
                "artifacts": jars, "sources": sources,
                "build_receipts": [seal(out / name) for name in
                                   ("SOURCE_CONTRACT.json", "COMMAND.json", "maven-build.log", "BUILD_RESULT.json")]}
        write_json(out / "JUNIT_LOCK.json", lock)
        print("M3_SOURCE_JUNIT_SUPPORT_PASS " + str(out / "JUNIT_LOCK.json"), flush=True)
        return 0
    except Exception as failure:
        result["failure"] = str(failure)
        write_json(out / "BUILD_RESULT.json", result)
        raise


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("repo-root", "jdk-home", "maven", "maven-repo", "out"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--maven-truststore", type=Path)
    parser.add_argument("--resolve-build-tools", action="store_true")
    parser.add_argument("--command-timeout", type=int, default=1800)
    args = parser.parse_args(argv)
    require(1 <= args.command_timeout <= 7200, "bounded command timeout required")
    return args


def main(argv=None):
    try:
        return prepare(parse_args(argv))
    except (Refusal, OSError, ValueError, KeyError, ET.ParseError, zipfile.BadZipFile) as failure:
        print("M3_SOURCE_JUNIT_SUPPORT_REFUSED: " + str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
