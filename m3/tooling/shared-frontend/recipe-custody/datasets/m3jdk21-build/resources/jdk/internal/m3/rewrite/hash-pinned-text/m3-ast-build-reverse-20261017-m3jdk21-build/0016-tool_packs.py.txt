# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Offline optional tool inputs. Descriptor closure remains owned by M3ModuleGraph.

A pack supplies explicit roots, a complete candidate JMOD set and retained runtime
files. The original tool's distribution/classloader layout is preserved. Merely
packaging a distribution does not certify its build integration or AST consumption.
"""
import csv
import hashlib
import io
import json
import re
import shutil
from dataclasses import dataclass
from pathlib import Path, PurePosixPath

MODULE = re.compile(r"[A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)*\Z")
PACK = re.compile(r"[a-z][a-z0-9-]*\Z")
SHA256 = re.compile(r"[0-9a-f]{64}\Z")
ASSET_ROOTS = frozenset(("bin", "conf", "lib", "legal"))


def digest(path):
    """Stream a byte identity without retaining an entire distribution in memory."""
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


@dataclass(frozen=True)
class LockedModule:
    module: str
    path: Path
    sha256: str


@dataclass(frozen=True)
class LockedAsset:
    path: Path
    target: str
    sha256: str
    executable: bool


@dataclass(frozen=True)
class ToolPacks:
    names: tuple = ()
    roots: tuple = ()
    modules: tuple = ()
    assets: tuple = ()
    provenance: tuple = ()
    lock_sha256: str = ""

    def receipt(self, selected_modules):
        """Report packaging evidence separately from execution/AST acceptance."""
        return {
            "packs": list(self.names), "roots": list(self.roots),
            "lock_sha256": self.lock_sha256,
            "provenance": list(self.provenance),
            "modules": [{"module": item.module, "sha256": item.sha256}
                        for item in self.modules if item.module in selected_modules],
            "assets": [{"target": item.target, "sha256": item.sha256,
                        "executable": item.executable} for item in self.assets],
            "tool_execution": "NOT_EXECUTED_BY_ASSEMBLER",
            "shared_ast_consumption": "NOT_ESTABLISHED_BY_PACKAGING",
        }


def _list(value, field):
    if not isinstance(value, list):
        raise ValueError("Expected list: " + field)
    return value


def _name(value, pattern, field):
    if not isinstance(value, str) or not pattern.fullmatch(value):
        raise ValueError("Invalid " + field)
    return value


def _input(root, entry):
    """Validate a reviewed regular file and its byte pin before creating output."""
    if not isinstance(entry, dict):
        raise ValueError("Invalid locked input")
    file = entry.get("file")
    checksum = entry.get("sha256")
    if not isinstance(file, str) or not file or not isinstance(checksum, str) or not SHA256.fullmatch(checksum):
        raise ValueError("Locked input requires file and SHA-256")
    path = (root / file).resolve(strict=True)
    if not path.is_file() or digest(path) != checksum:
        raise ValueError("Tool input checksum mismatch: " + file)
    return path, checksum


def _target(value):
    """Keep installed assets inside the image with portable, unambiguous paths."""
    if not isinstance(value, str) or not value or "\\" in value or "\x00" in value:
        raise ValueError("Invalid tool asset target")
    path = PurePosixPath(value)
    if (path.is_absolute() or str(path) != value or len(path.parts) < 2
            or path.parts[0] not in ASSET_ROOTS
            or any(part in (".", "..") or ":" in part for part in path.parts)
            or any(ord(char) < 32 for char in value)):
        raise ValueError("Unsafe tool asset target: " + value)
    return value


def load_tool_packs(lock_path, selections):
    """Open only selected packs; never infer selection from an available lock.

    Every selected pack explicitly lists its candidate dependency JMODs. Unselected
    pack artifacts may be absent. Shared inputs are deduplicated only when their
    content identities and installed modes agree.
    """
    names = tuple(sorted(set(selections)))
    if not names and lock_path is None:
        return ToolPacks()
    if lock_path is None or not names:
        raise ValueError("--tool-pack-lock and explicit --tool-pack selections are required together")
    lock_path = Path(lock_path).resolve(strict=True)
    lock_bytes = lock_path.read_bytes()
    lock = json.loads(lock_bytes)
    if not isinstance(lock, dict) or type(lock.get("schema_version")) is not int or lock["schema_version"] != 1:
        raise ValueError("Expected tool pack lock schema_version 1")
    packs = {}
    for pack in _list(lock.get("packs"), "packs"):
        if not isinstance(pack, dict):
            raise ValueError("Invalid pack")
        name = _name(pack.get("name"), PACK, "pack name")
        if name in packs:
            raise ValueError("Duplicate tool pack: " + name)
        packs[name] = pack
    if set(names) - packs.keys():
        raise ValueError("Unknown tool pack: " + ",".join(sorted(set(names) - packs.keys())))
    roots, modules, assets, provenance = set(), {}, {}, []
    for name in names:
        pack = packs[name]
        for field in ("version", "source", "license"):
            if not isinstance(pack.get(field), str) or not pack[field].strip():
                raise ValueError("Missing tool provenance: " + name + ": " + field)
        provenance_row = {field: pack[field] for field in ("name", "version", "source", "license")}
        provenance.append(provenance_row)
        pack_roots = _list(pack.get("roots"), name + ".roots")
        if not pack_roots:
            raise ValueError("Tool pack requires explicit module roots: " + name)
        roots.update(_name(root, MODULE, "module root") for root in pack_roots)
        local_modules, local_targets, distribution_entries = set(), set(), []
        for entry in _list(pack.get("modules", []), name + ".modules"):
            if not isinstance(entry, dict):
                raise ValueError("Invalid tool module")
            module = _name(entry.get("module"), MODULE, "module name")
            if module in local_modules:
                raise ValueError("Duplicate module within tool pack: " + module)
            local_modules.add(module)
            path, checksum = _input(lock_path.parent, entry)
            if path.name != module + ".jmod":
                raise ValueError("Tool JMOD filename mismatch: " + module)
            item = LockedModule(module, path, checksum)
            previous = modules.setdefault(module, item)
            if previous.sha256 != item.sha256:
                raise ValueError("Conflicting tool module: " + module)
        for entry in _list(pack.get("assets", []), name + ".assets"):
            if not isinstance(entry, dict):
                raise ValueError("Invalid tool asset")
            target = _target(entry.get("target"))
            if target in local_targets:
                raise ValueError("Duplicate target within tool pack: " + target)
            local_targets.add(target)
            path, checksum = _input(lock_path.parent, entry)
            executable = entry.get("executable", False)
            if type(executable) is not bool:
                raise ValueError("Asset executable must be a boolean")
            item = LockedAsset(path, target, checksum, executable)
            prefix = "lib/m3-tools/" + name + "/"
            if target.startswith(prefix):
                distribution_entries.append(target[len(prefix):] + "\t" + checksum
                                            + "\t" + ("755" if executable else "644") + "\n")
            previous = assets.setdefault(target, item)
            if (previous.sha256, previous.executable) != (item.sha256, item.executable):
                raise ValueError("Conflicting tool asset: " + target)
        if "distribution" in pack:
            distribution = pack["distribution"]
            if not isinstance(distribution, dict):
                raise ValueError("Invalid distribution inventory")
            tree_hash = hashlib.sha256("".join(sorted(distribution_entries)).encode("utf-8")).hexdigest()
            if (type(distribution.get("file_count")) is not int
                    or distribution["file_count"] != len(distribution_entries)
                    or distribution.get("tree_sha256") != tree_hash):
                raise ValueError("Tool distribution inventory mismatch: " + name)
            provenance_row["distribution"] = {"file_count": len(distribution_entries),
                                               "tree_sha256": tree_hash,
                                               "source_build_acceptance": "NOT_ESTABLISHED_BY_INPUT_INVENTORY"}
    targets = set(assets)
    for target in targets:
        if any(str(parent) in targets for parent in PurePosixPath(target).parents):
            raise ValueError("Tool asset file/directory collision: " + target)
    if lock_path.read_bytes() != lock_bytes:
        raise ValueError("Tool pack lock changed during admission")
    return ToolPacks(names, tuple(sorted(roots)), tuple(modules[k] for k in sorted(modules)),
                     tuple(assets[k] for k in sorted(assets)), tuple(provenance),
                     hashlib.sha256(lock_bytes).hexdigest())


def inventory_modules(inventory):
    """Read the existing kernel's TSV; do not implement another module resolver."""
    rows = list(csv.DictReader(io.StringIO(inventory), delimiter="\t"))
    result = {}
    for row in rows:
        name, checksum = row.get("module"), row.get("sha256")
        _name(name, MODULE, "inventory module")
        if name in result or not isinstance(checksum, str) or not SHA256.fullmatch(checksum):
            raise ValueError("Invalid or duplicate module inventory")
        result[name] = checksum
    if not result:
        raise ValueError("Empty module inventory")
    return result


def _copy(input_path, output_path, checksum, executable=False):
    """Bind read-only input, copied bytes and post-copy input to the same identity."""
    if digest(input_path) != checksum:
        raise ValueError("Tool input drift before copy: " + str(input_path))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with input_path.open("rb") as source, output_path.open("xb") as target:
        shutil.copyfileobj(source, target)
    if digest(input_path) != checksum or digest(output_path) != checksum:
        raise ValueError("Tool input drift during copy: " + str(input_path))
    output_path.chmod(0o755 if executable else 0o644)


def copy_selected_modules(selection, inventory, destination):
    """Materialize only optional archives in the actual selected descriptor closure."""
    selected = inventory_modules(inventory)
    for item in selection.modules:
        if item.module in selected:
            if selected[item.module] != item.sha256:
                raise ValueError("Tool descriptor identity drift: " + item.module)
            _copy(item.path, destination / (item.module + ".jmod"), item.sha256)
    return selected


def install_assets(selection, image):
    """Retain original distribution layout; refuse image collisions before any copy."""
    image = image.resolve(strict=True)
    for item in selection.assets:
        target = image / item.target
        if target.exists() or target.is_symlink():
            raise FileExistsError("Tool asset collides with linked image: " + item.target)
        parent = target.parent
        while parent != image:
            if parent.is_symlink() or (parent.exists() and not parent.is_dir()):
                raise ValueError("Unsafe existing tool asset parent: " + item.target)
            parent = parent.parent
        if digest(item.path) != item.sha256:
            raise ValueError("Tool asset drift before installation: " + item.target)
    for item in selection.assets:
        _copy(item.path, image / item.target, item.sha256, item.executable)
