# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mr_select.py <skija_root> <types_sources_jar> <out_src0> <report.tsv> [--jar <published.jar>]... [--release <N>]
                [--jar-view <dir>]

MR_SELECT v1, stage 0 of the leaf-1 absorption (CPU only, deterministic, fail-closed). The donor jars are Multi-Release
(JEP 238): on Java N a class loads from META-INF/versions/K for the highest K <= N, else from the jar root. The Skija
sources have the same layout (script/build.py, script/package_shared.py): shared/java is the jar root (javac --release
8) and shared/java<K> is META-INF/versions/K. For every top-level class this stage takes the source of the highest
version <= release (default 21, the feature release of M3jdk21), which is the class the published jar runs there.

Writes:
  <out_src0>    the selected sources at their package-relative paths, the root lombok.config (delombok reads it), and
                the HumbleUI Types sources (io/**, with their lombok.config) of <types_sources_jar>
  <report.tsv>  kind CLASS: one row per class that has a versioned source: versions present, selected version, the
                selected source and its SHA-256, the published jar entry that agrees, and the change against the root
                variant (the recipe took shared/java for every class before MR_SELECT); kind MODULE_DESCRIPTOR: one row
                per module-info, source or jar entry (not a class: jdk.m3.graphics has its own module-info.java)
  --jar-view    the classes of the --jar jars as Java <release> loads them (jar root, then META-INF/versions/K for
                K <= release in ascending K; module descriptors left out), for the published-surface gate

Fail-closed (exit 1): with --jar (repeatable), the versioned classes of the Multi-Release jars (K <= release; nested and
synthetic classes fold into their top-level class) must equal the selected versioned sources, class for class and
version for version. A class whose every source is above the release, a lombok.config inside a versioned source set,
a Types source that would overwrite a Skija source, and a non-empty <out_src0> also stop the run."""
import argparse
import hashlib
import os
import re
import shutil
import sys
import zipfile

M3JDK_FEATURE_RELEASE = 21
ROOT_VERSION = 0
ROOT_LABEL = "base"
SKIJA_PACKAGE = "io.github.humbleui.skija"
SOURCE_SET = re.compile(r"^java(\d*)$")
VERSIONED_CLASS = re.compile(r"^META-INF/versions/(\d+)/(.+)\.class$")
MULTI_RELEASE = re.compile(r"^Multi-Release:[ \t]*true[ \t]*\r?$", re.I | re.M)
MODULE_DECL = re.compile(r"\bmodule\s+([\w.]+)\s*\{")
MANIFEST = "META-INF/MANIFEST.MF"
MODULE_INFO = "module-info"
LOMBOK_CONFIG = "lombok.config"
TYPES_ROOT = "io/"
JAVA = ".java"
DESCRIPTOR_NOTE = "not absorbed: src/jdk.m3.graphics/share/classes/module-info.java"
REPORT_HEAD = ("kind", "name", "versions", "selected", "source", "sha256", "jar_entry", "change")


def label(version):
    return ROOT_LABEL if version == ROOT_VERSION else str(version)


def source_sets(skija_root):
    """[(version, directory)] of <skija_root>/shared/java*, the root (version 0) first."""
    shared = os.path.join(skija_root, "shared")
    found = []
    for name in sorted(os.listdir(shared)):
        m = SOURCE_SET.match(name)
        if m and os.path.isdir(os.path.join(shared, name)):
            found.append((int(m.group(1)) if m.group(1) else ROOT_VERSION, os.path.join(shared, name)))
    found.sort()
    if not found or found[0][0] != ROOT_VERSION:
        raise SystemExit("no root source set shared/java under " + skija_root)
    return found


def select_sources(skija_root, release=M3JDK_FEATURE_RELEASE):
    """MR_SELECT v1 over a Skija source tree. Returns ({rel: [(version, path)]}, {rel: (version, path)} selected,
    [(version, path)] module descriptors); rel is package-relative ("impl/Cleanable.java"), path is absolute."""
    variants, descriptors = {}, []
    for version, base in source_sets(skija_root):
        for dirpath, dirs, files in os.walk(base):
            dirs.sort()
            for f in sorted(files):
                p = os.path.join(dirpath, f)
                if f == LOMBOK_CONFIG and version != ROOT_VERSION:
                    raise SystemExit("lombok.config in a versioned source set is not supported: " + p)
                if not f.endswith(JAVA):
                    continue
                if f[:-len(JAVA)] == MODULE_INFO:
                    descriptors.append((version, p))
                    continue
                rel = os.path.relpath(p, base).replace(os.sep, "/")
                variants.setdefault(rel, []).append((version, p))
    selected = {}
    for rel, found in sorted(variants.items()):
        eligible = [v for v in found if v[0] <= release]
        if not eligible:
            raise SystemExit(f"{rel}: every source is above release {release}")
        selected[rel] = max(eligible)
    return variants, selected, descriptors


def fqn(rel):
    return SKIJA_PACKAGE + "." + rel[:-len(JAVA)].replace("/", ".")


def jar_versions(jars, release):
    """({top-level class: (version, entry)} by the JEP 238 rule over the versioned entries of the Multi-Release jars,
    [(jar file name, version, entry)] of their versioned module descriptors). A jar without "Multi-Release: true" in
    its manifest has no versioned classes at run time and contributes nothing."""
    chosen, descriptors = {}, []
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            names = sorted(z.namelist())
            manifest = z.read(MANIFEST).decode("utf-8", "replace") if MANIFEST in names else ""
            if not MULTI_RELEASE.search(manifest):
                continue
            for n in names:
                m = VERSIONED_CLASS.match(n)
                if not m or int(m.group(1)) > release:
                    continue
                version, binary = int(m.group(1)), m.group(2)
                if binary.rsplit("/", 1)[-1] == MODULE_INFO:
                    descriptors.append((os.path.basename(jar), version, n))
                    continue
                top = binary.split("$", 1)[0]
                if top.replace("/", ".") not in chosen or chosen[top.replace("/", ".")][0] < version:
                    chosen[top.replace("/", ".")] = (version, f"META-INF/versions/{version}/{top}.class")
    return chosen, descriptors


def write_jar_view(jars, release, out):
    """Writes the classes of the jars as Java <release> loads them and returns their count."""
    written = set()
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            names = sorted(z.namelist())
            manifest = z.read(MANIFEST).decode("utf-8", "replace") if MANIFEST in names else ""
            layers = [(ROOT_VERSION, n, n) for n in names if n.endswith(".class") and not n.startswith("META-INF/")]
            for n in (names if MULTI_RELEASE.search(manifest) else []):
                m = VERSIONED_CLASS.match(n)
                if m and int(m.group(1)) <= release:
                    layers.append((int(m.group(1)), n, m.group(2) + ".class"))
            for _, entry, target in sorted(layers, key=lambda layer: layer[0]):
                if target.rsplit("/", 1)[-1] == MODULE_INFO + ".class":
                    continue
                path = os.path.join(out, *target.split("/"))
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "wb") as w:
                    w.write(z.read(entry))
                written.add(target)
    return len(written)


def sha256(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def cross_check(selected, jar_chosen):
    src = {fqn(rel): v for rel, (v, _) in selected.items() if v != ROOT_VERSION}
    jar = {name: v for name, (v, _) in jar_chosen.items()}
    bad = [f"{n}\tsource {label(src[n]) if n in src else ROOT_LABEL}\tjar {label(jar[n]) if n in jar else ROOT_LABEL}"
           for n in sorted(set(src) | set(jar)) if src.get(n) != jar.get(n)]
    if bad:
        print("MR_MISMATCH", *bad, sep="\n")
        raise SystemExit("versioned sources and versioned jar classes disagree")


def write_sources(skija_root, selected, types_jar, out):
    os.makedirs(out, exist_ok=True)
    for rel, (_, p) in sorted(selected.items()):
        target = os.path.join(out, *rel.split("/"))
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(p, target)
    config = os.path.join(source_sets(skija_root)[0][1], LOMBOK_CONFIG)
    if os.path.isfile(config):
        shutil.copyfile(config, os.path.join(out, LOMBOK_CONFIG))
    types = 0
    with zipfile.ZipFile(types_jar) as z:
        for n in sorted(z.namelist()):
            if not n.startswith(TYPES_ROOT) or n.endswith("/"):
                continue
            target = os.path.join(out, *n.split("/"))
            if os.path.exists(target):
                raise SystemExit("Types source would overwrite a Skija source: " + n)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as w:
                w.write(z.read(n))
            types += n.endswith(JAVA)
    return types


def report_rows(skija_root, variants, selected, descriptors, jar_chosen, jar_descriptors):
    rel_root = lambda p: os.path.relpath(p, skija_root).replace(os.sep, "/")
    rows = []
    for rel, found in sorted(variants.items()):
        version, p = selected[rel]
        if len(found) == 1 and version == ROOT_VERSION:
            continue
        entry = jar_chosen.get(fqn(rel), (None, "-"))[1]
        change = f"{ROOT_LABEL} -> {label(version)}" if version != ROOT_VERSION else "unchanged"
        rows.append(("CLASS", fqn(rel), ",".join(label(v) for v, _ in found), label(version), rel_root(p), sha256(p),
                     entry, change))
    for version, p in descriptors:
        m = MODULE_DECL.search(open(p, encoding="utf-8", errors="replace").read())
        rows.append(("MODULE_DESCRIPTOR", m.group(1) if m else MODULE_INFO, label(version), "-", rel_root(p),
                     sha256(p), "-", DESCRIPTOR_NOTE))
    for jar, version, entry in jar_descriptors:
        rows.append(("MODULE_DESCRIPTOR", jar, label(version), "-", "-", "-", entry, DESCRIPTOR_NOTE))
    return rows


def main():
    ap = argparse.ArgumentParser(description="MR_SELECT v1: stage 0 of the Skija leaf-1 absorption")
    ap.add_argument("skija_root")
    ap.add_argument("types_sources_jar")
    ap.add_argument("out_src0")
    ap.add_argument("report")
    ap.add_argument("--jar", action="append", default=[])
    ap.add_argument("--release", type=int, default=M3JDK_FEATURE_RELEASE)
    ap.add_argument("--jar-view")
    a = ap.parse_args()
    if os.path.isdir(a.out_src0) and os.listdir(a.out_src0):
        raise SystemExit("output exists and is not empty: " + a.out_src0)
    variants, selected, descriptors = select_sources(a.skija_root, a.release)
    jar_chosen, jar_descriptors = jar_versions(a.jar, a.release)
    for jar in a.jar:
        print(f"jar\t{os.path.basename(jar)}\t{sha256(jar)}")
    if a.jar:
        cross_check(selected, jar_chosen)
    types = write_sources(a.skija_root, selected, a.types_sources_jar, a.out_src0)
    rows = report_rows(a.skija_root, variants, selected, descriptors, jar_chosen, jar_descriptors)
    with open(a.report, "w", encoding="utf-8", newline="\n") as w:
        w.write("\t".join(REPORT_HEAD) + "\n" + "".join("\t".join(r) + "\n" for r in rows))
    changed = [r for r in rows if r[0] == "CLASS" and r[7] != "unchanged"]
    for r in changed:
        print(f"MR_SELECTED\t{r[1]}\t{r[7]}\t{r[4]}")
    if a.jar_view:
        print(f"jar_view_classes={write_jar_view(a.jar, a.release, a.jar_view)}")
    descriptors_found = len([r for r in rows if r[0] == "MODULE_DESCRIPTOR"])
    print(f"release={a.release} classes={len(selected)} versioned={len(changed)} "
          f"root_only={len(selected) - len(changed)} descriptors={descriptors_found} "
          f"jar_versioned={len(jar_chosen)} types_sources={types}")


if __name__ == "__main__":
    main()
