#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Package the M3FS module with the existing M3 JMOD packer and verify a linked image.

This is a Linux packaging/probe wrapper. It does not build OpenJDK or establish that
a modified M3JDK21 source tree has passed the full OpenJDK test suite.
"""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

DONOR_COMMIT = "289e32f130c9c8e4a1cf0edc1066327d699393bf"
ROOTS = ("java.se", "jdk.m3fs", "jdk.unsupported", "jdk.zipfs")


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def input_file(value):
    path = value.resolve(strict=True)
    if not path.is_file() or any(character in str(path) for character in "\n\r\t"):
        raise ValueError("regular input file with a single-line path required: " + str(path))
    return path


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
    output = result.stdout + result.stderr
    log.write_text(output, encoding="utf-8")
    if result.returncode:
        raise RuntimeError("command failed with status %d; see %s" % (result.returncode, log))
    return result.stdout


def dynamic_info(path, environment):
    readelf = shutil.which("readelf", path=environment.get("PATH"))
    if readelf is None:
        raise ValueError("readelf is required to verify Linux SONAME and origin search paths")
    result = subprocess.run([readelf, "-d", str(path)], env=environment,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode:
        raise ValueError("cannot inspect native library: " + str(path))
    soname = re.search(r"\(SONAME\).*?\[([^\]]+)\]", result.stdout)
    paths = re.findall(r"\((?:RUNPATH|RPATH)\).*?\[([^\]]*)\]", result.stdout)
    return soname.group(1) if soname else None, paths


def native_notices(repository, module_root, artifact_module, sqlite, legal):
    legal.mkdir()
    owned_legal = next((path for path in (module_root / "legal", artifact_module / "legal",
                                         repository / "m3/fs/legal")
                        if (path / "LICENSE.txt").is_file()), None)
    candidates = ((owned_legal / "LICENSE.txt",) if owned_legal else ()) + (
                  module_root / "LICENSE-2.0.txt", module_root / "LICENSE",
                  repository / "m3/tooling/module-packs/legal/LICENSE")
    license_file = next((path for path in candidates if path.is_file()), None)
    if license_file is None:
        raise ValueError("Apache-2.0 license text missing from module and existing tooling owner")
    shutil.copyfile(license_file, legal / "LICENSE-2.0.txt")
    owned_notices = []
    if owned_legal is not None:
        for name in ("NOTICE.txt", "THIRD_PARTY_NOTICES.md"):
            source = input_file(owned_legal / name)
            shutil.copyfile(source, legal / name)
            owned_notices.append({"path": str(source), "sha256": digest(source)})
    (legal / "M3FS-NATIVE-NOTICE.txt").write_text(
        "M3JDK21 M3FS runtime\n"
        "Copyright 2026 Hitesh Soliwal and contributors\n"
        "M3FS Java and native source adaptations are licensed under Apache-2.0.\n"
        "Pinned source lineage: hsoliwal/com.synexia@" + DONOR_COMMIT + "\n"
        "The MIndex serialized image magic and SQLite schema version 1 are retained.\n"
        "The supplied SQLite runtime remains a separate upstream/distribution component.\n"
        "Its exact input and linked-image hashes are recorded separately in the receipts.\n"
        "SQLite upstream licensing: https://www.sqlite.org/copyright.html\n"
        "The supplied JDK distribution retains its original licenses and legal notices.\n",
        encoding="utf-8")
    notices = []
    for parent in sqlite.parents:
        candidate = parent / "share/doc/libsqlite3-0/copyright"
        if candidate.is_file():
            shutil.copyfile(candidate, legal / "SQLITE-DISTRIBUTION-COPYRIGHT.txt")
            notices.append({"path": str(candidate), "sha256": digest(candidate)})
            break
    return {"apache_license": str(license_file), "apache_sha256": digest(license_file),
            "m3fs_notices": owned_notices,
            "sqlite_distribution_notices": notices}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--native-library", type=Path, required=True)
    parser.add_argument("--sqlite-library", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--repository-root", type=Path,
                        default=Path(__file__).resolve().parents[3])
    args = parser.parse_args(argv)
    if sys.platform != "linux":
        raise ValueError("this wrapper qualifies Linux native origins through /proc/self/maps")
    jdk = args.jdk_home.resolve(strict=True)
    for command in ("java", "javac", "jmod", "jlink", "jar"):
        if not (jdk / "bin" / command).is_file():
            raise ValueError("JDK tool missing: " + command)
    if not (jdk / "jmods/java.base.jmod").is_file():
        raise ValueError("complete JDK with jmods required")
    jar = input_file(args.jar)
    native = input_file(args.native_library)
    sqlite = input_file(args.sqlite_library)
    with zipfile.ZipFile(jar) as archive:
        if "module-info.class" not in archive.namelist():
            raise ValueError("packaged modular M3FS jar required; run Maven install first")
    repository = args.repository_root.resolve(strict=True)
    packer = input_file(repository / "m3/tooling/jmod-recipes/tools/jmod_pack.py")
    graph = repository / "m3/tooling/module-packs/kernel/src/main/java"
    if not graph.is_dir():
        raise ValueError("existing module-packs graph owner missing")
    module_root = Path(__file__).resolve().parents[1]
    probe = input_file(module_root / "verification/M3FsRuntimeProbe.java")
    out = args.out.absolute()
    receipts = out.with_name(out.name + "-receipts")
    if os.path.lexists(out) or os.path.lexists(receipts):
        raise FileExistsError("fresh output and receipt directories required: %s and %s" % (out, receipts))
    environment, removed = clean_environment(jdk)
    native_soname, native_paths = dynamic_info(native, environment)
    sqlite_soname, _ = dynamic_info(sqlite, environment)
    if native_soname != "libm3fs.so" or native_paths != ["$ORIGIN"]:
        raise ValueError("M3FS JNI library must have SONAME libm3fs.so and only $ORIGIN runtime search path")
    if sqlite_soname is None or not re.fullmatch(r"libsqlite3\.so\.[0-9]+", sqlite_soname):
        raise ValueError("versioned SQLite runtime SONAME required")
    receipts.parent.mkdir(parents=True, exist_ok=True)
    receipts.mkdir()
    libraries = receipts / "native-libraries"
    libraries.mkdir()
    shutil.copyfile(native, libraries / "libm3fs.so")
    shutil.copyfile(sqlite, libraries / sqlite_soname)
    legal = receipts / "legal"
    notices = native_notices(repository, module_root, jar.parent.parent, sqlite, legal)
    release = (jdk / "release").read_text(encoding="utf-8") if (jdk / "release").is_file() else ""
    inputs = {
        "donor_commit": DONOR_COMMIT,
        "jar": {"path": str(jar), "sha256": digest(jar)},
        "jni_library": {"path": str(native), "sha256": digest(native)},
        "sqlite_library": {"path": str(sqlite), "soname": sqlite_soname, "sha256": digest(sqlite)},
        "existing_packer": {"path": str(packer), "sha256": digest(packer)},
        "probe_source": {"path": str(probe), "sha256": digest(probe)},
        "jdk_home": str(jdk), "jdk_release": release, "roots": ROOTS,
        "removed_environment_names": removed, "legal": notices,
        "scope": "Linked-image packaging and native probe on the supplied prebuilt JDK 21. "
                 "This wrapper does not build or qualify the full modified OpenJDK/M3JDK21 tree.",
    }
    (receipts / "inputs.json").write_text(json.dumps(inputs, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    image_tsv = receipts / "JMOD_IMAGE.tsv"
    with image_tsv.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("module", "jar", "disposition"))
        writer.writerow(("jdk.m3fs", str(jar), "MODULAR_JAR"))
    manifest = receipts / "DESCRIPTOR_MANIFEST.tsv"
    manifest.write_text("module\tunit_jars\ttarget\n", encoding="utf-8")
    command = [sys.executable, packer, "--image-tsv", image_tsv, "--manifest", manifest,
               "--layer-root", repository, "--jdk-home", jdk, "--out", out,
               "--add-modules", "java.se", "--native-library", "jdk.m3fs=" + str(libraries),
               "--legal-notices", legal]
    for root in ROOTS:
        command.extend(("--root-module", root))
    checked(command, environment, receipts / "pack.log")
    image = (out / "image").resolve(strict=True)
    classes = receipts / "probe-classes"
    checked([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
             "--module-path", jar, "--add-modules", "jdk.m3fs", "-d", classes, probe],
            environment, receipts / "probe-compile.log")
    probe_output = checked([image / "bin/java", "-Xcheck:jni", "-Dm3fs.native=required",
                            "--add-modules", "jdk.m3fs", "-cp", classes, "M3FsRuntimeProbe",
                            receipts / "probe.sqlite", image, sqlite_soname],
                           environment, receipts / "probe.log")
    values = dict(line.split("=", 1) for line in probe_output.splitlines() if "=" in line)
    if values.get("M3FS_RUNTIME_PROBE") != "PASS":
        raise RuntimeError("linked probe did not emit its success marker")
    linked_native = image / "lib/libm3fs.so"
    linked_sqlite = image / "lib" / sqlite_soname
    report = {
        "status": "PASS", "image": str(image), "probe": values,
        "linked_jni": {"path": str(linked_native), "sha256": digest(linked_native)},
        "linked_sqlite": {"path": str(linked_sqlite), "sha256": digest(linked_sqlite)},
        "input_hash_receipt": "inputs.json",
        "native_hash_note": "jlink --strip-debug can change copied native bytes; input and image hashes are distinct observations.",
        "scope": inputs["scope"],
    }
    (receipts / "runtime-proof.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"status": "PASS", "image": str(image), "receipts": str(receipts)}, sort_keys=True))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, zipfile.BadZipFile) as failure:
        print("M3FS_RUNTIME_PACKAGE_REFUSED: " + str(failure), file=sys.stderr)
        sys.exit(1)
