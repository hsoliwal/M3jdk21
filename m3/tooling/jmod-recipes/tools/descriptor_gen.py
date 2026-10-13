#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""M3SDK JMOD step 2, facts only (design section 6.2): module descriptors for the vendored jars that have none.
1. Candidates = JMOD_IMAGE.tsv NEEDS_DESCRIPTOR rows; keep the highest version per artifact; drop a jar whose packages
   a named (descriptor-carrying) module already owns (it is an older line of that library).
2. Split packages among the remaining jars (MERGE_GROUPS) are merged into one jar per group (module name = the shortest
   shared package), because JPMS forbids a package in two named modules.
3. jdeps --generate-module-info per unit (module path = named jars + the units); the generated module-info.java files and
   DESCRIPTOR_MANIFEST.tsv (module, unit jar(s), target path in the M3 refactor layer, sha256) are the input of the
   OpenRewrite recipe M3ModuleDescriptorRecipe, which writes them into the repository. The upstream jars never change."""
import argparse, csv, hashlib, io, re, shutil, subprocess, sys, zipfile
from collections import defaultdict
from pathlib import Path

JDK = Path(r"C:\Program Files\Java\jdk-21")
sys.path.insert(0, str(Path(__file__).resolve().parent))
from split_packages import packages  # noqa: E402


def run(*a):
    return subprocess.run([str(x) for x in a], capture_output=True, text=True, encoding="utf-8", errors="replace")


def vkey(v):
    return [int(x) if x.isdigit() else x for x in re.split(r"[.\-]", v)]


def class_names(jar):
    """Binary names (a.b.C, a.b.C$D) of the release-21 view of a jar, package-info/module-info excluded."""
    out = set()
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if not n.endswith(".class") or n.endswith(("module-info.class", "package-info.class")):
                continue
            if n.startswith("META-INF/versions/"):
                parts = n.split("/")
                if not parts[2].isdigit() or int(parts[2]) > 21:
                    continue
                n = "/".join(parts[3:])
            elif n.startswith("META-INF/"):
                continue
            out.add(n[:-6].replace("/", "."))
    return out


def normalize(text, jar, all_classes):
    """Make jdeps output compile: javac refuses `exports` of a package that holds only package-info, and `provides`
    clauses that name classes absent from the jars (jdeps copies META-INF/services lines verbatim). Returns the
    normalized text and the dropped clauses (recorded in DESCRIPTORS.tsv)."""
    own = class_names(jar)
    real_pkgs = {c.rsplit(".", 1)[0] for c in own if "." in c}
    dropped = []

    def keep_export(m):
        pkg = m.group(2)
        if pkg in real_pkgs:
            return m.group(0)
        dropped.append("%s %s (package-info only)" % (m.group(1), pkg))
        return ""
    text = re.sub(r"^\s*(exports|opens)\s+([\w.]+)\s*(?:to\s+[^;]+)?;\s*\n", keep_export, text, flags=re.M)

    def exists(name):
        return name.startswith(("java.", "javax.", "jdk.")) or name in all_classes or name.replace("$", ".") in all_classes

    def keep_provides(m):
        service, impls = m.group(1), [x.strip() for x in m.group(2).split(",") if x.strip()]
        ok = [i for i in impls if i in own or i.replace(".", "$") in own]
        ok = [i for i in impls if i in own] or ok
        if not exists(service) or not ok:
            dropped.append("provides %s (%s)" % (service, "service absent" if not exists(service) else "no implementation present"))
            return ""
        if len(ok) != len(impls):
            dropped.append("provides %s: dropped %s" % (service, ",".join(sorted(set(impls) - set(ok)))))
        return "    provides %s with\n        %s;\n" % (service, ",\n        ".join(ok))
    text = re.sub(r"^\s*provides\s+([\w.$]+)\s+with\s+([^;]+);\s*\n", keep_provides, text, flags=re.M)
    return text, dropped


def merge_jars(jars, dest):
    seen, services = set(), defaultdict(list)
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as out:
        for j in jars:
            with zipfile.ZipFile(j) as z:
                for n in z.namelist():
                    if n.endswith("/") or n.endswith("module-info.class"):
                        continue
                    if n.startswith("META-INF/services/"):
                        services[n].append(z.read(n)); continue
                    if n in seen:
                        continue  # first jar wins (MANIFEST, LICENSE, ...); class collisions are refused below
                    seen.add(n)
                    out.writestr(n, z.read(n))
        for n, parts in services.items():
            out.writestr(n, b"\n".join(p.rstrip(b"\n") for p in parts) + b"\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image-tsv", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--layer", default="m3/vendor/third_party-m3/descriptors", help="repository path of the M3 refactor layer")
    a = ap.parse_args()
    rows = list(csv.DictReader(io.open(a.image_tsv, encoding="utf-8", newline=""), delimiter="\t"))
    named = [r for r in rows if r["disposition"].startswith(("JMOD", "MODULAR_JAR"))]
    named_pkgs = set()
    for r in named:
        named_pkgs |= packages(r["jar"])
    report, best = [], {}
    for r in rows:
        if r["disposition"] != "NEEDS_DESCRIPTOR":
            continue
        cur = best.get(r["artifact"])
        if cur is None or vkey(r["version"]) > vkey(cur["version"]):
            if cur:
                report.append((cur["artifact"], cur["version"], "-", "OLDER_VERSION (image keeps %s)" % r["version"]))
            best[r["artifact"]] = r
        else:
            report.append((r["artifact"], r["version"], "-", "OLDER_VERSION (image keeps %s)" % cur["version"]))
    units = {}
    for art, r in sorted(best.items()):
        pk = packages(r["jar"])
        if pk & named_pkgs:
            report.append((art, r["version"], "-", "OWNED_BY_NAMED_MODULE (%s)" % ",".join(sorted(pk & named_pkgs))[:120])); continue
        units[art] = (r, pk)
    owners = defaultdict(set)
    for art, (r, pk) in units.items():
        for p in pk:
            owners[p].add(art)
    parent = {}

    def find(x):
        while parent.setdefault(x, x) != x:
            parent[x] = parent[parent[x]]; x = parent[x]
        return x
    for p, arts in owners.items():
        arts = sorted(arts)
        for x in arts[1:]:
            parent[find(x)] = find(arts[0])
    groups = defaultdict(list)
    for art in units:
        groups[find(art)].append(art)
    work = a.out / "units"; gen = a.out / "generated"
    for d in (work, gen):
        if d.exists():
            shutil.rmtree(d)
        d.mkdir(parents=True)
    unit_jars = {}
    for root, arts in sorted(groups.items()):
        arts = sorted(arts)
        if len(arts) == 1:
            unit_jars[arts[0]] = ([units[arts[0]][0]["jar"]], arts)
            continue
        shared = sorted(p for p, o in owners.items() if len(o) > 1 and o <= set(arts))
        name = min(shared, key=len)
        dest = work / (name + ".jar")
        merge_jars([units[x][0]["jar"] for x in arts], dest)
        # jdeps derives the module name from the file name: name the merged jar after its module
        unit_jars[name] = ([str(dest)], arts)
    all_jars = [r["jar"] for r in named] + [j[0] for j, _ in unit_jars.values()]
    all_classes = set()
    for j in all_jars:
        all_classes |= class_names(j)
    manifest = []
    for key, (jars, arts) in sorted(unit_jars.items()):
        jar = jars[0]
        before = {p.relative_to(gen).parts[0] for p in gen.glob("**/module-info.java")}
        mp = ";".join(x for x in all_jars if x != jar)  # the input jar must not also sit on the module path
        res = run(JDK / "bin/jdeps", "--multi-release", "21", "--ignore-missing-deps", "--module-path", mp, "--generate-module-info", gen, jar)
        new = sorted({p.relative_to(gen).parts[0] for p in gen.glob("**/module-info.java")} - before)
        if res.returncode != 0 or not new:
            msg = [l for l in (res.stdout + res.stderr).strip().splitlines() if l.strip()]
            report.append((",".join(arts), "-", "-", "JDEPS_FAILED: " + (msg[0][:200] if msg else "no output"))); continue
        mod = new[0]
        src = next(gen.joinpath(mod).glob("**/module-info.java"))
        normalized, dropped = normalize(src.read_text(encoding="utf-8"), jar, all_classes)
        src.write_text(normalized, encoding="utf-8", newline="\n")
        text = src.read_bytes()
        target = "%s/%s/module-info.java" % (a.layer, mod)
        manifest.append((mod, ",".join(arts), ";".join(jars), target, hashlib.sha256(text).hexdigest(), str(src)))
        report.append((",".join(arts), "-", mod, "DESCRIPTOR_GENERATED" + (" (merged split-package group)" if len(arts) > 1 else "")
                       + ("; normalized: " + " | ".join(dropped) if dropped else "")))
    with io.open(a.out / "DESCRIPTOR_MANIFEST.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n")
        for m in manifest:
            f.write("\t".join(m) + "\n")
    with io.open(a.out / "DESCRIPTORS.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("artifact\tversion\tmodule\tresult\n")
        for x in report:
            f.write("\t".join(x) + "\n")
    from collections import Counter
    print(Counter(x[3].split(":")[0].split(" ")[0] for x in report))
    return 0


if __name__ == "__main__":
    sys.exit(main())
