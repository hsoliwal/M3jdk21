#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD step 3: package every vendored library as a JMOD and jlink one image.
Named modules come from JMOD_IMAGE.tsv (published descriptors); the rest get the descriptor that the OpenRewrite
recipe M3ModuleDescriptorRecipe wrote into the M3 refactor layer (DESCRIPTOR_MANIFEST.tsv target paths under --layer-root).
Per descriptor module: javac --patch-module <module>=<unit jar>, copy the requested release view plus
module-info.class into a class tree, jmod create. Then jlink java.se + every vendored module, and validate."""
import argparse, csv, io, json, os, re, shutil, stat, subprocess, sys, zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from jmod_image import JDK, flatten_release  # noqa: E402


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def _main_attributes(data):
    """Read only the unfolded main manifest section; refuse ambiguous declarations."""
    attributes, current = {}, None
    for line in data.decode("utf-8").replace("\r\n", "\n").split("\n"):
        if not line:
            break
        if line.startswith(" "):
            if current is None:
                raise ValueError("orphan manifest continuation")
            attributes[current] += line[1:]
            continue
        key, separator, value = line.partition(": ")
        key = key.lower()
        if not separator or not re.fullmatch(r"[a-z0-9_-]+", key) or key in attributes:
            raise ValueError("invalid or duplicate main manifest attribute")
        attributes[key], current = value, key
    return attributes


def validate_jar(jar: Path, release: int) -> None:
    """Read-only admission for the existing release flattener; never writes a second view."""
    jar = Path(jar)
    if release < 9:
        raise ValueError("JMOD release must be at least 9")
    if not jar.is_file():
        raise ValueError("input JAR missing: " + str(jar))
    with zipfile.ZipFile(jar) as archive:
        paths, files, effective = set(), set(), {}
        manifest, has_versions = None, False
        for entry in archive.infolist():
            name = entry.filename
            path = name[:-1] if name.endswith("/") else name
            parts = path.split("/")
            if (entry.orig_filename != name or not path or name.startswith("/")
                    or "\\" in name or ":" in name
                    or any(part in ("", ".", "..") for part in parts)
                    or any(ord(char) < 32 or 127 <= ord(char) < 160 for char in name)):
                raise ValueError("unsafe ZIP path: " + repr(name))
            if path in paths:
                raise ValueError("duplicate ZIP path: " + name)
            paths.add(path)
            mode = entry.external_attr >> 16
            empty_dos_directory = (entry.is_dir() and entry.create_system == 3
                    and entry.external_attr == 0xffff0010 and entry.flag_bits == 0
                    and entry.compress_type == zipfile.ZIP_STORED
                    and entry.file_size == 0 and entry.compress_size == 0 and entry.CRC == 0
                    and archive.read(entry) == b"")
            if entry.flag_bits & 1 or (stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR)
                                      and not empty_dos_directory):
                raise ValueError("encrypted or special ZIP entry: " + name)
            upper = name.upper()
            if upper.startswith("META-INF/"):
                leaf = upper[len("META-INF/"):]
                if "/" not in leaf and (leaf.startswith("SIG-") or leaf.endswith((".SF", ".RSA", ".DSA", ".EC"))):
                    raise ValueError("signed JAR metadata requires separate admission: " + name)
            if upper == "META-INF/MANIFEST.MF":
                if name != "META-INF/MANIFEST.MF" or manifest is not None or entry.is_dir():
                    raise ValueError("noncanonical or repeated JAR manifest")
                manifest = entry
            version, logical = 0, name
            if parts[:2] == ["META-INF", "versions"]:
                if len(parts) == 2:
                    if not entry.is_dir():
                        raise ValueError("invalid versioned ZIP path: " + name)
                    continue
                has_versions = True
                if not parts[2].isdigit() or str(int(parts[2])) != parts[2] or int(parts[2]) < 9:
                    raise ValueError("invalid versioned ZIP release: " + name)
                version = int(parts[2])
                if len(parts) == 3:
                    if not entry.is_dir():
                        raise ValueError("invalid versioned ZIP path: " + name)
                    continue
                logical = "/".join(parts[3:])
                if parts[3].upper() == "META-INF":
                    raise ValueError("versioned META-INF resource is not admitted: " + name)
            if entry.is_dir():
                continue
            files.add(path)
            if version <= release and (logical not in effective or effective[logical][0] < version):
                effective[logical] = (version, entry)
        for path in paths:
            parts = path.split("/")
            if any("/".join(parts[:index]) in files for index in range(1, len(parts))):
                raise ValueError("ZIP file/directory conflict: " + path)
        attributes = _main_attributes(archive.read(manifest)) if manifest is not None else {}
        if has_versions and attributes.get("multi-release", "").lower() != "true":
            raise ValueError("versioned entries require Multi-Release: true")
        for logical in effective:
            parts = logical.split("/")
            if any("/".join(parts[:index]) in effective for index in range(1, len(parts))):
                raise ValueError("effective ZIP file/directory conflict: " + logical)
        for logical, (_, entry) in effective.items():
            if logical.endswith(".class"):
                with archive.open(entry) as stream:
                    header = stream.read(8)
                if len(header) != 8 or header[:4] != b"\xca\xfe\xba\xbe":
                    raise ValueError("invalid class header: " + entry.filename)
                major = int.from_bytes(header[6:8], "big")
                if major < 45 or major > release + 44:
                    raise ValueError("effective class major " + str(major) + " exceeds or invalidates release "
                                     + str(release) + ": " + entry.filename)


def _rows(path, required):
    with io.open(path, encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        header = reader.fieldnames or []
        if len(header) != len(set(header)) or not set(required).issubset(header):
            raise ValueError("invalid TSV header: " + str(path))
        rows = list(reader)
    if any(None in row or any(row.get(key) is None for key in header) for row in rows):
        raise ValueError("invalid TSV row: " + str(path))
    return rows


def _module_name(name):
    if not re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)*", name):
        raise ValueError("unsafe module name: " + repr(name))


def select_runtime(jdk, output, candidates, roots):
    """Use the existing inspected-descriptor graph; copy only its runtime closure.

    Descriptor generation/compilation remains the existing packer's responsibility.
    Every supplied candidate is still validated. Use selected input manifests when
    unrelated tool distributions must not participate in that materialization work.
    """
    owner = Path(__file__).resolve().parents[2] / "module-packs"
    sys.path.insert(0, str(owner))
    from tool_packs import (LockedModule, ToolPacks, copy_selected_modules, digest,
                            inventory_modules)
    classes = output / "selector-classes"
    sources = sorted((owner / "kernel/src/main/java").rglob("*.java"))
    if not sources:
        raise ValueError("Missing existing M3ModuleGraph source owner")
    compiled = run(jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                   "-d", classes, *sources)
    (output / "selection-compile.log").write_text(compiled.stdout + compiled.stderr)
    if compiled.returncode:
        raise ValueError("Module selection kernel compilation failed")
    command = [jdk / "bin/java", "-cp", classes, "com.m3.tools.modulepack.M3ModuleInventory",
               ",".join(roots), jdk / "jmods", candidates]
    inventory = run(*command)
    (output / "MODULES.tsv").write_text(inventory.stdout)
    (output / "selection.log").write_text(inventory.stderr)
    if inventory.returncode:
        raise ValueError("Selected module closure refused; see selection.log")
    replay = run(*command)
    if replay.returncode or replay.stdout != inventory.stdout:
        raise ValueError("Selected module inventory fixed-point mismatch")
    modules = tuple(LockedModule(path.stem, path, digest(path))
                    for path in sorted(candidates.glob("*.jmod")))
    destination = output / "jmods"
    destination.mkdir()
    selected = copy_selected_modules(ToolPacks(modules=modules), inventory.stdout, destination)
    installed = run(jdk / "bin/java", "-cp", classes, "com.m3.tools.modulepack.M3ModuleInventory",
                    ",".join(roots), jdk / "jmods", destination)
    if installed.returncode or inventory_modules(installed.stdout) != selected:
        raise ValueError("Copied JMOD runtime closure mismatch")
    names = [item.module for item in modules if item.module in selected]
    (output / "SELECTION.json").write_text(json.dumps({
        "roots": roots, "selected_vendored_modules": names,
        "supplied_candidates": len(modules),
        "candidate_materialization": "ALL_SUPPLIED_MANIFEST_INPUTS_VALIDATED",
        "resolver": "com.m3.tools.modulepack.M3ModuleGraph",
    }, indent=2, sort_keys=True) + "\n")
    return destination, names


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--image-tsv", type=Path, required=True)
    ap.add_argument("--manifest", type=Path, required=True)
    ap.add_argument("--layer-root", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", str(JDK))))
    ap.add_argument("--add-modules", default="java.se")
    ap.add_argument("--release", type=int, default=21,
                    help="Effective JAR release view and generated descriptor target (default: 21)")
    ap.add_argument("--date", help="Optional ISO-8601 timestamp for reproducible JMOD entries")
    ap.add_argument("--jmods-only", action="store_true",
                    help="Package JMODs without linking or executing an image")
    ap.add_argument("--root-module", action="append", default=[], metavar="MODULE",
                    help="Opt-in runtime roots; repeat to select a closure instead of all candidates")
    ap.add_argument("--native-library", action="append", default=[], metavar="MODULE=DIRECTORY")
    ap.add_argument("--legal-notices", type=Path)
    a = ap.parse_args(argv)
    if a.jmods_only and a.root_module:
        raise ValueError("--jmods-only requires selected input manifests, not --root-module")
    if a.legal_notices is not None and not a.legal_notices.is_dir():
        raise ValueError("legal notices directory required")
    legal = ["--legal-notices", a.legal_notices] if a.legal_notices else []
    jdk = a.jdk_home
    if a.release < 9:
        raise ValueError("JMOD release must be at least 9")
    version = re.search(r'(?m)^JAVA_VERSION="([0-9]+)', (jdk / "release").read_text())
    if version is None or int(version.group(1)) < a.release:
        raise ValueError("JDK is older than requested JMOD release: " + str(a.release))
    date = ["--date", a.date] if a.date else []
    native = {}
    for value in a.native_library:
        module, separator, directory = value.partition("=")
        if not separator or module in native or not Path(directory).is_dir():
            raise ValueError("invalid native library binding: " + value)
        native[module] = directory
    named = {}
    for row in _rows(a.image_tsv, ("module", "jar", "disposition")):
        if not row["disposition"].startswith(("JMOD", "MODULAR_JAR")):
            continue
        name = row["module"]
        _module_name(name)
        if name in named:
            raise ValueError("duplicate module name: " + name)
        named[name] = row
    rows = _rows(a.manifest, ("module", "unit_jars", "target"))
    seen = set(named)
    for row in rows:
        name = row["module"]
        _module_name(name)
        if name in seen:
            raise ValueError("duplicate or cross-lane module name: " + name)
        seen.add(name)
    jmods = a.out / ("candidate-jmods" if a.root_module else "jmods")
    if a.out.exists() or a.out.is_symlink():
        raise FileExistsError("fresh output directory required: " + str(a.out))
    for jar in sorted({row["jar"] for row in named.values()} | {row["unit_jars"] for row in rows}):
        validate_jar(Path(jar), a.release)
    jmods.mkdir(parents=True)
    report, mods = [], []
    for name, r in sorted(named.items()):  # published descriptors: same packaging as jmod_image.py
        flat = a.out / "flat" / name
        mr = flatten_release(r["jar"], flat, release=a.release)
        libs = ["--libs", native[name]] if name in native else []
        res = run(jdk / "bin/jmod", "create", *date, "--class-path", flat if mr else r["jar"], *libs, *legal, jmods / (name + ".jmod"))
        report.append((name, "published", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(name) if res.returncode == 0 else None
    mp = os.pathsep.join([r["jar"] for r in named.values()] + [x["unit_jars"] for x in rows])
    for x in rows:
        mod, unit = x["module"], x["unit_jars"]
        src = a.layer_root / x["target"]
        classes = a.out / "classes" / mod
        flatten_release(unit, classes, release=a.release)
        res = run(jdk / "bin/javac", "--release", str(a.release), "--module-path", os.pathsep.join(p for p in mp.split(os.pathsep) if p != unit),
                  "--patch-module", "%s=%s" % (mod, unit), "-d", a.out / "descriptor-classes" / mod, src)
        if res.returncode != 0:
            report.append((mod, "m3-descriptor-for-jmod", "JAVAC_FAILED " + (res.stdout + res.stderr).strip().splitlines()[0][:200])); continue
        shutil.copyfile(a.out / "descriptor-classes" / mod / "module-info.class", classes / "module-info.class")
        libs = ["--libs", native[mod]] if mod in native else []
        res = run(jdk / "bin/jmod", "create", *date, "--class-path", classes, *libs, *legal, jmods / (mod + ".jmod"))
        report.append((mod, "m3-descriptor-for-jmod", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(mod) if res.returncode == 0 else None
    if set(native) - set(mods) or len(mods) != len(named) + len(rows):
        print("JMOD_PACK_REFUSED", report)
        return 1
    if a.jmods_only:
        with io.open(a.out / "JMOD_PACK.tsv", "w", encoding="utf-8", newline="\n") as f:
            f.write("module\tdescriptor_source\tresult\n")
            for row in report:
                f.write("\t".join(row) + "\n")
        print("JMOD_PACK_PASS", "modules", len(mods), "release", a.release, "runtime", "NOT_EXECUTED")
        return 0
    roots = sorted(set(a.root_module + a.add_modules.split(","))) if a.root_module else sorted(mods) + a.add_modules.split(",")
    if a.root_module:
        jmods, mods = select_runtime(jdk, a.out, jmods, roots)
    image = a.out / "image"
    res = run(jdk / "bin/jlink", "--module-path", os.pathsep.join(map(str, (jdk / "jmods", jmods))), "--add-modules", ",".join(roots),
              "--output", image, "--no-header-files", "--no-man-pages", "--strip-debug")
    with io.open(a.out / "JMOD_PACK.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("module\tdescriptor_source\tresult\n")
        for r in report:
            f.write("\t".join(r) + "\n")
    if res.returncode != 0:
        print("JLINK_FAILED", (res.stdout + res.stderr).strip()[:1500]); return 1
    val = run(image / "bin/java", "--validate-modules")
    listed = run(image / "bin/java", "--list-modules").stdout.split()
    print("image", image, "modules", len(listed), "vendored", len(mods), "validate rc", val.returncode, (val.stdout + val.stderr).strip()[:300])
    return 0 if val.returncode == 0 else 2


if __name__ == "__main__":
    sys.exit(main())
