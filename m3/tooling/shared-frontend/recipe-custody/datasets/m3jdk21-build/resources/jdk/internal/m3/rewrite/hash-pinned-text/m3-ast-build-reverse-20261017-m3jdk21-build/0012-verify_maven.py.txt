#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Execute a retained Maven distribution and real in-process/forked compilation.

Offline fixture proof, separate from acceptance of the customized Maven repository.
Use --require-m3-ast only with a compiler image that actually supplies the plugin.
"""
import argparse
import hashlib
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

NAMESPACE = "http://maven.apache.org/POM/4.0.0"
EXPECTED = "M3_MAVEN_FIXTURE_PASS:21"
SOURCE = '''public final class Main {
    private Main() {}
    public static void main(String[] args) {
        int result = java.util.stream.IntStream.rangeClosed(1, 6).sum();
        if (result != 21) throw new AssertionError(result);
        System.out.println("M3_MAVEN_FIXTURE_PASS:" + result);
    }
}
'''


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def run(arguments, log, image, timeout=180):
    environment = os.environ.copy()
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "MAVEN_OPTS", "MAVEN_ARGS"):
        environment.pop(name, None)
    environment["JAVA_HOME"] = str(image)
    result = subprocess.run([str(value) for value in arguments], text=True, env=environment,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    log.write_text(result.stdout)
    if result.returncode:
        raise RuntimeError("Tool fixture failed: " + str(log))
    return result.stdout


def write_project(project, compiler_version, capture, trace, image):
    """A real Maven compiler-plugin consumer retaining existing compiler arguments."""
    project.mkdir()
    source = project / "src/main/java/Main.java"
    source.parent.mkdir(parents=True)
    source.write_text(SOURCE)
    document = ET.fromstring('''<project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion><groupId>com.m3.pack.fixture</groupId>
      <artifactId>actual-maven-compile</artifactId><version>1</version>
      <properties><maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
      <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId><version/>
        <configuration><compilerArgs><arg>-Xlint:all</arg></compilerArgs></configuration>
      </plugin></plugins></build>
    </project>''')
    document.find("{%s}build/{%s}plugins/{%s}plugin/{%s}version" % ((NAMESPACE,) * 4)).text = compiler_version
    if capture:
        profiles = ET.SubElement(document, "{" + NAMESPACE + "}profiles")
        profile = ET.parse(image / "conf/m3-tools/maven-m3ast.xml").getroot()
        argument = profile.find(".//{" + NAMESPACE + "}arg")
        if argument is None or argument.text != "-Xplugin:M3AST":
            raise ValueError("Expected the installed M3AST compiler profile")
        argument.text += " trace=" + str(trace)
        profiles.append(profile)
    ET.register_namespace("", NAMESPACE)
    ET.ElementTree(document).write(project / "pom.xml", encoding="utf-8", xml_declaration=True)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--image", type=Path, required=True)
    parser.add_argument("--maven-repository", type=Path, required=True)
    parser.add_argument("--compiler-plugin-version", default="3.13.0")
    parser.add_argument("--require-m3-ast", action="store_true")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    image, repository = args.image.resolve(strict=True), args.maven_repository.resolve(strict=True)
    output = args.out.absolute()
    if output.resolve().is_relative_to(image):
        raise ValueError("Fixture output cannot modify the supplied image")
    output.mkdir(parents=True, exist_ok=False)
    settings = output / "settings.xml"
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><offline>true</offline></settings>\n')
    launcher = image / "bin/m3-mvn"
    version = run([launcher, "--version"], output / "maven-version.log", image)
    if "Apache Maven" not in version or str(image) not in version:
        raise ValueError("Maven did not report the selected image's Java runtime")
    cases, class_hashes = [], []
    for name, forked in (("inprocess", False), ("forked", True)):
        project, trace = output / name, output / (name + "-capture.tsv")
        write_project(project, args.compiler_plugin_version, args.require_m3_ast, trace, image)
        arguments = [launcher, "-o", "-B", "-ntp", "-s", settings, "-f", project / "pom.xml",
                     "-Dmaven.repo.local=" + str(repository),
                     "-Dmaven.compiler.fork=" + str(forked).lower(),
                     "org.apache.maven.plugins:maven-compiler-plugin:" + args.compiler_plugin_version + ":compile"]
        if args.require_m3_ast:
            arguments.insert(1, "-Pm3-ast")
        run(arguments, output / (name + "-compile.log"), image)
        classes = project / "target/classes"
        result = run([image / "bin/java", "-cp", classes, "Main"], output / (name + "-run.log"), image)
        if EXPECTED not in result:
            raise ValueError("Maven compiled fixture behavior mismatch")
        bytecode = digest(classes / "Main.class")
        class_hashes.append(bytecode)
        row = {"case": name, "forked": forked, "class_sha256": bytecode, "behavior": EXPECTED}
        if args.require_m3_ast:
            if not trace.is_file() or trace.stat().st_size == 0:
                raise ValueError("Actual compiler invocation did not publish its M3AST trace")
            row["trace_sha256"] = digest(trace)
        cases.append(row)
    if len(set(class_hashes)) != 1:
        raise ValueError("In-process and forked fixture bytecode differ")
    receipt = {"status": "REAL_MAVEN_DISTRIBUTION_COMPILE_PROVEN", "cases": cases,
               "maven_version_output": version, "m3_ast_requested": args.require_m3_ast,
               "custom_maven_fork_source_build_acceptance": False,
               "gradle_fork_source_build_acceptance": False}
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("M3_MAVEN_PACK_FIXTURE_PASS", output)


if __name__ == "__main__":
    main()
