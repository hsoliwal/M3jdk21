#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Qualify the optional parser module in explicit lint/compile/test/runtime order.

The four existing M3 javac owners are compiled as an external jdk.compiler
patch. The linked image retains its supplied compiler module descriptor; this
proof does not qualify plugin discovery or embed the patch in a product JDK.
Every build javac invocation, including consumer compilation, precedes tests.
A changed source/toolchain or a failed phase requires a fresh output directory.
The SQLite JDBC driver used by the facts contracts, with the slf4j-api its POM
declares, is resolved from the local Maven repository onto the consumer
classpath only; nothing is downloaded.
"""
import argparse
import ast
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


MODULE = "jdk.m3.parser"
PACK = Path("m3/tooling/module-packs/parser")
PHASES = ("lint", "compile", "test", "runtime")
COMPILER_ROOT = Path("src/jdk.compiler/share/classes")
RECEIVER_CHECKER = Path("m3/compatibility/check_synexia_full_borrow_receiver.py")
RECEIVER_INPUTS = ("pin.tsv", "synexia-estate.tsv", "synexia-dag.tsv",
                   "receiver-status.tsv", "historical-evidence.tsv", "promotion-phases.tsv")
COMPILER_SOURCES = (
    "com/sun/source/util/M3AST.java",
    "com/sun/tools/javac/m3/M3ASTPC.java",
    "com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java",
    "com/sun/tools/javac/main/JavaCompiler.java",
)
CONTRACTS = (
    ("jdk.m3.parser.test.M3ParserFrameworkContract", "M3_PARSER_FRAMEWORK_PASS"),
    ("jdk.m3.parser.test.M3JavacParserContract", "M3_JAVAC_PARSER_PASS"),
    ("jdk.m3.parser.test.M3CrossFrontendContract", "M3_AST_MULTIFRONTEND_PASS"),
    ("jdk.m3.parser.test.M3ASTDetailsContract", "M3_AST_DETAILS_PASS"),
    ("jdk.m3.parser.test.M3ASTInteropContract", "M3_AST_INTEROP_PASS"),
    ("jdk.m3.parser.test.M3ASTTreeSitterContract", "M3_AST_TREESITTER_PASS"),
    ("jdk.m3.parser.test.M3ASTRelationsContract", "M3_AST_RELATIONS_PASS"),
    ("jdk.m3.parser.test.M3ASTIndexProjectionContract", "M3_AST_INDEX_PROJECTION_PASS"),
    ("jdk.m3.parser.test.M3JavacTreeViewContract", "M3_JAVAC_TREE_VIEW_PASS"),
    ("jdk.m3.parser.test.M3JavacFactImageContract", "M3_JAVAC_FACT_IMAGE_PASS"),
    ("jdk.m3.parser.test.M3CulpritsContract", "M3_CULPRITS_PASS"),
    ("jdk.m3.parser.test.M3FactsWriterContract", "M3_FACTS_WRITER_PASS"),
    ("jdk.m3.parser.test.M3FactsTransactionContract", "M3_FACTS_TRANSACTION_PASS"),
    ("jdk.m3.parser.test.M3FactsStreamContract", "M3_FACTS_STREAM_PASS"),
    ("jdk.m3.parser.test.M3FileInputContract", "M3_FILE_INPUT_PASS"),
)
COMPILER_CONTRACTS = (("M3ASTProjectionStateTest", "M3_AST_PROJECTION_STATE_PASS"),)
SMOKE = ("jdk.m3.parser.test.M3ParserImageSmoke", "M3_PARSER_IMAGE_PASS")
# Consumer-classpath-only JDBC driver for the facts contracts; never a module dependency.
SQLITE_JDBC = Path("org/xerial/sqlite-jdbc")
SQLITE_JDBC_FAMILY = "3.45."
SLF4J_API = Path("org/slf4j/slf4j-api")
POM_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def value_sha256(value):
    return hashlib.sha256(canonical(value).encode("utf-8")).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def checked_file(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("regular source file required: " + str(path))
    return path


def sqlite_driver(maven_repository):
    """Newest local sqlite-jdbc 3.45.x jar; absent means refusal, never a download."""
    family = maven_repository / SQLITE_JDBC
    expected = family / (SQLITE_JDBC_FAMILY + "<x>") / ("sqlite-jdbc-" + SQLITE_JDBC_FAMILY + "<x>.jar")
    candidates = []
    for directory in family.glob(SQLITE_JDBC_FAMILY + "*"):
        jar = directory / ("sqlite-jdbc-" + directory.name + ".jar")
        if re.fullmatch(r"3\.45(?:\.\d+)+", directory.name) and jar.is_file() and not jar.is_symlink():
            candidates.append((tuple(int(part) for part in directory.name.split(".")), jar))
    if not candidates:
        raise ValueError("SQLite JDBC driver required at " + expected.as_posix())
    return max(candidates)[1]


def slf4j_api(maven_repository, driver):
    """The slf4j-api version the driver's own sibling POM declares; org.sqlite.JDBC needs it to initialize."""
    pom = driver.with_suffix(".pom")
    versions = [dependency.findtext("m:version", namespaces=POM_NAMESPACE)
                for dependency in ET.parse(pom).getroot().iterfind(".//m:dependency", POM_NAMESPACE)
                if dependency.findtext("m:artifactId", namespaces=POM_NAMESPACE) == "slf4j-api"]
    if len(versions) != 1 or not versions[0] or "$" in versions[0]:
        raise ValueError("one literal slf4j-api version required in " + pom.as_posix())
    jar = maven_repository / SLF4J_API / versions[0] / ("slf4j-api-" + versions[0] + ".jar")
    if jar.is_symlink() or not jar.is_file():
        raise ValueError("slf4j-api declared by sqlite-jdbc required at " + jar.as_posix())
    return jar


def input_rows(repository, jdk, maven_repository, jars):
    """Seal the source closure, every supplied JMOD and the consumer JDBC jars before creating output."""
    pack = repository / PACK
    source_files = sorted((pack / "src/main/java").rglob("*.java"))
    test_files = sorted((pack / "src/test/java").rglob("*.java"))
    if not source_files or not test_files:
        raise ValueError("complete parser source and consumer trees required")
    required = [pack / name for name in
                ("build.py", "pom.xml", "DESIGN.md", "FOSS_REUSE_DECISIONS.tsv")]
    required += [repository / COMPILER_ROOT / name for name in COMPILER_SOURCES]
    required.append(repository / RECEIVER_CHECKER)
    required += [repository / "m3/synexia-import/current-full-borrow" / name
                 for name in RECEIVER_INPUTS]
    required += [repository / "m3/tooling/jmod-recipes/tools" / name
                 for name in ("jmod_pack.py", "jmod_image.py")]
    required += [pack / "legal/LICENSE.txt", pack / "legal/NOTICE.txt"]
    required += sorted(path for path in (pack / "legal").rglob("*") if path.is_file())
    required += [repository / "m3/docs/name-mapping.d/m3-parser-jmod-20261018.json",
                 repository / ".github/workflows/m3-parser-jmod-20261018.yml"]
    required += [pack / "src/main/java/module-info.java"]
    for name, unused in (*CONTRACTS, SMOKE):
        required.append(pack / "src/test/java" / (name.replace(".", "/") + ".java"))
    required += [repository / "test/langtools/tools/javac/m3" / (name + ".java")
                 for name, unused in COMPILER_CONTRACTS]
    rows = []
    for path in sorted(set(source_files + test_files + required)):
        checked_file(path)
        if not path.resolve().is_relative_to(repository):
            raise ValueError("source escapes repository: " + str(path))
        rows.append({"owner": "repository", "path": path.relative_to(repository).as_posix(),
                     "bytes": path.stat().st_size, "sha256": sha256(path)})
    tools = [jdk / "release", jdk / "lib/modules"]
    tools += [jdk / "bin" / executable(name) for name in
              ("java", "javac", "jar", "jmod", "jlink")]
    jmods = sorted((jdk / "jmods").glob("*.jmod"))
    if not jmods:
        raise ValueError("an intact development JDK with JMODs is required")
    for path in sorted(tools + jmods):
        if not path.is_file():
            raise ValueError("missing JDK input: " + str(path))
        rows.append({"owner": "jdk", "path": path.relative_to(jdk).as_posix(),
                     "bytes": path.stat().st_size, "sha256": sha256(path)})
    for path in jars:
        rows.append({"owner": "maven-repository", "path": path.relative_to(maven_repository).as_posix(),
                     "bytes": path.stat().st_size, "sha256": sha256(path)})
    return rows


def executable(name):
    return name + (".exe" if os.name == "nt" else "")


class Qualification:
    """One source-bound, fail-closed sequence; no implicit replay or repair."""

    def __init__(self, args):
        self.repository = args.repo_root.resolve(strict=True)
        self.jdk = args.jdk_home.resolve(strict=True)
        self.output = args.out.absolute()
        self.phase = args.phase
        self.pack = self.repository / PACK
        self.commands = []
        self.logs = []
        for parent in (self.output, *self.output.parents):
            if parent.is_symlink():
                raise ValueError("output symlink refused: " + str(parent))
        self.output = self.output.resolve()
        if (self.output.is_relative_to(self.jdk) or self.jdk.is_relative_to(self.output)
                or self.repository.is_relative_to(self.output)
                or self.output.is_relative_to(self.pack / "src")
                or self.output.is_relative_to(self.pack / "legal")):
            raise ValueError("output must be separate from JDK and source inputs")
        release = (self.jdk / "release").read_text(encoding="utf-8")
        if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release, re.MULTILINE):
            raise ValueError("Java 21 development toolchain required")
        self.maven_repository = args.maven_repository.absolute()
        self.driver = sqlite_driver(self.maven_repository)
        self.slf4j = slf4j_api(self.maven_repository, self.driver)
        self.jars = (self.driver, self.driver.with_suffix(".pom"), self.slf4j)
        self.inputs = input_rows(self.repository, self.jdk, self.maven_repository, self.jars)
        self.input_sha = value_sha256(self.inputs)
        if self.phase == "lint":
            if self.output.exists():
                raise FileExistsError("lint requires a fresh output directory")
            self.output.mkdir(parents=True)
            self.state = {"schema": "M3_PARSER_PHASE_STATE_V1", "completed": [],
                          "input_sha256": self.input_sha, "receipts": {},
                          "active": None, "failed": None}
            write_json(self.output / "INPUTS.json", {"schema": "M3_PARSER_INPUTS_V1",
                                                    "inputs": self.inputs,
                                                    "input_sha256": self.input_sha,
                                                    "maven_repository": self.maven_repository.as_posix(),
                                                    "sqlite_jdbc": {"path": self.driver.as_posix(),
                                                                    "sha256": sha256(self.driver)},
                                                    "slf4j_api": {"path": self.slf4j.as_posix(),
                                                                  "sha256": sha256(self.slf4j)}})
            self.save_state()
        else:
            self.state = json.loads((self.output / "STATE.json").read_text(encoding="utf-8"))
            self.verify_chain()
            if self.state.get("failed") or self.state.get("active"):
                raise ValueError("interrupted/failed sequence requires a fresh output directory")
            completed = self.state["completed"]
            if completed != list(PHASES[:len(completed)]) or len(completed) >= len(PHASES):
                raise ValueError("invalid or completed verification sequence")
            if PHASES[len(completed)] != self.phase:
                raise ValueError("verification order refused; expected " + PHASES[len(completed)])
            self.require_unchanged()

    def save_state(self):
        write_json(self.output / "STATE.json", self.state)

    def event(self, status, detail):
        with (self.output / "EVENTS.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(canonical({"phase": self.phase, "status": status,
                                    "detail": detail, "input_sha256": self.input_sha}) + "\n")

    def verify_chain(self):
        previous = None
        for phase in self.state["completed"]:
            path = self.output / (phase + "-receipt.json")
            if sha256(path) != self.state["receipts"].get(phase):
                raise ValueError("phase receipt drift: " + phase)
            receipt = json.loads(path.read_text(encoding="utf-8"))
            if (receipt.get("phase") != phase or receipt.get("status") != "PASS"
                    or receipt.get("input_sha256") != self.state["input_sha256"]
                    or receipt.get("previous_receipt_sha256") != previous):
                raise ValueError("invalid phase receipt chain: " + phase)
            for row in receipt["artifacts"]:
                artifact = self.output / row["path"]
                if not artifact.resolve().is_relative_to(self.output):
                    raise ValueError("artifact path escapes proof output")
                if sha256(artifact) != row["sha256"]:
                    raise ValueError("verified artifact drift: " + row["path"])
            previous = self.state["receipts"][phase]

    def require_unchanged(self):
        recorded = json.loads((self.output / "INPUTS.json").read_text(encoding="utf-8"))
        if (recorded.get("inputs") != self.inputs
                or recorded.get("input_sha256") != self.input_sha
                or self.state["input_sha256"] != self.input_sha):
            raise ValueError("source or toolchain changed; start a fresh lint sequence")

    def run(self, name, arguments, marker=None):
        argv = [str(value) for value in arguments]
        if self.phase in ("test", "runtime") and Path(argv[0]).name in ("javac", "javac.exe"):
            raise ValueError("build compiler invocation after compile phase refused")
        log = self.output / (self.phase + "-" + name + ".log")
        command = {"name": name, "argv": argv, "exit": None}
        self.commands.append(command)
        command_path = self.output / (self.phase + "-commands.json")
        write_json(command_path, self.commands)
        try:
            result = subprocess.run(argv, cwd=self.output, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                                    errors="replace", timeout=300)
        except subprocess.TimeoutExpired as failure:
            output = failure.stdout or b""
            if isinstance(output, bytes):
                output = output.decode("utf-8", errors="replace")
            log.write_text("$ " + canonical(argv) + "\n" + output + "\nTIMEOUT\n", encoding="utf-8")
            command["exit"] = "TIMEOUT"
            write_json(command_path, self.commands)
            raise RuntimeError("command timed out: " + str(log)) from failure
        command["exit"] = result.returncode
        write_json(command_path, self.commands)
        log.write_text("$ " + canonical(argv) + "\n" + result.stdout
                       + "\nEXIT " + str(result.returncode) + "\n", encoding="utf-8")
        self.logs.append(log)
        if result.returncode:
            raise RuntimeError("command failed: " + str(log))
        if marker is not None and marker not in result.stdout:
            raise RuntimeError("required completion marker missing: " + str(log))
        return result.stdout

    def tool(self, name):
        return self.jdk / "bin" / executable(name)

    def lint(self):
        ast.parse((self.pack / "build.py").read_text(encoding="utf-8"))
        ET.parse(self.pack / "pom.xml")
        json.loads((self.repository / "m3/docs/name-mapping.d/m3-parser-jmod-20261018.json")
                   .read_text(encoding="utf-8"))
        source_count = 0
        for path in sorted((self.pack / "src").rglob("*.java")):
            text = path.read_text(encoding="utf-8")
            if not text.endswith("\n") or "\0" in text or "\r" in text:
                raise ValueError("source encoding/newline contract failed: " + str(path))
            if "SPDX-License-Identifier: Apache-2.0" not in text:
                raise ValueError("source license header missing: " + str(path))
            if re.search(r"(?m)^\s*(?:package|import(?:\s+static)?)\s+com\.synexia\.", text):
                raise ValueError("Synexia runtime package dependency refused: " + str(path))
            source_count += 1
        descriptor = (self.pack / "src/main/java/module-info.java").read_text(encoding="utf-8")
        if not re.search(r"\bmodule\s+jdk\.m3\.parser\s*\{", descriptor):
            raise ValueError("unexpected parser module identity")
        self.run("full-borrow-receiver", [sys.executable, self.repository / RECEIVER_CHECKER],
                 "SYNEXIA_FULL_BORROW_RECEIVER_PASS")
        log = self.output / "lint-structure.log"
        log.write_text("PASS Python AST, XML, naming JSON, Java UTF-8/LF/license/package structure\n"
                       + "java_sources=" + str(source_count) + "\n"
                       + "sqlite_jdbc=" + self.driver.as_posix() + " sha256=" + sha256(self.driver) + "\n"
                       + "slf4j_api=" + self.slf4j.as_posix() + " sha256=" + sha256(self.slf4j) + "\n"
                       + "Java compiler warnings are checked with -Xlint:all -Werror in compile phase.\n",
                       encoding="utf-8")
        return [self.output / "INPUTS.json", log]

    def compile(self):
        patch_source = self.output / "compiler-source"
        patch_classes = self.output / "compiler-patch"
        empty_source = self.output / "empty-sourcepath"
        modules = self.output / "modules"
        tests = self.output / "test-classes"
        for directory in (patch_source, patch_classes, empty_source, modules, tests):
            directory.mkdir()
        sources = []
        for relative in COMPILER_SOURCES:
            source = self.repository / COMPILER_ROOT / relative
            target = patch_source / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            if sha256(source) != sha256(target):
                raise ValueError("compiler source staging mismatch")
            sources.append(target)
        common = [self.tool("javac"), "-source", "21", "-target", "21",
                  "-proc:none", "-implicit:none", "-Xlint:all", "-Werror"]
        self.run("compiler-patch", [*common, "--patch-module", "jdk.compiler=" + str(patch_source),
                                   "-sourcepath", empty_source, "-d", patch_classes, *sources])
        if (patch_classes / "module-info.class").exists():
            raise ValueError("compiler descriptor must not be patched in this proof")
        parser_sources = sorted((self.pack / "src/main/java").rglob("*.java"))
        self.run("parser-module", [*common, "-Xlint:-module", "--patch-module",
                                  "jdk.compiler=" + str(patch_classes), "-d", modules / MODULE,
                                  *parser_sources])
        test_sources = sorted((self.pack / "src/test/java").rglob("*.java"))
        test_sources += [self.repository / "test/langtools/tools/javac/m3" / (name + ".java")
                         for name, unused in COMPILER_CONTRACTS]
        self.run("all-consumers", [*common, "--patch-module", "jdk.compiler=" + str(patch_classes),
                                  "--module-path", modules, "--add-modules", MODULE,
                                  "-d", tests, *test_sources])
        classes = sorted(patch_classes.rglob("*.class")) + sorted(modules.rglob("*.class"))
        classes += sorted(tests.rglob("*.class"))
        if not classes or not (modules / MODULE / "module-info.class").is_file():
            raise ValueError("compiled source/module closure missing")
        return classes + sorted(patch_source.rglob("*.java"))

    def consumer(self, name, java, contract, linked):
        main, marker = contract
        work = self.output / "consumer-work" / (self.phase + "-" + name)
        work.parent.mkdir(exist_ok=True)
        work.mkdir()
        arguments = [java, "-ea", "-Dm3.parser.test.work=" + str(work), "-Dorg.sqlite.tmpdir=" + str(work),
                     "--patch-module", "jdk.compiler=" + str(self.output / "compiler-patch")]
        if not linked:
            arguments += ["--module-path", self.output / "modules"]
        arguments += ["--add-modules", MODULE, "-cp",
                      os.pathsep.join((str(self.output / "test-classes"), str(self.driver), str(self.slf4j))), main]
        return self.run(name, arguments, marker)

    def test(self):
        for main, marker in (*CONTRACTS, *COMPILER_CONTRACTS):
            self.consumer(main.rsplit(".", 1)[-1], self.tool("java"), (main, marker), False)
        self.consumer("consumer-smoke", self.tool("java"), SMOKE, False)
        return []

    def runtime(self):
        jar = self.output / (MODULE + ".jar")
        self.run("jar", [self.tool("jar"), "--create", "--file", jar,
                         "--date", "2026-01-01T00:00:00Z", "-C", self.output / "modules" / MODULE, "."])
        self.run("jar-descriptor", [self.tool("jar"), "--describe-module", "--file", jar], MODULE)
        image_tsv = self.output / "INPUT_JMOD_IMAGE.tsv"
        image_tsv.write_text("artifact\tversion\tjar\tmodule\trequires\tdisposition\n"
                             + "\t".join((MODULE, "1", str(jar), MODULE, "jdk.compiler", "JMOD")) + "\n",
                             encoding="utf-8")
        manifest = self.output / "EMPTY_DESCRIPTOR_MANIFEST.tsv"
        manifest.write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n", encoding="utf-8")
        # A published descriptor and no --root-module prevent packager-side javac.
        packager = self.repository / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
        self.run("jmod-pack", [sys.executable, packager, "--jdk-home", self.jdk,
                               "--image-tsv", image_tsv, "--manifest", manifest,
                               "--layer-root", self.repository, "--out", self.output / "pack",
                               "--legal-notices", self.pack / "legal", "--add-modules",
                               "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps"])
        jmod = self.output / "pack/jmods" / (MODULE + ".jmod")
        self.run("jmod-descriptor", [self.tool("jmod"), "describe", jmod], MODULE)
        entries = self.run("jmod-contents", [self.tool("jmod"), "list", jmod])
        required = ("classes/module-info.class", "classes/jdk/m3/parser/M3File.class",
                    "classes/jdk/m3/parser/M3Parser.class", "classes/jdk/m3/parser/M3JavacParser.class",
                    "classes/jdk/m3/parser/grammar/M3Grammar.class", "legal/LICENSE.txt", "legal/NOTICE.txt")
        if not set(required).issubset(set(entries.splitlines())):
            raise ValueError("JMOD classes or legal notices missing")
        image = self.output / "pack/image"
        java = image / "bin" / executable("java")
        for tool in ("java", "javac", "jar", "jmod", "jlink", "jdeps"):
            if not (image / "bin" / executable(tool)).is_file():
                raise ValueError("missing linked developer tool: " + tool)
        self.run("image-modules", [java, "--list-modules"], MODULE)
        for main, marker in (*CONTRACTS, *COMPILER_CONTRACTS):
            self.consumer("image-" + main.rsplit(".", 1)[-1], java, (main, marker), True)
        first = self.consumer("image-smoke-1", java, SMOKE, True)
        second = self.consumer("image-smoke-2", java, SMOKE, True)
        if first != second:
            raise ValueError("independent image consumer replay differs")
        proof = self.output / "RUNTIME_PROOF.json"
        write_json(proof, {"schema": "M3_PARSER_RUNTIME_PROOF_V1", "status": "PASS",
                           "module": MODULE, "jmod_sha256": sha256(jmod), "jar_sha256": sha256(jar),
                           "linked_modules_sha256": sha256(image / "lib/modules"),
                           "compiler_patch": "EXPLICIT_EXTERNAL_SOURCE_COMPILED_PATCH",
                           "compiler_source_files": list(COMPILER_SOURCES),
                           "replay_stdout_sha256": hashlib.sha256(first.encode("utf-8")).hexdigest(),
                           "build_javac_after_test": False,
                           "pending": ["full M3JDK/HotSpot source build and jtreg",
                                       "M3AST plugin/service discovery in rebuilt compiler descriptor",
                                       "other platform and performance qualification"],
                           "scope": "Optional JMOD and linked image; existing compiler sources supplied by explicit external --patch-module."})
        return [jar, jmod, image / "lib/modules", image_tsv, manifest, proof]

    def execute(self):
        self.state["active"] = self.phase
        self.save_state()
        self.event("START", "source and preceding artifacts sealed")
        try:
            artifacts = getattr(self, self.phase)()
            if input_rows(self.repository, self.jdk, self.maven_repository, self.jars) != self.inputs:
                raise ValueError("inputs changed during phase")
            artifacts += self.logs
            commands = self.output / (self.phase + "-commands.json")
            if commands.exists():
                artifacts.append(commands)
            previous = self.state["completed"][-1] if self.state["completed"] else None
            receipt = {"schema": "M3_PARSER_PHASE_RECEIPT_V1", "phase": self.phase,
                       "status": "PASS", "input_sha256": self.input_sha,
                       "previous_receipt_sha256": self.state["receipts"].get(previous),
                       "artifacts": [{"path": path.relative_to(self.output).as_posix(),
                                      "sha256": sha256(path), "bytes": path.stat().st_size}
                                     for path in sorted(set(artifacts))]}
            receipt_path = self.output / (self.phase + "-receipt.json")
            write_json(receipt_path, receipt)
            self.state["completed"].append(self.phase)
            self.state["receipts"][self.phase] = sha256(receipt_path)
            self.state["active"] = None
            self.save_state()
            self.event("PASS", "phase receipt committed")
            print("M3_PARSER_PHASE_PASS phase=" + self.phase + " input_sha256=" + self.input_sha)
        except BaseException as failure:
            self.state["failed"] = {"phase": self.phase, "reason": str(failure)}
            self.state["active"] = None
            self.save_state()
            self.event("FAIL", str(failure))
            raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[4])
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--phase", choices=PHASES, required=True)
    parser.add_argument("--maven-repository", type=Path, default=Path.home() / ".m2/repository",
                        help="local Maven repository holding org/xerial/sqlite-jdbc/3.45.x and its slf4j-api (no download)")
    args = parser.parse_args()
    try:
        Qualification(args).execute()
    except (OSError, ValueError, RuntimeError, KeyError, ET.ParseError) as failure:
        print("M3_PARSER_PHASE_REFUSED " + str(failure), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
