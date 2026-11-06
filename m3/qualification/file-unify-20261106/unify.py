#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FILE_UNIFY v1: one Maven module for the package jdk.m3.file (m3/file and m3/ports/file become m3/file).

The split package jdk.m3.file was owned by two modules: m3/file (descriptor jdk.m3.file, artifact com.m3:m3-file, 18
main source types at dec547222b30, java.base only) and m3/ports/file (descriptor org.m3jdk.file, artifact
com.m3:m3-ports-file, 82 main source types, four JNI sources, one qualification lane). This recipe moves every tracked file of m3/ports/file into m3/file (git mv, bytes
unchanged; native/ becomes src/main/native/), writes the union of the two module descriptors and of the two poms from
templates/ (descriptor org.m3jdk.file, artifact m3-file, requires transitive jdk.m3.collections, every plugin and
execution of both poms), adapts verify.py (module name, dependency module path) and rewrites the path strings that
name m3/ports/file or m3-ports-file in the moved BUILD.md and verify.sh, the consumers (m3/search/pom.xml, m3/pom.xml,
m3/fs and m3/eclipse/optional-jmod module descriptors and build.py, the m3fmt, jmod-receiver and eclipse-core
workflows, the nlp-search module pack, the two migration recipes and their tests, the character-flags differential,
m3/search/README.md, m3/file/NOTICE) and adds the Apache Commons Text attribution line of #2531 to
m3/THIRD_PARTY_NOTICES.md.

Held pairs (NEC, never decided here): jdk.m3.file.M3File and jdk.m3.file.M3FileDelta exist on both sides with
different bytes (different classes under one name since the JDK naming rename fe35ea98841 moved the ports package
into jdk.m3.file). Both files of each pair are moved under held/<side>/ next to this recipe and NEC.tsv records blobs,
sizes, dependents and lineage; the unified module does not compile until an owner names one side (see README.md).

Hash-pinned: every move and edit names the git blob of its preimage at BASE_COMMIT. A source must carry the pinned
blob (the move or edit is applied) or the target must already carry the postimage (applied, no-op); anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and the
ledgers MOVES.tsv (old path, new path, blob before, blob after, kind) and EDITS.tsv (file, before sha256, after
sha256, rule) are fixed points. Payload anchors must occur exactly the declared number of times.

usage: python -I unify.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import subprocess
import sys
from pathlib import Path

RECIPE = "unify.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "m3/qualification/file-unify-20261106"
BASE_COMMIT = "dec547222b30df77f758135609adf2b7bf4948d4"
OLD = "m3/ports/file"
NEW = "m3/file"
HELD_REL = RECIPE_DIR_REL + "/held"
MOVES_NAME = "MOVES.tsv"
EDITS_NAME = "EDITS.tsv"
MOVES_COLUMNS = ("old_path", "new_path", "blob_before", "blob_after", "kind")
EDITS_COLUMNS = ("file", "before_blob", "before_sha256", "after_sha256", "rule")
OLD_SRC = OLD + "/src/main/java/jdk/m3/file/"
NEW_SRC = NEW + "/src/main/java/jdk/m3/file/"

# Simple names that exist on both sides with different bytes: held, never picked (NEC.tsv).
HELD_PAIRS = ("M3File.java", "M3FileDelta.java")
# Files of m3/ports/file whose bytes are consumed by a UNION rule instead of being moved.
ABSORBED = {"pom.xml": NEW + "/pom.xml", "src/main/java/module-info.java": NEW + "/src/main/java/module-info.java"}
# Pinned blobs of the held pairs at BASE_COMMIT: (side, path, blob).
HELD_BLOBS = (
    ("file", NEW_SRC + "M3File.java", "288ddcff3fec30bb2a131e6759fb4a777cf63505"),
    ("file", NEW_SRC + "M3FileDelta.java", "0456626bdb7d0c490a83f0c3901000fb46efa724"),
    ("ports-file", OLD_SRC + "M3File.java", "06336f349498f4d4dfccbfbd9bd7cd8447d3daf9"),
    ("ports-file", OLD_SRC + "M3FileDelta.java", "72108c3448ba83d4d845634b49b9bc2b769d315b"),
)


class Refused(Exception):
    pass


def target_of(rel: str) -> str:
    """Where a file of m3/ports/file lives in m3/file: native/ becomes src/main/native/, the rest keeps its path."""
    if rel.startswith("native/"):
        return NEW + "/src/main/native/" + rel[len("native/"):]
    return NEW + "/" + rel


MIGRATION = "m3/tooling/migration-recipes/src/"
PATH_LITERAL = ("m3/ports/file/src/main/java/jdk/m3/file/", "m3/file/src/main/java/jdk/m3/file/")

# kind, path (after the moves), preimage blob at BASE_COMMIT, rule note, payload.
#   UNION:    payload = (template name, second preimage path, second preimage blob); the second preimage is absorbed.
#   TEMPLATE: payload = template name (whole postimage).
#   EDIT:     payload = list of ("sub", old, new) [exactly once] or ("subn", old, new, count) [exactly count times].
RULES = (
    ("UNION", NEW + "/src/main/java/module-info.java", "69d747ed456217624530aae1078b872f28b1699a",
     "descriptor org.m3jdk.file = union of jdk.m3.file (exports jdk.m3.file) and org.m3jdk.file (requires transitive"
     " jdk.m3.collections; exports jdk.m3.file, jdk.m3.file.json); neither side has opens, uses or provides",
     ("module-info.java", OLD + "/src/main/java/module-info.java", "985858e1561f3537737ae0f6466fba8abaaed4c6")),
    ("UNION", NEW + "/pom.xml", "c64ebfc3ccdf32808eea9855bf30cd9175b57d97",
     "artifact m3-file = union of both poms: dependencies m3-collections (ports) + junit-jupiter test (file); plugins"
     " compiler 3.13.0 (identical on both sides), surefire 3.2.5 useModulePath=false failIfNoTests=true (file),"
     " jar 3.4.2 (ports), exec 3.5.0 file-source-class-content-contract (file, plus --module-path), dependency 3.7.0"
     " properties (new: the dependency jars for verify.py); name and description from the ports pom, rewritten",
     ("pom.xml", OLD + "/pom.xml", "f928fb9071a469d768df730710f296a68566e397")),
    ("EDIT", NEW + "/verify.py", "e43693ff159b5fa74910cfa092c27030cac2782c",
     "module name constant MODULE = org.m3jdk.file (was the literal jdk.m3.file, 15 occurrences) and --module-path for"
     " the dependency modules on every javac/java/jlink module path; no contract, marker or inventory changed;"
     " preimage re-pinned at dec547222b30 (#2531 added the bounded probe lane, TEMPLATE became anchored EDIT rows)", [
         ("sub", "# Modified 2026-11-05: Isolate mutable contract fixtures from the source/evidence tree.\n",
          "# Modified 2026-11-05: Isolate mutable contract fixtures from the source/evidence tree.\n"
          "# Modified 2026-11-06 by " + RECIPE_DIR_REL + " (FILE_UNIFY v1): the module is org.m3jdk.file\n"
          "# and requires jdk.m3.collections: the module name is a constant, --module-path carries the dependency modules.\n"),
         ("sub", "import uuid\n\n\n",
          "import uuid\n\n#FLLDCIM: FILE_UNIFY - module org.m3jdk.file (union of jdk.m3.file and org.m3jdk.file), dependency"
          " modules on --module-path\nMODULE = 'org.m3jdk.file'\n\n\n"),
         ("sub", "    parser.add_argument('--classes', type=Path, help='Reuse the classes already compiled by Maven')\n",
          "    parser.add_argument('--classes', type=Path, help='Reuse the classes already compiled by Maven')\n"
          "    parser.add_argument('--module-path', default='',\n"
          "                        help='Dependency modules the module requires (jdk.m3.collections, jdk.m3.arrays: jars or '\n"
          "                             'exploded directories joined with the platform path separator)')\n"),
         ("sub", "    suffix = '.exe' if os.name == 'nt' else ''\n",
          "    suffix = '.exe' if os.name == 'nt' else ''\n"
          "    dependency_modules = [entry for entry in args.module_path.split(os.pathsep) if entry]\n\n"
          "    def module_path(*entries):\n"
          "        return os.pathsep.join([str(entry) for entry in entries] + dependency_modules)\n"),
         ("sub", "        'scope': 'jdk.m3.file module and direct compiler fixture',\n",
          "        'scope': MODULE + ' module (packages jdk.m3.file, jdk.m3.file.json) and direct compiler fixture',\n"
          "        'module': MODULE,\n        'dependency_module_path': dependency_modules,\n"),
         ("subn", "raise RuntimeError('missing original jdk.m3.file module descriptor')",
          "raise RuntimeError('missing original ' + MODULE + ' module descriptor')", 2),
         ("sub", "            run('compile-module', [tool('javac'), '--release', '21', '-Xlint:all,-module', '-Werror',\n"
                 "                                   '-d', classes,",
          "            run('compile-module', [tool('javac'), '--release', '21', '-Xlint:all,-module', '-Werror',\n"
          "                                   *(['--module-path', module_path()] if dependency_modules else []),\n"
          "                                   '-d', classes,"),
         ("sub", "if 'jdk.m3.file' not in descriptor or", "if MODULE not in descriptor or"),
         ("sub", "'--module-path', module_jar, '--add-modules', 'jdk.m3.file',\n",
          "'--module-path', module_path(module_jar), '--add-modules', MODULE,\n"),
         ("sub", "[tool('java'), '--module-path', module_jar, '--add-modules',\n"
                 "                                    'jdk.m3.file,jdk.compiler', '-cp', tests, main_class])",
          "[tool('java'), '--module-path', module_path(module_jar), '--add-modules',\n"
          "                                    MODULE + ',jdk.compiler', '-cp', tests, main_class])"),
         ("sub", "'--module-path', module_jar, '--patch-module', 'jdk.m3.file=' + str(root / 'src/test/java'),",
          "'--module-path', module_path(module_jar), '--patch-module', MODULE + '=' + str(root / 'src/test/java'),"),
         ("sub", "command += ['--add-modules', 'jdk.m3.file,jdk.compiler,jdk.zipfs', '--patch-module',\n"
                 "                            'jdk.m3.file=' + str(module_tests), '-m', 'jdk.m3.file/jdk.m3.file.' + name]",
          "command += ['--add-modules', MODULE + ',jdk.compiler,jdk.zipfs', '--patch-module',\n"
          "                            MODULE + '=' + str(module_tests), '-m', MODULE + '/jdk.m3.file.' + name]"),
         ("sub", "command += ['--add-modules', 'jdk.m3.file', '--patch-module',\n"
                 "                                'jdk.m3.file=' + str(module_tests), '-m',\n"
                 "                                'jdk.m3.file/jdk.m3.file.' + name, argument]",
          "command += ['--add-modules', MODULE, '--patch-module',\n"
          "                                MODULE + '=' + str(module_tests), '-m',\n"
          "                                MODULE + '/jdk.m3.file.' + name, argument]"),
         ("sub", "tool('java'), 'host', module_jar, host_has_javac=True)",
          "tool('java'), 'host', module_path(module_jar), host_has_javac=True)"),
         ("sub", "'--module-path', os.pathsep.join((str(toolchain / 'jmods'),\n"
                 "                                 str(module_jar))), '--add-modules', 'jdk.m3.file,jdk.compiler,jdk.zipfs',",
          "'--module-path', module_path(toolchain / 'jmods', module_jar),\n"
          "                                 '--add-modules', MODULE + ',jdk.compiler,jdk.zipfs',"),
         ("sub", "        if 'jdk.m3.file' not in {line.partition('@')[0] for line in modules.splitlines()}:\n"
                 "            raise RuntimeError('jdk.m3.file is absent from linked image')\n",
          "        if MODULE not in {line.partition('@')[0] for line in modules.splitlines()}:\n"
          "            raise RuntimeError(MODULE + ' is absent from linked image')\n"),
         ("sub", "[image / 'bin' / ('java' + suffix), '--add-modules',\n"
                 "                                       'jdk.m3.file,jdk.compiler', '-cp', tests, main_class])",
          "[image / 'bin' / ('java' + suffix), '--add-modules',\n"
          "                                       MODULE + ',jdk.compiler', '-cp', tests, main_class])")]),
    ("EDIT", NEW + "/BUILD.md", "1d2be83dd12d2ae4f0ceeb9fec0d4cb3688ca9e3",
     "path strings: module directory, artifact, reactor module id, native directory, qualification path", [
         ("sub", "# m3/ports/file build\n", "# m3/file build\n"),
         ("sub", "`com.m3:m3-ports-file:0.1.0-SNAPSHOT`, Java 21;", "`com.m3:m3-file:0.1.0-SNAPSHOT`, Java 21;"),
         ("sub", "`mvn -f m3/ports/file/pom.xml package`", "`mvn -f m3/file/pom.xml package`"),
         ("sub", "`mvn -f m3/pom.xml -pl ports/file package` (`ports/file` is a default module of `m3/pom.xml`)",
          "`mvn -f m3/pom.xml -pl file package` (`file` is a default module of `m3/pom.xml`)"),
         ("sub", "`cmake -S m3/ports/file/native -B <build> && cmake --build <build>`",
          "`cmake -S m3/file/src/main/native -B <build> && cmake --build <build>`"),
         ("sub", "`bash m3/ports/file/qualification/m3fmt-shared-lexicon-20261021/verify.sh`",
          "`bash m3/file/qualification/m3fmt-shared-lexicon-20261021/verify.sh`"),
         ("sub", "  <artifactId>m3-ports-file</artifactId>\n", "  <artifactId>m3-file</artifactId>\n")]),
    ("EDIT", NEW + "/qualification/m3fmt-shared-lexicon-20261021/verify.sh", "dd747b121aece1128f9b0471431f23994397b240",
     "path strings: repository root is four levels up (was five), owner m3/file, natives under src/main/native", [
         ("sub", '/../../../../.." && pwd)"\n', '/../../../.." && pwd)"\n'),
         ("sub", 'OWNER="$ROOT/m3/ports/file"\n', 'OWNER="$ROOT/m3/file"\n'),
         ("sub", '"$OWNER/native/m3fmt_varints_jni.c"', '"$OWNER/src/main/native/m3fmt_varints_jni.c"'),
         ("sub", 'cmake -S "$OWNER/native"', 'cmake -S "$OWNER/src/main/native"')]),
    ("EDIT", "m3/search/pom.xml", "d4cb187e342d8c2c2ad1b9a2b7e7bfe2e6b37953", "consumer: artifact m3-ports-file -> m3-file", [
         ("sub", "<artifactId>m3-ports-file</artifactId>", "<artifactId>m3-file</artifactId>")]),
    ("EDIT", "m3/search/README.md", "5060ab09ea7088acd3abc7957e95b16acb57cf92", "path string of the loader owner", [
         ("sub", "(m3/ports/file, #2492).", "(m3/file since " + RECIPE_DIR_REL + ", #2492).")]),
    ("EDIT", "m3/pom.xml", "9b9d2c0cbf4805c932f81061ef710c9c486f1a25",
     "reactor module ports/file -> file at the same position (after collections, its dependency); preimage re-pinned"
     " at dec547222b30 (#2528 registered compiler/nullness-facts after compiler/lifetime-slots)", [
         ("sub", "    <module>ports/file</module>\n", "    <module>file</module>\n")]),
    ("EDIT", "m3/fs/src/main/module/module-info.java", "368d4f71cecd79cb185c7a492aeeb9a3f1ac0e09",
     "consumer descriptor: requires jdk.m3.file -> org.m3jdk.file (the unified module name)", [
         ("sub", "    requires jdk.m3.file;\n", "    requires org.m3jdk.file;\n")]),
    ("EDIT", "m3/eclipse/optional-jmod/src/main/java/module-info.java", "86ce3fe61925dfdb5aa4e95a70cebbdcd32c1ee3",
     "consumer descriptor: requires transitive jdk.m3.file -> org.m3jdk.file", [
         ("sub", "    requires transitive jdk.m3.file;\n", "    requires transitive org.m3jdk.file;\n")]),
    ("EDIT", "m3/eclipse/optional-jmod/build.py", "93d10ef3bc46a65bdcf0e89bd8594edbb7284720",
     "consumer build: the m3/file source root is staged as module org.m3jdk.file (after jdk.m3.collections, which it"
     " requires)", [
         ("sub", '    ("jdk.m3.file", ROOT / "m3/file/src/main/java", None),\n',
          '    ("org.m3jdk.file", ROOT / "m3/file/src/main/java", None),\n')]),
    ("EDIT", ".github/workflows/m3-eclipse-core-optional-jmod.yml", "fccedbe89d095536d2198d2801296f159b7207a1",
     "descriptor check names the unified module", [
         ("sub", "          grep -q 'jdk.m3.file' proof/eclipse-core-module-descriptor.txt\n",
          "          grep -q 'org.m3jdk.file' proof/eclipse-core-module-descriptor.txt\n")]),
    ("EDIT", NEW + "/NOTICE", "17f3b32dc24284ef056d97a1bff0e741b81048cf", "module name of the unified module", [
         ("sub", "M3 file family (module jdk.m3.file)\n", "M3 file family (module org.m3jdk.file)\n")]),
    ("EDIT", "m3/THIRD_PARTY_NOTICES.md", "2a773f58781c435a5a93f88b4778e13cf063e67d",
     "one attribution line for Apache Commons Text (LevenshteinDistance rolling-row lineage of #2531), wording of"
     " m3/file/NOTICE", [
         ("sub", "`m3/compatibility/synexia-*-receipt.json` and `m3/docs/name-mapping.d/*.json`. Nothing in this\n"
                 "section requires a third-party notice.\n",
          "`m3/compatibility/synexia-*-receipt.json` and `m3/docs/name-mapping.d/*.json`. Nothing in this\n"
          "section requires a third-party notice.\n\n"
          "Attribution (2026-11-06, `m3/file`): `jdk.m3.file.M3ChunkDeltaGenerator.editDistance`"
          " (`m3/file/recipe/delta-generator-20261106`) adapts the rolling-row recurrence of Apache Commons Text"
          " `LevenshteinDistance` (`src/main/java/org/apache/commons/text/similarity/LevenshteinDistance.java` at"
          " `fcf5f4838bd59a303043c1b5e3b0b48b36452664`), Apache License 2.0 (upstream NOTICE: Apache Commons Text,"
          " Copyright 2014-2026 The Apache Software Foundation; this product includes software developed at The Apache"
          " Software Foundation, https://www.apache.org/); no Commons Text code is redistributed and no runtime"
          " dependency on Commons Text exists; the upstream LICENSE and NOTICE are kept verbatim under"
          " `m3/file/recipe/delta-generator-20261106/donor/` and `m3/file/NOTICE` carries the module notice.\n")]),
    ("EDIT", ".github/workflows/m3fmt-shared-lexicon-20261021.yml", "b53cade39909bed9ecc70bf3a8448cbd5aff1931",
     "path triggers and the verify.sh path", [
         ("sub", "      - 'm3/ports/file/src/main/java/jdk/m3/file/M3Fmt*.java'\n",
          "      - 'm3/file/src/main/java/jdk/m3/file/M3Fmt*.java'\n"),
         ("sub", "      - 'm3/ports/file/native/m3fmt_varints_jni.c'\n", "      - 'm3/file/src/main/native/m3fmt_varints_jni.c'\n"),
         ("sub", "      - 'm3/ports/file/qualification/m3fmt-shared-lexicon-20261021/**'\n",
          "      - 'm3/file/qualification/m3fmt-shared-lexicon-20261021/**'\n"),
         ("sub", "        run: bash m3/ports/file/qualification/m3fmt-shared-lexicon-20261021/verify.sh\n",
          "        run: bash m3/file/qualification/m3fmt-shared-lexicon-20261021/verify.sh\n")]),
    ("EDIT", ".github/workflows/m3sdk-jmod-receiver.yml", "54cd42d687f3f99c2f817526d5254a9361bb904f", "path trigger", [
         ("sub", "      - 'm3/ports/file/**'\n", "      - 'm3/file/**'\n")]),
    ("EDIT", "m3/tooling/module-packs/nlp-search/build.py", "442bf36b866e5c36f329f9426f199e8df40c7574",
     "path strings: module source root and native directory (the INPUTS.json lock keeps its 2026-10-18 inventory)", [
         ("sub", 'root / "m3/ports/file/src/main/java")', 'root / "m3/file/src/main/java")'),
         ("sub", '(root / "m3/ports/file/native").glob("*.c")', '(root / "m3/file/src/main/native").glob("*.c")'),
         ("sub", '"-I" + str(root / "m3/ports/file/native")', '"-I" + str(root / "m3/file/src/main/native")')]),
    ("EDIT", "m3/tooling/module-packs/nlp-search/README.md", "9d668154c0c35c108d69064038a9c1fcff64f822",
     "path strings: the module link and the indexed sample path", [
         ("sub", "[`m3/ports/file`](../../../ports/file).", "[`m3/file`](../../../file)."),
         ("sub", "  m3/ports/file/src/main/java/jdk/m3/file/M3File.java\n", "  m3/file/src/main/java/jdk/m3/file/M3File.java\n")]),
    ("EDIT", MIGRATION + "main/java/jdk/m3/rewrite/backport/M3MopStreamTraversalRecipe.java",
     "f51243eac16d3c05b0ca594f65b4a5668fb427a2", "source-path literals of the recipe (3)", [("subn", *PATH_LITERAL, 3)]),
    ("EDIT", MIGRATION + "main/java/jdk/m3/rewrite/backport/M3FileByteEventFactRecipe.java",
     "db57826c324d831c50b09fd9a31b07465b70267d", "source-path literals of the recipe (2)", [("subn", *PATH_LITERAL, 2)]),
    ("EDIT", MIGRATION + "test/java/jdk/m3/rewrite/backport/M3FileByteEventFactRecipeTest.java",
     "e3800b435b42c8de7b385d957c0454f07c80570c", "source-path literals of the recipe test (7)", [("subn", *PATH_LITERAL, 7)]),
    ("EDIT", MIGRATION + "test/java/jdk/m3/rewrite/backport/M3MopStreamTraversalRecipeTest.java",
     "a82f4edd7c0bf679d66532db952b9b069833ad06", "source-path literals of the recipe test (3)", [("subn", *PATH_LITERAL, 3)]),
    ("EDIT", "m3/ports/indexstring/recipe/character-flags-20261106/differential.sh",
     "690e885e063ea148142d51be12744b11cd8765df", "path string of the pre-combine sibling (comment and SIBLING)", [
         ("sub", "jdk.m3.file.M3CharacterFlags (m3/ports/file) must be byte-identical",
          "jdk.m3.file.M3CharacterFlags (m3/file) must be byte-identical"),
         ("sub", 'SIBLING="$ROOT/m3/ports/file/src/main/java/jdk/m3/file/M3CharacterFlags.java"\n',
          'SIBLING="$ROOT/m3/file/src/main/java/jdk/m3/file/M3CharacterFlags.java"\n')]),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git(root: Path, *args: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        detail = getattr(failure, "stderr", b"") or b""
        raise Refused("git %s failed: %s %s" % (" ".join(args[:3]), failure, detail.decode(errors="replace").strip()))


def git_blob(root: Path, blob: str) -> bytes:
    return git(root, "cat-file", "blob", blob)


def base_tree(root: Path, prefix: str) -> list[tuple[str, str]]:
    """(path, blob) of every file under prefix at BASE_COMMIT."""
    rows = []
    for line in git(root, "ls-tree", "-r", BASE_COMMIT, "--", prefix).decode().splitlines():
        meta, path = line.split("\t", 1)
        rows.append((path, meta.split()[2]))
    return rows


def plan_moves(root: Path) -> list[tuple[str, str, str, str]]:
    """(old, new, blob, kind) for every file of m3/ports/file and the held files of m3/file."""
    moves = []
    for path, blob in base_tree(root, OLD):
        rel = path[len(OLD) + 1:]
        if rel in ABSORBED:
            moves.append((path, ABSORBED[rel], blob, "ABSORBED"))
        elif path.startswith(OLD_SRC) and rel.rsplit("/", 1)[-1] in HELD_PAIRS:
            moves.append((path, HELD_REL + "/ports-file/" + rel, blob, "HELD"))
        else:
            moves.append((path, target_of(rel), blob, "MOVE"))
    for side, path, blob in HELD_BLOBS:
        if side == "file":
            moves.append((path, HELD_REL + "/file/" + path[len(NEW) + 1:], blob, "HELD"))
    pinned = {(path, blob) for _, path, blob in HELD_BLOBS}
    at_base = dict(base_tree(root, NEW_SRC))
    for old, new, blob, kind in moves:
        if kind == "HELD" and ((old, blob) not in pinned or at_base.get(old, blob) != blob):
            raise Refused("held file %s has blob %s at the base commit, HELD_BLOBS pins another" % (old, blob))
    return moves


def eol_of(data: bytes) -> bytes:
    return b"\r\n" if b"\r\n" in data else b"\n"


def with_eol(text: str, eol: bytes) -> str:
    return text.replace("\n", eol.decode())


def apply_edit(text: str, edit: tuple, eol: bytes, path: str) -> str:
    mode, old, new, *rest = edit
    expected = rest[0] if rest else 1
    old, new = with_eol(old, eol), with_eol(new, eol)
    count = text.count(old)
    if count != expected:
        raise Refused("anchor occurs %d times (need %d) in %s: %r" % (count, expected, path, old[:60]))
    return text.replace(old, new)


def template(name: str, eol: bytes) -> bytes:
    lines = (RECIPE_DIR / "templates" / name).read_text(encoding="utf-8").splitlines()
    return (eol.decode().join(lines) + eol.decode()).encode("utf-8")


def postimage(kind: str, pre: bytes, payload, path: str) -> bytes:
    eol = eol_of(pre)
    if kind == "UNION":
        return template(payload[0], eol)
    if kind == "TEMPLATE":
        return template(payload, eol)
    text = pre.decode("utf-8")
    for edit in payload:
        text = apply_edit(text, edit, eol, path)
    return text.encode("utf-8")


def read(root: Path, rel: str) -> bytes | None:
    target = root / rel
    return target.read_bytes() if target.is_file() else None


def move(root: Path, old: str, new: str, check: bool) -> None:
    if check:
        return
    (root / new).parent.mkdir(parents=True, exist_ok=True)
    git(root, "mv", "--", old, new)


def run_moves(root: Path, moves, edited: dict, check: bool) -> tuple[list, int]:
    """Moves first; a moved file that an EDIT rule rewrites is complete when its target holds the pinned blob or the
    postimage (recorded by run_edits through `edited`)."""
    rows, changed = [], 0
    for old, new, blob, kind in moves:
        on_old = read(root, old)
        on_new = read(root, new)
        if kind == "ABSORBED":
            state = "UNCHANGED" if on_old is None else "ABSORBED"
            if on_old is not None and blob_sha(on_old) != blob:
                raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (old, blob_sha(on_old), blob))
            rows.append((old, new, blob, "-", kind))
            changed += state != "UNCHANGED"
            print("%s %s %s -> %s" % (kind, state, old, new))
            continue
        if on_old is not None:
            if blob_sha(on_old) != blob:
                raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (old, blob_sha(on_old), blob))
            if on_new is not None:
                raise Refused("both %s and %s exist; the move cannot be applied" % (old, new))
            move(root, old, new, check)
            state = "MOVED"
        elif on_new is not None:
            after = blob_sha(on_new)
            if after != blob and new not in edited:
                raise Refused("drifted target %s: on disk %s, pinned blob %s" % (new, after, blob))
            state = "UNCHANGED"
        else:
            raise Refused("missing preimage %s (pinned blob %s) and no target %s" % (old, blob, new))
        rows.append((old, new, blob, blob, kind))
        changed += state != "UNCHANGED"
        print("%s %s %s -> %s" % (kind, state, old, new))
    return rows, changed


def run_edits(root: Path, moves, check: bool) -> tuple[list, int, dict]:
    pending = {new: old for old, new, _, kind in moves if kind == "MOVE"}
    rows, changed, edited = [], 0, {}
    for kind, rel, blob, note, payload in RULES:
        on_disk = read(root, rel)
        if on_disk is None and check and rel in pending:
            on_disk = read(root, pending[rel])   # --check before the moves: the preimage still sits at the old path
        pre = on_disk if on_disk is not None and blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(kind, pre, payload, rel)
        if on_disk is None:
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        if kind == "UNION":
            second = read(root, payload[1])
            if second is not None and blob_sha(second) != payload[2]:
                raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (payload[1], blob_sha(second), payload[2]))
            if second is not None and not check:
                git(root, "rm", "-q", "--", payload[1])
        state = "UNCHANGED"
        if on_disk != post:
            state = "CHANGED"
            if not check:
                (root / rel).write_bytes(post)
                git(root, "add", "--", rel)
        changed += state != "UNCHANGED"
        edited[rel] = blob_sha(post)
        rows.append((rel, blob, sha256(pre), sha256(post), kind + ": " + note))
        print("%s %s %s" % (kind, state, rel))
    return rows, changed, edited


def prune(root: Path, planned: set, check: bool) -> list[str]:
    """Removes the emptied m3/ports/file tree; returns the files that keep it alive (never deleted). Under --check the
    files a run would move are not leftovers."""
    base = root / OLD
    if not base.exists():
        return []
    leftovers = [str(path.relative_to(root)).replace(os.sep, "/") for path in base.rglob("*") if path.is_file()]
    if check:
        leftovers = [path for path in leftovers if path not in planned]
    if leftovers or check:
        return leftovers
    for directory in sorted((path for path in base.rglob("*") if path.is_dir()), reverse=True):
        directory.rmdir()
    base.rmdir()
    return []


def write_ledger(name: str, columns, rows, check: bool) -> bool:
    text = ("\n".join(["\t".join(columns)] + ["\t".join(row) for row in rows]) + "\n").encode("utf-8")
    path = RECIPE_DIR / name
    state = not path.exists() or path.read_bytes() != text
    if state and not check:
        path.write_bytes(text)
    return state


def run(root: Path, check: bool) -> int:
    moves = plan_moves(root)
    # Edits whose target is a moved file run after the moves; on a second run the moved target already carries the
    # postimage, so the moves need the postimage blobs first.
    edited = {rel: None for kind, rel, *_ in RULES}
    move_rows, moved = run_moves(root, moves, edited, check)
    edit_rows, changed, edited = run_edits(root, moves, check)
    for index, (old, new, before, after, kind) in enumerate(move_rows):
        if new in edited and kind == "MOVE":
            move_rows[index] = (old, new, before, edited[new], "MOVE+EDIT")
    leftovers = prune(root, {old for old, *_ in moves}, check)
    for path in leftovers:
        print("LEFTOVER %s" % path)
    ledgers = write_ledger(MOVES_NAME, MOVES_COLUMNS, move_rows, check)
    ledgers |= write_ledger(EDITS_NAME, EDITS_COLUMNS, edit_rows, check)
    kinds = [row[4] for row in move_rows]
    print("UNIFY recipe=%s %s base=%s moves=%d held=%d absorbed=%d edits=%d changed=%d leftovers=%d ledgers=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], sum(kind.startswith("MOVE") for kind in kinds), kinds.count("HELD"),
        kinds.count("ABSORBED"), len(edit_rows), moved + changed, len(leftovers), "CHANGED" if ledgers else "UNCHANGED"))
    if leftovers and not check:
        return 2
    return 1 if check and (moved or changed or ledgers or leftovers) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
