#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Lock the complete Maven-resolved OpenRewrite framework runtime as an optional tool pack.

Build the source-owned frontend and tool POMs first. The tool's package phase
emits runtime-classpath.txt and runtime-dependencies.txt with the actual Maven
dependency plugin. This assembler retains every resolved JAR, its POM and parent
POM metadata, and the source-built runner. It never relocates packages, extracts
classes, substitutes a parser, or claims that packaging proves AST consumption.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import sys
import textwrap
import xml.etree.ElementTree as ET
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from tool_packs import digest, load_tool_packs

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
MAIN = "org.m3jdk.tooling.rewrite.M3RewriteTool"
REWRITE_VERSION = "8.17.1"
ROOTS = ["java.se", "jdk.compiler", "jdk.crypto.ec", "jdk.management", "jdk.unsupported", "jdk.zipfs"]
FRAMEWORK_ARTIFACTS = {
    "rewrite-core", "rewrite-gradle", "rewrite-groovy", "rewrite-hcl", "rewrite-java",
    "rewrite-java-8", "rewrite-java-11", "rewrite-java-17", "rewrite-java-21",
    "rewrite-json", "rewrite-maven", "rewrite-properties", "rewrite-protobuf",
    "rewrite-xml", "rewrite-yaml",
}
FRAMEWORK_COMMIT = "3bd8271be963c025ce0856072d0f09360e3184cd"
FRAMEWORK_SETTINGS_BLOB = "5ae46344dd79723f0965b756dcac141e30f124ee"
COORDINATE = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]*\Z")
EXPORT = re.compile(r"--add-exports=jdk\.compiler/com\.sun\.tools\.javac(?:\.[a-z][a-z0-9]*)+=ALL-UNNAMED\Z")
REQUIRED_CLASSES = {
    "org/openrewrite/Recipe.class",
    "org/openrewrite/config/Environment.class",
    "org/openrewrite/java/JavaParser.class",
    "org/openrewrite/java/isolated/ReloadableJava21ParserVisitor.class",
    "org/openrewrite/yaml/YamlParser.class",
    "org/m3jdk/tooling/rewrite/M3RewriteFrontend.class",
}


def checked_file(path):
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("Expected an explicit regular file: " + str(path))
    return path.resolve(strict=True)


def coordinate(value, name):
    if not isinstance(value, str) or not COORDINATE.fullmatch(value):
        raise ValueError("Invalid Maven " + name)
    if name == "groupId" and any(not COORDINATE.fullmatch(part) for part in value.split(".")):
        raise ValueError("Invalid Maven groupId component")
    return value


def pom_identity(path, snapshots=None):
    path = checked_file(path)
    data = path.read_bytes()
    checksum = hashlib.sha256(data).hexdigest()
    if snapshots is not None and snapshots.setdefault(path, checksum) != checksum:
        raise ValueError("Input changed after runtime admission: " + str(path))
    root = ET.fromstring(data)
    parent = root.find("m:parent", NS)
    def value(name):
        result = root.findtext("m:" + name, namespaces=NS)
        if result is None and parent is not None and name in ("groupId", "version"):
            result = parent.findtext("m:" + name, namespaces=NS)
        return coordinate(result, name)
    return root, (value("groupId"), value("artifactId"), value("version"))


def pom_location(repository, identity):
    group, artifact, version = identity
    return repository.joinpath(*group.split("."), artifact, version,
                               artifact + "-" + version + ".pom")


def repository_identity(jar, repository):
    try:
        relative = jar.relative_to(repository)
    except ValueError as failure:
        raise ValueError("Resolved runtime JAR is outside the specified Maven repository") from failure
    parts = relative.parts
    if len(parts) < 4 or jar.suffix != ".jar":
        raise ValueError("Expected Maven repository JAR layout: " + str(jar))
    group, artifact, version = ".".join(parts[:-3]), parts[-3], parts[-2]
    identity = tuple(coordinate(v, n) for v, n in
                     zip((group, artifact, version), ("groupId", "artifactId", "version")))
    prefix = artifact + "-" + version
    if jar.name != prefix + ".jar" and not (jar.name.startswith(prefix + "-") and jar.name.endswith(".jar")):
        raise ValueError("JAR filename does not match its Maven coordinate")
    return relative, identity


def copy_pinned(source, target, expected=None):
    source = checked_file(source)
    checksum = digest(source)
    if expected is not None and checksum != expected:
        raise ValueError("Input changed after runtime admission: " + str(source))
    target.parent.mkdir(parents=True, exist_ok=True)
    with source.open("rb") as data, target.open("xb") as output:
        shutil.copyfileobj(data, output)
    if digest(source) != checksum or digest(target) != checksum:
        raise ValueError("Input changed during runtime assembly: " + str(source))
    target.chmod(0o644)
    return checksum


def retain_pom(source, repository, destination, retained, snapshots):
    source = checked_file(source)
    root, identity = pom_identity(source, snapshots)
    if identity in retained:
        if retained[identity] != digest(source):
            raise ValueError("Conflicting Maven POM identities")
        return
    relative = Path(*identity[0].split("."), identity[1], identity[2],
                    identity[1] + "-" + identity[2] + ".pom")
    retained[identity] = copy_pinned(source, destination / "poms" / relative, snapshots[source])
    parent = root.find("m:parent", NS)
    if parent is not None:
        parent_id = tuple(coordinate(parent.findtext("m:" + name, namespaces=NS), name)
                          for name in ("groupId", "artifactId", "version"))
        retain_pom(pom_location(repository, parent_id), repository, destination, retained, snapshots)


def admit_pom_tree(source, repository, snapshots, seen):
    source = checked_file(source)
    if source in seen:
        return
    seen.add(source)
    root, _ = pom_identity(source, snapshots)
    parent = root.find("m:parent", NS)
    if parent is not None:
        parent_id = tuple(coordinate(parent.findtext("m:" + name, namespaces=NS), name)
                          for name in ("groupId", "artifactId", "version"))
        admit_pom_tree(pom_location(repository, parent_id), repository, snapshots, seen)


def launcher_text(classpath, exports, windows=False):
    if windows:
        return ("@echo off\r\nsetlocal\r\nset \"M3_IMAGE_HOME=%~dp0..\"\r\n"
                + '\"%M3_IMAGE_HOME%\\bin\\java.exe\" ' + " ".join(exports)
                + ' -jar \"%M3_IMAGE_HOME%\\lib\\m3-tools\\openrewrite\\launch.jar\" %*\r\n'
                + "exit /b %ERRORLEVEL%\r\n")
    return ('#!/bin/sh\nset -eu\n'
            + 'M3_IMAGE_HOME=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)\n'
            + 'exec "$M3_IMAGE_HOME/bin/java" ' + " ".join(exports)
            + ' -jar "$M3_IMAGE_HOME/lib/m3-tools/openrewrite/launch.jar" "$@"\n')


def launcher_jar(path, classpath):
    """A manifest-only launcher keeps original JARs and avoids long Windows command lines."""
    lines = []
    for line in ("Manifest-Version: 1.0", "Main-Class: " + MAIN,
                 "Class-Path: " + " ".join(classpath)):
        lines.extend(textwrap.wrap(line, width=70, subsequent_indent=" ",
                                   break_long_words=True, break_on_hyphens=False,
                                   replace_whitespace=False, drop_whitespace=False))
    entry = zipfile.ZipInfo("META-INF/MANIFEST.MF", (2026, 1, 1, 0, 0, 0))
    entry.external_attr = 0o644 << 16
    with zipfile.ZipFile(path, "x", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(entry, ("\r\n".join(lines) + "\r\n\r\n").encode("ascii"))
    path.chmod(0o644)


def listed_dependencies(text):
    """Read actual dependency:list runtime coordinates, including optional classifiers."""
    result = {}
    pattern = re.compile(r"([^:\s]+):([^:\s]+):jar:(?:([^:\s]+):)?([^:\s]+):"
                         r"(compile|runtime):(.+?\.jar)(?:\s+-- module .*)?\Z")
    for line in text.splitlines():
        line = line.strip()
        if not line or line == "The following files have been resolved:":
            continue
        match = pattern.fullmatch(line)
        if match is None:
            raise ValueError("Unrecognized Maven runtime dependency:list entry: " + line)
        group, artifact, classifier, version, scope, filename = match.groups()
        if classifier is not None:
            coordinate(classifier, "classifier")
        jar = checked_file(filename)
        if jar in result:
            raise ValueError("Duplicate Maven runtime dependency:list entry")
        result[jar] = ((group, artifact, version), classifier, scope)
    return result


def assemble(project_pom, frontend_pom, runner_jar, runtime_classpath,
             runtime_dependencies, repository, destination, lock_path, source,
             source_licenses, runtime_pom, source_proofs=()):
    inputs = [checked_file(p) for p in
              (project_pom, frontend_pom, runner_jar, runtime_classpath, runtime_dependencies)]
    project_pom, frontend_pom, runner_jar, runtime_classpath, runtime_dependencies = inputs
    runtime_pom = checked_file(runtime_pom)
    inputs.append(runtime_pom)
    source_licenses = [checked_file(path) for path in source_licenses]
    source_proofs = [checked_file(path) for path in source_proofs]
    legal_names = [path.name for path in source_licenses]
    if "LICENSE" not in legal_names or len(set(legal_names)) != len(legal_names):
        raise ValueError("Retained source LICENSE and unique legal filenames are required")
    if any(not COORDINATE.fullmatch(name) for name in legal_names):
        raise ValueError("Invalid source legal filename")
    snapshots = {path: digest(path) for path in inputs + source_licenses + source_proofs}
    repository = Path(repository).resolve(strict=True)
    destination, lock_path = Path(destination).resolve(), Path(lock_path).resolve()
    if destination.exists() or lock_path.exists():
        raise FileExistsError("Runtime destination and lock must be new")
    if (lock_path.is_relative_to(destination) or destination.is_relative_to(repository)
            or any(p.is_relative_to(destination) for p in inputs + source_licenses + source_proofs)):
        raise ValueError("Runtime output must be separate from the inputs and lock")
    if not source.strip():
        raise ValueError("Reviewed source provenance is required")
    project, identity = pom_identity(project_pom, snapshots)
    if identity[:2] != ("org.m3jdk.tooling", "m3-openrewrite-tool"):
        raise ValueError("Expected the source-owned M3 OpenRewrite tool project")
    runtime, runtime_id = pom_identity(runtime_pom, snapshots)
    if runtime_id[:2] != ("org.m3jdk.tooling", "m3-openrewrite-runtime"):
        raise ValueError("Expected the source-owned complete OpenRewrite runtime POM")
    if runtime.findtext("m:properties/m:rewrite.version", namespaces=NS) != REWRITE_VERSION:
        raise ValueError("Runtime POM declares another OpenRewrite framework version")
    if (runtime.findtext("m:properties/m:m3.framework.source.commit", namespaces=NS) != FRAMEWORK_COMMIT
            or runtime.findtext("m:properties/m:m3.framework.settings.gitblob", namespaces=NS) != FRAMEWORK_SETTINGS_BLOB):
        raise ValueError("Runtime framework source membership pin differs")
    direct = {dep.findtext("m:artifactId", namespaces=NS)
              for dep in runtime.findall("m:dependencies/m:dependency", NS)
              if dep.findtext("m:groupId", namespaces=NS) == "org.openrewrite"}
    if direct != FRAMEWORK_ARTIFACTS:
        raise ValueError("Runtime POM omits or changes the published production framework module set")
    frontend, _ = pom_identity(frontend_pom, snapshots)
    exports = shlex.split(project.findtext("m:properties/m:m3.compiler.exports", namespaces=NS) or "")
    frontend_exports = shlex.split(frontend.findtext("m:properties/m:m3.compiler.exports", namespaces=NS) or "")
    if not exports or len(set(exports)) != len(exports) or any(not EXPORT.fullmatch(x) for x in exports):
        raise ValueError("Expected exact compiler exports from the tool POM")
    if exports != frontend_exports:
        raise ValueError("Tool and frontend compiler exports disagree")
    raw_classpath = runtime_classpath.read_text(encoding="utf-8").strip()
    entries = raw_classpath.split(os.pathsep) if raw_classpath else []
    dependencies_text = runtime_dependencies.read_text(encoding="utf-8")
    listed = listed_dependencies(dependencies_text)
    if not entries or any(not entry.strip() for entry in entries):
        raise ValueError("The Maven-resolved runtime classpath is empty")
    jars, seen, classes = [], set(), set()
    for entry in entries:
        jar = checked_file(entry)
        snapshots[jar] = digest(jar)
        relative, dep_id = repository_identity(jar, repository)
        if jar in seen:
            raise ValueError("Duplicate runtime classpath JAR")
        seen.add(jar)
        if jar not in listed:
            raise ValueError("Classpath JAR is absent from Maven dependency:list output")
        listed_id, classifier, _ = listed[jar]
        filename = dep_id[1] + "-" + dep_id[2] + ("-" + classifier if classifier else "") + ".jar"
        if listed_id != dep_id or jar.name != filename:
            raise ValueError("Runtime Maven coordinate disagrees with its JAR path")
        if pom_identity(pom_location(repository, dep_id), snapshots)[1] != dep_id:
            raise ValueError("Runtime POM disagrees with its JAR coordinate")
        if dep_id[0] == "org.openrewrite" and dep_id[2] != REWRITE_VERSION:
            raise ValueError("Mixed OpenRewrite runtime versions")
        with zipfile.ZipFile(jar) as archive:
            classes.update(REQUIRED_CLASSES.intersection(archive.namelist()))
        if digest(jar) != snapshots[jar]:
            raise ValueError("Input changed after runtime admission: " + str(jar))
        jars.append((jar, relative, dep_id))
    if set(listed) != seen:
        raise ValueError("Maven classpath and runtime dependency list disagree")
    frameworks = {identity[1] for _, _, identity in jars if identity[0] == "org.openrewrite"}
    if frameworks != FRAMEWORK_ARTIFACTS:
        raise ValueError("Resolved runtime omits or changes the published production framework module set")
    if classes != REQUIRED_CLASSES:
        raise ValueError("The resolved runtime lacks the complete required Rewrite frontend: "
                         + ",".join(sorted(REQUIRED_CLASSES - classes)))
    with zipfile.ZipFile(runner_jar) as archive:
        if MAIN.replace(".", "/") + ".class" not in archive.namelist():
            raise ValueError("Source-built executable runner class is absent")
    admitted_poms = set()
    framework_bom = pom_location(repository, ("org.openrewrite", "rewrite-bom", REWRITE_VERSION))
    for pom in [project_pom, frontend_pom, runtime_pom, framework_bom,
                *[pom_location(repository, dep_id) for _, _, dep_id in jars]]:
        admit_pom_tree(pom, repository, snapshots, admitted_poms)
    for path, checksum in snapshots.items():
        if digest(path) != checksum:
            raise ValueError("Input changed after runtime admission: " + str(path))
    destination.mkdir(parents=True)
    retained_poms, components, classpath = {}, [], []
    for jar, relative, dep_id in jars:
        target = Path("lib") / relative
        checksum = copy_pinned(jar, destination / target, snapshots[jar])
        classpath.append(target.as_posix())
        retain_pom(pom_location(repository, dep_id), repository, destination, retained_poms, snapshots)
        with zipfile.ZipFile(destination / target) as archive:
            notices = [name for name in archive.namelist()
                       if not name.endswith("/") and any(part in Path(name).name.upper()
                                                         for part in ("LICENSE", "NOTICE", "COPYING"))]
            legal_entries = [{"path": name, "bytes": archive.getinfo(name).file_size,
                              "sha256": hashlib.sha256(archive.read(name)).hexdigest()}
                             for name in sorted(notices)]
        components.append({"coordinate": ":".join(dep_id), "jar": target.as_posix(),
                           "sha256": checksum, "embedded_legal_entries": legal_entries})
    runner_relative = Path("lib", *identity[0].split("."), identity[1], identity[2],
                           identity[1] + "-" + identity[2] + ".jar")
    runner_hash = copy_pinned(runner_jar, destination / runner_relative, snapshots[runner_jar])
    classpath.insert(0, runner_relative.as_posix())
    retain_pom(project_pom, repository, destination, retained_poms, snapshots)
    retain_pom(frontend_pom, repository, destination, retained_poms, snapshots)
    retain_pom(runtime_pom, repository, destination, retained_poms, snapshots)
    retain_pom(framework_bom, repository, destination, retained_poms, snapshots)
    for path in source_licenses:
        copy_pinned(path, destination / "legal/m3" / path.name, snapshots[path])
    for index, path in enumerate(source_proofs):
        copy_pinned(path, destination / "provenance" / ("source-proof-%03d" % index + path.suffix), snapshots[path])
    for path, name in zip(inputs, ("tool-pom.xml", "frontend-pom.xml", None,
                                  "runtime-classpath.txt", "runtime-dependencies.txt", "runtime-pom.xml")):
        if name is not None:
            copy_pinned(path, destination / "provenance" / name, snapshots[path])
    launcher_jar(destination / "launch.jar", classpath)
    inventory = {"schema_version": 1, "source": source, "runner_coordinate": ":".join(identity),
                 "runner_sha256": runner_hash, "rewrite_version": REWRITE_VERSION,
                 "runtime_components": components, "classpath": classpath,
                 "compiler_exports": exports,
                 "launcher": {"file": "launch.jar", "sha256": digest(destination / "launch.jar"),
                              "format": "MANIFEST_ONLY_ORIGINAL_RUNTIME_CLASSPATH"},
                 "source_input_sha256": [{"file": str(path), "sha256": snapshots[path]}
                                         for path in inputs + source_licenses + source_proofs],
                 "pom_sha256": {":".join(key): value for key, value in sorted(retained_poms.items())},
                 "scope": "OPENREWRITE_COMPLETE_PUBLISHED_FRAMEWORK_AND_RESOLVED_RUNTIME",
                 "framework_artifacts": sorted(FRAMEWORK_ARTIFACTS),
                 "framework_source_commit": FRAMEWORK_COMMIT,
                 "framework_settings_gitblob": FRAMEWORK_SETTINGS_BLOB,
                 "cli_input_scope": "EXPLICIT_JAVA_SOURCES_VIA_SHARED_M3_COMPILER",
                 "runtime_execution": "NOT_ESTABLISHED_BY_ASSEMBLY",
                 "shared_ast_consumption": "NOT_ESTABLISHED_BY_ASSEMBLY"}
    (destination / "RUNTIME_INVENTORY.json").write_text(json.dumps(inventory, indent=2, sort_keys=True) + "\n")
    (destination / "runtime-classpath.txt").write_text("\n".join(classpath) + "\n")
    assets, tree = [], []
    for path in sorted(destination.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(destination).as_posix()
        checksum = digest(path)
        assets.append({"file": str(path), "target": "lib/m3-tools/openrewrite/" + relative,
                       "sha256": checksum, "executable": False})
        tree.append(relative + "\t" + checksum + "\t644\n")
    for windows, suffix in ((False, ""), (True, ".cmd")):
        path = destination / ("m3-rewrite" + suffix)
        path.write_bytes(launcher_text(classpath, exports, windows).encode("utf-8"))
        path.chmod(0o644 if windows else 0o755)
        assets.append({"file": str(path), "target": "bin/" + path.name,
                       "sha256": digest(path), "executable": not windows})
    pack = {"name": "openrewrite", "version": REWRITE_VERSION + "+m3-" + identity[2],
            "source": source, "license": "Original component POMs and embedded LICENSE/NOTICE retained",
            "roots": ROOTS, "modules": [], "assets": assets,
            "distribution": {"file_count": len(tree),
                             "tree_sha256": hashlib.sha256("".join(sorted(tree)).encode("utf-8")).hexdigest()}}
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    for path, checksum in snapshots.items():
        if digest(path) != checksum:
            raise ValueError("Input changed after runtime admission: " + str(path))
    with lock_path.open("x", encoding="utf-8", newline="\n") as output:
        json.dump({"schema_version": 1, "packs": [pack]}, output, indent=2, sort_keys=True)
        output.write("\n")
    load_tool_packs(lock_path, ["openrewrite"])
    return inventory


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("project-pom", "frontend-pom", "runtime-pom", "runner-jar", "runtime-classpath",
                 "runtime-dependencies", "maven-repository", "distribution", "out"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--source-license", type=Path, action="append", required=True,
                        help="Original source LICENSE and related legal files; repeat to retain each")
    parser.add_argument("--source-proof", type=Path, action="append", default=[],
                        help="Retain source recipe/build qualification receipt bytes")
    args = parser.parse_args(argv)
    inventory = assemble(args.project_pom, args.frontend_pom, args.runner_jar,
                         args.runtime_classpath, args.runtime_dependencies, args.maven_repository,
                         args.distribution, args.out, args.source, args.source_license, args.runtime_pom,
                         args.source_proof)
    print("M3_OPENREWRITE_RUNTIME_LOCK_PASS", len(inventory["runtime_components"]), args.out)


if __name__ == "__main__":
    main()
