#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prepare the genuine zlibsvm 3.0.0 pair as one external module owner.

This leaf preserves bytes and custody using the existing OpenNLP archive owner.
The caller owns dependency admission, jdeps, descriptor recipes and ordered gates.
No descriptor, JMOD, Java invocation or upstream build is produced here.
"""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Mapping, Sequence

import opennlp_pack as archive_owner


GROUP = "de.hs-heilbronn.mi"
VERSION = "3.0.0"
MODULE = "zlibsvm.core"
OWNERS = ("zlibsvm-api", "zlibsvm-core")
CLASS_COUNTS = {"zlibsvm-api": 12, "zlibsvm-core": 16}
PACKAGES = (
    "de.hhn.mi.configuration",
    "de.hhn.mi.domain",
    "de.hhn.mi.exception",
    "de.hhn.mi.process",
    "de.hhn.mi.shade.org.apache.commons.lang3",
    "de.hhn.mi.util",
)
HASH_KEYS = frozenset((*OWNERS, "zlibsvm-parent.pom", "LICENSE", "NOTICE"))
NS = archive_owner.NS


def _regular_file(path: Path) -> Path:
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("regular non-symlink input required: " + str(path))
    return path.resolve(strict=True)


def _identity(path: Path) -> dict:
    archive = archive_owner._read_archive(path)
    entries = archive["entries"]
    maven = [name for name in entries if name.startswith("META-INF/maven/")
             and name.endswith("/pom.properties")]
    if len(maven) != 1:
        raise ValueError("one exact zlibsvm Maven coordinate required")
    props = archive_owner._properties(entries[maven[0]])
    artifact = props.get("artifactId")
    prefix = f"META-INF/maven/{GROUP}/{artifact}/"
    if (props.get("groupId") != GROUP or props.get("version") != VERSION
            or artifact not in OWNERS or path.name != f"{artifact}-{VERSION}.jar"
            or maven[0] != prefix + "pom.properties" or prefix + "pom.xml" not in entries):
        raise ValueError("unexpected zlibsvm artifact, classifier or version: " + path.name)
    if "META-INF/MANIFEST.MF" not in entries:
        raise ValueError("original zlibsvm manifest required")
    if any(name.startswith(archive_owner.SERVICES) for name in entries):
        raise ValueError("zlibsvm service providers require separate admission")
    pom = ET.fromstring(entries[prefix + "pom.xml"])
    parent = pom.find("m:parent", NS)
    if (pom.findtext("m:artifactId", namespaces=NS) != artifact or parent is None
            or pom.findtext("m:groupId", default=GROUP, namespaces=NS) != GROUP
            or pom.findtext("m:version", default=VERSION, namespaces=NS) != VERSION
            or parent.findtext("m:groupId", namespaces=NS) != GROUP
            or parent.findtext("m:artifactId", namespaces=NS) != "zlibsvm"
            or parent.findtext("m:version", namespaces=NS) != VERSION):
        raise ValueError("zlibsvm embedded POM identity drift: " + artifact)
    return {"artifact": artifact, "coordinate": f"{GROUP}:{artifact}:{VERSION}",
            "pom": pom, **archive}


def _dependencies(parent_bytes: bytes, archives: list[dict]) -> list[dict]:
    """Confirm the fixed published POM facts; do not resolve Maven dependencies."""
    parent = ET.fromstring(parent_bytes)
    if (parent.findtext("m:groupId", namespaces=NS) != GROUP
            or parent.findtext("m:artifactId", namespaces=NS) != "zlibsvm"
            or parent.findtext("m:version", namespaces=NS) != VERSION
            or parent.findtext("m:packaging", namespaces=NS) != "pom"):
        raise ValueError("zlibsvm parent POM identity drift")
    urls = {node.findtext("m:url", namespaces=NS)
            for node in parent.findall("m:licenses/m:license", NS)}
    if "https://www.apache.org/licenses/LICENSE-2.0.txt" not in urls:
        raise ValueError("zlibsvm parent Apache-2.0 declaration absent")
    properties = {node.tag.rsplit("}", 1)[-1]: node.text or ""
                  for node in parent.findall("m:properties/*", NS)}
    required = {"zlibsvm.version": VERSION, "libsvm.version": "3.35",
                "slf4j.version": "2.0.17", "java.source": "21", "java.target": "21"}
    if any(properties.get(key) != value for key, value in required.items()):
        raise ValueError("zlibsvm parent dependency or release drift")

    def expand(value: str | None) -> str:
        if not value:
            raise ValueError("missing zlibsvm Maven version")
        result = re.sub(r"\$\{([^}]+)\}", lambda match: properties[match[1]], value)
        if "${" in result:
            raise ValueError("unresolved zlibsvm Maven version")
        return result

    managed = {}
    for dep in parent.findall("m:dependencyManagement/m:dependencies/m:dependency", NS):
        key = (dep.findtext("m:groupId", namespaces=NS),
               dep.findtext("m:artifactId", namespaces=NS))
        managed[key] = expand(dep.findtext("m:version", namespaces=NS))
    if parent.findall("m:dependencies/m:dependency", NS):
        raise ValueError("new inherited zlibsvm dependency requires admission")
    expected = {"zlibsvm-api": set(), "zlibsvm-core": {
        (GROUP, "zlibsvm-api", VERSION), ("tw.edu.ntu.csie", "libsvm", "3.35"),
        ("org.slf4j", "slf4j-api", "2.0.17")}}
    for archive in archives:
        found = set()
        for dep in archive["pom"].findall("m:dependencies/m:dependency", NS):
            scope = dep.findtext("m:scope", default="compile", namespaces=NS)
            if scope == "test":
                continue
            if (scope != "compile"
                    or dep.findtext("m:optional", default="false", namespaces=NS) != "false"
                    or dep.findtext("m:type", default="jar", namespaces=NS) != "jar"
                    or dep.find("m:classifier", NS) is not None):
                raise ValueError("zlibsvm dependency scope requires separate admission")
            key = (dep.findtext("m:groupId", namespaces=NS),
                   dep.findtext("m:artifactId", namespaces=NS))
            raw = dep.findtext("m:version", namespaces=NS)
            found.add((*key, expand(raw) if raw else managed[key]))
        if found != expected[archive["artifact"]]:
            raise ValueError("zlibsvm dependency closure drift: " + archive["artifact"])
    return [
        {"groupId": "tw.edu.ntu.csie", "artifactId": "libsvm", "version": "3.35",
         "module": "libsvm"},
        {"groupId": "org.slf4j", "artifactId": "slf4j-api", "version": "2.0.17",
         "module": "org.slf4j", "receiving_selection": "CALLER_INPUT_LOCK_OWNS_VERSION"},
    ]


def _class_inventory(archives: list[dict]) -> list[dict]:
    owners = {}
    packages = set()
    rows = []
    counts = {}
    for archive in archives:
        artifact = archive["artifact"]
        counts[artifact] = 0
        for name, data in sorted(archive["entries"].items()):
            if not name.endswith(".class"):
                continue
            if name in owners:
                raise ValueError("class entry has two zlibsvm owners: " + name)
            owners[name] = artifact
            package, separator, _ = name.rpartition("/")
            if (not separator or name.startswith("META-INF/")
                    or not archive_owner.BINARY_NAME.fullmatch(package.replace("/", ".") + ".Type")
                    or len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe"
                    or int.from_bytes(data[4:6], "big") != 0
                    or int.from_bytes(data[6:8], "big") != 65):
                raise ValueError("ordinary Java 21 zlibsvm class required: " + name)
            counts[artifact] += 1
            packages.add(package.replace("/", "."))
            rows.append({"artifact": artifact, "entry": name,
                         "sha256": archive_owner._sha(data), "class_major": 65})
    if counts != CLASS_COUNTS or packages != set(PACKAGES):
        raise ValueError("zlibsvm class or package inventory drift")
    return rows


def prepare(jars: Sequence[Path], output: Path, *, parent_pom: Path,
            license_file: Path, notice_file: Path,
            expected_sha256: Mapping[str, str]) -> dict:
    """Return one fresh archive and exact facts for the caller's descriptor recipe.

    All five hashes are explicit. A failure can leave partial output, but never a
    successful MANIFEST.json. The original pair must not also enter the module path.
    """
    expected = dict(expected_sha256)
    if set(expected) != HASH_KEYS or any(not isinstance(value, str)
            or re.fullmatch(r"[0-9a-f]{64}", value) is None for value in expected.values()):
        raise ValueError("exact five zlibsvm input SHA-256 pins required")
    if isinstance(jars, (str, Path)):
        raise ValueError("two explicit zlibsvm JAR paths required")
    paths = [_regular_file(path) for path in jars]
    if len(paths) != 2:
        raise ValueError("exactly one core and one API JAR required")
    support_paths = {"zlibsvm-parent.pom": _regular_file(parent_pom),
                     "LICENSE": _regular_file(license_file),
                     "NOTICE": _regular_file(notice_file)}
    output = Path(output).absolute()
    if output.exists() or output.is_symlink():
        raise FileExistsError("fresh output directory required: " + str(output))
    if not output.parent.is_dir() or output.parent.resolve() != output.parent:
        raise ValueError("existing canonical output parent required")
    if any(output.is_relative_to(path.parent) for path in [*paths, *support_paths.values()]):
        raise ValueError("output must be outside every input directory")
    archives = [_identity(path) for path in paths]
    by_owner = {archive["artifact"]: archive for archive in archives}
    if len(by_owner) != len(archives) or set(by_owner) != set(OWNERS):
        raise ValueError("exactly one core and one API JAR required")
    archives = [by_owner[owner] for owner in OWNERS]
    for archive in archives:
        if archive["sha256"] != expected[archive["artifact"]]:
            raise ValueError("zlibsvm input hash drift: " + archive["artifact"])
    support = {key: path.read_bytes() for key, path in support_paths.items()}
    for key, data in support.items():
        if archive_owner._sha(data) != expected[key]:
            raise ValueError("zlibsvm input hash drift: " + key)
    if (b"Apache License" not in support["LICENSE"]
            or b"Version 2.0" not in support["LICENSE"]
            or any(token not in support["NOTICE"] for token in (
                b"zlibsvm-api:3.0.0", b"zlibsvm-core:3.0.0", b"libsvm:3.35"))):
        raise ValueError("complete declared OpenNLP supporting rights required")
    declared_dependencies = _dependencies(support["zlibsvm-parent.pom"], archives)
    classes = _class_inventory(archives)
    payload, custody, providers = archive_owner._merge(archives)
    if providers:
        raise ValueError("unexpected zlibsvm service inventory")
    support_targets = {
        "zlibsvm-parent.pom": [archive_owner.CUSTODY + "zlibsvm-parent/pom.xml"],
        "LICENSE": [archive_owner.CUSTODY + "opennlp-support/LICENSE", "META-INF/LICENSE"],
        "NOTICE": [archive_owner.CUSTODY + "opennlp-support/NOTICE", "META-INF/NOTICE"],
    }
    for key, targets in support_targets.items():
        for name in targets:
            if (name in payload or any(parent.as_posix() in payload for parent in Path(name).parents
                                       if parent.as_posix() != ".")
                    or any(entry.startswith(name + "/") for entry in payload)):
                raise ValueError("supporting rights output collision: " + name)
            payload[name] = support[key]
    payload["META-INF/MANIFEST.MF"] = (
        "Manifest-Version: 1.0\r\nAutomatic-Module-Name: " + MODULE + "\r\n\r\n").encode()
    facts = {"module": MODULE, "open": True, "packages": list(PACKAGES),
             "exports": list(PACKAGES), "uses": [], "provides": {},
             "requires": [{"module": "libsvm", "modifiers": ["transitive"]},
                          {"module": "org.slf4j", "modifiers": []}],
             "descriptor_authority": "ROOT_JDEPS_AND_M3ModuleDescriptorRecipe_OWNED",
             "declared_dependency_coordinates": declared_dependencies}
    output.mkdir()
    merged = output / (MODULE + ".jar")
    archive_owner._write_jar(merged, payload)
    facts_path = output / "DESCRIPTOR_FACTS.json"
    facts_path.write_bytes(archive_owner._json(facts))
    all_paths = {archive["artifact"]: archive["path"] for archive in archives}
    all_paths.update(support_paths)
    post_hashes = {key: archive_owner._digest(_regular_file(path))
                   for key, path in all_paths.items()}
    if post_hashes != expected:
        raise ValueError("input drift during zlibsvm aggregation")
    hashes = {path.name: archive_owner._digest(path) for path in sorted(output.iterdir())}
    manifest = {
        "schema": "m3-zlibsvm-external-aggregate-v1", "status": "PREPARED_NOT_RUNTIME_QUALIFIED",
        "module": MODULE, "original_automatic_modules": ["zlibsvm.api", "zlibsvm.core"],
        "inputs": [{"artifact": a["artifact"], "coordinate": a["coordinate"],
                    "path": str(a["path"]), "sha256": a["sha256"],
                    "post_sha256": post_hashes[a["artifact"]],
                    "embedded_legal_entries": sorted(name for name in a["entries"]
                                                      if archive_owner._legal(name))}
                   for a in archives],
        "supporting_rights": [{"input": key, "path": str(support_paths[key]),
                              "origin": ("PUBLISHED_ZLIBSVM_PARENT_POM" if key == "zlibsvm-parent.pom"
                                         else "OPENNLP_ROOT_SUPPORTING_RIGHTS"),
                              "sha256": expected[key], "post_sha256": post_hashes[key],
                              "outputs": targets} for key, targets in support_targets.items()],
        "declared_dependency_coordinates": declared_dependencies,
        "entries": custody, "classes": classes, "outputs": hashes,
        "input_sha256": expected, "input_post_sha256": post_hashes,
        "all_input_posthashes_match": True,
        "provenance_limit": "COORDINATES_ARE_NOT_BUILD_PROOF;_CALLER_OWNS_ARTIFACT_ADMISSION",
        "shaded_classes": "ORIGINAL_ARRAYUTILS_BYTES_PRESERVED;_NO_SOURCE_HEADER_COMPLETENESS_CLAIM",
        "runtime_requirement": "DO_NOT_RESOLVE_ORIGINAL_ZLIBSVM_API_ALONGSIDE_RECEIVING_OWNER",
    }
    manifest_path = output / "MANIFEST.json"
    with manifest_path.open("xb") as stream:
        stream.write(archive_owner._json(manifest))
    return {"merged_jar": merged, "descriptor_facts": facts_path, "manifest": manifest_path,
            "packages": list(PACKAGES), "services": {},
            "declared_dependency_coordinates": declared_dependencies,
            "input_sha256": expected, "output_sha256": {
                **hashes, manifest_path.name: archive_owner._digest(manifest_path)}}
