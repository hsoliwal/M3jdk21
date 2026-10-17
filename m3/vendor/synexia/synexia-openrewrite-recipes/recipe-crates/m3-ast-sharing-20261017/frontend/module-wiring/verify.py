#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exercise the ordinary receiver Maven modules after actual recipe materialization."""
from hashlib import sha256
from pathlib import Path
import argparse
import json
import os
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

HERE = Path(__file__).resolve().parent
FRONTEND = HERE.parent


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--jdk", type=Path, required=True)
    p.add_argument("--maven", type=Path, required=True)
    p.add_argument("--settings", type=Path, required=True)
    p.add_argument("--m2", type=Path, required=True)
    p.add_argument("--target", type=Path, required=True)
    p.add_argument("--native", type=Path, required=True)
    p.add_argument("--patch", type=Path, required=True)
    p.add_argument("--canonical-checkout", type=Path)
    p.add_argument("--online", action="store_true")
    args = p.parse_args()
    env = os.environ.copy()
    env["JAVA_HOME"] = str(args.jdk)
    env["MAVEN_OPTS"] = env.get("MAVEN_OPTS", "") + " -Xmx512m"
    work = HERE / "target/module-proof"
    evidence = HERE / "evidence"
    work.mkdir(parents=True, exist_ok=True)
    evidence.mkdir(parents=True, exist_ok=True)
    commands = []

    def run(label, command, cwd, expected=0):
        command = [str(x) for x in command]
        log = evidence / (label + ".log")
        with log.open("w") as output:
            result = subprocess.run(command, cwd=cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
        commands.append({"label": label, "argv": command, "cwd": str(cwd), "exit_code": result.returncode, "log_sha256": digest(log)})
        if expected is not None and ((expected == 0 and result.returncode != 0) or (expected != 0 and result.returncode == 0)):
            raise RuntimeError(f"unexpected exit {result.returncode} for {label}; see {log}")
        print(label + ": " + str(result.returncode), flush=True)
        return log.read_text()

    mvn = [args.maven, "-B", "-ntp", "-s", args.settings, "-Dmaven.repo.local=" + str(args.m2)]
    if not args.online:
        mvn.append("-o")
    overrides = ["-Dasm.version=9.6", "-Djunit.artifact.version=5.10.2"]
    core = args.target / "m3/ports/compiletime"
    optional = args.target / "m3/ports/compiletime-openrewrite"
    closure = json.loads(HERE.joinpath("VENDOR_CLOSURE.json").read_text())
    vendor = {args.target / "m3/vendor/synexia" / rel for rel in closure["vendor_sources"]}
    ordinary = set(core.joinpath("src/main/java").rglob("*.java"))
    assert len(vendor) == 103 and all(path.is_file() for path in vendor)
    assert len(list(core.joinpath("src/main/java/com/m3/compiletime/ast").glob("*.java"))) == 10
    assert all(path.is_file() for path in [args.native, args.patch])
    pins = json.loads(HERE.joinpath("SOURCE_PINS.json").read_text())
    ordinary_fix = json.loads(HERE.joinpath("local-scope-fix/SOURCE_PIN.json").read_text())
    assert digest(args.target / ordinary_fix["path"]) == ordinary_fix["after_sha256"]
    canonical_root = args.canonical_checkout if args.canonical_checkout else HERE / "target/candidate/canonical"
    canonical_service = canonical_root / "synexia-mindex/compiler/src/main/resources/META-INF/services/com.sun.source.util.Plugin"
    assert canonical_service.read_text() == ("com.synexia.mindex.compiler.MIndexCompilerPlugin\n"
            "com.synexia.mindex.experiment.compiler.MIndexCompilerPlugin\n"
            "com.synexia.mindex.compiler.ast.M3ASTPlugin\n")
    for recipe in pins["recipes"]:
        if recipe["crate"] == "m3-ast-wiring-receiver-20261017":
            for item in recipe["files"]:
                assert digest(args.target / item["path"]) == item["after_sha256"], item["path"]

    reports = core / "target/surefire-reports"

    def clear_reports():
        if reports.exists():
            shutil.rmtree(reports)

    def tests():
        rows = []
        for path in sorted(reports.glob("TEST-*.xml")):
            report = ET.parse(path).getroot()
            rows.append({"class": report.get("name"), **{key: int(report.get(key, 0)) for key in ("tests", "errors", "failures", "skipped")}})
        return rows

    # The receiver POM already documents these two CI dependency overrides.
    clear_reports()
    run("target-normal-maven", [*mvn, *overrides, "install"], core)
    normal_tests = tests()
    assert normal_tests and all(row["errors"] == row["failures"] == row["skipped"] == 0 for row in normal_tests)
    assert not any(row["class"].endswith("M3ASTWorkspaceCacheNativeTest") for row in normal_tests)
    inputs = core / "target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst"
    compiled = {Path(line).resolve() for line in inputs.read_text().splitlines()}
    expected = {path.resolve() for path in vendor | ordinary}
    assert compiled == expected, {"unexpected": sorted(map(str, compiled - expected)), "missing": sorted(map(str, expected - compiled))}
    core_jar = core / "target/m3-compiletime-precompute-0.1.0-SNAPSHOT.jar"
    with zipfile.ZipFile(core_jar) as archive:
        assert archive.read("META-INF/services/com.sun.source.util.Plugin").decode() == "com.m3.compiletime.ast.M3ASTPlugin\n"
        names = archive.namelist()
        assert not any("openrewrite" in name.lower() for name in names)
        assert "com/m3/compiletime/M3CompileTimeStringInventory.class" in names
        assert "com/m3/compiletime/ast/M3JavacASTIndex.class" in names

    leaf_output = run("upstream-leaf-index-selftest", [args.jdk / "bin/java", "-Xmx384m", "-Xverify:all", "-cp",
            os.pathsep.join((str(core / "target/classes"), str(core / "target/test-classes"))),
            "com.m3.compiletime.M3ASTLeafIndexSelfTest"], HERE)
    assert "PASS" in leaf_output

    run("target-optional-maven", [*mvn, *overrides, "package"], optional)
    optional_jar = optional / "target/m3-compiletime-openrewrite-0.1.0-SNAPSHOT.jar"
    with zipfile.ZipFile(optional_jar) as archive:
        assert "com/m3/compiletime/ast/M3OpenRewriteSharedAST.class" in archive.namelist()

    clear_reports()
    run("target-native-missing-refusal", [*mvn, *overrides, "-Pm3-ast-native", "-Dm3.cache.native=" + str(work / "intentionally-missing-native-library"), "test"], core, expected=1)
    missing_tests = tests()
    missing = [row for row in missing_tests if row["class"].endswith("M3ASTWorkspaceCacheNativeTest")]
    assert len(missing) == 1 and missing[0]["tests"] == missing[0]["failures"] == 2 and missing[0]["errors"] == 0
    assert all(row["errors"] == row["failures"] == 0 for row in missing_tests if row not in missing)
    clear_reports()
    run("target-native-positive", [*mvn, *overrides, "-Pm3-ast-native", "-Dm3.cache.native=" + str(args.native), "test"], core)
    native_tests = tests()
    assert all(row["errors"] == row["failures"] == row["skipped"] == 0 for row in native_tests)
    assert any(row["class"].endswith("M3ASTWorkspaceCacheNativeTest") and row["tests"] == 2 for row in native_tests)

    # Service-loader discovery from the normal Maven artifact, using the qualified compiler patch.
    plugin_source = work / "BuildProbe.java"
    plugin_source.write_text('class BuildProbe { String value() { return "ordinary" + "compiler"; } }\n')
    plain_out, plugin_out, images = (work / name for name in ("plain", "plugin", "images"))
    for path in (plain_out, plugin_out):
        path.mkdir(exist_ok=True)
    if images.exists():
        shutil.rmtree(images)
    patch_flags = ["-J-Xmx512m", "-J--patch-module=jdk.compiler=" + str(args.patch), "-J--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"]
    run("module-plugin-reference", [args.jdk / "bin/javac", *patch_flags, "--release", "21", "-proc:none", "-d", plain_out, plugin_source], HERE)
    run("module-plugin-discovery", [args.jdk / "bin/javac", *patch_flags, "--release", "21", "-proc:none", "-cp", core_jar, "-d", plugin_out, "-Xplugin:M3ASTIndex out=" + str(images), plugin_source], HERE)
    assert (plain_out / "BuildProbe.class").read_bytes() == (plugin_out / "BuildProbe.class").read_bytes()
    receipts = list(images.glob("compilation-*/receipt.json"))
    assert len(receipts) == 1
    plugin_receipt = json.loads(receipts[0].read_text())
    assert plugin_receipt["parseEvents"] == 1 and plugin_receipt["compilerErrors"] == 0
    assert len(list(receipts[0].parent.glob("*.m3ast"))) == 1

    # Reuse the qualified adversarial fixture against both the canonical and receiver namespaces.
    sdk_report = ET.parse(HERE / "target/surefire-reports/TEST-com.synexia.rewrite.ModuleWiringRecipeTest.xml").getroot()
    dependency_classpath = next(x.get("value") for x in sdk_report.findall("./properties/property") if x.get("name") == "java.class.path")
    exports = [flag for package in ("api", "code", "comp", "tree", "util") for flag in ("--add-exports", "jdk.compiler/com.sun.tools.javac." + package + "=ALL-UNNAMED")]
    runtime_patch = ["--patch-module", "jdk.compiler=" + str(args.patch), "--add-exports", "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"]
    optional_results = {}
    for label in ("canonical", "receiver"):
        folder = work / ("optional-" + label)
        classes = folder / "classes"
        classes.mkdir(parents=True, exist_ok=True)
        sources = []
        for name in ("SharedASTProof.java", "NoOpenRewriteProbe.java"):
            text = (FRONTEND / "openrewrite/proof" / name).read_text()
            if label == "receiver":
                text = text.replace("import com.synexia.mindex.compiler.ast.", "import com.m3.compiletime.ast.")
            source = folder / name
            source.write_text(text)
            sources.append(source)
        classpath = os.pathsep.join([str(core_jar), str(optional_jar), dependency_classpath])
        if label == "canonical":
            sources.append(FRONTEND / "openrewrite/target/candidate/synexia-mindex/compiler-openrewrite/src/main/java/com/synexia/mindex/compiler/ast/M3OpenRewriteSharedAST.java")
        run("optional-" + label + "-compile", [args.jdk / "bin/javac", "-J-Xmx512m", "-source", "21", "-target", "21", "-proc:none", "-Xlint:all", "-Werror", *exports, "-cp", classpath, "-d", classes, *sources], HERE)
        load_log = folder / "class-load.log"
        output = run("optional-" + label + "-patched-runtime", [args.jdk / "bin/java", "-Xmx768m", "-Xverify:all", "-Xlog:class+load=info:file=" + str(load_log), *runtime_patch, *exports, "-cp", str(classes) + os.pathsep + classpath, "SharedASTProof", folder / "compilations"], HERE)
        loads = load_log.read_text()
        assert re.search(r"org\.openrewrite\.java\.isolated\.ReloadableJava21ParserVisitor\s+source:", loads)
        assert not re.search(r"org\.openrewrite\.java\.isolated\.ReloadableJava21Parser(?:\$[\w$]+)?\s+source:", loads)
        plain_output = folder / "without-openrewrite"
        plain_output.mkdir(exist_ok=True)
        run("optional-" + label + "-no-openrewrite", [args.jdk / "bin/java", "-Xmx512m", "-Xverify:all", *runtime_patch, "-cp", str(classes) + os.pathsep + str(core_jar), "NoOpenRewriteProbe", plain_output], HERE)
        optional_results[label] = {"status": "PASSED", "stdout": output.strip(), "independent_openrewrite_parser_loaded": False, "proof_source_sha256": {p.name: digest(p) for p in sources}}

    canonical_available = False
    canonical_reason = "no canonical checkout supplied; canonical metadata and preserved providers verified from actual SDK candidates"
    if args.canonical_checkout:
        canonical = args.canonical_checkout / "synexia-mindex"
        reactor_log = run("canonical-reactor-availability", [*mvn, "-pl", "compiler", "-am", "-DskipTests", "compile"], canonical, expected=None)
        canonical_available = commands[-1]["exit_code"] == 0
        if not canonical_available and ("Child module" not in reactor_log or "does not exist" not in reactor_log):
            raise RuntimeError("canonical reactor failed for an unexpected reason")
        canonical_reason = None if canonical_available else "exact pinned parent references modules not hydrated in the supplied checkout"

    result = {"schema": "M3_AST_NORMAL_MODULE_PROOF_V1", "status": "PASSED" if canonical_available else "PASSED_WITH_CANONICAL_REACTOR_UNAVAILABLE",
              "dependency_mode": {"asm.version": "9.6", "junit.artifact.version": "5.10.2", "authority": "existing receiver POM CI override comment", "custom_source_artifact_defaults": "not qualified in this environment"},
              "source_counts": {"ordinary_including_target_facades": len(ordinary), "vendor": len(vendor), "actual_maven_compiler_inputs": len(compiled)},
              "normal_tests": normal_tests, "native_missing_profile_tests": missing_tests, "native_positive_profile_tests": native_tests,
              "ordinary_compile_repair": ordinary_fix, "upstream_leaf_index_selftest": leaf_output.strip(),
              "core_jar_sha256": digest(core_jar), "optional_jar_sha256": digest(optional_jar), "compiler_patch_sha256": digest(args.patch), "native_library_sha256": digest(args.native),
              "core_has_openrewrite": False, "module_plugin_discovery": plugin_receipt, "plugin_bytecode_reference_equal": True,
              "optional_patched_runtime": optional_results,
              "canonical_reactor": {"status": "PASSED" if canonical_available else "UNAVAILABLE", "reason": canonical_reason,
                                    "canonical_metadata": "actual text recipe outputs", "service_source": str(canonical_service)},
              "limits": ["Linux x86_64 stock Java 21.0.2 and pinned compiler patch", "default custom source artifact repository not qualified", "full M3 JDK build, jtreg/JCK, and hosted CI not run in this lane", "optional LST provider retains its documented lifecycle and lazy source completion refusal limits"],
              "commands": commands}
    evidence.joinpath("RESULTS.json").write_text(json.dumps(result, indent=2) + "\n")
    evidence.joinpath("MAVEN_COMPILE_INPUTS.tsv").write_text("path\tsha256\n" + "".join(str(path.relative_to(args.target)) + "\t" + digest(path) + "\n" for path in sorted(compiled)))
    print(json.dumps({"status": result["status"], "source_counts": result["source_counts"], "normal_tests": sum(row["tests"] for row in normal_tests), "native_positive_tests": sum(row["tests"] for row in native_tests)}))


if __name__ == "__main__":
    main()
