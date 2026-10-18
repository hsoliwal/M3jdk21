#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD prototype: for every Maven-built vendored module (-m3-source-1 jar in the build repository), read its
module descriptor with the JDK 21 `jar --describe-module`, keep explicit (named) modules only, pick one version per
module name (highest), drop modules whose non-static `requires` cannot be resolved inside the set + the JDK image,
`jmod create` each survivor (falling back to the modular jar for jlink when jmod refuses), then `jlink` one image and
list its modules. Writes JMOD_IMAGE.tsv (module, version, jar, descriptor kind, requires, disposition) beside --out."""
import argparse, csv, io, re, shutil, stat, subprocess, sys
import ctypes
import os
from pathlib import Path, PurePosixPath, PureWindowsPath

JDK = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-21" if os.name == "nt" else "/usr/lib/jvm/java-21-openjdk-amd64"))
RESERVE = 8 * 1024**3
MAX_OUTPUT = 512 * 1024**2
TEMP_ROOT = Path(r"K:\codex-work\agent-temp")


def no_reparse(path):
    """Refuse links/junctions and Git ownership before resolving an output."""
    for part in (path, *path.parents):
        if part.exists() or part.is_symlink():
            attrs = part.lstat()
            if part.is_symlink() or getattr(attrs, "st_file_attributes", 0) & 0x400:
                raise ValueError("reparse output path: %s" % part)
        if (part / ".git").exists():
            raise ValueError("output is inside a source/Git tree: %s" % part)


def check_space(owner, budget):
    if not 0 < budget <= MAX_OUTPUT:
        raise ValueError("output budget must be between 1 and 512 MiB")
    if shutil.disk_usage(owner).free - budget < RESERVE:
        raise ValueError("8 GiB reserve would be crossed")


def guard_output(out, owner, budget, estimated=0):
    """Write a fresh owned child; enforce this workstation's K lane on Windows."""
    out, owner = Path(out), Path(owner)
    if not out.is_absolute() or not owner.is_absolute() or ".." in out.parts or ".." in owner.parts:
        raise ValueError("absolute, normalized owned output paths are required")
    no_reparse(owner)
    no_reparse(out)
    if not owner.is_dir() or owner == Path(owner.anchor):
        raise ValueError("output owner must be an existing non-root directory")
    if os.name == "nt" and not owner.resolve().is_relative_to(TEMP_ROOT.resolve()):
        raise ValueError("output owner must exist below K:/codex-work/agent-temp")
    out, owner = out.resolve(), owner.resolve()
    if out == owner or not out.is_relative_to(owner) or out.exists():
        raise ValueError("output must be a fresh strict child of its owner; existing output is preserved")
    check_space(owner, budget)
    if estimated > budget:
        raise ValueError("measured input/output estimate exceeds the output budget")
    return out


def output_arguments(parser):
    parser.add_argument("--output-root", type=Path, required=True, help="existing owned output directory; Windows uses the K temp lane")
    parser.add_argument("--output-budget-bytes", type=int, required=True, help="measured output allowance, at most 512 MiB")


def allocation_unit(owner):
    """Read the destination's allocation unit rather than charge payload bytes."""
    if os.name == "nt":
        sectors, sector_bytes, free_clusters, clusters = (ctypes.c_ulong() for _ in range(4))
        if not ctypes.windll.kernel32.GetDiskFreeSpaceW(str(Path(owner).anchor), ctypes.byref(sectors),
                ctypes.byref(sector_bytes), ctypes.byref(free_clusters), ctypes.byref(clusters)):
            raise OSError("cannot measure output allocation unit")
        unit = sectors.value * sector_bytes.value
    else:
        unit = os.statvfs(owner).f_frsize
    if unit <= 0:
        raise ValueError("cannot measure output allocation unit")
    return unit


def archive_allocation(archive, owner):
    """Charge files and parent directories using the destination's measured cluster size."""
    unit = allocation_unit(owner)
    directories, allocated = set(), 0
    for item in archive.infolist():
        relative = relative_path(item.filename.rstrip("/"))
        directories.update(relative.parents)
        if not item.is_dir():
            allocated += max(unit, ((item.file_size + unit - 1) // unit) * unit)
    return allocated + len(directories) * unit


def measured_archive_size(jar, owner):
    import zipfile
    with zipfile.ZipFile(jar) as archive:
        validate_archive(archive)
        return archive_allocation(archive, owner)


def validate_archive(archive):
    """Validate every entry before a valid earlier entry can cause an effect."""
    names, files, parents = set(), set(), set()
    for item in archive.infolist():
        relative = relative_path(item.filename.rstrip("/"))
        key = str(relative).casefold() if os.name == "nt" else str(relative)
        if key in names:
            raise ValueError("duplicate or aliased archive path: %s" % item.filename)
        names.add(key)
        if not item.is_dir():
            files.add(key)
        parents.update(str(p).casefold() if os.name == "nt" else str(p) for p in relative.parents if p != Path("."))
        if stat.S_ISLNK(item.external_attr >> 16):
            raise ValueError("archive links refused")
    if files & parents:
        raise ValueError("archive file is also a parent directory")


def relative_path(value):
    """Validate portable archive/manifest paths before any output is created."""
    parts = value.replace("\\", "/").split("/")
    if (not value or PurePosixPath(value).is_absolute() or PureWindowsPath(value).drive
            or value.startswith(("/", "\\")) or ":" in value or any(p in ("", ".", "..") for p in parts)):
        raise ValueError("unsafe relative path: %s" % value)
    if ".git" in parts:
        raise ValueError("Git archive/manifest path refused")
    return Path(*parts)


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def vkey(v):
    return [int(x) if x.isdigit() else x for x in re.split(r"[.\-]", v)]


def flatten_release(jar, dest, release=21, *, output_root, output_budget_bytes):
    """Release-21 view of a multi-release jar as a plain class tree (jmod rejects META-INF/versions entries)."""
    import zipfile
    dest = guard_output(dest, output_root, output_budget_bytes)
    with zipfile.ZipFile(jar) as z:
        validate_archive(z)
        if archive_allocation(z, output_root) > output_budget_bytes:
            raise ValueError("expanded archive exceeds output budget")
        names = z.namelist()
        base = [n for n in names if not n.startswith("META-INF/versions/") and not n.endswith("/")]
        versioned = sorted({int(n.split("/")[2]) for n in names if n.startswith("META-INF/versions/") and n.split("/")[2].isdigit()})
        selected = {n: n for n in base}
        for v in (x for x in versioned if x <= release):
            pre = "META-INF/versions/%d/" % v
            for n in names:
                if n.startswith(pre) and not n.endswith("/"):
                    selected[n[len(pre):]] = n
        for relative, archive_name in selected.items():
            if relative == "META-INF/MANIFEST.MF":
                continue  # jmod writes no manifest; never materialize one for deletion
            check_space(output_root, output_budget_bytes)
            target = dest / relative_path(relative)
            target.parent.mkdir(parents=True, exist_ok=True)
            with z.open(archive_name) as source, target.open("xb") as output:
                shutil.copyfileobj(source, output, length=65536)
    return bool(versioned)


def describe(jar):
    res = run(JDK / "bin/jar", "--describe-module", "--file", jar)
    if "specify --release" in res.stdout + res.stderr:
        res = run(JDK / "bin/jar", "--describe-module", "--file", jar, "--release", "21")
    out = res.stdout + res.stderr
    if "No module descriptor found" in out or "Unable to derive" in out or not res.stdout.strip():
        return None
    lines = [l for l in res.stdout.strip().splitlines() if l.strip() and not l.startswith(("releases:", "Picked up"))]
    head = next((l for l in lines if "module-info.class" in l), lines[0])
    name = head.split()[0].split("@")[0]
    req = []
    for l in lines[1:]:
        m = re.match(r"\s*requires\s+(.*)", l)
        if m:
            parts = m.group(1).split()
            req.append((parts[0], "static" in parts[1:]))
    return name, req


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--modules", type=Path, required=True)
    ap.add_argument("--m2", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    output_arguments(ap)
    a = ap.parse_args()
    a.out = guard_output(a.out, a.output_root, a.output_budget_bytes)
    rows = [r for r in csv.DictReader(io.open(a.modules, encoding="utf-8", newline=""), delimiter="\t") if r["kind"] == "MODULE"]
    jdk_mods = {l.split("@")[0] for l in run(JDK / "bin/java", "--list-modules").stdout.split()}
    found, report = {}, []
    for r in rows:
        jar = a.m2 / r["group"].replace(".", "/") / r["artifact"] / r["version"] / ("%s-%s.jar" % (r["artifact"], r["version"]))
        if not jar.is_file():
            report.append((r["artifact"], r["version"], str(jar), "-", "-", "NO_JAR")); continue
        d = describe(jar)
        if d is None:
            report.append((r["artifact"], r["version"], str(jar), "AUTOMATIC", "-", "NEEDS_DESCRIPTOR")); continue
        name, req = d
        cur = found.get(name)
        if cur is None or vkey(r["version"]) > vkey(cur[1]):
            if cur is not None:
                report.append((cur[0], cur[1], str(cur[2]), name, "-", "OLDER_VERSION_OF_SAME_MODULE"))
            found[name] = (r["artifact"], r["version"], jar, req)
        else:
            report.append((r["artifact"], r["version"], str(jar), name, "-", "OLDER_VERSION_OF_SAME_MODULE"))
    changed = True
    while changed:
        changed = False
        for name in sorted(found):
            missing = [m for m, static in found[name][3] if not static and m not in found and m not in jdk_mods]
            if missing:
                art, ver, jar, req = found.pop(name)
                report.append((art, ver, str(jar), name, ",".join(m for m, _ in req), "UNRESOLVED_REQUIRES:" + ",".join(missing)))
                changed = True
    estimate = 2 * sum(measured_archive_size(x[2], a.output_root) for x in found.values())
    estimate += sum(measured_archive_size(p, a.output_root) for p in (JDK / "jmods").glob("*.jmod"))
    guard_output(a.out, a.output_root, a.output_budget_bytes, estimate)
    jmods = a.out / "jmods"; jmods.mkdir(parents=True)
    path_entries = [str(JDK / "jmods")]
    for name in sorted(found):
        art, ver, jar, req = found[name]
        target = jmods / (name + ".jmod")
        flat = a.out / "flat" / name
        mr = flatten_release(jar, flat, output_root=a.output_root, output_budget_bytes=a.output_budget_bytes)
        check_space(a.output_root, a.output_budget_bytes)
        res = run(JDK / "bin/jmod", "create", "--class-path", flat if mr else jar, target)
        if res.returncode == 0:
            disp = "JMOD_FLATTENED_RELEASE_21" if mr else "JMOD"
            path_entries.append(str(target))
        else:
            disp = "MODULAR_JAR (jmod: %s)" % res.stderr.strip().splitlines()[0][:120] if res.stderr.strip() else "MODULAR_JAR"
            path_entries.append(str(jar))
        report.append((art, ver, str(jar), name, ",".join(m for m, _ in req), disp))
    image = a.out / "image"
    check_space(a.output_root, a.output_budget_bytes)
    res = run(JDK / "bin/jlink", "--module-path", os.pathsep.join(path_entries), "--add-modules", ",".join(sorted(found) + ["java.se"]),
              "--output", image, "--no-header-files", "--no-man-pages", "--strip-debug")
    with io.open(a.out / "JMOD_IMAGE.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("artifact\tversion\tjar\tmodule\trequires\tdisposition\n")
        for r in sorted(report):
            f.write("\t".join(r) + "\n")
    if res.returncode != 0:
        print("JLINK_FAILED", (res.stdout + res.stderr).strip()[:2000]); return 1
    listed = run(image / "bin/java", "--list-modules").stdout.split()
    vendored = [m for m in listed if m.split("@")[0] in found]
    print("image", image, "modules", len(listed), "vendored", len(vendored))
    from collections import Counter
    print(Counter(r[5].split(":")[0].split(" (")[0] for r in report))
    return 0


if __name__ == "__main__":
    sys.exit(main())
