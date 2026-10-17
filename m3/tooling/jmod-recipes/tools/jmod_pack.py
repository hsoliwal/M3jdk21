#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD step 3: package every vendored library as a JMOD and jlink one image.
Named modules come from JMOD_IMAGE.tsv (published descriptors); the rest get the descriptor that the OpenRewrite
recipe M3ModuleDescriptorRecipe wrote into the M3 refactor layer (DESCRIPTOR_MANIFEST.tsv target paths under --layer-root).
Per descriptor module: javac --patch-module <module>=<unit jar>, copy the unit jar's release-21 view plus
module-info.class into a class tree, jmod create. Then jlink java.se + every vendored module, and validate."""
import argparse, csv, io, os, shutil, subprocess, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from jmod_image import JDK, check_space, flatten_release, guard_output, measured_archive_size, output_arguments, relative_path  # noqa: E402


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image-tsv", type=Path, required=True)
    ap.add_argument("--manifest", type=Path, required=True)
    ap.add_argument("--layer-root", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    output_arguments(ap)
    a = ap.parse_args()
    a.out = guard_output(a.out, a.output_root, a.output_budget_bytes)
    named = {r["module"]: r for r in csv.DictReader(io.open(a.image_tsv, encoding="utf-8", newline=""), delimiter="\t")
             if r["disposition"].startswith(("JMOD", "MODULAR_JAR"))}
    rows = list(csv.DictReader(io.open(a.manifest, encoding="utf-8", newline=""), delimiter="\t"))
    for row in rows:
        target = relative_path(row["target"])
        if target.parts[:4] != ("m3", "vendor", "third_party-m3", "descriptors") or len(target.parts) != 6 or target.name != "module-info.java":
            raise ValueError("descriptor target is outside the refactor layer")
    for name in [*named, *(row["module"] for row in rows)]:
        if len(relative_path(name).parts) != 1:
            raise ValueError("module name cannot contain a path")
    estimate = 3 * sum(measured_archive_size(p, a.output_root) for p in {*(r["jar"] for r in named.values()), *(r["unit_jars"] for r in rows)})
    estimate += sum(measured_archive_size(p, a.output_root) for p in (JDK / "jmods").glob("*.jmod"))
    guard_output(a.out, a.output_root, a.output_budget_bytes, estimate)
    jmods = a.out / "jmods"
    jmods.mkdir(parents=True)
    report, mods = [], []
    for name, r in sorted(named.items()):  # published descriptors: same packaging as jmod_image.py
        flat = a.out / "flat" / name
        mr = flatten_release(r["jar"], flat, output_root=a.output_root, output_budget_bytes=a.output_budget_bytes)
        check_space(a.output_root, a.output_budget_bytes)
        res = run(JDK / "bin/jmod", "create", "--class-path", flat if mr else r["jar"], jmods / (name + ".jmod"))
        report.append((name, "published", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(name) if res.returncode == 0 else None
    mp = os.pathsep.join([r["jar"] for r in named.values()] + [x["unit_jars"] for x in rows])
    for x in rows:
        mod, unit = x["module"], x["unit_jars"]
        src = a.layer_root.resolve() / relative_path(x["target"])
        classes = a.out / "classes" / mod
        flatten_release(unit, classes, output_root=a.output_root, output_budget_bytes=a.output_budget_bytes)
        check_space(a.output_root, a.output_budget_bytes)
        res = run(JDK / "bin/javac", "--release", "21", "--module-path", os.pathsep.join(p for p in mp.split(os.pathsep) if p != unit),
                  "--patch-module", "%s=%s" % (mod, unit), "-d", a.out / "descriptor-classes" / mod, src)
        if res.returncode != 0:
            report.append((mod, "m3-descriptor-for-jmod", "JAVAC_FAILED " + (res.stdout + res.stderr).strip().splitlines()[0][:200])); continue
        shutil.copyfile(a.out / "descriptor-classes" / mod / "module-info.class", classes / "module-info.class")
        res = run(JDK / "bin/jmod", "create", "--class-path", classes, jmods / (mod + ".jmod"))
        report.append((mod, "m3-descriptor-for-jmod", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(mod) if res.returncode == 0 else None
    image = a.out / "image"
    check_space(a.output_root, a.output_budget_bytes)
    res = run(JDK / "bin/jlink", "--module-path", os.pathsep.join(map(str, [JDK / "jmods", jmods])), "--add-modules", ",".join(sorted(mods) + ["java.se"]),
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
