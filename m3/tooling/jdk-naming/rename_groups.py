#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Rename the com.synexia Maven groupIds of the modules this repository builds (recipe JDK_NAMING_GROUPS,
decision 2026-11-10).

The JDK naming rename (jdk_naming.py) keeps every <groupId>com.synexia...</groupId> as a span (KEEP_SPAN GROUP_ID):
renaming a dependency on another repository would hide it from the standalone check. A group that a live pom of this
repository itself produces is no such dependency; it is a naming leftover (147 module groupIds and 71 coordinates on
master ec67062ba552). This recipe renames exactly those groups, wherever they are used as Maven coordinates, to this
repository's Maven naming (com.m3.collections, com.m3.tooling, com.m3.verification, ...):
  com.synexia -> com.m3, com.synexia.m3 -> com.m3, com.synexia.m3.<x> -> com.m3.<x>, com.synexia.<x> -> com.m3.<x>
Rewritten (bytes kept, CRLF too): in a Maven project file (a pom by name, or an XML file that is one by content), every
<groupId> element whose text is a produced group; in another script or configuration file, a pom snippet's <groupId>g
(inside a parent, dependency, plugin, extension or exclusion only when the artifact next to it is produced here), a
coordinate g:artifact of a produced module and -DgroupId=g. Prose that names the donor repository, a donor package or a
donor pin is no coordinate and is left as it is. Out of scope: the retained-variants archive, the pinned lineage crate
(m3/runtime-integration/regex-rope-affix-effects/), donor files recovered into custody (source-recovery/source-results/)
and this recipe crate (its fixtures spell the patterns).
Refused (nothing written): a renamed coordinate that collides with one already produced, and a reference to a produced
group whose artifact no live project file produces. Pins on the rewritten files are carried by reseal.py (--base the
tree before, --tree the renamed one). A second plan after apply finds nothing (fixed point).
CLI: python -I rename_groups.py plan|apply [--root DIR] [--tsv OUT]
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PREFIX = "com.synexia"
TARGET = "com.m3"
# the archive, the pinned lineage crate, this recipe crate, and donor files recovered into custody (their coordinates
# are the donor's own)
EXCLUDED = re.compile(r"^(?:m3/qualification/(?:[^/]+/)+retained-variants/|m3/runtime-integration/regex-rope-affix-effects/"
                      r"|m3/tooling/jdk-naming/)|/source-recovery/source-results/")
# the files a build, a script or a workflow reads coordinates from (the standalone check's scope plus XML); Java
# sources and JSON evidence carry no coordinate a build resolves and are left as they are
TEXT_SUFFIXES = (".yml", ".yaml", ".sh", ".py", ".mjs", ".js", ".properties", ".gradle", ".toml", ".cfg", ".ini", ".xml")
POM_NS = re.compile(r"^\{[^}]*\}")
GROUP_XML = re.compile(r"(<groupId>\s*)(com\.synexia(?:\.[A-Za-z_][\w]*)*)(\s*</groupId>)")
COORDINATE = re.compile(r"(?<![\w.:/-])(com\.synexia(?:\.[A-Za-z_][\w]*)*):([A-Za-z0-9_.-]+)")
D_GROUP = re.compile(r"(-DgroupId=)(com\.synexia(?:\.[A-Za-z_][\w]*)*)(?![\w.])")
REFERENCE_TAGS = ("<parent>", "<dependency>", "<plugin>", "<extension>", "<exclusion>")
ARTIFACT_XML = re.compile(r"<artifactId>\s*([^<\s]+)\s*</artifactId>")
PROJECT_XML = re.compile(r"<project[\s>]")


def target(group: str) -> str:
    rest = group[len(PREFIX):]
    if rest == ".m3" or rest.startswith(".m3."):
        rest = rest[len(".m3"):]
    return TARGET + rest


def is_pom(path: str) -> bool:
    name = path.rsplit("/", 1)[-1]
    return name == "pom.xml" or name.startswith("pom-") and name.endswith(".xml")


def is_project(path: str, text: str) -> bool:
    """A Maven project file: a pom by name, or an XML file that is one by content (analysis-pom.xml, retention-pom.xml)."""
    return is_pom(path) or path.endswith(".xml") and "<modelVersion>" in text and PROJECT_XML.search(text) is not None


def coordinates(text: str):
    """(own (group, artifact), [(tag, group, artifact)]) of a project file; (None, []) when it does not parse."""
    try:
        project = ET.fromstring(text)
    except ET.ParseError:
        return None, []

    def child(element, name):
        for c in element:
            if POM_NS.sub("", c.tag) == name:
                return (c.text or "").strip()
        return ""
    refs = [(POM_NS.sub("", e.tag), child(e, "groupId"), child(e, "artifactId")) for e in project.iter()
            if POM_NS.sub("", e.tag) in ("parent", "dependency", "plugin", "extension", "exclusion")]
    parent = next((g for t, g, _ in refs if t == "parent"), "")
    return (child(project, "groupId") or parent, child(project, "artifactId")), refs


def tracked(root: Path) -> list[str]:
    out = subprocess.run(["git", "-C", str(root), "ls-files", "-z"], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8", "surrogateescape").split("\0") if p]


def read(root: Path, path: str) -> str | None:
    try:
        return (root / path).read_bytes().decode("utf-8")
    except (OSError, UnicodeDecodeError):
        return None


def embedded(text: str, produced: set, mapping: dict) -> str:
    """Rename the groupIds of a pom snippet embedded in a script: a project's own group always; a group inside a
    parent, dependency, plugin, extension or exclusion only when the artifact next to it is produced here."""
    def rename(match):
        group = match.group(2)
        if group not in mapping:
            return match.group(0)
        before = text[max(0, match.start() - 300):match.start()]
        opened = max(before.rfind(tag) for tag in REFERENCE_TAGS)
        closed = max(before.rfind(tag.replace("<", "</")) for tag in REFERENCE_TAGS)
        if opened > closed:
            artifact = ARTIFACT_XML.search(text, match.end(), match.end() + 300)
            if artifact is None or (group, artifact.group(1)) not in produced:
                return match.group(0)
        return match.group(1) + mapping[group] + match.group(3)
    return GROUP_XML.sub(rename, text)


def plan(root: Path) -> tuple[dict, list, list, dict]:
    """(mapping, rewrites [(path, line, kind, before, after)], refusals, new texts)."""
    paths = [p for p in tracked(root) if not EXCLUDED.search(p) and (is_pom(p) or p.endswith(TEXT_SUFFIXES))]
    texts_before = {p: t for p in paths for t in [read(root, p)] if t is not None and (is_pom(p) or PREFIX in t)}
    projects = {p: t for p, t in texts_before.items() if is_project(p, t)}
    parsed = {p: coordinates(t) for p, t in projects.items()}
    produced = {own for own, _ in parsed.values() if own and own[1]}
    mapping = {g: target(g) for g, _ in produced if g == PREFIX or g.startswith(PREFIX + ".")}
    refusals = []
    for g, a in produced:
        if g in mapping and sum(1 for g2, a2 in produced if (mapping.get(g2, g2), a2) == (mapping[g], a)) > 1:
            refusals.append(f"COLLISION\t{g}:{a} -> {mapping[g]}:{a}")
    for p, (_own, refs) in parsed.items():
        for tag, g, a in refs:
            if g in mapping and a and (g, a) not in produced and tag != "exclusion":
                refusals.append(f"UNPRODUCED\t{p}\t{tag} {g}:{a}")
    rewrites, texts = [], {}
    for p, text in texts_before.items():
        if PREFIX not in text:
            continue
        if p in projects:
            new = GROUP_XML.sub(lambda m: m.group(1) + mapping.get(m.group(2), m.group(2)) + m.group(3), text)
        else:
            new = embedded(text, produced, mapping)
            new = COORDINATE.sub(lambda m: (mapping[m.group(1)] if (m.group(1), m.group(2)) in produced
                                            else m.group(1)) + ":" + m.group(2), new)
            new = D_GROUP.sub(lambda m: m.group(1) + mapping.get(m.group(2), m.group(2)), new)
        if new != text:
            kind = "POM" if p in projects else "TEXT"
            rewrites += [(p, n, kind, old.strip(), now.strip())
                         for n, (old, now) in enumerate(zip(text.split("\n"), new.split("\n")), 1) if old != now]
            texts[p] = new
    return mapping, rewrites, sorted(set(refusals)), texts


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("mode", choices=("plan", "apply"))
    parser.add_argument("--root", default=".")
    parser.add_argument("--tsv")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    mapping, rewrites, refusals, texts = plan(root)
    if args.tsv:
        Path(args.tsv).write_text("kind\tpath\tline\tbefore\tafter\n" + "".join(
            f"{k}\t{p}\t{n}\t{b}\t{a}\n" for p, n, k, b, a in rewrites), encoding="utf-8", newline="\n")
    for row in refusals:
        print(row)
    print(f"JDK_NAMING_GROUPS groups={len(mapping)} files={len(texts)} rewrites={len(rewrites)} refusals={len(refusals)}"
          + "".join(f" {g}->{t}" for g, t in sorted(mapping.items())))
    if refusals:
        return 1
    if args.mode == "apply":
        for path, text in texts.items():
            (root / path).write_bytes(text.encode("utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
