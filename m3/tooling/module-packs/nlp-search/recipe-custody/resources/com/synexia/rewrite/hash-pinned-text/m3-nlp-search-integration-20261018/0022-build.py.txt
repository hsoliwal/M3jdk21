#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Build source-vendored Lucene, OpenNLP, the full M3File port, and their M3AST consumer."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import resource
import shutil
import subprocess
import sys
import time

import compiler_source_proof


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--compiler-source-proof", action="store_true",
                        help="Explicitly build only the pinned M3 compiler hook on a stock Java 21 JVM")
    parser.add_argument("--runtime-java-arg", action="append", default=[],
                        help="Additional VM option for candidate runtime probes; repeat as needed")
    args = parser.parse_args()
    owner = Path(__file__).resolve().parent
    root = owner.parents[3]
    home = args.jdk_home.resolve()
    out = args.out.resolve()
    if out.exists():
        parser.error("--out must be a fresh directory")
    for name in ("java", "javac", "jar", "jmod", "jlink"):
        if not (home / "bin" / name).is_file():
            parser.error("Missing full JDK executable: " + name)
    out.mkdir(parents=True)
    commands = []

    def run(name, argv):
        argv = list(map(str, argv))
        before = resource.getrusage(resource.RUSAGE_CHILDREN)
        started = time.monotonic()
        result = subprocess.run(argv, cwd=root, text=True, encoding="utf-8", errors="replace",
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=360)
        after = resource.getrusage(resource.RUSAGE_CHILDREN)
        (out / (name + ".log")).write_text(result.stdout, encoding="utf-8")
        commands.append({"name": name, "argv": argv, "exit": result.returncode,
            "wall_seconds": round(time.monotonic() - started, 6),
            "child_cpu_seconds": round(after.ru_utime + after.ru_stime - before.ru_utime - before.ru_stime, 6),
            "cumulative_child_maxrss_platform_units": after.ru_maxrss})
        (out / "COMMANDS.json").write_text(json.dumps(commands, indent=2) + "\n")
        if result.returncode:
            raise RuntimeError(name + " failed; see " + str(out / (name + ".log")) + "\n" + result.stdout[-5000:])
        return result.stdout

    lock = json.loads((owner / "INPUTS.json").read_text())
    for name, expected in lock["files"].items():
        if digest(root / name) != expected:
            raise ValueError("NLP/file integration input drift: " + name)
    for directory, expected in lock["java_source_sets"].items():
        actual = sorted(str(p.relative_to(root)) for p in (root / directory).rglob("*.java"))
        if actual != expected:
            raise ValueError("Incomplete or unexpected module source inventory: " + directory)
    for directory, expected in lock["native_source_sets"].items():
        actual = sorted(str(p.relative_to(root)) for p in (root / directory).rglob("*")
                        if p.is_file() and p.suffix in (".c", ".h"))
        if actual != expected:
            raise ValueError("Incomplete or unexpected native source inventory: " + directory)
    run("input-jdk", [home / "bin/java", "-version"])
    jdk = (compiler_source_proof.build(root, home, out / "compiler-proof", run)
           if args.compiler_source_proof else home)
    # Ordinary builds use the actual supplied M3JDK. Reject an unrelated compiler
    # before spending time building tools or producing an unusable frontend module.
    if "com.sun.source.util.M3AST" not in run("m3-compiler-api", [jdk / "bin/javap", "--module",
                                                                "jdk.compiler", "com.sun.source.util.M3AST"]):
        raise ValueError("The supplied JDK must contain the M3AST compiler API")
    source_owner = root / "m3/vendor/synexia/nlp-search"
    spec = importlib.util.spec_from_file_location("synexia_source_build", source_owner / "build.py")
    source_build = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(source_build)
    view = out / "donor"
    view.mkdir()
    (view / "third_party").symlink_to(root / "m3/vendor/third_party", target_is_directory=True)
    # The independent oracle has no M3 overlays. Both builds validate the complete
    # unchanged upstream source/resource lock before compiling their own classes.
    baseline = source_build.compile_tools(view, out / "baseline-tools", jdk)
    jars = source_build.compile_tools(view, out / "tools", jdk, root / "m3/vendor/third_party-m3/nlp-search")
    names = {"slf4j-api-2.0.12": "org.slf4j", "slf4j-simple": "org.slf4j.simple",
             "opennlp-tools": "org.apache.opennlp.tools"}
    names.update({"lucene-" + name: "org.apache.lucene." + ("analysis.common" if name == "analysis-common" else name)
                  for name in ("core", "analysis-common", "queries", "sandbox", "queryparser")})

    def compile_module(artifact, module, source):
        sources = sorted(source.rglob("*.java"))
        classes = out / "classes" / artifact
        classes.mkdir(parents=True)
        source_args = out / (artifact + ".args")
        source_args.write_text("\n".join('"' + str(p) + '"' for p in sources) + "\n")
        run(artifact + "-compile", [jdk / "bin/javac", "-J-Xmx512m", "-proc:none", "-encoding", "UTF-8",
            "-source", "21", "-target", "21", "-Xlint:all", "-Werror", "--module-path",
            os.pathsep.join(map(str, jars)), "-d", classes, "@" + str(source_args)])
        legal = classes / "META-INF/m3-nlp-search"
        legal.mkdir(parents=True)
        for filename in ("SOURCE_REF.json", "FILE_PORT_MAP.json", "NOTICE.txt", "LICENSE.txt"):
            original = owner / filename
            if original.is_file():
                shutil.copy2(original, legal / filename)
        jar = out / "tools" / (artifact + ".jar")
        run(artifact + "-jar", [jdk / "bin/jar", "--create", "--file", jar,
            "--date=2026-10-18T00:00:00Z", "-C", classes, "."])
        jars.append(jar)
        names[artifact] = module

    # Compile against the exact existing descriptor, avoiding an automatic-module
    # approximation while checking the new exported frontend surface.
    nlp_descriptor = root / "m3/tooling/module-packs/nlp-search/src/org.apache.opennlp.tools/module-info.java"
    descriptor_classes = out / "opennlp-descriptor"
    run("opennlp-compile-descriptor", [jdk / "bin/javac", "-source", "21", "-target", "21",
        "--module-path", os.pathsep.join(map(str, (jars[0], jars[1]))), "--patch-module",
        "org.apache.opennlp.tools=" + str(jars[2]), "-d", descriptor_classes, nlp_descriptor])
    modular_nlp = out / "tools/opennlp-tools.modular.jar"
    shutil.copy2(jars[2], modular_nlp)
    run("opennlp-modular-jar", [jdk / "bin/jar", "--update", "--file", modular_nlp,
        "--date=2026-10-18T00:00:00Z", "-C", descriptor_classes, "module-info.class"])
    jars[2] = modular_nlp

    compile_module("m3-file", "org.m3jdk.file", root / "m3/ports/file/src/main/java")
    compile_module("m3-source-search", "org.m3jdk.tools.search", owner / "frontend/src/main/java")
    native_sources = sorted((root / "m3/ports/file/native").glob("*.c"))
    native_args = []
    if native_sources:
        native = out / "native"
        native.mkdir()
        run("m3-file-native", [os.environ.get("CC", "cc"), "-std=c11", "-O2", "-fPIC", "-shared",
            "-Wall", "-Wextra", "-Werror", "-I" + str(home / "include"),
            "-I" + str(home / "include/linux"), "-I" + str(root / "m3/ports/file/native"),
            *native_sources, "-o", native / "libm3file.so"])
        native_args = ["--native-library", "org.m3jdk.file=" + str(native)]
    rows = ["artifact\tversion\tjar\tmodule\trequires\tdisposition"]
    for artifact, module in names.items():
        if artifact != "opennlp-tools":
            rows.append(f"{artifact}\t1\t{out / 'tools' / (artifact + '.jar')}\t{module}\t-\tJMOD")
    (out / "IMAGE.tsv").write_text("\n".join(rows) + "\n")
    descriptor = "m3/tooling/module-packs/nlp-search/src/org.apache.opennlp.tools/module-info.java"
    (out / "DESCRIPTORS.tsv").write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n"
        + f"org.apache.opennlp.tools\topennlp-tools\t{out / 'tools/opennlp-tools.jar'}\t{descriptor}\t"
        + f"{digest(root / descriptor)}\t{root / descriptor}\n")
    run("pack", [sys.executable, root / "m3/tooling/jmod-recipes/tools/jmod_pack.py", "--jdk-home", jdk,
        "--image-tsv", out / "IMAGE.tsv", "--manifest", out / "DESCRIPTORS.tsv", "--layer-root", root,
        "--out", out / "pack", "--add-modules",
        "java.se,jdk.unsupported,jdk.management,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps,jdk.charsets", *native_args])
    image = out / "pack/image"
    symbols = compiler_source_proof.copy_symbols(home, image)
    modules = ",".join(names.values())
    runtime = [image / "bin/java", "-Xmx256m", "-ea", "-Xcheck:jni", *args.runtime_java_arg]
    probe = out / "probe"
    probe.mkdir()
    run("tools-probe-compile", [image / "bin/javac", "-source", "21", "-target", "21",
        "--add-modules", modules, "-d", probe, owner / "fixtures/src/test/java/ToolsProbe.java"])
    behavior = run("image.behavior", [*runtime, "-Djava.util.logging.SimpleFormatter.format=%4$s: %5$s%n",
        "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn", "--add-modules", modules, "--add-opens",
        "org.apache.lucene.queryparser/org.apache.lucene.queryparser.classic=ALL-UNNAMED", "-cp", probe, "ToolsProbe"])
    if "TOOLS_BEHAVIOR 1ebf2383e1199b0b9bc516852f02a727c8681cb36ec086a33252afb599dd900e" not in behavior:
        raise ValueError("Frozen full-tools behavior changed")
    if "TokenizerME" not in run("image.cli", [*runtime, "-m", "org.apache.opennlp.tools/opennlp.tools.cmdline.CLI"]):
        raise ValueError("OpenNLP CLI provider is missing")
    lucene = owner / "fixtures/lucene/src/test/java"
    lucene_classes = out / "lucene-probe"
    readers = "org.apache.lucene.analysis.common,org.apache.lucene.queryparser"
    reads = "org.apache.lucene.core=" + readers
    run("lucene-probe-compile", [image / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
        "--patch-module", "org.apache.lucene.core=" + str(lucene), "--add-modules", readers,
        "--add-reads", reads, "-d", lucene_classes, *sorted(lucene.glob("*.java"))])
    lucene_runtime = [*runtime, "-XX:-OmitStackTraceInFastThrow", "--patch-module",
        "org.apache.lucene.core=" + str(lucene_classes), "--add-modules", readers, "--add-reads", reads]
    for main in ("M3ReaderBaselineCheck", "M3ReaderCompatibilityProbe"):
        baseline_text = run(main + "-baseline", [*runtime,
            "-XX:-OmitStackTraceInFastThrow", "--limit-modules", "java.se,jdk.unsupported", "-cp",
            os.pathsep.join(map(str, [lucene_classes, *baseline])), "org.apache.lucene.analysis." + main])
        candidate_text = run(main + "-image", [*lucene_runtime, "-m", "org.apache.lucene.core/org.apache.lucene.analysis." + main])
        if baseline_text != candidate_text:
            raise ValueError("Legacy Lucene reader differential failed: " + main)
    for main, marker in (("M3TextReadersCheck", "LUCENE_M3_READER_PASS"),
                         ("M3LuceneContractProbe", "LUCENE_M3_CONTRACT_PASS")):
        if marker not in run(main + "-image", [*lucene_runtime, "-m", "org.apache.lucene.core/org.apache.lucene.analysis." + main]):
            raise ValueError("Lucene probe completion missing: " + main)
    nlp = owner / "fixtures/opennlp/src/test/java/OpenNlpM3Probe.java"
    run("opennlp-probe-compile", [image / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
        "--add-modules", "org.apache.opennlp.tools", "-d", probe, nlp])
    run("opennlp-probe-image", [*runtime, "--add-modules", "org.apache.opennlp.tools", "-cp", probe,
        "OpenNlpM3Probe", os.pathsep.join(map(str, baseline[:3]))])
    # Each full-family fixture has a declared module entry point and completion
    # marker; the sealed contract is data, not a replacement transformation engine.
    for family in ("file", "frontend"):
        contract = json.loads((owner / (family.upper() + "_PROBES.json")).read_text())
        source = owner / "fixtures" / family / "src/test/java"
        classes = out / (family + "-probe")
        module = contract["module"]
        run(family + "-probe-compile", [image / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
            "--patch-module", module + "=" + str(source), "--add-modules", modules,
            "-d", classes, *sorted(source.rglob("*.java"))])
        for case in contract["runs"]:
            substitutions = {"image": str(image), "out": str(out)}
            java_args = [value.format_map(substitutions) for value in case.get("java_args", [])]
            program_args = [value.format_map(substitutions) for value in case.get("args", [])]
            output = run(case["name"], [*runtime, *java_args, "--add-modules", modules,
                "--patch-module", module + "=" + str(classes), "-m", module + "/" + case["main"],
                *program_args])
            if case["marker"] not in output:
                raise ValueError("Probe completion missing: " + case["name"])
    run("shared-frontend-contracts", [sys.executable, root / "m3/tooling/shared-frontend/verify_frontend.py",
        "--jdk", image, "--out", out / "compiler-contracts"])
    run("final-module-validation", [*runtime, "--validate-modules"])
    result = {
        "schema": "M3_NLP_SEARCH_FILE_JMOD_PROOF_1", "status": "PASS", "jmod_count": len(names),
        "jmods": {p.name: digest(p) for p in sorted((out / "pack/jmods").glob("*.jmod"))},
        "modules": sorted(names.values()), "input_lock_sha256": digest(owner / "INPUTS.json"),
        "ct_sym_sha256": symbols, "runtime_dependency_on_synexia": False,
        "compiler_source_proof": args.compiler_source_proof,
        "runtime_vm_options": args.runtime_java_arg,
        "runtime_vm_option_scope": "Primary and Lucene-oracle launches; OpenNLP diagnostic child and shared-frontend harness retain their own declared VM options",
        "whole_jdk_or_m3_string_promotion": False,
        "resource_evidence": "COMMANDS.json records wall time, child CPU, and cumulative child RSS; no speedup claim",
        "full_upstream_suites": "NOT_RUN", "cross_platform_native_qualification": "PENDING",
    }
    (out / "RESULT.json").write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    print("M3_NLP_SEARCH_FILE_PASS modules=" + str(len(names)) + " runtimeDependencyOnSynexia=false")


if __name__ == "__main__":
    main()
