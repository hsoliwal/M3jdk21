#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Qualify the complete M3FS Java module and bounded VFS core with the shared JMOD packer.

This explicit offline qualification lane does not replace Maven/native verification.
It invokes the existing jmod_pack.py owner, then runs an independent named consumer
from the resulting supplied-JDK-21 image. It does not build OpenJDK from source.
"""
import argparse
import bz2
import gzip
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tarfile
import xml.etree.ElementTree as ET
import zipfile

from vfs_pack_support import (VFS_PREFIX, collect_legal, digest, input_file, load_dependencies,
                              selected_vfs_source, verify_jar_legal, verify_linked_legal,
                              verify_source_lock, write_pack_inputs)

PROBE = "src/test/java/com/m3/fs/verification/M3VfsImageProbe.java"
DATE = "2026-10-20T00:00:00Z"
SCOPE = ("Complete existing M3FS Java sources plus the 237-source attributed VFS core, "
         "JAR/JMOD assembly through the existing shared packer, and named-consumer execution "
         "on an image linked from the supplied prebuilt JDK 21. Maven lifecycle, JNI/SQLite "
         "runtime verification, and source-built OpenJDK/M3JDK21 qualification remain separate gates.")


def clean_environment(jdk):
    environment = dict(os.environ)
    removed = []
    for name in list(environment):
        if name in {"LD_LIBRARY_PATH", "LD_PRELOAD", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS",
                    "_JAVA_OPTIONS", "CLASSPATH"} or name.startswith("DYLD_"):
            removed.append(name)
            environment.pop(name)
    environment["JAVA_HOME"] = str(jdk)
    environment["LC_ALL"] = "C"
    return environment, sorted(removed)


def checked(command, environment, log):
    result = subprocess.run([str(value) for value in command], env=environment,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    Path(log).write_text(result.stdout + result.stderr, encoding="utf-8")
    if result.returncode:
        raise RuntimeError("command failed with status %d; see %s" % (result.returncode, log))
    return result.stdout


def write_manifest(pom, destination):
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    project = ET.parse(pom)
    entries = project.find(".//m:plugin[m:artifactId='maven-jar-plugin']/m:configuration/m:archive/m:manifestEntries", namespace)
    if entries is None:
        raise ValueError("Maven JAR/OSGi manifest entries are required")
    attributes = [("Manifest-Version", "1.0")]
    attributes.extend((entry.tag.rsplit("}", 1)[-1], entry.text or "") for entry in entries)
    lines = []
    for key, value in attributes:
        # Current reviewed metadata is ASCII; refusing other input avoids byte-splitting UTF-8.
        line = (key + ": " + value).encode("ascii")
        while len(line) > 70:
            lines.append(line[:70])
            line = b" " + line[70:]
        lines.append(line)
    destination.write_bytes(b"\r\n".join(lines) + b"\r\n\r\n")


def fixtures(destination):
    destination.mkdir()
    payload = b"M3FS VFS image payload\n"
    (destination / "payload.txt").write_bytes(payload)
    for extension in ("zip", "jar"):
        with zipfile.ZipFile(destination / ("payload." + extension), "w") as archive:
            item = zipfile.ZipInfo("payload.txt", (2026, 10, 20, 0, 0, 0))
            archive.writestr(item, payload)
    tar_bytes = io.BytesIO()
    with tarfile.open(fileobj=tar_bytes, mode="w", format=tarfile.USTAR_FORMAT) as archive:
        item = tarfile.TarInfo("payload.txt")
        item.size, item.mtime, item.mode = len(payload), 0, 0o644
        archive.addfile(item, io.BytesIO(payload))
    data = tar_bytes.getvalue()
    (destination / "payload.tar").write_bytes(data)
    (destination / "payload.tgz").write_bytes(gzip.compress(data, mtime=0))
    (destination / "payload.tbz2").write_bytes(bz2.compress(data))
    (destination / "payload.txt.gz").write_bytes(gzip.compress(payload, mtime=0))
    (destination / "payload.txt.bz2").write_bytes(bz2.compress(payload))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--repository-root", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--dependency-cache", type=Path, required=True)
    parser.add_argument("--dependency-lock", type=Path)
    parser.add_argument("--source-lock", type=Path, required=True,
                        help="Reviewed canonical-materialization TSV inventory; path and sha256 columns")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    module = args.module_root.resolve(strict=True)
    repository = args.repository_root.resolve(strict=True)
    source_proof = verify_source_lock(module, args.source_lock)
    lock = args.dependency_lock or module / "tools/vfs-dependencies.lock.json"
    dependencies = load_dependencies(lock, args.dependency_cache, module)
    jdk = args.jdk_home.resolve(strict=True)
    release = input_file(jdk / "release").read_text(encoding="utf-8")
    if re.search(r'(?m)^JAVA_VERSION="21(?:\.|\+|\")', release) is None:
        raise ValueError("this qualification requires an actual JDK 21")
    for name in ("java", "javac", "jar", "jmod", "jlink"):
        input_file(jdk / "bin" / name)
    input_file(jdk / "jmods/java.base.jmod")
    packer = input_file(repository / "m3/tooling/jmod-recipes/tools/jmod_pack.py")
    kernel = repository / "m3/tooling/module-packs/kernel/src/main/java"
    if not kernel.is_dir() or not list(kernel.rglob("*.java")):
        raise ValueError("existing shared module-pack graph owner is missing")
    probe = input_file(module / PROBE)
    out = args.out.absolute()
    if os.path.lexists(out):
        raise FileExistsError("fresh output required: " + str(out))
    out.mkdir(parents=True)
    environment, removed = clean_environment(jdk)
    receipts = {
        "scope": SCOPE, "source": source_proof, "jdk_home": str(jdk), "jdk_release": release,
        "dependency_lock": {"path": str(input_file(lock)), "sha256": digest(lock)},
        "dependencies": [{"module": row["module"], "jar": str(row["jar"]),
                           "sha256": row["sha256"], "descriptor_kind": row["descriptor_kind"]}
                          for row in dependencies],
        "existing_packer": {"path": str(packer), "sha256": digest(packer)},
        "probe": {"path": str(probe), "sha256": digest(probe)},
        "removed_environment_names": removed,
    }
    (out / "inputs.json").write_text(json.dumps(receipts, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    classes = out / "classes"
    classes.mkdir()
    classpath = os.pathsep.join(str(row["jar"]) for row in dependencies)
    source_paths = sorted((module / "src/main/java").rglob("*.java"))
    vfs = [path for path in source_paths if selected_vfs_source(path.relative_to(module).as_posix())]
    owned = [path for path in source_paths if not path.relative_to(module).as_posix().startswith(VFS_PREFIX)]
    if not owned or not any(path.name == "M3FS.java" for path in owned):
        raise ValueError("complete existing M3FS product sources required")
    checked([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-implicit:none",
             "-sourcepath", "", "-cp", classpath, "-d", classes, *vfs],
            environment, out / "provider-compile.log")
    checked([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-implicit:none",
             "-sourcepath", "", "-cp", str(classes) + os.pathsep + classpath, "-d", classes, *owned],
            environment, out / "m3fs-strict-compile.log")
    checked([jdk / "bin/javac", "--release", "21", "-Xlint:all,-requires-automatic,-requires-transitive-automatic", "-Werror",
             "--module-path", classpath, "--patch-module", "jdk.m3fs=" + str(classes),
             "-d", classes, module / "src/main/module/module-info.java"],
            environment, out / "module-descriptor-compile.log")
    resources = module / "src/main/resources"
    if resources.is_dir():
        shutil.copytree(resources, classes, dirs_exist_ok=True)
    shutil.copytree(module / "legal", classes / "META-INF/m3fs-legal")
    manifest = out / "MANIFEST.MF"
    write_manifest(module / "pom.xml", manifest)
    jar = out / "m3-fs-0.1.0-SNAPSHOT.jar"
    checked([jdk / "bin/jar", "--create", "--file", jar, "--manifest", manifest,
             "--date", DATE, "--module-version", "0.1.0", "-C", classes, "."],
            environment, out / "jar-create.log")
    verify_jar_legal(module, jar)
    described = checked([jdk / "bin/jar", "--describe-module", "--file", jar],
                        environment, out / "module-description.txt")
    if not described.startswith("jdk.m3fs@"):
        raise ValueError("the product JAR must retain module jdk.m3fs")
    image_tsv, descriptor_manifest = write_pack_inputs(out, jar, dependencies)
    legal = out / "legal"
    legal_hashes = collect_legal(module, dependencies, legal)
    (out / "legal-sha256.json").write_text(json.dumps(legal_hashes, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    pack_output = out / "pack"
    checked([sys.executable, packer, "--image-tsv", image_tsv, "--manifest", descriptor_manifest,
             "--layer-root", module, "--out", pack_output, "--jdk-home", jdk,
             "--release", "21", "--date", DATE, "--add-modules", "java.base",
             "--root-module", "jdk.m3fs", "--legal-notices", legal],
            environment, out / "shared-packer.log")
    legal_proof = verify_linked_legal(pack_output, dependencies, legal_hashes)
    consumer = out / "consumer-classes"
    consumer.mkdir()
    descriptor = out / "consumer-module/module-info.java"
    descriptor.parent.mkdir()
    descriptor.write_text("module m3fs.vfs.consumer { requires jdk.m3fs; }\n", encoding="utf-8")
    checked([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
             "--module-path", str(jar) + os.pathsep + classpath,
             "-d", consumer, descriptor, probe], environment, out / "consumer-compile.log")
    fixture_root = out / "fixtures"
    fixtures(fixture_root)
    image = pack_output / "image"
    result = checked([image / "bin/java", "--module-path", consumer, "--module",
                      "m3fs.vfs.consumer/com.m3.fs.verification.M3VfsImageProbe", fixture_root],
                     environment, out / "consumer-run.log")
    if "M3FS_VFS_IMAGE_PROBE=PASS" not in result.splitlines():
        raise RuntimeError("linked-image consumer did not emit its success marker")
    # Replay immutable-input admission after use; drift cannot become a PASS receipt.
    verify_source_lock(module, args.source_lock)
    load_dependencies(lock, args.dependency_cache, module)
    report = {
        "status": "PASS", "scope": SCOPE, "image": str(image),
        "jar": {"path": str(jar), "sha256": digest(jar)},
        "compiled_m3fs_java_sources": len(owned), "compiled_vfs_java_sources": len(vfs),
        "provider_lint_policy": "-Xlint:all; upstream warnings recorded, not errors",
        "m3fs_lint_policy": "-Xlint:all -Werror; existing strict gate retained",
        "module_descriptor_lint_exception": "requires-automatic and requires-transitive-automatic only, for the unchanged Maven-input Commons Logging 1.2 JAR; JMOD descriptor is explicit",
        "legal": legal_proof, "consumer": dict(line.split("=", 1) for line in result.splitlines() if "=" in line),
        "maven_lifecycle": "NOT_RUN_BY_THIS_HELPER", "native_sqlite_probe": "NOT_RUN",
        "source_built_m3jdk21": "NOT_RUN", "omitted_remote_providers": "SOURCE_CUSTODY_ONLY",
    }
    (out / "qualification.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"status": "PASS", "scope": "M3FS_JAVA_VFS_CORE_LINKED_IMAGE",
                      "image": str(image), "receipts": str(out)}, sort_keys=True))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, KeyError, zipfile.BadZipFile) as failure:
        print("M3FS_VFS_QUALIFICATION_REFUSED: " + str(failure), file=sys.stderr)
        sys.exit(1)
