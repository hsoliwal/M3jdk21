#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD step 3: package every vendored library as a JMOD and jlink one image.
Named modules come from JMOD_IMAGE.tsv (published descriptors); the rest get the descriptor that the OpenRewrite
recipe M3ModuleDescriptorRecipe wrote into the M3 refactor layer (DESCRIPTOR_MANIFEST.tsv target paths under --layer-root).
Per descriptor module: javac --patch-module <module>=<unit jar>, copy the unit jar's release-21 view plus
module-info.class into a class tree, jmod create. Then jlink java.se + every vendored module, and validate."""
import argparse, csv, io, os, shutil, subprocess, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from jmod_image import JDK, flatten_release  # noqa: E402


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image-tsv", type=Path, required=True)
    ap.add_argument("--manifest", type=Path, required=True)
    ap.add_argument("--layer-root", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--jdk-home", type=Path, default=Path(os.environ.get("JAVA_HOME", str(JDK))))
    ap.add_argument("--add-modules", default="java.se")
    ap.add_argument("--native-library", action="append", default=[], metavar="MODULE=DIRECTORY")
    ap.add_argument("--legal-notices", type=Path)
    a = ap.parse_args()
    if a.legal_notices is not None and not a.legal_notices.is_dir():
        raise ValueError("legal notices directory required")
    legal = ["--legal-notices", a.legal_notices] if a.legal_notices else []
    jdk = a.jdk_home
    native = {}
    for value in a.native_library:
        module, separator, directory = value.partition("=")
        if not separator or module in native or not Path(directory).is_dir():
            raise ValueError("invalid native library binding: " + value)
        native[module] = directory
    named = {r["module"]: r for r in csv.DictReader(io.open(a.image_tsv, encoding="utf-8", newline=""), delimiter="\t")
             if r["disposition"].startswith(("JMOD", "MODULAR_JAR"))}
    rows = list(csv.DictReader(io.open(a.manifest, encoding="utf-8", newline=""), delimiter="\t"))
    jmods = a.out / "jmods"
    if a.out.exists():
        raise FileExistsError("fresh output directory required: " + str(a.out))
    jmods.mkdir(parents=True)
    report, mods = [], []
    for name, r in sorted(named.items()):  # published descriptors: same packaging as jmod_image.py
        flat = a.out / "flat" / name
        mr = flatten_release(r["jar"], flat)
        libs = ["--libs", native[name]] if name in native else []
        res = run(jdk / "bin/jmod", "create", "--class-path", flat if mr else r["jar"], *libs, *legal, jmods / (name + ".jmod"))
        report.append((name, "published", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(name) if res.returncode == 0 else None
    mp = os.pathsep.join([r["jar"] for r in named.values()] + [x["unit_jars"] for x in rows])
    for x in rows:
        mod, unit = x["module"], x["unit_jars"]
        src = a.layer_root / x["target"]
        classes = a.out / "classes" / mod
        flatten_release(unit, classes)
        res = run(jdk / "bin/javac", "--release", "21", "--module-path", os.pathsep.join(p for p in mp.split(os.pathsep) if p != unit),
                  "--patch-module", "%s=%s" % (mod, unit), "-d", a.out / "descriptor-classes" / mod, src)
        if res.returncode != 0:
            report.append((mod, "m3-descriptor-for-jmod", "JAVAC_FAILED " + (res.stdout + res.stderr).strip().splitlines()[0][:200])); continue
        shutil.copyfile(a.out / "descriptor-classes" / mod / "module-info.class", classes / "module-info.class")
        libs = ["--libs", native[mod]] if mod in native else []
        res = run(jdk / "bin/jmod", "create", "--class-path", classes, *libs, *legal, jmods / (mod + ".jmod"))
        report.append((mod, "m3-descriptor-for-jmod", "JMOD" if res.returncode == 0 else "JMOD_FAILED " + res.stderr.strip()[:160]))
        mods.append(mod) if res.returncode == 0 else None
    if set(native) - set(mods) or len(mods) != len(named) + len(rows):
        print("JMOD_PACK_REFUSED", report)
        return 1
    image = a.out / "image"
    res = run(jdk / "bin/jlink", "--module-path", os.pathsep.join(map(str, (jdk / "jmods", jmods))), "--add-modules", ",".join(sorted(mods) + a.add_modules.split(",")),
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
