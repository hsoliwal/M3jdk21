#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Bounded, offline JMOD/jlink assembler. Upstream inputs are read-only and checksum-pinned."""
import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import stat
import subprocess
from pathlib import Path

STAMP = "2026-01-01T00:00:00Z"
FX_MODULES = {"javafx.base", "javafx.graphics", "javafx.controls"}
SDK_CDS = {"lib/server/classes.jsa": "-XX:+UseCompressedOops",
           "lib/server/classes_nocoops.jsa": "-XX:-UseCompressedOops"}
SDK_MODULE = re.compile(r"[A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)*\Z")


def digest(path):
    """Streaming byte identity atom."""
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def java_home(path):
    """Require an actual Java 21 development toolchain, not the ambient PATH."""
    home = path.resolve(strict=True)
    release = (home / "release").read_text()
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release, re.MULTILINE):
        raise ValueError("Java 21 toolchain required")
    for name in ("java", "javac", "jar", "jmod", "jlink"):
        if not (home / "bin" / name).is_file():
            raise ValueError("Missing JDK tool: " + name)
    return home


def load_fx(lock_path):
    """Validate supplied provenance/target/checksum facts; do not invent or fetch artifacts."""
    lock_path = lock_path.resolve(strict=True)
    lock = json.loads(lock_path.read_text())
    if lock.get("schema_version") != 1 or not str(lock.get("version", "")).startswith("21."):
        raise ValueError("Expected versioned JavaFX 21 lock")
    host_arch = {"amd64": "x86_64", "arm64": "aarch64"}.get(platform.machine(), platform.machine())
    if lock.get("os") != platform.system().lower() or lock.get("arch") != host_arch:
        raise ValueError("JavaFX native platform mismatch")
    artifacts = lock.get("artifacts", [])
    modules = [entry.get("module") for entry in artifacts]
    if len(modules) != 3 or set(modules) != FX_MODULES:
        raise ValueError("Exactly base, graphics and controls are required")
    result = []
    for entry in artifacts:
        checksum = entry.get("sha256", "")
        if not re.fullmatch(r"[0-9a-f]{64}", checksum):
            raise ValueError("Missing reviewed SHA-256")
        if not entry.get("license") or not entry.get("source"):
            raise ValueError("Missing source/license provenance")
        path = (lock_path.parent / entry["file"]).resolve(strict=True)
        if path.name != entry["module"] + ".jmod":
            raise ValueError("JMOD identity mismatch")
        if digest(path) != checksum:
            raise ValueError("JavaFX checksum mismatch: " + path.name)
        result.append((path, checksum))
    return lock, sorted(result)


def command(arguments, log, timeout=180):
    """No shell evaluation; preserve complete tool receipts and fail on nonzero exit."""
    run = subprocess.run([str(value) for value in arguments], text=True,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    log.write_text(run.stdout)
    if run.returncode:
        raise RuntimeError("Tool failed; see " + str(log))
    return run.stdout


def compile_module(home, source, classes, log, module_path=None):
    """Compile one source module using Java21, with no lint exclusions."""
    classes.mkdir()
    args = [home / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-d", classes]
    if module_path is not None:
        args += ["--module-path", module_path]
    command(args + sorted(source.rglob("*.java")), log)


def sdk_tree(root):
    """Inventory literal filesystem identities without traversing symlink directories."""
    root = root.resolve(strict=True)
    result = {}
    for path in sorted(root.rglob("*")):
        name = path.relative_to(root).as_posix()
        if "\\" in name or any(ord(char) < 32 or ord(char) == 127 for char in name):
            raise ValueError("Unsupported SDK asset path: " + repr(name))
        before = path.lstat()
        entry = {"mode": stat.S_IMODE(before.st_mode)}
        if stat.S_ISLNK(before.st_mode):
            target = os.readlink(path)
            if (not target or Path(target).is_absolute() or "\\" in target
                    or any(ord(char) < 32 or ord(char) == 127 for char in target)):
                raise ValueError("Unsafe SDK symlink: " + name)
            try:
                resolved = path.resolve(strict=True)
            except (OSError, RuntimeError) as failure:
                raise ValueError("Dangling or cyclic SDK symlink: " + name) from failure
            if not resolved.is_relative_to(root):
                raise ValueError("External SDK symlink: " + name)
            raw = target.encode("utf-8")
            entry.update(kind="symlink", target=target, bytes=len(raw),
                         sha256=hashlib.sha256(raw).hexdigest())
        elif stat.S_ISREG(before.st_mode):
            entry.update(kind="file", bytes=before.st_size, sha256=digest(path))
        elif stat.S_ISDIR(before.st_mode):
            entry.update(kind="directory")
        else:
            raise ValueError("Special SDK filesystem entry: " + name)
        after = path.lstat()
        identity = lambda value: (value.st_dev, value.st_ino, value.st_mode, value.st_size,
                                  value.st_mtime_ns, value.st_ctime_ns)
        if identity(before) != identity(after):
            raise ValueError("SDK asset changed during inventory: " + name)
        result[name] = entry
    return result


def sdk_release(text):
    """Read the existing release format as data; retain its original byte layout separately."""
    fields = {}
    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue
        match = re.fullmatch(r"([A-Za-z_][A-Za-z_0-9]*)=(.*)", line)
        if match is None or match[1] in fields:
            raise ValueError("Invalid or duplicate SDK release property")
        fields[match[1]] = match[2]
    value = fields.get("MODULES", "")
    if len(value) < 2 or not value.startswith('"') or not value.endswith('"'):
        raise ValueError("Missing quoted SDK release MODULES")
    modules = value[1:-1].split()
    if (not modules or len(modules) != len(set(modules)) or "java.base" not in modules
            or any(SDK_MODULE.fullmatch(name) is None for name in modules)):
        raise ValueError("Invalid SDK release module inventory")
    return fields, set(modules)


def sdk_module_versions(text):
    """Read java --list-modules without ignoring warnings, duplicates or malformed records."""
    result = {}
    for line in text.splitlines():
        name, separator, version = line.partition("@")
        if (SDK_MODULE.fullmatch(name) is None or name in result
                or (separator and (not version or any(char.isspace() for char in version)))):
            raise ValueError("Invalid SDK runtime module inventory: " + line)
        result[name] = version if separator else ""
    if not result:
        raise ValueError("Empty SDK runtime module inventory")
    return result


def sdk_preflight(home, tools, fx_artifacts):
    """Admit a complete, closed source SDK and reject selected-input collisions before output."""
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
        if os.environ.get(name):
            raise ValueError("Uncontrolled VM options for SDK profile: " + name)
    entries = sdk_tree(home)
    required = ("release", "lib/modules", "lib/src.zip", "include/jni.h", "include/jvmti.h",
                "include/linux/jni_md.h", "man/man1/java.1")
    for name in required:
        entry = entries.get(name)
        if entry is None or entry["kind"] not in ("file", "symlink") or not (home / name).is_file():
            raise ValueError("Incomplete source SDK asset: " + name)
    if entries["release"]["kind"] != "file" or entries["lib/modules"]["kind"] != "file":
        raise ValueError("SDK release and lib/modules must be regular files")
    release = (home / "release").read_bytes().decode("utf-8")
    _, release_modules = sdk_release(release)
    modules = set()
    for name, entry in entries.items():
        if name.endswith(".jmod"):
            path = Path(name)
            if len(path.parts) != 2 or path.parts[0] != "jmods" or entry["kind"] != "file":
                raise ValueError("Noncanonical source SDK JMOD: " + name)
            if SDK_MODULE.fullmatch(path.stem) is None:
                raise ValueError("Invalid source SDK JMOD name: " + name)
            modules.add(path.stem)
    if modules != release_modules:
        raise ValueError("Source SDK release/JMOD module inventory mismatch")
    cds = sorted(name for name, entry in entries.items()
                 if entry["kind"] != "directory" and name.lower().endswith(".jsa"))
    if any(name not in SDK_CDS or entries[name]["kind"] != "file" for name in cds):
        raise ValueError("Unsupported source SDK CDS archive")
    if cds and entries.get("lib/classlist", {}).get("kind") != "file":
        raise ValueError("Source SDK CDS requires a regular lib/classlist")
    if modules.intersection(item.module for item in tools.modules) or modules.intersection(
            path.stem for path, _ in fx_artifacts):
        raise ValueError("Selected pack collides with a source SDK module")
    for item in tools.assets:
        parts = Path(item.target).parts
        if item.target in entries or any(
                entries.get(Path(*parts[:index]).as_posix(), {}).get("kind") in ("file", "symlink")
                for index in range(1, len(parts))):
            raise ValueError("Selected pack collides with a source SDK asset: " + item.target)
    return {"entries": entries, "modules": sorted(modules), "release": release, "cds": cds,
            "root_mode": stat.S_IMODE(home.stat().st_mode)}


def link_arguments(home, module_path, roots, image, profile, baseline=None):
    """Reuse native jlink owners; retain the exact old profile options."""
    arguments = [home / "bin/jlink", "--module-path", module_path, "--add-modules", ",".join(roots)]
    if profile == "sdk":
        if baseline is None:
            raise ValueError("SDK source inventory required before linking")
        arguments += ["--release-info", home / "release", "--keep-packaged-modules", image / "jmods"]
        if baseline["cds"]:
            arguments.append("--generate-cds-archive")
    elif profile in ("diagnostics", "developer"):
        arguments += ["--strip-debug", "--no-header-files", "--no-man-pages"]
    else:
        raise ValueError("Unknown image profile: " + profile)
    return arguments + ["--output", image]


def sdk_release_bytes(original, linked, selected):
    """Keep original release bytes except the MODULES value supplied by the native plugin."""
    before, _ = sdk_release(original)
    after, linked_modules = sdk_release(linked)
    if linked_modules != set(selected):
        raise ValueError("Linked SDK release module inventory mismatch")
    if {key: value for key, value in before.items() if key != "MODULES"} != {
            key: value for key, value in after.items() if key != "MODULES"}:
        raise ValueError("Unaccounted SDK release property drift")
    lines = []
    for line in original.splitlines(keepends=True):
        if line.startswith("MODULES="):
            body = line.rstrip("\r\n")
            line = "MODULES=" + after["MODULES"] + line[len(body):]
        lines.append(line)
    return "".join(lines).encode("utf-8")


def sdk_restore_assets(home, image, baseline, selected):
    """Restore exact source assets; admit only byte-equivalent native legal-file aliases."""
    if sdk_tree(home) != baseline["entries"]:
        raise ValueError("Source SDK changed before asset retention")
    linked = sdk_tree(image)
    if linked.get("lib/modules", {}).get("kind") != "file" or linked.get("release", {}).get("kind") != "file":
        raise ValueError("Linked SDK runtime/release missing or nonregular")
    regenerated = {"release", "lib/modules", *SDK_CDS}
    expected_release = sdk_release_bytes(baseline["release"], (image / "release").read_bytes().decode("utf-8"), selected)
    aliases = set()
    for name, entry in baseline["entries"].items():
        if name in regenerated:
            continue
        actual = linked.get(name)
        if actual is None:
            continue
        if entry["kind"] == actual["kind"]:
            if entry["kind"] == "directory":
                continue
            if entry["kind"] == "file" and entry["sha256"] == actual["sha256"]:
                continue
            if entry["kind"] == "symlink" and entry["target"] == actual["target"]:
                continue
        source, destination = home / name, image / name
        if (name.startswith("legal/") and entry["kind"] != "directory" and actual["kind"] != "directory"
                and source.is_file() and destination.is_file() and digest(source) == digest(destination)):
            aliases.add(name)
            continue
        raise ValueError("Unaccounted linked SDK asset collision: " + name)
    for name, entry in baseline["entries"].items():
        if entry["kind"] == "directory":
            (image / name).mkdir(parents=True, exist_ok=True)
    for kind in ("file", "symlink"):
        for name, entry in baseline["entries"].items():
            if entry["kind"] != kind or name in regenerated:
                continue
            source, destination = home / name, image / name
            if name in aliases:
                destination.unlink()
            if not destination.exists() and not destination.is_symlink():
                if kind == "file":
                    shutil.copy2(source, destination)
                else:
                    destination.symlink_to(entry["target"])
            shutil.copystat(source, destination, follow_symlinks=False)
    (image / "release").write_bytes(expected_release)
    return {"linked": linked, "release_sha256": hashlib.sha256(expected_release).hexdigest(),
            "legal_aliases_restored": sorted(aliases)}


def sdk_verify_cds(image, baseline, log):
    """Verify native-plugin archives explicitly; never copy or silently accept an old archive."""
    archives = sorted(path.relative_to(image).as_posix() for path in image.rglob("*")
                      if not path.is_dir() and path.name.lower().endswith(".jsa"))
    if (any(name not in SDK_CDS or (image / name).is_symlink() for name in archives)
            or (not baseline["cds"] and archives) or not set(baseline["cds"]).issubset(archives)):
        raise ValueError("Unaccounted or missing composed SDK CDS archive")
    results = []
    for name in archives:
        archive = image / name
        if not archive.is_file() or not archive.stat().st_size:
            raise ValueError("Empty composed SDK CDS archive: " + name)
        arguments = [image / "bin/java", "-Xms128m", "-Xmx128m", SDK_CDS[name],
                     "-XX:SharedArchiveFile=" + str(archive), "-Xshare:on", "-version"]
        target = log / (Path(name).name + ".verify.log")
        command(arguments, target)
        results.append({"path": name, "source_sha256": baseline["entries"].get(name, {}).get("sha256"),
                        "image_sha256": digest(archive), "generation": "native-jlink-generate-cds-archive",
                        "generation_options": ["-Xshare:dump"] + (["-XX:-UseCompressedOops"]
                            if name.endswith("_nocoops.jsa") else []),
                        "verification_arguments": [str(value) for value in arguments],
                        "verification_log": target.name})
    return results


def sdk_finish(home, image, output, baseline, retained, selected, tools, cds, versions):
    """Prove complete source coverage, exact retained JMODs and a closed final image frontier."""
    for name, entry in sorted(baseline["entries"].items(), reverse=True):
        if entry["kind"] == "directory":
            shutil.copystat(home / name, image / name, follow_symlinks=False)
        elif name in {"release", "lib/modules", *SDK_CDS} and (image / name).is_file():
            (image / name).chmod(entry["mode"])
    image.chmod(baseline["root_mode"])
    if sdk_tree(home) != baseline["entries"] or stat.S_IMODE(home.stat().st_mode) != baseline["root_mode"]:
        raise ValueError("Source SDK changed during composition")
    final = sdk_tree(image)
    expected_paths = set(baseline["entries"]) | set(retained["linked"])
    for item in tools.assets:
        expected_paths.add(item.target)
        expected_paths.update(str(parent) for parent in Path(item.target).parents if str(parent) != ".")
    if set(final) != expected_paths:
        raise ValueError("Unaccounted final SDK asset path drift")
    regenerated = {"release", "lib/modules", *SDK_CDS}
    for name, entry in baseline["entries"].items():
        if name not in regenerated and final.get(name) != entry:
            raise ValueError("Original SDK asset not retained exactly: " + name)
    if final["release"]["sha256"] != retained["release_sha256"]:
        raise ValueError("Composed SDK release changed after retention")
    if final["lib/modules"]["sha256"] != retained["linked"]["lib/modules"]["sha256"]:
        raise ValueError("Composed SDK runtime changed after linking")
    final_jmods = {Path(name).stem: entry["sha256"] for name, entry in final.items()
                   if name.startswith("jmods/") and name.endswith(".jmod") and entry["kind"] == "file"}
    if final_jmods != selected:
        raise ValueError("Installed SDK JMOD names or hashes differ from selected inventory")
    for name, entry in retained["linked"].items():
        if name not in baseline["entries"] and name not in SDK_CDS and final[name] != entry:
            raise ValueError("Selected linked SDK asset changed: " + name)
    for item in tools.assets:
        entry = final[item.target]
        if entry["kind"] != "file" or entry["sha256"] != item.sha256 or entry["mode"] != (0o755 if item.executable else 0o644):
            raise ValueError("Selected SDK tool asset changed: " + item.target)
    for archive in cds:
        if final[archive["path"]]["sha256"] != archive["image_sha256"]:
            raise ValueError("Verified SDK CDS archive changed")
    for filename, entries in (("SDK_SOURCE_ASSETS.json", baseline["entries"]), ("SDK_IMAGE_ASSETS.json", final)):
        (output / filename).write_text(json.dumps(entries, indent=2, sort_keys=True) + "\n")
    coverage = ["path\tsource_kind\timage_kind\tsource_mode\timage_mode\tsource_sha256\timage_sha256"
                "\tsource_link_target\timage_link_target\tdisposition\n"]
    for name, entry in final.items():
        original = baseline["entries"].get(name, {})
        disposition = "PRESERVED_SOURCE" if original else "SELECTED_LINKED_RESOURCE"
        if name == "release":
            disposition = "MODULES_VALUE_REGENERATED"
        elif name == "lib/modules":
            disposition = "RUNTIME_RELINKED"
        elif name in SDK_CDS:
            disposition = "CDS_REGENERATED_AND_VERIFIED"
        elif name in {item.target for item in tools.assets}:
            disposition = "SELECTED_TOOL_ASSET"
        cells = [name, original.get("kind", "ABSENT"), entry["kind"],
                 format(original["mode"], "04o") if original else "-", format(entry["mode"], "04o"),
                 original.get("sha256", "-"), entry.get("sha256", "-"),
                 original.get("target", "-"), entry.get("target", "-"), disposition]
        coverage.append("\t".join(cells) + "\n")
    (output / "SDK_ASSET_COVERAGE.tsv").write_text("".join(coverage))
    return {"status": "LOCAL_FULL_SDK_PROFILE_PROVEN", "source_modules": baseline["modules"],
            "installed_modules": sorted(versions), "source_asset_entries": len(baseline["entries"]),
            "image_asset_entries": len(final), "source_release_sha256": baseline["entries"]["release"]["sha256"],
            "image_release_sha256": final["release"]["sha256"], "release_change": "MODULES_VALUE_ONLY",
            "source_assets_sha256": digest(output / "SDK_SOURCE_ASSETS.json"),
            "image_assets_sha256": digest(output / "SDK_IMAGE_ASSETS.json"),
            "coverage_sha256": digest(output / "SDK_ASSET_COVERAGE.tsv"),
            "all_source_assets_accounted": True, "source_tree_unchanged": True,
            "jmods": final_jmods, "cds": cds, "legal_aliases_restored": retained["legal_aliases_restored"],
            "stripping": "NONE", "m3jdk21_product_acceptance": False}


def main(argv=None):
    """Assemble only into a fresh explicit directory; never install into the supplied JDK."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--javafx-lock", type=Path)
    parser.add_argument("--tool-pack-lock", type=Path,
                        help="Offline, checksum-pinned optional build-tool inputs")
    parser.add_argument("--tool-pack", action="append", default=[], metavar="NAME",
                        help="Explicit optional tool selection; repeat to compose packs")
    parser.add_argument("--profile", choices=("diagnostics", "developer", "sdk"), default="diagnostics")
    args = parser.parse_args(argv)
    home = java_home(args.jdk)
    if args.profile == "developer" and platform.system() != "Linux":
        raise ValueError("Developer image/native launcher proof is Linux-only in this packet")
    if args.profile == "sdk" and platform.system() != "Linux":
        raise ValueError("Full SDK image/native launcher proof is Linux-only in this packet")
    output = args.out.absolute()
    if output.resolve().is_relative_to(home):
        raise ValueError("Output inside JAVA_HOME is forbidden")
    fx_lock, fx_artifacts = load_fx(args.javafx_lock) if args.javafx_lock else (None, [])
    from tool_packs import (copy_selected_modules, install_assets, inventory_modules,
                            load_tool_packs)
    tools = load_tool_packs(args.tool_pack_lock, args.tool_pack)
    baseline = sdk_preflight(home, tools, fx_artifacts) if args.profile == "sdk" else None
    output.mkdir(parents=True, exist_ok=False)
    log = output / "logs"
    log.mkdir()
    if baseline is not None:
        source_modules = sdk_module_versions(command([home / "bin/java", "--list-modules"],
                                                    log / "source-sdk-modules.log"))
        if set(source_modules) != set(baseline["modules"]):
            raise ValueError("Source SDK runtime/JMOD module inventory mismatch")
    jmods = output / "jmods"
    jmods.mkdir()
    source = Path(__file__).resolve().parent
    classes = output / "tool-classes"
    compile_module(home, source / "kernel/src/main/java", classes, log / "tool-compile.log")
    tool_jar = output / "m3-module-pack.jar"
    command([home / "bin/jar", "--create", "--file", tool_jar, "--date", STAMP,
             "-C", classes, ".", "-C", source / "legal", "."], log / "jar.log")
    command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", tool_jar,
             "--legal-notices", source / "legal", jmods / "com.m3tooling.modulepack.jmod"], log / "tool-jmod.log")
    for path, checksum in fx_artifacts:
        target = jmods / path.name
        shutil.copyfile(path, target)
        if digest(target) != checksum:
            raise ValueError("Input drift during copy: " + str(path))
    module_path = os.pathsep.join([str(home / "jmods"), str(jmods)])
    smoke_classes = output / "diagnostics-classes"
    compile_module(home, source / "fixtures/diagnostics", smoke_classes,
                   log / "diagnostics-compile.log")
    command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", smoke_classes,
             "--legal-notices", source / "legal", jmods / "com.m3pack.diagnostics.jmod"], log / "diagnostics-jmod.log")
    roots = ["com.m3tooling.modulepack", "com.m3pack.diagnostics", "jdk.jcmd", "jdk.jdeps"]
    if args.profile == "developer":
        from developer_tools import DEVELOPER_ROOTS
        roots = sorted(set(roots).union(DEVELOPER_ROOTS))
    elif baseline is not None:
        roots = baseline["modules"].copy()
    if tools.names:
        roots = sorted(set(roots).union(tools.roots))
    if fx_lock:
        fx_classes = output / "javafx-classes"
        compile_module(home, source / "fixtures/javafx", fx_classes,
                       log / "javafx-compile.log", module_path)
        command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", fx_classes,
                 "--legal-notices", source / "legal", jmods / "com.m3pack.fx.jmod"], log / "javafx-jmod.log")
        if baseline is not None:
            roots = sorted(set(roots).union(FX_MODULES))
        else:
            roots.append("com.m3pack.fx")
    inventory = [home / "bin/java", "-cp", classes, "jdk.m3.tools.modulepack.M3ModuleInventory",
                 ",".join(roots), home / "jmods", jmods,
                 *[item.path for item in tools.modules]]
    first = command(inventory, output / "MODULES.tsv")
    if command(inventory, log / "inventory-replay.tsv") != first:
        raise ValueError("Inventory fixed-point mismatch")
    selected_modules = copy_selected_modules(tools, first, jmods)
    if tools.names:
        installed_inventory = command(
            [home / "bin/java", "-cp", classes, "jdk.m3.tools.modulepack.M3ModuleInventory",
             ",".join(roots), home / "jmods", jmods], log / "installed-inventory.tsv")
        if inventory_modules(installed_inventory) != selected_modules:
            raise ValueError("Installed tool module closure differs from admitted inventory")
    if fx_lock:
        observed = {line.split("\t")[0]: line.split("\t")[1] for line in first.splitlines()[1:]}
        for name in FX_MODULES:
            if observed.get(name) != fx_lock["version"]:
                raise ValueError("Locked JavaFX descriptor version mismatch")
    for archive in sorted(jmods.glob("*.jmod")):
        command([home / "bin/jmod", "describe", archive], log / (archive.stem + ".describe.log"))
    image = output / "image"
    command(link_arguments(home, module_path, roots, image, args.profile, baseline), log / "jlink.log")
    listed = command([image / "bin/java", "--list-modules"], log / "image-modules.log")
    retained = None
    if baseline is not None:
        versions = sdk_module_versions(listed)
        if set(versions) != set(selected_modules) or any(
                versions.get(name) != version for name, version in source_modules.items()):
            raise ValueError("Full SDK installed module inventory or original version mismatch")
        retained = sdk_restore_assets(home, image, baseline, selected_modules)
    install_assets(tools, image)
    smoke_path = ["--module-path", smoke_classes] if baseline is not None else []
    smoke = command([image / "bin/java", "-Xcheck:jni", *smoke_path, "-m",
                     "com.m3pack.diagnostics/jdk.m3.pack.diagnostics.M3DiagnosticsSmoke",
                     output / "probe.jfr"], log / "diagnostics-run.log")
    if "M3_DIAGNOSTICS_PASS" not in smoke:
        raise ValueError("Missing diagnostic proof")
    if fx_lock:
        fx_path = ["--module-path", fx_classes] if baseline is not None else []
        fx = command([image / "bin/java", "-Xcheck:jni", *fx_path, "-m",
                      "com.m3pack.fx/jdk.m3.pack.fx.M3FxSmoke"], log / "javafx-run.log", 60)
        if "M3_JAVAFX_PASS" not in fx:
            raise ValueError("Missing JavaFX native/toolkit proof")
    developer = None
    if args.profile == "developer":
        from developer_tools import copy_link_inputs, verify_developer_tools, verify_link_pins
        copied = copy_link_inputs(home, output)
        verify_link_pins(first, copied, home)
        developer = verify_developer_tools(image, output, output / "link-inputs")
    sdk = None
    if baseline is not None:
        from developer_tools import verify_developer_tools, verify_link_pins
        copied = [(name + ".jmod", digest(image / "jmods" / (name + ".jmod")))
                  for name in baseline["modules"]]
        verify_link_pins(first, copied, home)
        command([image / "bin/java", "--validate-modules"], log / "sdk-validate-modules.log")
        cds = sdk_verify_cds(image, baseline, log)
        developer = verify_developer_tools(image, output, image / "jmods")
        sdk = sdk_finish(home, image, output, baseline, retained, selected_modules, tools, cds, versions)
    release = (home / "release").read_text()
    if baseline is not None:
        (output / "JDK_RELEASE.txt").write_bytes(baseline["release"].encode("utf-8"))
    else:
        (output / "JDK_RELEASE.txt").write_text(release)
    receipt = {"status": "LOCAL_IMAGE_PROVEN", "jdk_release_sha256": digest(home / "release"),
               "javafx": fx_lock, "roots": roots, "profile": args.profile, "developer": developer, "m3jdk21_product_acceptance": False,
               "maven_junit_jacoco": "NOT_EXECUTED_BY_THIS_SCRIPT"}
    if sdk is not None:
        receipt["sdk"] = sdk
    if tools.names:
        receipt["tool_packs"] = tools.receipt(selected_modules)
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    files = sorted(path for path in image.rglob("*") if path.is_file())
    (output / "IMAGE_SHA256.tsv").write_text("sha256\tpath\n" + "".join(
        digest(path) + "\t" + path.relative_to(image).as_posix() + "\n" for path in files))
    print("M3_MODULE_PACK_PASS", output)


if __name__ == "__main__":
    main()
