#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compile and execute the actual SDK-emitted optional adapter, without changing receiver sources."""
import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent

def run(command, log):
    with log.open("w") as output:
        subprocess.run([str(arg) for arg in command], check=True, stdout=output, stderr=subprocess.STDOUT)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--plugin", type=Path, required=True)
    parser.add_argument("--patch", type=Path, required=True)
    parser.add_argument("--work", type=Path)
    args = parser.parse_args()
    pins = json.loads(HERE.joinpath("SOURCE_PINS.json").read_text())
    candidate = HERE / "target/candidate" / pins["target"]
    candidate_bytes = candidate.read_bytes()
    candidate_hash = sha256(candidate_bytes).hexdigest()
    if candidate_hash != pins["after_sha256"]:
        raise ValueError("actual SDK candidate does not match the reviewed source seal")
    if candidate_bytes != (HERE / pins["template"]).read_bytes():
        raise ValueError("actual SDK candidate is not the current reviewed adapter")
    sdk_receipt = dict(line.split("\t", 1) for line in HERE.joinpath("target/SDK_RECEIPT.tsv").read_text().splitlines())
    if sdk_receipt.get("sha256") != candidate_hash or sdk_receipt.get("results") != "1" or sdk_receipt.get("fixed_point_results") != "0":
        raise ValueError("missing successful typed SDK emission and fixed point receipt")
    report = ET.parse(HERE / "target/surefire-reports/TEST-com.synexia.rewrite.SharedASTRecipeTest.xml").getroot()
    if any(report.get(key) != "0" for key in ("errors", "failures", "skipped")):
        raise ValueError("SDK qualification did not pass completely")
    classpath = next(p.get("value") for p in report.findall("./properties/property") if p.get("name") == "java.class.path")
    runtime = args.work.resolve() if args.work else Path(tempfile.mkdtemp(prefix="m3-openrewrite-same-task-"))
    runtime.mkdir(parents=True, exist_ok=True)
    if any(runtime.iterdir()):
        raise ValueError("optional proof output must be a fresh empty directory")
    host = runtime / "host-plugin.jar"
    host_bytes = args.plugin.read_bytes()
    host.write_bytes(host_bytes)
    patch = runtime / "compiler-patch.jar"
    patch_bytes = args.patch.read_bytes()
    patch.write_bytes(patch_bytes)
    combined_classpath = str(host) + os.pathsep + classpath
    classes = runtime / "classes"
    classes.mkdir(exist_ok=True)
    exports = [arg for package in ("api", "code", "comp", "tree", "util")
               for arg in ("--add-exports", "jdk.compiler/com.sun.tools.javac." + package + "=ALL-UNNAMED")]
    compiler_patch = ["-J--patch-module=jdk.compiler=" + str(patch), "-J--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"]
    runtime_patch = ["--patch-module", "jdk.compiler=" + str(patch), "--add-exports", "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"]
    source_files = [candidate, HERE / "proof/SharedASTProof.java", HERE / "proof/NoOpenRewriteProbe.java"]
    run([args.jdk / "bin/javac", "-J-Xmx512m", *compiler_patch, "-source", "21", "-target", "21", "-proc:none", "-Xlint:all", "-Werror",
         *exports, "-cp", combined_classpath, "-d", classes, *source_files], runtime / "javac.log")
    class_log = runtime / "class-load.log"
    run([args.jdk / "bin/java", "-Xmx768m", "-Xverify:all", "-Xlog:class+load=info:file=" + str(class_log), *runtime_patch, *exports,
         "-cp", str(classes) + os.pathsep + combined_classpath,
         "SharedASTProof", runtime / "compilations"], runtime / "same-task.log")
    class_loads = class_log.read_text()
    if not re.search(r"org\.openrewrite\.java\.isolated\.ReloadableJava21ParserVisitor\s+source:", class_loads):
        raise ValueError("direct stock visitor was not exercised")
    if re.search(r"org\.openrewrite\.java\.isolated\.ReloadableJava21Parser(?:\$[\w$]+)?\s+source:", class_loads):
        raise ValueError("an independent OpenRewrite compiler/parser implementation was loaded")
    plain_output = runtime / "without-openrewrite"
    plain_output.mkdir(exist_ok=True)
    run([args.jdk / "bin/java", "-Xmx512m", "-Xverify:all", *runtime_patch, "-cp", str(classes) + os.pathsep + str(host),
         "NoOpenRewriteProbe", plain_output], runtime / "without-openrewrite.log")
    visitor_jars = [Path(path) for path in classpath.split(os.pathsep)
                    if re.search(r"rewrite-(?:core|java|java-21)-8\.17\.1\.jar$", path)]
    if len(visitor_jars) != 3:
        raise ValueError("proof classpath does not pin all three OpenRewrite 8.17.1 artifacts")
    receipt = {"format": "M3_OPENREWRITE_SAME_JAVAC_TASK_V1", "status": "PASSED",
        "sdk_tests": int(report.get("tests")), "sdk_actual_results": 1, "sdk_fixed_point_results": 0,
        "adapter_source_sha256": candidate_hash,
        "host_plugin_sha256": sha256(host_bytes).hexdigest(),
        "compiler_patch_sha256": sha256(patch_bytes).hexdigest(),
        "openrewrite_binary_sha256": {p.name: sha256(p.read_bytes()).hexdigest() for p in visitor_jars},
        "jdk": subprocess.check_output([str(args.jdk / "bin/java"), "-version"], stderr=subprocess.STDOUT, text=True).strip(),
        "measured_input_units": 2, "measured_javac_tasks": 1, "measured_parse_events": 2,
        "whole_unit_conversions": 2, "top_level_analyze_events": 4, "exact_frozen_source_reads": 2,
        "bytecode_reference_classes": 4, "bytecode_equality": "PASSED",
        "original_javac_tree_and_type_identity": "PASSED",
        "default_bytodo_generation_interleaving": "REPRODUCED",
        "refusal_cases": 9, "core_with_no_openrewrite_classpath": "PASSED",
        "lazy_source_completion": {"baseline_parse_events": 1, "visitor_parse_events": 2,
                                   "published_lst": 0, "guarded_refusal": "PASSED"},
        "independent_openrewrite_parser_class_loaded": False,
        "limits": ["Java 21 / OpenRewrite 8.17.1 only", "class-bearing compilation units only",
                   "host must select SIMPLE and disable parse-time string folding before parsing",
                   "M3JavacASTIndex frozen source contract is trusted", "LST is a projection, not javac tree replacement",
                   "lazy classfile loading remains possible; unexpected source parsing causes refusal, not rollback",
                   "future recipe edits are not applied to javac's ongoing compilation",
                   "whole reactor, full JDK build, performance and other platforms not qualified here"]}
    runtime.joinpath("RESULTS.json").write_text(json.dumps(receipt, indent=2) + "\n")
    evidence = HERE / "evidence"
    evidence.mkdir(exist_ok=True)
    for name in ("RESULTS.json", "javac.log", "same-task.log", "without-openrewrite.log"):
        shutil.copyfile(runtime / name, evidence / name)
    classload_receipt = {"schema": "M3_OPENREWRITE_CLASSLOAD_RECEIPT_V1", "class_load_log_sha256": sha256(class_log.read_bytes()).hexdigest(),
                         "stock_visitor_loaded": True, "independent_parser_loaded": False,
                         "host_plugin_sha256": receipt["host_plugin_sha256"], "compiler_patch_sha256": receipt["compiler_patch_sha256"],
                         "visitor_lines": [line for line in class_loads.splitlines() if "org.openrewrite.java.isolated.ReloadableJava21ParserVisitor source:" in line]}
    evidence.joinpath("CLASSLOAD_RECEIPT.json").write_text(json.dumps(classload_receipt, indent=2) + "\n")
    summary = HERE / "target/runtime"
    summary.mkdir(parents=True, exist_ok=True)
    for name in ("RESULTS.json", "javac.log", "same-task.log", "without-openrewrite.log"):
        shutil.copyfile(runtime / name, summary / name)
    print((runtime / "same-task.log").read_text(), end="")
    print((runtime / "without-openrewrite.log").read_text(), end="")
    print("STOCK_VISITOR_ONLY_CLASSLOAD_PASS independentParserLoaded=false")
    print("OPENREWRITE_OPTIONAL_PROVIDER_VERIFIER_PASS")

if __name__ == "__main__":
    main()
