#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe PROGRESS_MONITOR_UNIFY v1: one owner for the progress-monitor contract across the m3/** Maven modules.

Owner: jdk.m3.collections.M3ProgressMonitor (m3/collections). jdk.m3.algorithm.M3ProgressMonitor (m3/algorithms),
jdk.m3.file.M3ProgressMonitor (m3/ports/file) and jdk.m3.collections.M3PoolProgressMonitor (m3/collections, the
immutable-pool contract of #2516) become adapters that extend the owner (TEMPLATE rules, the postimage is a file under
templates/); jdk.m3.file.M3Progress, M3ProgressChild and M3ProgressState, duplicates of the owner's types with no user
left, are retired (RETIRE rules); poms, module descriptors, the reactor pom, BUILD.md and the two relocation recipes
whose FILE rows the adapters supersede (families-20261106: RELOCATION_MAP.tsv SUPERSEDED row, relocate.py,
differential.sh DEP_CP, verify.sh, PROVENANCE.tsv, README.md, RECIPE_MEMORY.tsv; immutable-pools-20261106: the same
minus the classpath, the owner is in the same module) get anchored EDIT or APPEND rules. DESIGN.md holds the decision
per type and the mapping tables; GATES.tsv the proof.

Hash-pinned: every rule names the git blob sha of its preimage at the base commit. A target must equal the preimage
(the rule is applied) or the postimage (already applied, no-op); anything else is a drifted preimage and the recipe
refuses (exit 2) without writing. Once applied, the preimage is read back with git cat-file so --check recomputes every
postimage from the pinned bytes. Payloads are written with LF and re-joined with the preimage's line ending; every
anchor must occur exactly once. Idempotent: a second run reports changed=0 and PLAN.tsv (file, kind, before blob,
before sha256, after sha256, rule note) is a fixed point; the per-run state (CHANGED, RETIRED, UNCHANGED) is printed.

usage: python -I unify.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "unify.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "make/m3/qualification/progress-monitor-unify-20261106"
BASE_COMMIT = "4072669632159f9cd58e3a5c8c185f09316edfe1"
OWNER = "jdk.m3.collections.M3ProgressMonitor"
PLAN_NAME = "PLAN.tsv"
PLAN_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "rule")
FAMILIES = "m3/algorithms/recipe/families-20261106"
DEPENDENCY = (
    "    <dependency>\n"
    "      <groupId>com.m3</groupId>\n"
    "      <artifactId>m3-collections</artifactId>\n"
    "      <version>0.1.0-SNAPSHOT</version>\n"
    "    </dependency>\n")
POOLS = "make/m3/collections/recipe/immutable-pools-20261106"
POOL_MONITOR_TARGET = "src/main/java/jdk/m3/collections/M3PoolProgressMonitor.java"
POOL_SUPERSEDED_NOTE = (
    "; superseded 2026-11-06 by " + RECIPE_DIR_REL + "/unify.py v1: an adapter over " + OWNER
    + " of the same module (begin -> beginTask, cancelled -> isCanceled, long units on both sides), donor blob "
    "6328187b7bf3e6dde317632c3178159c5053911c sha256 c35ebbac6a3cdaeea05df363263ad0c0667d885cbdd0db769fd4c32fea52b17d "
    "(URL by the REPO row scheme); relocate.py lists it and writes its donor source into the --donor-subset, never the "
    "target, so the donor side of differential.sh still compiles")
MONITOR_TARGET = "src/main/java/jdk/m3/algorithm/M3ProgressMonitor.java"
SUPERSEDED_NOTE = (
    "; superseded 2026-11-06 by " + RECIPE_DIR_REL + "/unify.py v1: an adapter over " + OWNER
    + " (int work units widen to long), donor blob 6d0b659af85339180c93248397794a96e65c784b sha256 "
    "c2c7d6112e03f6e35f95475d7bcad384c1ebf80b9a98b3469b85ae796dff7b2a (URL by the REPO row scheme); relocate.py "
    "no longer writes the target, differential.sh still compiles this donor source on the donor side")


class Refused(Exception):
    pass


# kind, path, preimage blob, note, payload. EDIT payload: list of ("sub", old, new) or ("re", pattern, repl).
RULES = (
    # --- m3/algorithms: adapter 1, int work units over the owner -------------------------------------------------
    ("TEMPLATE", "m3/algorithms/" + MONITOR_TARGET, "e6187b72560cc865ee120dc8189519bdacc02665",
     "adapter 1: interface extends the owner; int defaults widen to long; owner abstract methods get no-op defaults",
     "jdk.m3.algorithm.M3ProgressMonitor.java"),
    ("EDIT", "m3/algorithms/src/main/java/module-info.java", "52217b09fe91c5da0392242d8e2a79db5bea4bf1",
     "requires transitive jdk.m3.collections (exported adapter extends an owner type; -Xlint:exports)", [
         ("sub",
          " * donor (recipe/families-20261106). Java 21, java.base only.\n */\nmodule org.m3jdk.algorithms {\n"
          "    exports jdk.m3.algorithm;\n",
          " * donor (recipe/families-20261106). Java 21; requires jdk.m3.collections, the progress-monitor owner the\n"
          " * M3ProgressMonitor adapter extends (" + RECIPE_DIR_REL + ").\n */\nmodule org.m3jdk.algorithms {\n"
          "    requires transitive jdk.m3.collections;\n    exports jdk.m3.algorithm;\n")]),
    ("EDIT", "m3/algorithms/pom.xml", "8cb27d1ec4c2bd0d6ab8c27cb267abb55344f3f8",
     "dependency com.m3:m3-collections; description names the owner", [
         ("sub",
          "    every loop observes. Pure Java 21, no dependency outside java.base. Relocated by recipe\n",
          "    every loop observes (an adapter over the owner " + OWNER + " since\n"
          "    " + RECIPE_DIR_REL + "). Pure Java 21; com.m3:m3-collections is the only dependency\n"
          "    outside java.base. Relocated by recipe\n"),
         ("sub",
          "  <dependencies>\n    <dependency>\n      <groupId>org.junit.jupiter</groupId>\n",
          "  <dependencies>\n" + DEPENDENCY + "    <dependency>\n      <groupId>org.junit.jupiter</groupId>\n")]),
    # --- m3/ports/file: adapter 2, long units and the snapshot model over the owner; three duplicates retired ------
    ("TEMPLATE", "m3/ports/file/src/main/java/jdk/m3/file/M3ProgressMonitor.java",
     "223cad76aa790aa7ac7f9e352f8d58c82bce20fb",
     "adapter 2: interface extends the owner; keeps snapshot(), state() derived from it, covariant split, noop()",
     "jdk.m3.file.M3ProgressMonitor.java"),
    ("RETIRE", "m3/ports/file/src/main/java/jdk/m3/file/M3Progress.java", "0e97fae48ee4cb1b979a83f938eea068b0bc7564",
     "duplicate of the owner interface; users: only the retired child and the adapter's former extends", None),
    ("RETIRE", "m3/ports/file/src/main/java/jdk/m3/file/M3ProgressChild.java",
     "3ca57efb05802f13a1cdd0e8b15f175df1792472",
     "package-private duplicate of the owner's M3ChildProgressMonitor, implementation of the retired M3Progress.split",
     None),
    ("RETIRE", "m3/ports/file/src/main/java/jdk/m3/file/M3ProgressState.java",
     "630d2975a2110898a0a61036b557839e6ecbb33a",
     "identical record to jdk.m3.collections.M3ProgressState, which the adapter's state() now returns", None),
    ("EDIT", "m3/ports/file/src/main/java/module-info.java", "530c18ee421770fb1eebdb1815647167b3d97e40",
     "requires transitive jdk.m3.collections", [
         ("sub",
          "/** Whole MIndexFile family forward port over canonical JDK String owners and file metadata. */\n"
          "module org.m3jdk.file {\n  exports jdk.m3.file;\n",
          "/**\n * Whole MIndexFile family forward port over canonical JDK String owners and file metadata. The progress\n"
          " * contract is the owner " + OWNER + " (" + RECIPE_DIR_REL + ").\n */\n"
          "module org.m3jdk.file {\n  requires transitive jdk.m3.collections;\n  exports jdk.m3.file;\n")]),
    ("EDIT", "m3/ports/file/pom.xml", "0659305e01e01e48277415c6072a6878b29b17b0",
     "dependency com.m3:m3-collections; description names the owner", [
         ("sub",
          "    M3JniLibraryLoader with M3NativeLibrarySpec, M3Progress*, M3JsonStructuralIndex. Pure Java 21, no\n"
          "    dependency outside java.base; the natives under native/ are built by their CMakeLists.txt and loaded at\n",
          "    M3JniLibraryLoader with M3NativeLibrarySpec, M3ProgressMonitor (an adapter over the owner\n"
          "    " + OWNER + ", with the snapshot model), M3JsonStructuralIndex. Pure Java 21;\n"
          "    com.m3:m3-collections is the only dependency outside java.base; the natives under native/ are built by\n"
          "    their CMakeLists.txt and loaded at\n"),
         ("sub", "  </properties>\n  <build>\n",
          "  </properties>\n  <dependencies>\n" + DEPENDENCY + "  </dependencies>\n  <build>\n")]),
    ("EDIT", "m3/ports/file/BUILD.md", "b5603451d38d7ef620e745edd765dd210b1d830f", "the dependency sentence", [
         ("sub", "`com.m3:m3-ports-file:0.1.0-SNAPSHOT`, Java 21, no dependency outside `java.base`.\n",
          "`com.m3:m3-ports-file:0.1.0-SNAPSHOT`, Java 21; `com.m3:m3-collections` (the progress-monitor owner\n"
          "`" + OWNER + "`, which `jdk.m3.file.M3ProgressMonitor` extends since\n"
          "`" + RECIPE_DIR_REL + "`) is the only dependency outside `java.base`.\n")]),
    # --- reactor: two default modules now depend on m3-collections, so arrays and collections join the default set --
    ("EDIT", "make/m3/pom.xml", "d543afc55dcdc79e523f8c7b2b3956fc181f2645",
     "arrays and collections move from the m3-extended profile to the default modules (before ports/file); preimage re-pinned"
     " at 40726696321 (98deed79d8d registered ports/collections and native)", [
         ("sub", "        <module>arrays</module>\n        <module>collections</module>\n        <module>search</module>\n",
          "        <module>search</module>\n"),
         ("sub", "    <module>ports/file</module>\n",
          "    <module>arrays</module>\n    <module>collections</module>\n    <module>ports/file</module>\n")]),
    # --- families relocation recipe: the monitor row is SUPERSEDED, the relocated side needs the owner on its path ---
    ("EDIT", FAMILIES + "/RELOCATION_MAP.tsv", "e8f34ce4c70ad84ecbf912795b4b47e1ee9dd260",
     "FILE row of the monitor becomes SUPERSEDED (new kind); row-kinds comment and the NEC row record the decision", [
         ("sub", " -> a human decides equivalence.\n",
          " -> a human decides equivalence. SUPERSEDED: a FILE row a later recipe took over (named in the note): "
          "relocate.py no longer writes the target, differential.sh still compiles the donor source on the donor side.\n"),
         ("re", r"^FILE\t([^\t\n]+)\t" + re.escape(MONITOR_TARGET) + r"\t([^\n]*?)(\r?)$",
          r"SUPERSEDED\t\1\t" + MONITOR_TARGET + r"\t\2" + SUPERSEDED_NOTE.replace("\\", "\\\\") + r"\3"),
         ("sub", "one monitor owner (adapter or body edits) is a human decision\n",
          "one monitor owner (adapter or body edits) is a human decision; decided 2026-11-06: the owner is " + OWNER
          + " and jdk.m3.algorithm.M3ProgressMonitor its adapter (" + RECIPE_DIR_REL + ", see the SUPERSEDED row)\n")]),
    ("EDIT", FAMILIES + "/relocate.py", "24bc24ee2edd98b0ff49f0157b16f02dce178709", "accept the SUPERSEDED row kind", [
         ("sub", "The donor repository and its paths live only in RELOCATION_MAP.tsv.\n",
          "The donor repository and its paths live only in RELOCATION_MAP.tsv.\n"
          "A SUPERSEDED row is a FILE row a later recipe owns (named in its note): listed, never written.\n"),
         ("sub", '"DROP_LINE": {}, "DROP_DUMP": {}, "SKIP": [], "NEC": []}\n',
          '"DROP_LINE": {}, "DROP_DUMP": {}, "SKIP": [], "NEC": [], "SUPERSEDED": []}\n'),
         ("sub", '        elif kind in ("SKIP", "NEC"):\n', '        elif kind in ("SKIP", "NEC", "SUPERSEDED"):\n')]),
    ("EDIT", FAMILIES + "/PROVENANCE.tsv", "0ce3456121d63ed4fc8401f37d0f16859f927a8a",
     "drop the monitor row: relocate.py regenerates PROVENANCE.tsv from FILE rows only", [
         ("re", r"^[^\n]*\t" + re.escape(MONITOR_TARGET) + r"\t[^\n]*\n", "")]),
    ("EDIT", FAMILIES + "/differential.sh", "3bb76449610d9b444d5c2065119d5421d954e1d5",
     "donor side compiles FILE and SUPERSEDED rows; relocated side takes DEP_CP (owner and arrays jars)", [
         ("sub", "[cases-per-family]   (JAVA_HOME = JDK 21; after mvn package)\n",
          "[cases-per-family]   (JAVA_HOME = JDK 21; after mvn package)\n"
          "# env:   DEP_CP = the module's dependency jars for the relocated side (m3-collections and m3-arrays: the monitor\n"
          "#        is an adapter over jdk.m3.collections.M3ProgressMonitor), joined with ; on Windows and : elsewhere\n"),
         ("sub", "# 1. the donor main sources (FILE rows whose target lies under src/main/java), compiled from the donor tree\n",
          "# 1. the donor main sources (FILE and SUPERSEDED rows whose target lies under src/main/java), compiled from the\n"
          "#    donor tree; a SUPERSEDED row (the progress monitor) is owned by a later recipe on the relocated side only\n"),
         ("sub", "awk -F'\\t' '$1==\"FILE\" && $3 ~ /^src\\/main\\/java\\// {print $2}' \"$MAP\" | while read -r path; do\n",
          "awk -F'\\t' '($1==\"FILE\" || $1==\"SUPERSEDED\") && $3 ~ /^src\\/main\\/java\\// {print $2}' \"$MAP\""
          " | while read -r path; do\n"),
         ("sub", "'$1==\"FILE\" && $3 ~ /^src\\/main\\/java\\// && index($2, t) {print $2; exit}'",
          "'($1==\"FILE\" || $1==\"SUPERSEDED\") && $3 ~ /^src\\/main\\/java\\// && index($2, t) {print $2; exit}'"),
         ("sub", "-cp \"$MODULE/target/classes$SEP$MODULE/target/test-classes\" \"$DRIVER\"",
          "-cp \"$MODULE/target/classes$SEP$MODULE/target/test-classes${DEP_CP:+$SEP$DEP_CP}\" \"$DRIVER\"")]),
    ("EDIT", FAMILIES + "/verify.sh", "81f6f4a2f32aa16c93432afe6f3cf17b61d9d40e", "export DEP_CP from the Maven repository", [
         ("sub", "REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}\n",
          "REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}\n"
          "case \"$(uname -s)\" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac\n"
          "M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}   # on Windows pass MAVEN_REPO_LOCAL as a drive path\n"
          "GROUP=com.m3   # the group directory is spelled through the variable: the naming recipe rewrites it as a package path\n"
          "# the relocated side of the differential loads jdk.m3.collections.M3ProgressMonitor, the progress-monitor owner\n"
          "export DEP_CP=\"$M2/${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar$SEP"
          "$M2/${GROUP//.//}/m3-arrays/0.1.0-SNAPSHOT/m3-arrays-0.1.0-SNAPSHOT.jar\"\n")]),
    ("EDIT", FAMILIES + "/README.md", "c1a2784f2780b3e445757545c137a7fbe0bacb6c",
     "dependency sentence, monitor row, map and body_match counts", [
         ("sub", "Java 21, no dependency outside `java.base`. First leaf",
          "Java 21; `com.m3:m3-collections` (owner of the progress contract) is the only dependency outside `java.base`."
          " First leaf"),
         ("sub", "`checkCanceled()`, every method default, `none()` |\n",
          "`checkCanceled()`, every method default, `none()`. Since 2026-11-06 an adapter over"
          " `" + OWNER + "` (`" + RECIPE_DIR_REL + "`, int units widen to long); its map row is `SUPERSEDED` |\n"),
         ("sub", "1 `REPO`, 18 `FILE`,", "1 `REPO`, 17 `FILE`, 1 `SUPERSEDED` (the progress monitor, since 2026-11-06),"),
         ("sub", "`body_match=TRUE` for all 18 files", "`body_match=TRUE` for all 17 files")]),
    ("APPEND", FAMILIES + "/RECIPE_MEMORY.tsv", "c037cb19db31e682ae308974808f45dea8061f65", "lesson: SUPERSEDED row kind",
     "2026-11-06\trelocate.py\tv1\ta FILE row a later recipe takes over becomes SUPERSEDED (new kind, read like SKIP):"
     " relocate.py stops writing the target and its PROVENANCE row, --check stays at a fixed point, and differential.sh"
     " keeps compiling the donor source so the donor side still builds; the relocated side then needs the module's"
     " dependency jars (DEP_CP), because the superseding adapter extends " + OWNER
     + "\t" + RECIPE_DIR_REL + " (unify.py v1): relocate.py --check files=17 changed=0 body_match=17/17; differential"
     " sha256 f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9 unchanged on both sides\n"),
    # --- m3/collections: adapter 3, the immutable-pool contract (long units) over the owner of the same module ------
    ("TEMPLATE", "m3/collections/" + POOL_MONITOR_TARGET, "4fb1bc344e09b0dec4f9f78bea0aca7518de7265",
     "adapter 3: interface extends the owner; begin -> beginTask, cancelled -> isCanceled; owner abstract methods get"
     " no-op defaults; UNKNOWN, checkCancelled message, none() and None kept", "jdk.m3.collections.M3PoolProgressMonitor.java"),
    ("EDIT", POOLS + "/RELOCATION_MAP.tsv", "70f805ad4bfaba196768bc1d2e2c9ae3460eb14a",
     "FILE row of the pool monitor becomes SUPERSEDED (new kind); row-kinds comment and the NEC row record the decision", [
         ("sub", " NEC: equivalence not decided (hashes, dates and the suggestion recorded).\n",
          " NEC: equivalence not decided (hashes, dates and the suggestion recorded). SUPERSEDED: a FILE row a later recipe"
          " took over (named in the note): relocate.py lists it and writes its donor source into the --donor-subset, never"
          " the target.\n"),
         ("re", r"^FILE\t([^\t\n]+)\t" + re.escape(POOL_MONITOR_TARGET) + r"\t([^\n]*?)(\r?)$",
          r"SUPERSEDED\t\1\t" + POOL_MONITOR_TARGET + r"\t\2" + POOL_SUPERSEDED_NOTE.replace("\\", "\\\\") + r"\3"),
         ("re", r"^(NEC\t[^\t\n]*/ProgressMonitor\.java \(blob 6328187b7bf3\)[^\n]*?)(\r?)$",
          r"\1; decided 2026-11-06: the owner is " + OWNER + " and M3PoolProgressMonitor its adapter (" + RECIPE_DIR_REL
          + r", see the SUPERSEDED row)\2")]),
    ("EDIT", POOLS + "/relocate.py", "7086f7c497d80b919f1ab19e3f6f41e84cfe6f5b",
     "accept the SUPERSEDED row kind; the donor subset carries its donor source", [
         ("sub", "usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check] [--donor-subset DIR]\n",
          "usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check] [--donor-subset DIR]\n"
          "A SUPERSEDED row is a FILE row a later recipe owns (named in its note): listed, written verbatim into the donor\n"
          "subset, never relocated.\n"),
         ("sub", '             "INSERT_AFTER", "SKIP", "NEC")\n', '             "INSERT_AFTER", "SKIP", "NEC", "SUPERSEDED")\n'),
         ("sub", '    mapping.update({"FILE": [], "SKIP": [], "NEC": []})\n',
          '    mapping.update({"FILE": [], "SKIP": [], "NEC": [], "SUPERSEDED": []})\n'),
         ("sub", '        if kind in ("FILE", "SKIP", "NEC"):\n', '        if kind in ("FILE", "SKIP", "NEC", "SUPERSEDED"):\n'),
         ("sub", '    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()\n'
                 '    for donor_path, target in mapping["FILE"]:\n'
                 '        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()\n'
                 '        data = git(donor, "cat-file", "blob", blob)\n'
                 '        subset, counts = relocate(data, donor_path, blob, commit, mapping, True)\n',
          '    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()\n'
          '    for donor_path, target in mapping["FILE"] + mapping["SUPERSEDED"]:\n'
          '        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()\n'
          '        data = git(donor, "cat-file", "blob", blob)\n'
          '        subset, counts = relocate(data, donor_path, blob, commit, mapping, True)\n'),
         ("sub", 'files=%d dir=%s" % (commit, len(mapping["FILE"]), out)',
          'files=%d dir=%s" % (commit, len(mapping["FILE"]) + len(mapping["SUPERSEDED"]), out)')]),
    ("EDIT", POOLS + "/PROVENANCE.tsv", "d85f9dc0c9ed0396569065c77a31fcc56288941d",
     "drop the pool monitor row: relocate.py regenerates PROVENANCE.tsv from FILE rows only", [
         ("re", r"^[^\n]*\t" + re.escape(POOL_MONITOR_TARGET) + r"\t[^\n]*\n", "")]),
    ("EDIT", POOLS + "/README.md", "e45d18c1ce224ad9639fcf6ae45e373fc0c03f40", "monitor row, map and recipe counts", [
         ("sub", "all default, `none()`); carried under its own name, see NEC |\n",
          "all default, `none()`); carried under its own name, see NEC. Since 2026-11-06 an adapter over"
          " `" + OWNER + "` (`" + RECIPE_DIR_REL + "`: `begin` -> `beginTask`, `cancelled` -> `isCanceled`); its map row"
          " is `SUPERSEDED` |\n"),
         ("sub", "1 `REPO`, 13 `FILE`, 2 `PACKAGE`,",
          "1 `REPO`, 12 `FILE`, 1 `SUPERSEDED` (the pool monitor, since 2026-11-06), 2 `PACKAGE`,"),
         ("sub", "`files=13 changed=0 provenance=UNCHANGED body_match=13/13` |\n",
          "`files=12 changed=0 provenance=UNCHANGED body_match=12/12` (13/13 before the monitor row became `SUPERSEDED`,"
          " 2026-11-06) |\n")]),
    ("APPEND", POOLS + "/RECIPE_MEMORY.tsv", "998a53b6ef333100c89eee0d7bdf5a663491a0d0", "lesson: SUPERSEDED row kind",
     "2026-11-06\trelocate.py\tv2\ta FILE row a later recipe takes over becomes SUPERSEDED (new kind, read like SKIP, and"
     " --donor-subset still writes its donor source so the donor side of differential.sh compiles): relocate.py stops"
     " writing the target and its PROVENANCE row and --check stays at a fixed point; the superseding adapter lives in the"
     " same module as the owner, so the relocated side of the differential needs no extra classpath\t" + RECIPE_DIR_REL
     + " (unify.py v1): relocate.py --check files=12 changed=0 body_match=12/12; differential and oracle digests identical"
     " to GATES.tsv gate 4\n"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> bytes:
    return b"\r\n" if b"\r\n" in data else b"\n"


def with_eol(text: str, eol: bytes) -> str:
    return text.replace("\n", eol.decode())


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(kind: str, pre: bytes, payload, path: str) -> bytes | None:
    """The bytes a rule produces from its pinned preimage; None retires the file."""
    if kind == "RETIRE":
        return None
    eol = eol_of(pre)
    if kind == "TEMPLATE":
        lines = (RECIPE_DIR / "templates" / payload).read_text(encoding="utf-8").splitlines()
        return (eol.decode().join(lines) + eol.decode()).encode("utf-8")
    text = pre.decode("utf-8")
    if kind == "APPEND":
        if not text.endswith(eol.decode()):
            text += eol.decode()
        return (text + with_eol(payload, eol)).encode("utf-8")
    for edit in payload:
        text = apply_edit(text, edit, eol, path)
    return text.encode("utf-8")


def apply_edit(text: str, edit: tuple, eol: bytes, path: str) -> str:
    mode, old, new = edit
    if mode == "sub":
        old, new = with_eol(old, eol), with_eol(new, eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        return text.replace(old, new)
    text, count = re.subn(old, new, text, flags=re.MULTILINE)
    if count != 1:
        raise Refused("pattern matched %d times (need 1) in %s: %r" % (count, path, old[:60]))
    return text


def run(root: Path, check: bool) -> int:
    rows, changed = [], 0
    for kind, rel, blob, note, payload in RULES:
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        if on_disk is not None and blob_sha(on_disk) == blob:
            pre = on_disk
        else:
            pre = git_blob(root, blob)
        post = postimage(kind, pre, payload, rel)
        if on_disk is not None and blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        if on_disk is None and post is not None:
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        state = "UNCHANGED"
        if post is None:
            if on_disk is not None:
                state = "RETIRED"
                if not check:
                    target.unlink()
        elif on_disk != post:
            state = "CHANGED"
            if not check:
                target.write_bytes(post)
        changed += state != "UNCHANGED"
        rows.append((rel, kind, blob, sha256(pre), "-" if post is None else sha256(post), note))
        print("%s %s %s" % (kind, state, rel))
    plan = "\n".join(["\t".join(PLAN_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    plan_path = RECIPE_DIR / PLAN_NAME
    plan_state = not plan_path.exists() or plan_path.read_bytes() != plan.encode("utf-8")
    if plan_state and not check:
        plan_path.write_bytes(plan.encode("utf-8"))
    print("UNIFY recipe=%s %s base=%s owner=%s rules=%d changed=%d templates=%d edits=%d retired=%d plan=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], OWNER, len(rows), changed,
        sum(kind == "TEMPLATE" for kind, *_ in RULES), sum(kind in ("EDIT", "APPEND") for kind, *_ in RULES),
        sum(kind == "RETIRE" for kind, *_ in RULES), "CHANGED" if plan_state else "UNCHANGED"))
    return 1 if check and (changed or plan_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
