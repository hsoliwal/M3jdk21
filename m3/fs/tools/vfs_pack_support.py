#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Input admission and manifests for the existing M3 JMOD packer; no archive assembler."""
import csv
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import zipfile

UPSTREAM_COMMIT = "685b3b723a83e56034391350ea1a02231411142f"
RECEIVER_BASE = "73f59218dc4ef1a3ef79aad990e77273db2a2d60"
DEPENDENCY_MODULES = frozenset({"commons.logging", "org.apache.commons.lang3",
                                "org.apache.commons.io", "org.apache.commons.compress",
                                "org.apache.commons.codec"})
OMITTED_PROVIDERS = frozenset({"ftp", "ftps", "http", "https", "http4", "http4s",
                              "http5", "http5s", "sftp"})
OMITTED_FILES = frozenset({"cache/LRUFilesCache.java", "provider/URLFileName.java",
                          "provider/URLFileNameParser.java"})
VFS_PREFIX = "src/main/java/com/m3/fs/vfs/"
VFS_LEGAL_SHA256 = {
    "LICENSE.txt": "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30",
    "NOTICE.txt": "c3b9e52e1b1a64d99b76e0f529d38d70c7ed01b97d01fd68e4c2fe4b6bf7bbbb",
}


def digest(path):
    value = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def input_file(path):
    path = Path(path).resolve(strict=True)
    if not path.is_file() or any(c in str(path) for c in "\r\n\t" + os.pathsep):
        raise ValueError("regular input file with an unambiguous path required: " + str(path))
    return path


def relative_path(value):
    if not isinstance(value, str):
        raise ValueError("relative path must be text")
    path = PurePosixPath(value)
    if (not value or str(path) != value or path.is_absolute()
            or any(part in {".", ".."} for part in path.parts)
            or any(c in value for c in "\\\r\n\t:")):
        raise ValueError("unsafe relative path: " + repr(value))
    return path


def selected_vfs_source(relative):
    """Select the single published m3-vfs-core source profile, retaining full custody."""
    value = str(relative)
    if not value.startswith(VFS_PREFIX):
        return False
    tail = value[len(VFS_PREFIX):]
    parts = tail.split("/")
    return tail not in OMITTED_FILES and not (
        len(parts) > 2 and parts[0] == "provider" and parts[1] in OMITTED_PROVIDERS)


def source_inventory(module_root):
    """The sealed inventory includes uncompiled donor custody as well as build inputs."""
    root = Path(module_root)
    selected = {root / "pom.xml"}
    for directory in ("src/main/java", "src/main/module", "src/main/dependency-modules",
                      "src/main/resources", "legal"):
        selected.update(path for path in (root / directory).rglob("*") if path.is_file())
    for name in ("qualify_vfs.py", "vfs_pack_support.py", "vfs-dependencies.lock.json"):
        selected.add(root / "tools" / name)
    selected.add(root / "src/test/java/com/m3/fs/verification/M3VfsImageProbe.java")
    return sorted(path.relative_to(root).as_posix() for path in selected)


def verify_source_lock(module_root, lock_path):
    root = Path(module_root).resolve(strict=True)
    with input_file(lock_path).open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != ["path", "sha256"]:
            raise ValueError("source lock must have exactly path and sha256 TSV columns")
        rows = list(reader)
    locked = {}
    for row in rows:
        if None in row or row.get("sha256") is None:
            raise ValueError("malformed source-lock row")
        relative_path(row["path"])
        if row["path"] in locked or not re.fullmatch(r"[0-9a-f]{64}", row["sha256"]):
            raise ValueError("duplicate path or invalid SHA-256 in source lock")
        locked[row["path"]] = row["sha256"]
    actual = set(source_inventory(root))
    if set(locked) != actual:
        raise ValueError("source-lock inventory mismatch; missing=" + repr(sorted(actual - set(locked)))
                         + "; extra=" + repr(sorted(set(locked) - actual)))
    for name, expected in sorted(locked.items()):
        source = input_file(root / name)
        if not source.is_relative_to(root) or digest(source) != expected:
            raise ValueError("source SHA-256 drift: " + name)
    donor_sources = [name for name in locked if name.startswith(VFS_PREFIX) and name.endswith(".java")]
    if len(donor_sources) != 311:
        raise ValueError("expected all 311 relocated Apache Commons VFS Java sources in custody")
    compiled = [name for name in donor_sources if selected_vfs_source(name)]
    if len(compiled) != 237:
        raise ValueError("m3-vfs-core source selection drift; expected 237 sources")
    for name, expected in VFS_LEGAL_SHA256.items():
        legal = root / "legal/commons-vfs2" / name
        if digest(input_file(legal)) != expected:
            raise ValueError("exact Apache Commons VFS legal text drift: " + name)
    return {"source_lock": str(input_file(lock_path)), "source_lock_sha256": digest(lock_path),
            "input_files": len(locked), "donor_java_custody": len(donor_sources),
            "donor_java_compiled": len(compiled), "receiver_base": RECEIVER_BASE,
            "vfs_upstream_commit": UPSTREAM_COMMIT}


def load_dependencies(lock_path, cache, module_root):
    lock = json.loads(input_file(lock_path).read_text(encoding="utf-8"))
    if lock.get("schema") != 1 or lock.get("vfs_upstream_commit") != UPSTREAM_COMMIT:
        raise ValueError("unsupported dependency lock or upstream source pin")
    rows = lock.get("dependencies")
    if not isinstance(rows, list) or len(rows) != len(DEPENDENCY_MODULES):
        raise ValueError("the complete five-module VFS dependency closure is required")
    seen, resolved = set(), []
    cache = Path(cache).resolve(strict=True)
    root = Path(module_root).resolve(strict=True)
    for row in rows:
        name = row.get("module")
        if name not in DEPENDENCY_MODULES or name in seen:
            raise ValueError("duplicate or unexpected dependency module: " + str(name))
        seen.add(name)
        archive = input_file(cache / relative_path(row["repository_path"]))
        if not archive.is_relative_to(cache):
            raise ValueError("dependency must remain inside the supplied cache")
        expected = row.get("sha256", "")
        if not re.fullmatch(r"[0-9a-f]{64}", expected) or digest(archive) != expected:
            raise ValueError("dependency SHA-256 drift: " + name)
        with zipfile.ZipFile(archive) as source:
            names = source.namelist()
            if len(names) != len(set(names)):
                raise ValueError("duplicate dependency ZIP entries: " + name)
            legal = row.get("legal_sha256", {})
            if set(legal) != {"META-INF/LICENSE.txt", "META-INF/NOTICE.txt"}:
                raise ValueError("both original upstream LICENSE and NOTICE pins required: " + name)
            for entry, expected_legal in legal.items():
                if entry not in names or hashlib.sha256(source.read(entry)).hexdigest() != expected_legal:
                    raise ValueError("dependency legal text drift: " + name + "/" + entry)
            has_descriptor = any(p == "module-info.class" or
                                 re.fullmatch(r"META-INF/versions/[0-9]+/module-info.class", p)
                                 for p in names)
            if has_descriptor != (row.get("descriptor_kind") == "published"):
                raise ValueError("dependency descriptor kind drift: " + name)
        checked = dict(row, jar=archive)
        if name == "commons.logging":
            if row.get("descriptor_kind") != "injected":
                raise ValueError("Commons Logging requires the explicit descriptor lane")
            descriptor = input_file(root / relative_path(row["descriptor_target"]))
            if not descriptor.is_relative_to(root) or digest(descriptor) != row["descriptor_sha256"]:
                raise ValueError("Commons Logging descriptor SHA-256 drift")
            checked["descriptor"] = descriptor
        resolved.append(checked)
    if seen != DEPENDENCY_MODULES:
        raise ValueError("missing dependency module")
    return sorted(resolved, key=lambda row: row["module"])


def write_pack_inputs(receipts, jar, dependencies):
    image_tsv = Path(receipts) / "JMOD_IMAGE.tsv"
    manifest = Path(receipts) / "DESCRIPTOR_MANIFEST.tsv"
    with image_tsv.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("module", "jar", "disposition"))
        writer.writerow(("jdk.m3fs", str(input_file(jar)), "MODULAR_JAR"))
        for row in dependencies:
            if row["descriptor_kind"] == "published":
                writer.writerow((row["module"], str(row["jar"]), "MODULAR_JAR"))
    with manifest.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("module", "unit_jars", "target"))
        for row in dependencies:
            if row["descriptor_kind"] == "injected":
                # unit_jars is exactly one path, as required by the shared packer.
                writer.writerow((row["module"], str(row["jar"]), row["descriptor_target"]))
    return image_tsv, manifest


def collect_legal(module_root, dependencies, destination):
    """Keep originals, namespaced by owner; the packer's shared legal tree is unchanged."""
    destination = Path(destination)
    destination.mkdir(parents=True, exist_ok=True)
    for source in sorted((Path(module_root) / "legal").rglob("*")):
        if not source.is_file():
            continue
        target = destination / "m3fs" / source.relative_to(Path(module_root) / "legal")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    for row in dependencies:
        with zipfile.ZipFile(row["jar"]) as source:
            for entry in sorted(row["legal_sha256"]):
                target = destination / row["module"] / entry
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(source.read(entry))
    return {path.relative_to(destination).as_posix(): digest(path)
            for path in sorted(destination.rglob("*")) if path.is_file()}


def verify_jar_legal(module_root, jar):
    with zipfile.ZipFile(jar) as archive:
        for path in sorted((Path(module_root) / "legal").rglob("*")):
            if path.is_file():
                name = "META-INF/m3fs-legal/" + path.relative_to(Path(module_root) / "legal").as_posix()
                if archive.read(name) != path.read_bytes():
                    raise ValueError("M3FS JAR legal bytes drift: " + name)


def verify_linked_legal(pack_output, dependencies, legal_hashes):
    output = Path(pack_output)
    modules = ["jdk.m3fs"] + [row["module"] for row in dependencies]
    checked = 0
    for name in modules:
        archive_path = output / "jmods" / (name + ".jmod")
        with zipfile.ZipFile(archive_path) as archive:
            for relative, expected in sorted(legal_hashes.items()):
                if hashlib.sha256(archive.read("legal/" + relative)).hexdigest() != expected:
                    raise ValueError("JMOD legal bytes drift: " + name + "/" + relative)
                image_file = output / "image/legal" / name / relative
                if not image_file.resolve(strict=True).is_relative_to((output / "image").resolve()):
                    raise ValueError("image legal symlink escapes the image")
                if digest(image_file) != expected:
                    raise ValueError("linked-image legal bytes drift: " + name + "/" + relative)
                checked += 1
            row = next((row for row in dependencies if row["module"] == name), None)
            if row is not None:
                for entry, expected in row["legal_sha256"].items():
                    if hashlib.sha256(archive.read("classes/" + entry)).hexdigest() != expected:
                        raise ValueError("dependency class-resource legal bytes drift: " + name)
    return {"modules": modules, "jmod_and_image_legal_entries_checked": checked,
            "all_dependency_original_license_and_notice_resources_retained": True}
