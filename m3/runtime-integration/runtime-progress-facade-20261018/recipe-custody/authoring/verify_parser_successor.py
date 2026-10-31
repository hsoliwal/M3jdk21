#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile original/repaired donor modules from exact qualified recipe outputs."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

DATASET = "m3-openrewrite-javadoc"
OWNER = "rewrite-java-21/src/main/java/org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor.java"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def classes(root):
    return {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob("*.class"))}


def main():
    parser = argparse.ArgumentParser()
    for name in ("crate", "recipe-owner", "upstream", "jdk", "maven", "settings", "repository", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location("original_parser_successor_receiver", args.crate / "receive.py")
    receiver = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(receiver)
    qualified = receiver.qualification(args.crate.resolve(), DATASET, args.recipe_owner.resolve())
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    if len(qualified["outputs"]) != 15:
        raise ValueError("unexpected parser module source closure")
    rows = []
    receipt = {"schema": "M3_OPENREWRITE_SOURCE_BUILT_PARSER_V1", "status": "RUNNING",
        "sdk_inputs_sha256": sha(qualified["candidate"] / "SDK_INPUTS.tsv"),
        "sdk_results_sha256": sha(qualified["candidate"] / "SDK_RESULTS.tsv"),
        "coordinate": "com.synexia.sourcebuilt.openrewrite:rewrite-java-21:8.17.1-m3-javadoc-1",
        "original_donor": "openrewrite/rewrite@3bd8271be963c025ce0856072d0f09360e3184cd",
        "commands": rows, "legacy_jdk_compiler_gate_changed": False,
        "scope": "bounded Javadoc atoms and paired donor class proof; no full upstream TCK claim"}
    env = os.environ.copy()
    env["JAVA_HOME"] = str(args.jdk.resolve())
    env["PATH"] = env["JAVA_HOME"] + "/bin:" + env["PATH"]
    env["MAVEN_OPTS"] = "-Xmx256m -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
    try:
        for variant in ("baseline", "candidate"):
            for relative, data in sorted(qualified["outputs"].items()):
                target = output / variant / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                if variant == "baseline" and relative == OWNER:
                    data = (args.upstream / relative).read_bytes()
                    if hashlib.sha256(data).hexdigest() != qualified["manifests"][relative][0]:
                        raise ValueError("original visitor preimage drift")
                target.write_bytes(data)
            module = output / variant / "rewrite-java-21"
            command = [str(args.maven.resolve()), "-s", str(args.settings.resolve()), "-B", "-ntp",
                "-f", str(module / "pom.xml"), "-Dmaven.repo.local=" + str(args.repository.resolve()),
                "install" if variant == "candidate" else "verify"]
            log = output / (variant + "-maven.log")
            started = time.time()
            with log.open("w") as stream:
                result = subprocess.run(command, env=env, stdout=stream, stderr=subprocess.STDOUT, timeout=240)
            report = module / "target/surefire-reports/TEST-M3RuntimeJavadocParserTest.xml"
            row = {"variant": variant, "command": command, "exit_code": result.returncode,
                "elapsed_seconds": time.time() - started, "log_sha256": sha(log)}
            rows.append(row)
            if not report.is_file():
                raise AssertionError("parser regression tests did not run: " + variant)
            suite = ET.parse(report).getroot()
            counts = {name: int(suite.get(name, "0")) for name in ("tests", "failures", "errors", "skipped")}
            row["tests"] = counts
            row["report_sha256"] = sha(report)
            if counts["tests"] != 4 or counts["errors"] or counts["skipped"]:
                raise AssertionError("unexpected parser regression corpus: " + str(counts))
            if variant == "baseline":
                if result.returncode == 0 or counts["failures"] != 3:
                    raise AssertionError("original donor must fail exactly the three new syntax witnesses")
            elif result.returncode != 0 or counts["failures"]:
                raise AssertionError("repaired parser module did not pass all witnesses")
            config = module / "target/source/lombok.config"
            if sha(config) != sha(module / "lombok.config.txt"):
                raise AssertionError("original Lombok configuration was not restored exactly")
        baseline = classes(output / "baseline/rewrite-java-21/target/classes")
        candidate = classes(output / "candidate/rewrite-java-21/target/classes")
        if set(baseline) != set(candidate):
            raise AssertionError("parser repair changed class owners")
        changed = [name for name in baseline if baseline[name] != candidate[name]]
        outer = "org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor.class"
        nested = {"org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor$JavaVisitor.class",
            "org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor$1.class"}
        if outer not in changed or set(changed) - {outer, *nested}:
            raise AssertionError("unexpected compiled owner changes: " + str(changed))
        debug_only = []
        for inner in sorted(nested.intersection(changed)):
            normalized = []
            for variant in ("baseline", "candidate"):
                command = [str(args.jdk / "bin/javap"), "-v", "-p", str(output / variant / "rewrite-java-21/target/classes" / inner)]
                result = subprocess.run(command, capture_output=True, text=True, check=True, timeout=30)
                (output / (variant + "-" + Path(inner).name + ".javap.txt")).write_text(result.stdout)
                lines = [line for line in result.stdout.splitlines()
                    if not line.startswith("Classfile ") and not line.lstrip().startswith(("Last modified ", "SHA-256 checksum "))]
                normalized.append(re.sub(r"(?m)^(\s+line )\d+(: \d+)$", r"\g<1>N\2", "\n".join(lines)))
            if normalized[0] != normalized[1]:
                raise AssertionError("nested donor visitor changed beyond shifted source line numbers")
            debug_only.append(inner)
        artifact = output / "candidate/rewrite-java-21/target/rewrite-java-21-8.17.1-m3-javadoc-1.jar"
        installed = args.repository / "com/synexia/sourcebuilt/openrewrite/rewrite-java-21/8.17.1-m3-javadoc-1" / artifact.name
        if sha(artifact) != sha(installed):
            raise AssertionError("installed source-built parser differs from tested artifact")
        receipt.update({"status": "PASS", "compiled_classes": len(candidate), "changed_classes": changed,
            "line_number_only_changes": debug_only,
            "unchanged_class_hashes": {name: baseline[name] for name in baseline if name not in changed},
            "artifact_sha256": sha(artifact), "artifact": str(artifact),
            "original_main_warnings": [line for line in (output / "baseline-maven.log").read_text().splitlines() if "[WARNING]" in line],
            "candidate_main_warnings": [line for line in (output / "candidate-maven.log").read_text().splitlines() if "[WARNING]" in line]})
    except BaseException as failure:
        receipt["status"] = "FAIL"
        receipt["failure"] = type(failure).__name__ + ": " + str(failure)
        raise
    finally:
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")


if __name__ == "__main__":
    main()
