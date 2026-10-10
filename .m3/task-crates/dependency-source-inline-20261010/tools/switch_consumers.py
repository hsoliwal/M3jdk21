#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""C6 DEPENDENCY-SOURCE-INLINE, phase COMPOSE (consumers): point every Synexia consumer at the
source-built coordinate (<version>-synexia-source-1) of each admitted third_party module.

Edits are textual and minimal (one <version> value per dependency entry), applied to pom blobs through
git plumbing so that poms outside the sparse cone are changed in the index without a checkout:
  git show origin/develop:<path> -> edit -> git hash-object -w -> git update-index --cacheinfo.
Rules (recorded in CONSUMER_SWITCH.tsv):
  * BOM: suffix the <version> of the managed <dependency> entry for the artifact (the shared property
    such as ${jackson.version} is left alone so artifacts that were not inlined keep resolving).
  * explicit <version>X</version> pins in consumer poms: X -> X-synexia-source-1 when the module exists.
  * never switched: artifacts in NATIVE_IN_BINARY (natives live in the binary jar) and M3jdk21 consumers
    (no published repository for the source-built coordinate).
Dry-run by default; --apply writes the index entries (worktree files inside the cone are written too).
"""
from __future__ import annotations
import argparse, csv, io, re, subprocess, sys
from pathlib import Path

SUFFIX = "-synexia-source-1"
NATIVE_IN_BINARY = {"sqlite-jdbc", "jna", "zstd-jni"}
# (pom path, group, artifact, version) -> how the pin is expressed; "bom" = managed entry under dependencyManagement
TARGETS = [
    ("synexia-bom/pom.xml", "bom"),
    ("pom.xml", "bom"),
    ("synexia-indexstring/pom.xml", "explicit"),
    ("synexia-mindex/codec-toml/pom.xml", "explicit"),
    ("synexia-mindex/codec-yaml/pom.xml", "explicit"),
    ("synexia-mindex/runtime/pom.xml", "explicit"),
    ("synexia-mindex/integration/fixed-univocity/pom.xml", "explicit"),
    ("synexia-mindex/pom.xml", "explicit"),
    ("synexia-openrewrite-recipes/pom.xml", "explicit"),
    ("synexia-common/pom.xml", "explicit"),
]
DEP_RE = re.compile(r"<dependency>(.*?)</dependency>", re.S)


def git(repo: Path, *args: str, data: bytes | None = None) -> bytes:
    return subprocess.run(["git", "-C", str(repo)] + list(args), input=data, check=True, capture_output=True).stdout


def main() -> int:
    global SUFFIX
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", type=Path, required=True)
    ap.add_argument("--base", default="origin/develop")
    ap.add_argument("--modules", type=Path, required=True, help="THIRD_PARTY_MODULES.tsv")
    ap.add_argument("--out", type=Path, required=True, help="CONSUMER_SWITCH.tsv")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--targets", type=Path, help="optional file: one <pom path><TAB><mode> per line (default: the Synexia list)")
    ap.add_argument("--suffix", default=SUFFIX)
    args = ap.parse_args()
    SUFFIX = args.suffix
    targets = TARGETS if not args.targets else [tuple(l.rstrip(chr(10)).split(chr(9))[:2]) for l in io.open(args.targets, encoding="utf-8") if l.strip()]
    modules = {}
    for m in csv.DictReader(io.open(args.modules, encoding="utf-8", newline=""), delimiter="\t"):
        if m["kind"] == "MODULE":
            modules[(m["group"], m["artifact"], m["version"][: -len(SUFFIX)])] = m
    by_ga = {}
    for (g, a, v) in modules:
        by_ga.setdefault((g, a), set()).add(v)
    rows = []
    for path, mode in targets:
        try:
            original = git(args.repo, "show", "%s:%s" % (args.base, path)).decode("utf-8")
        except subprocess.CalledProcessError:
            rows.append((path, "", "", "", "", "POM_NOT_FOUND")); continue
        text = original
        props = dict(re.findall(r"<([A-Za-z0-9._-]+\.version)>([^<]*)</\1>", original))

        def resolve(v: str) -> str:
            m = re.fullmatch(r"\$\{([^}]+)\}", v.strip())
            return props.get(m.group(1), v) if m else v.strip()

        plugin_spans = [(m.start(), m.end()) for m in re.finditer(r"<plugin>.*?</plugin>", original, re.S)]

        def repl(match):
            block = match.group(0)
            if any(a <= match.start() < b for a, b in plugin_spans):
                g0 = re.search(r"<artifactId>([^<]*)</artifactId>", block)
                if g0:
                    rows.append((path, "", g0.group(1).strip(), "", "", "PLUGIN_DEPENDENCY_UNTOUCHED (plugins resolve from repositories, not the reactor)"))
                return block
            g = re.search(r"<groupId>([^<]*)</groupId>", block)
            a = re.search(r"<artifactId>([^<]*)</artifactId>", block)
            v = re.search(r"<version>([^<]*)</version>", block)
            if not (g and a and v):
                return block
            ga = (g.group(1).strip(), a.group(1).strip())
            raw = v.group(1)
            effective = resolve(raw)
            if raw.strip().endswith(SUFFIX):
                rows.append((path, ga[0], ga[1], raw.strip(), raw.strip(), "ALREADY_SOURCE", effective)); return block
            if ga not in by_ga:
                return block
            if effective not in by_ga[ga]:
                rows.append((path, ga[0], ga[1], raw.strip(), effective, "VERSION_NOT_INLINED (inlined: %s)" % ",".join(sorted(by_ga[ga])), effective)); return block
            if ga[1] in NATIVE_IN_BINARY:
                rows.append((path, ga[0], ga[1], raw.strip(), effective, "KEPT_BINARY_NATIVE_LIBRARIES", effective)); return block
            new_v = raw.strip() + SUFFIX
            rows.append((path, ga[0], ga[1], raw.strip(), new_v, "SWITCHED", effective))
            return block.replace("<version>%s</version>" % raw, "<version>%s</version>" % new_v, 1)

        text = DEP_RE.sub(repl, text)
        if text != original:
            if args.apply:
                blob = git(args.repo, "hash-object", "-w", "--stdin", data=text.encode("utf-8")).decode().strip()
                git(args.repo, "update-index", "--add", "--cacheinfo", "100644,%s,%s" % (blob, path))
                wt = args.repo / path
                if wt.exists():
                    wt.write_text(text, encoding="utf-8", newline="\n")
                print("applied", path, blob[:12], file=sys.stderr)
            else:
                print("would change", path, file=sys.stderr)
    args.out.write_text("\n".join(["pom\tgroup\tartifact\tdeclared\teffective_or_new\taction\tresolved"] + ["\t".join(r + ("",) * (7 - len(r))) for r in rows]) + "\n", encoding="utf-8", newline="\n")
    print("switch rows", len(rows), "switched", sum(1 for r in rows if r[5] == "SWITCHED"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
