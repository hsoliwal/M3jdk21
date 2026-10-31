#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prepare the pinned OpenNLP CPU toolkit for the existing M3 JMOD packer.

This leaf aggregates genuine Maven output; it does not build, rewrite algorithms,
resolve a module graph, compile descriptors, create JMODs, or run Java. The caller
owns the ordered build and runtime gates and supplies separately admitted external
dependencies to the existing descriptor recipe and JMOD packer.
"""

from __future__ import annotations

import hashlib
import io
import json
import os
import re
import stat
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from typing import Sequence


PINNED_HEAD = "368337311ad94b665fd178f8d3ced005a73b2370"
VERSION = "3.0.0-SNAPSHOT"
GROUP = "org.apache.opennlp"
MODULE = "org.apache.opennlp.tools"
# Explicit POM owners, in one stable order independent of caller argument order.
OWNERS = {
    "opennlp-api": "opennlp-api",
    "opennlp-ml-commons": "opennlp-core/opennlp-ml/opennlp-ml-commons",
    "opennlp-ml-maxent": "opennlp-core/opennlp-ml/opennlp-ml-maxent",
    "opennlp-ml-bayes": "opennlp-core/opennlp-ml/opennlp-ml-bayes",
    "opennlp-ml-perceptron": "opennlp-core/opennlp-ml/opennlp-ml-perceptron",
    "opennlp-ml-libsvm": "opennlp-core/opennlp-ml/opennlp-ml-libsvm",
    "opennlp-runtime": "opennlp-core/opennlp-runtime",
    "opennlp-model-resolver": "opennlp-core/opennlp-model-resolver",
    "opennlp-formats": "opennlp-core/opennlp-formats",
    "opennlp-cli": "opennlp-core/opennlp-cli",
    "opennlp-tools": "opennlp-tools",
}
EXCLUDED = (
    "opennlp-dl", "opennlp-dl-gpu", "opennlp-morfologik", "opennlp-spellcheck",
    "opennlp-uima", "opennlp-docs", "opennlp-distr", "opennlp-eval-tests",
    "pretrained-models",
)
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
CUSTODY = "META-INF/m3-donor/"
SERVICES = "META-INF/services/"
BINARY_NAME = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+")


def _sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _digest(path: Path) -> str:
    with path.open("rb") as stream:
        value = hashlib.sha256()
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
        return value.hexdigest()


def _json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=True) + "\n").encode()


def _git(root: Path, *args: str) -> bytes:
    result = subprocess.run(["git", "-C", str(root), *args], check=True,
                            capture_output=True)
    return result.stdout


def _declared_crlf_checkout(root: Path, head: str, name: str,
                            expected_blob: str, data: bytes) -> dict:
    """Admit only a complete CRLF checkout of the committed LF bytes."""
    committed = _git(root, "cat-file", "blob", expected_blob)
    normalized = data.replace(b"\r\n", b"\n")
    if (b"\n" not in normalized or b"\r" in normalized
            or normalized.replace(b"\n", b"\r\n") != data or normalized != committed):
        raise ValueError("tracked source byte drift: " + name)
    info = Path(_git(root, "rev-parse", "--git-path", "info/attributes").decode().strip())
    if not info.is_absolute():
        info = root / info
    if info.is_symlink() or (info.exists() and any(
            line.strip() and not line.startswith(b"#") for line in info.read_bytes().splitlines())):
        raise ValueError("repository info attributes cannot authorize CRLF checkout: " + name)
    result = subprocess.run(
        ["git", "-C", str(root), "-c", "core.attributesFile=" + os.devnull,
         "check-attr", "--source=" + head, "--all", "-z", "--", name],
        check=True, capture_output=True, env={**os.environ, "GIT_ATTR_NOSYSTEM": "1"})
    fields = result.stdout.split(b"\0")
    if fields[-1] or (len(fields) - 1) % 3:
        raise ValueError("invalid native Git attribute receipt: " + name)
    attributes = {}
    for index in range(0, len(fields) - 1, 3):
        path, attribute, value = (field.decode("utf-8") for field in fields[index:index + 3])
        if path != name or attribute in attributes:
            raise ValueError("invalid native Git attribute receipt: " + name)
        attributes[attribute] = value
    # --all omits absent attributes; an explicit value "unspecified" is still active.
    if (attributes.get("text") != "set" or attributes.get("eol") != "crlf"
            or "filter" in attributes or "working-tree-encoding" in attributes):
        raise ValueError("committed plain text/eol=crlf attributes required: " + name)
    return {"conversion": "git-declared-lf-to-crlf", "attribute_source_head": head,
            "git_blob": expected_blob, "git_blob_sha256": _sha(committed),
            "attributes": attributes, "system_and_global_attributes": "disabled",
            "repository_info_attributes": "absent-or-comments-only"}


def _source_inventory(root: Path, expected_head: str) -> list[dict]:
    """Bind every tracked byte to the explicitly admitted Git commit."""
    if not re.fullmatch(r"[0-9a-f]{40}", expected_head):
        raise ValueError("exact expected source commit required")
    if _git(root, "rev-parse", "HEAD").decode().strip() != expected_head:
        raise ValueError("OpenNLP source commit drift")
    if Path(_git(root, "rev-parse", "--show-toplevel").decode().strip()).resolve() != root:
        raise ValueError("OpenNLP root must be the repository root")
    if _git(root, "status", "--porcelain=v1", "--untracked-files=all"):
        raise ValueError("clean committed OpenNLP source tree required")
    rows = []
    for entry in _git(root, "ls-tree", "-rz", "--full-tree", "HEAD").split(b"\0"):
        if not entry:
            continue
        metadata, relative = entry.split(b"\t", 1)
        mode, kind, expected_blob = metadata.decode().split()
        name = relative.decode("utf-8")
        path = root / name
        if kind != "blob" or mode not in ("100644", "100755"):
            raise ValueError("unsupported tracked source mode: " + name)
        if path.is_symlink() or not path.resolve().is_relative_to(root):
            raise ValueError("source symlink or escaped path: " + name)
        data = path.read_bytes()
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        row = {"path": name, "sha256": _sha(data)}
        if blob != expected_blob:
            row["checkout"] = _declared_crlf_checkout(root, expected_head, name, expected_blob, data)
        rows.append(row)
    if not rows:
        raise ValueError("empty source inventory")
    return sorted(rows, key=lambda row: row["path"])


def _pom_dependencies(root: Path) -> list[dict]:
    """Read only the fixed toolkit owners and their real Maven parent dependencies."""
    parent = ET.parse(root / "pom.xml").getroot()
    if parent.findtext("m:version", namespaces=NS) != VERSION:
        raise ValueError("OpenNLP source version drift")
    properties = {node.tag.rsplit("}", 1)[-1]: node.text or ""
                  for node in parent.findall("m:properties/*", NS)}
    properties.update({"project.groupId": GROUP, "project.version": VERSION})

    def expand(value: str) -> str:
        value = re.sub(r"\$\{([^}]+)\}", lambda match: properties[match[1]], value)
        if "${" in value or not value:
            raise ValueError("unresolved Maven coordinate")
        return value

    managed = {}
    for dep in parent.findall("m:dependencyManagement/m:dependencies/m:dependency", NS):
        group = expand(dep.findtext("m:groupId", namespaces=NS) or "")
        artifact = expand(dep.findtext("m:artifactId", namespaces=NS) or "")
        managed[(group, artifact)] = expand(dep.findtext("m:version", namespaces=NS) or "")
    external = {}
    for artifact, owner in OWNERS.items():
        model = ET.parse(root / owner / "pom.xml").getroot()
        if model.findtext("m:artifactId", namespaces=NS) != artifact:
            raise ValueError("Maven owner drift: " + owner)
        directory = root / owner
        while True:
            pom = ET.parse(directory / "pom.xml").getroot()
            for dep in pom.findall("m:dependencies/m:dependency", NS):
                scope = dep.findtext("m:scope", default="compile", namespaces=NS)
                if scope == "test":
                    continue
                group = expand(dep.findtext("m:groupId", namespaces=NS) or "")
                name = expand(dep.findtext("m:artifactId", namespaces=NS) or "")
                if group == GROUP:
                    if name not in OWNERS:
                        raise ValueError("CPU toolkit dependency closure changed: " + name)
                    continue
                raw = dep.findtext("m:version", namespaces=NS)
                version = expand(raw) if raw else managed[(group, name)]
                optional = dep.findtext("m:optional", default="false", namespaces=NS) == "true"
                key = (group, name, version)
                row = external.setdefault(key, {"groupId": group, "artifactId": name,
                    "version": version, "optional": optional, "owners": []})
                row["optional"] = row["optional"] and optional
                if artifact not in row["owners"]:
                    row["owners"].append(artifact)
            if directory == root:
                break
            directory = directory.parent
    return [external[key] for key in sorted(external)]


def _safe_name(info: zipfile.ZipInfo) -> str:
    name = info.filename
    pieces = name.rstrip("/").split("/")
    mode = stat.S_IFMT(info.external_attr >> 16)
    if (name != info.orig_filename or not name or "\\" in name or ":" in name or "//" in name
            or "\0" in name or any(part in ("", ".", "..") for part in pieces)
            or mode not in (0, stat.S_IFREG, stat.S_IFDIR) or info.flag_bits & 1):
        raise ValueError("unsafe archive entry: " + repr(name))
    return name


def _properties(data: bytes) -> dict[str, str]:
    result = {}
    for line in data.decode("iso-8859-1").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or "\\" in line or key in result:
            raise ValueError("ambiguous Maven pom.properties")
        result[key] = value
    return result


def _read_archive(path: Path) -> dict:
    """Admit ordinary archive bytes without inferring a Maven owner."""
    if path.is_symlink() or not path.is_file():
        raise ValueError("regular Maven JAR input required: " + str(path))
    data = path.read_bytes()
    entries = {}
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        seen = set()
        for info in archive.infolist():
            name = _safe_name(info)
            if name in seen:
                raise ValueError("duplicate entry within JAR: " + name)
            seen.add(name)
            upper = name.upper()
            if (re.fullmatch(r"META-INF/[^/]+\.(?:SF|RSA|DSA|EC)", upper)
                    or upper.startswith("META-INF/SIG-")):
                raise ValueError("signed JAR requires separate admission: " + name)
            if name.startswith((CUSTODY, "META-INF/versions/")) or name.endswith("module-info.class"):
                raise ValueError("foreign custody, versioned or modular OpenNLP input: " + name)
            if name.lower().endswith((".so", ".dll", ".dylib", ".jnilib", ".exe")):
                raise ValueError("native input requires separate admission: " + name)
            if not info.is_dir():
                entries[name] = archive.read(info)
    manifest = entries.get("META-INF/MANIFEST.MF", b"")
    # Manifest continuation lines must be unfolded before signature/sealing checks.
    manifest = re.sub(rb"\r?\n ", b"", manifest)
    if re.search(rb"(?im)^(?:Signature-Version:|[^:\r\n]*-Digest(?:-[^:]*)?:|(?:Sealed|Multi-Release):\s*true\s*$)", manifest):
        raise ValueError("signed or sealed manifest requires separate admission")
    for name in entries:
        if (name + "/" in seen or any(parent.as_posix() in entries
                for parent in Path(name).parents if parent.as_posix() != ".")):
            raise ValueError("file/directory archive collision: " + name)
    return {"path": path, "sha256": _sha(data), "entries": entries}


def _read_jar(path: Path) -> dict:
    archive = _read_archive(path)
    entries = archive["entries"]
    maven = [name for name in entries
             if name.startswith("META-INF/maven/" + GROUP + "/") and name.endswith("/pom.properties")]
    if len(maven) != 1:
        raise ValueError("one exact OpenNLP Maven coordinate required")
    props = _properties(entries[maven[0]])
    artifact = props.get("artifactId")
    if (props.get("groupId") != GROUP or props.get("version") != VERSION or artifact not in OWNERS
            or path.name != f"{artifact}-{VERSION}.jar"
            or maven[0] != f"META-INF/maven/{GROUP}/{artifact}/pom.properties"):
        raise ValueError("unexpected Maven artifact, classifier or version: " + path.name)
    return {"artifact": artifact, **archive}


def _legal(name: str) -> bool:
    upper = name.upper()
    return bool(re.fullmatch(r"(?:META-INF/)?(?:LICENSE|NOTICE|DEPENDENCIES)(?:\.[A-Z]+)?", upper)
                or upper.startswith("META-INF/LICENSES/"))


def _merge(archives: list[dict]) -> tuple[dict[str, bytes], list[dict], dict[str, list[str]]]:
    payload = {}
    custody = []
    providers = {}
    for archive in archives:
        owner = archive["artifact"]
        for name, data in sorted(archive["entries"].items()):
            target = name
            if name == "META-INF/MANIFEST.MF" or _legal(name) or name.startswith(SERVICES):
                target = CUSTODY + owner + "/" + name
            if target in payload and payload[target] != data:
                raise ValueError("conflicting payload bytes: " + name)
            payload[target] = data
            custody.append({"artifact": owner, "entry": name, "output": target, "sha256": _sha(data)})
            if name.startswith(SERVICES):
                service = name[len(SERVICES):]
                if not BINARY_NAME.fullmatch(service):
                    raise ValueError("invalid service name: " + service)
                values = providers.setdefault(service, [])
                for line in data.decode("utf-8").splitlines():
                    provider = line.partition("#")[0].strip()
                    if provider and not BINARY_NAME.fullmatch(provider):
                        raise ValueError("invalid provider name: " + provider)
                    if provider and provider not in values:
                        values.append(provider)
    for service, values in sorted(providers.items()):
        for name in [service, *values]:
            if name.replace(".", "/") + ".class" not in payload:
                raise ValueError("service type/provider absent from CPU aggregate: " + name)
        payload[SERVICES + service] = ("\n".join(values) + "\n").encode()
    for name in payload:
        if any(parent.as_posix() in payload for parent in Path(name).parents
               if parent.as_posix() != "."):
            raise ValueError("file/directory aggregate collision: " + name)
    return payload, custody, providers


def _descriptor_facts(root: Path, payload: dict[str, bytes], providers: dict) -> dict:
    packages = set()
    for name in payload:
        if not name.endswith(".class") or name.startswith("META-INF/"):
            continue
        package, separator, _ = name.rpartition("/")
        if not separator or not BINARY_NAME.fullmatch(package.replace("/", ".") + ".Type"):
            raise ValueError("unnamed or invalid package: " + name)
        packages.add(package.replace("/", "."))
    uses = set(providers)
    loads = []
    for owner in OWNERS.values():
        for source in sorted((root / owner / "src/main/java").rglob("*.java")):
            text = source.read_text(encoding="utf-8")
            package = re.search(r"(?m)^package\s+([\w.]+)\s*;", text)
            for match in re.finditer(r"public\s+interface\s+(\w+)\s+extends\s+Provider\s*<", text):
                if package is None:
                    raise ValueError("provider interface without package")
                uses.add(package[1] + "." + match[1])
            for match in re.finditer(r"ServiceLoader\.(?:load|loadInstalled)\s*\(", text):
                loads.append({"path": source.relative_to(root).as_posix(),
                    "line": text.count("\n", 0, match.start()) + 1,
                    "source_sha256": _sha(text.encode()),
                    "statement": text[match.start():text.find(";", match.start()) + 1].strip(),
                    "owner_module_add_uses_present": "addUses(spi)" in text})
    for service in uses:
        if service.replace(".", "/") + ".class" not in payload:
            raise ValueError("declared service use absent from aggregate: " + service)
    return {"module": MODULE, "open": True, "packages": sorted(packages),
            "exports": sorted(packages), "uses": sorted(uses), "provides": providers,
            "service_loads": loads,
            "dynamic_service_admission": "VERIFY_GENERIC_PROVIDERS_WITH_CUSTOM_SPI_IN_LINKED_IMAGE",
            "requires": "ROOT_JDEPS_AND_M3ModuleDescriptorRecipe_OWNED"}


def _write_jar(path: Path, payload: dict[str, bytes]) -> None:
    """Stored entries avoid dependence on compression-library versions."""
    order = sorted(payload, key=lambda name: (name != "META-INF/MANIFEST.MF", name))
    with zipfile.ZipFile(path, "x", compression=zipfile.ZIP_STORED) as archive:
        for name in order:
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, payload[name])


def prepare(opennlp_root: Path, jars: Path | Sequence[Path], output: Path, *,
            expected_source_head: str = PINNED_HEAD) -> dict:
    """Return paths and receipts for a fresh deterministic aggregate.

    A successor source commit must be admitted explicitly through
    expected_source_head; tracked bytes must equal their blobs or their exact
    committed text/eol=crlf checkout. Raw hashes and conversion facts are sealed.
    Failure may leave partial output, but never a successful MANIFEST.json.
    """
    root = Path(opennlp_root).resolve(strict=True)
    output = Path(output).absolute()
    if output.exists() or output.is_symlink():
        raise FileExistsError("fresh output directory required: " + str(output))
    if not output.parent.is_dir() or output.parent.resolve() != output.parent:
        raise ValueError("existing non-symlink output parent required")
    if output.is_relative_to(root):
        raise ValueError("output must be outside the OpenNLP source tree")
    if isinstance(jars, (str, Path)):
        directory = Path(jars).resolve(strict=True)
        if not directory.is_dir() or output.is_relative_to(directory):
            raise ValueError("JAR directory and external output required")
        paths = sorted(directory.glob("*.jar"))
    else:
        paths = [Path(path) for path in jars]
    if any(output.is_relative_to(path.parent.resolve()) for path in paths):
        raise ValueError("output must be outside JAR input directories")
    sources_before = _source_inventory(root, expected_source_head)
    dependencies = _pom_dependencies(root)
    archives = [_read_jar(path) for path in paths]
    by_owner = {archive["artifact"]: archive for archive in archives}
    if len(by_owner) != len(archives) or set(by_owner) != set(OWNERS):
        raise ValueError("exactly one main JAR for every selected CPU owner required")
    archives = [by_owner[owner] for owner in OWNERS]
    payload, custody, providers = _merge(archives)
    for name in ("LICENSE", "NOTICE"):
        payload["META-INF/" + name] = (root / name).read_bytes()
    payload["META-INF/MANIFEST.MF"] = (
        "Manifest-Version: 1.0\r\nAutomatic-Module-Name: " + MODULE
        + "\r\nMain-Class: opennlp.tools.cmdline.CLI\r\n\r\n").encode()
    facts = _descriptor_facts(root, payload, providers)
    output.mkdir()
    merged = output / (MODULE + ".jar")
    _write_jar(merged, payload)
    (output / "DESCRIPTOR_FACTS.json").write_bytes(_json(facts))
    source_lines = "".join(row["sha256"] + "  " + row["path"] + "\n" for row in sources_before)
    (output / "SOURCE.sha256").write_text(source_lines, encoding="utf-8", newline="\n")
    if _source_inventory(root, expected_source_head) != sources_before:
        raise ValueError("source drift during aggregation")
    for archive in archives:
        if _digest(archive["path"]) != archive["sha256"]:
            raise ValueError("JAR input drift during aggregation: " + archive["artifact"])
    hashes = {path.name: _digest(path) for path in sorted(output.iterdir())}
    manifest = {"schema": "m3-opennlp-cpu-aggregate-v1", "status": "PREPARED_NOT_RUNTIME_QUALIFIED",
        "module": MODULE, "source_head": expected_source_head,
        "source_inventory_sha256": hashes["SOURCE.sha256"], "source_file_count": len(sources_before),
        "declared_crlf_checkouts": [row for row in sources_before if "checkout" in row],
        "inputs": [{"artifact": a["artifact"], "owner": OWNERS[a["artifact"]],
                    "jar": a["path"].name, "sha256": a["sha256"]} for a in archives],
        "dependency_coordinates": dependencies, "excluded_separate_admission": EXCLUDED,
        "entries": custody, "outputs": hashes,
        "source_and_jar_posthashes_match": True,
        "jar_provenance_limit": "MAVEN_BUILD_RECEIPT_OWNED_BY_CALLER;_COORDINATES_ARE_NOT_BUILD_PROOF"}
    manifest_path = output / "MANIFEST.json"
    manifest_path.write_bytes(_json(manifest))
    return {"merged_jar": merged, "manifest": manifest_path,
            "descriptor_facts": output / "DESCRIPTOR_FACTS.json", "packages": facts["packages"],
            "services": providers, "dependency_coordinates": dependencies,
            "source_head": expected_source_head, "input_sha256": {
                a["artifact"]: a["sha256"] for a in archives}, "output_sha256": {
                **hashes, manifest_path.name: _digest(manifest_path)}}
