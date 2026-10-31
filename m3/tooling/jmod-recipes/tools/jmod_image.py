#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD prototype: for every Maven-built vendored module (-m3-source-1 jar in the build repository), read its
module descriptor with the JDK 21 `jar --describe-module`, keep explicit (named) modules only, pick one version per
module name (highest), drop modules whose non-static `requires` cannot be resolved inside the set + the JDK image,
`jmod create` each survivor (falling back to the modular jar for jlink when jmod refuses), then `jlink` one image and
list its modules. Writes JMOD_IMAGE.tsv (module, version, jar, descriptor kind, requires, disposition) beside --out."""
import argparse, csv, io, re, subprocess, sys
from pathlib import Path

JDK = Path(r"C:\Program Files\Java\jdk-21")


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def vkey(v):
    return [int(x) if x.isdigit() else x for x in re.split(r"[.\-]", v)]


def flatten_release(jar, dest, release=21):
    """Release-21 view of a multi-release jar as a plain class tree (jmod rejects META-INF/versions entries)."""
    import shutil, zipfile
    if dest.exists():
        shutil.rmtree(dest)
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        base = [n for n in names if not n.startswith("META-INF/versions/") and not n.endswith("/")]
        versioned = sorted({int(n.split("/")[2]) for n in names if n.startswith("META-INF/versions/") and n.split("/")[2].isdigit()})
        for n in base:
            (dest / n).parent.mkdir(parents=True, exist_ok=True)
            (dest / n).write_bytes(z.read(n))
        for v in (x for x in versioned if x <= release):
            pre = "META-INF/versions/%d/" % v
            for n in names:
                if n.startswith(pre) and not n.endswith("/"):
                    (dest / n[len(pre):]).parent.mkdir(parents=True, exist_ok=True)
                    (dest / n[len(pre):]).write_bytes(z.read(n))
    mf = dest / "META-INF/MANIFEST.MF"
    if mf.exists():
        mf.unlink()  # jmod writes no manifest; Multi-Release no longer applies to the flattened view
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
    a = ap.parse_args()
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
    jmods = a.out / "jmods"; jmods.mkdir(parents=True, exist_ok=True)
    path_entries = [str(JDK / "jmods")]
    for name in sorted(found):
        art, ver, jar, req = found[name]
        target = jmods / (name + ".jmod")
        if target.exists():
            target.unlink()
        flat = a.out / "flat" / name
        mr = flatten_release(jar, flat)
        res = run(JDK / "bin/jmod", "create", "--class-path", flat if mr else jar, target)
        if res.returncode == 0:
            disp = "JMOD_FLATTENED_RELEASE_21" if mr else "JMOD"
            path_entries.append(str(target))
        else:
            disp = "MODULAR_JAR (jmod: %s)" % res.stderr.strip().splitlines()[0][:120] if res.stderr.strip() else "MODULAR_JAR"
            path_entries.append(str(jar))
        report.append((art, ver, str(jar), name, ",".join(m for m, _ in req), disp))
    image = a.out / "image"
    if image.exists():
        import shutil; shutil.rmtree(image)
    res = run(JDK / "bin/jlink", "--module-path", ";".join(path_entries), "--add-modules", ",".join(sorted(found) + ["java.se"]),
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
