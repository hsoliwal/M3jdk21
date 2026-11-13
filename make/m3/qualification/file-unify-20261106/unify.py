#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FILE_UNIFY v2: one Maven module for the package jdk.m3.file (m3/file and m3/ports/file become m3/file).

The split package jdk.m3.file was owned by two modules: m3/file (descriptor jdk.m3.file, artifact com.m3:m3-file, 18
main source types, java.base only) and m3/ports/file (descriptor org.m3jdk.file, artifact com.m3:m3-ports-file, 82 main
source types, four JNI sources, one qualification lane). This recipe moves every tracked file of m3/ports/file into
m3/file (git mv, bytes unchanged; native/ becomes src/main/native/), writes the union of the two module descriptors and
of the two poms from templates/ (descriptor org.m3jdk.file, artifact m3-file, requires transitive jdk.m3.collections,
every plugin and execution of both poms), adapts verify.py (module name, dependency module path) and rewrites the path
strings that name m3/ports/file or m3-ports-file in the moved BUILD.md and verify.sh and in the consumers.

Name collision (v1 held it, NEC; decided 2026-11-06 by the naming owner, lane session 45): jdk.m3.file.M3File
and jdk.m3.file.M3FileDelta named two different types, one per former module, since the JDK naming rename
fe35ea98841 moved the ports package into jdk.m3.file. The donor-mapped side keeps the name (MIndexFile -> M3File, the
token file of m3/ports/file, and its record M3FileDelta); the m3/file side, the NIO read boundary adapted from
MIndexResourceAccess, takes the role prefix: M3NioFile and M3NioFileDelta (RENAME rows: git mv plus the token rename).
Every user of the NIO pair follows through declared rows: "words" renames every whole-word M3FileDelta and M3File token
of a file whose tokens all mean the NIO pair (in-module m3/file users, m3/fs, eclipse optional-jmod, native-api);
"qualified" renames only jdk.m3.file.M3File(Delta) where a file also uses another M3File (jdk.m3.parser.M3File in the
parser integration contracts). Each row pins the exact count per token. KEEP lists the users whose only mentions are
prose and whose bytes an immutable receipt pins (m3/history/verify_m3_handoff.py over
m3/docs/m3-m3jdk21-eclipse-recipe-handoff-20261022.tsv): they are not edited, and the recipe refuses if a KEEP file
names the pair outside a comment line. m3/tooling/jdk-naming/RENAME_MAP.tsv records the two path moves and its
RECIPE_MEMORY.tsv the pattern (APPEND rows).

Hash-pinned: every move, rename and edit names the git blob of its preimage at BASE_COMMIT. A source must carry the
pinned blob (the step is applied) or the target must already carry the postimage (applied, no-op); anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Order: renames (they vacate the two paths), moves,
edits. Idempotent: a second run reports changed=0, and the ledgers MOVES.tsv (old path, new path, blob before, blob
after, kind) and EDITS.tsv (file, before blob, before sha256, after sha256, rule) are fixed points. Anchors must occur
exactly the declared number of times.

usage: python -I unify.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "unify.py"
VERSION = "v2"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "m3/qualification/file-unify-20261106"
BASE_COMMIT = "c4e268d495a0f87ea672f52c2f05c923562f4c04"
OLD = "m3/ports/file"
NEW = "m3/file"
MOVES_NAME = "MOVES.tsv"
EDITS_NAME = "EDITS.tsv"
MOVES_COLUMNS = ("old_path", "new_path", "blob_before", "blob_after", "kind")
EDITS_COLUMNS = ("file", "before_blob", "before_sha256", "after_sha256", "rule")
OLD_SRC = OLD + "/src/main/java/jdk/m3/file/"
NEW_SRC = NEW + "/src/main/java/jdk/m3/file/"

# Files of m3/ports/file whose bytes are consumed by a UNION rule instead of being moved.
ABSORBED = {"pom.xml": NEW + "/pom.xml", "src/main/java/module-info.java": NEW + "/src/main/java/module-info.java"}

#FLLDCIM: FILE_UNIFY - SPLIT_PACKAGE_NAME_COLLISION: the non-donor-mapped side takes the role prefix Nio
RENAMED = (("M3FileDelta", "M3NioFileDelta"), ("M3File", "M3NioFile"))   # longer name first
WORDS = tuple((re.compile(r"\b" + old + r"\b"), new) for old, new in RENAMED)
QUALIFIED = tuple((re.compile(r"\bjdk\.m3\.file\." + old + r"\b"), "jdk.m3.file." + new) for old, new in RENAMED)
# Users of the NIO pair whose only mentions are prose and whose bytes an immutable receipt pins: never edited.
KEEP = (
    (NEW_SRC + "M3FileMoveCandidates.java", "m3/docs/m3-m3jdk21-eclipse-recipe-handoff-20261022.tsv (verify_m3_handoff.py)"),
    (NEW_SRC + "M3ResourceLocks.java", "m3/docs/m3-m3jdk21-source-successions.tsv (verify_m3_handoff.py)"),
    (NEW + "/src/test/java/jdk/m3/file/M3FileMoveCandidatesContract.java",
     "m3/docs/m3-m3jdk21-eclipse-recipe-handoff-20261022.tsv (verify_m3_handoff.py)"),
)
PROSE_LINE = re.compile(r"^\s*(?:\*|//|/\*)")


class Refused(Exception):
    pass


def target_of(rel: str) -> str:
    """Where a file of m3/ports/file lives in m3/file: native/ becomes src/main/native/, the rest keeps its path."""
    if rel.startswith("native/"):
        return NEW + "/src/main/native/" + rel[len("native/"):]
    return NEW + "/" + rel


MIGRATION = "m3/tooling/migration-recipes/src/"
PATH_LITERAL = ("m3/ports/file/src/main/java/jdk/m3/file/", "m3/file/src/main/java/jdk/m3/file/")
WORDS_ALL = "words"
FS = "m3/fs/src/"
ECLIPSE = "m3/eclipse/optional-jmod/src/"
PARSER_IT = "m3/tooling/module-packs/parser/integration/src/test/java/jdk/m3/parser/test/"
NATIVE_API = "m3/tooling/module-packs/build-support/native-api/"
FILE_TEST = NEW + "/src/test/java/jdk/m3/file/"

# kind, path (after the moves), preimage blob at BASE_COMMIT, rule note, payload.
#   UNION:  payload = (template name, second preimage path, second preimage blob); the second preimage is absorbed.
#   RENAME: path = new path; blob = blob of the old path; payload = (old path, edits).
#   EDIT:   payload = edits: ("sub", old, new) [once], ("subn", old, new, n) [n times], ("words", n_delta, n_file) and
#           ("qualified", n_delta, n_file) [exact counts of M3FileDelta and M3File renamed].
#   APPEND: payload = text appended to the pinned preimage.
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
    ("EDIT", "m3/pom.xml", "6d88b5304aa484609dd5c7ef89c4bb07a5dc9bdb",
     "reactor module ports/file -> file at the same position (after collections, its dependency); preimage re-pinned"
     " at dec547222b30 (#2528 registered compiler/nullness-facts) and at c4e268d495a0 (#2549 intern, #2534 optane)", [
         ("sub", "    <module>ports/file</module>\n", "    <module>file</module>\n")]),
    ("EDIT", "m3/fs/src/main/module/module-info.java", "368d4f71cecd79cb185c7a492aeeb9a3f1ac0e09",
     "consumer descriptor: requires jdk.m3.file -> org.m3jdk.file (the unified module name)", [
         ("sub", "    requires jdk.m3.file;\n", "    requires org.m3jdk.file;\n")]),
    ("EDIT", "m3/eclipse/optional-jmod/src/main/java/module-info.java", "86ce3fe61925dfdb5aa4e95a70cebbdcd32c1ee3",
     "consumer descriptor: requires transitive jdk.m3.file -> org.m3jdk.file", [
         ("sub", "    requires transitive jdk.m3.file;\n", "    requires transitive org.m3jdk.file;\n")]),
    ("EDIT", "m3/eclipse/optional-jmod/build.py", "636b2450bbc6a1c70ab0b40d25bf872b6292b004",
     "consumer build: the m3/file source root is staged as module org.m3jdk.file (after jdk.m3.collections, which it"
     " requires); preimage re-pinned at 0dc1f07786fb (#2547 restored the retained build.py)", [
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
    # --- v2: the name collision, decided (naming owner, lane session 45): NIO pair -> M3NioFile / M3NioFileDelta -------
    ("RENAME", NEW_SRC + "M3NioFile.java", "02bc8fbd09d9f8be9d15bb1e411689500680da13",
     "NIO read boundary (from MIndexResourceAccess) takes the role prefix; the path M3File.java receives the donor-mapped"
     " token file of m3/ports/file", (NEW_SRC + "M3File.java", [("words", 0, 7)])),
    ("RENAME", NEW_SRC + "M3NioFileDelta.java", "0456626bdb7d0c490a83f0c3901000fb46efa724",
     "NIO content-delta evidence takes the role prefix; the path M3FileDelta.java receives the record of m3/ports/file",
     (NEW_SRC + "M3FileDelta.java", [("words", 2, 3)])),
    ("EDIT", NEW_SRC + "M3FileBuildImpact.java", "8c7828e4b0e041d6dc256df03f4f2287ad297271", "NIO user", [("words", 0, 2)]),
    ("EDIT", NEW_SRC + "M3FileDeltaCli.java", "5b74090884d923a1cbac646ba71b8d46f288cc08", "NIO user", [("words", 13, 1)]),
    ("EDIT", NEW_SRC + "M3FileDeltaLedger.java", "e303305e04001a9286b35c9dcd8f3b3222eedc27", "NIO user", [("words", 0, 25)]),
    ("EDIT", NEW_SRC + "M3FileSource.java", "a612cd7d0ceea937fb52f0fb89402400fbba6d60", "NIO user", [("words", 0, 7)]),
    ("EDIT", NEW_SRC + "M3FileStreams.java", "abf74510ab7e5349acb17e10e4a3f0a39d802813", "NIO user", [("words", 0, 2)]),
    ("EDIT", NEW_SRC + "M3ReactorImpactPlan.java", "83a9403f97756035c70f8fd1386f24e02414619c", "NIO user", [("words", 0, 2)]),
    ("EDIT", FILE_TEST + "M3FileBuildImpactContract.java", "238a5eea3536658e94d138cb245846267afcd0f6", "NIO user (test)",
     [("words", 0, 1)]),
    ("EDIT", FILE_TEST + "M3FileDeltaContractSelfTest.java", "7c4881c67813a4ad1360faa417e11237f7805414", "NIO user (test)",
     [("words", 35, 1)]),
    ("EDIT", FILE_TEST + "M3FileDeltaLedgerContract.java", "fd5309ef7d6569edb3374e8019f298fb62437f97", "NIO user (test)",
     [("words", 0, 9)]),
    ("EDIT", FILE_TEST + "M3FileDeltaSelfTest.java", "7f9e0f978ff0d8c605d6eb6439fe90beaa00d007", "NIO user (test)",
     [("words", 102, 2)]),
    ("EDIT", FILE_TEST + "M3FileObservationContract.java", "a9b42b0757a33b9d0c13405813cf0da753d796e8", "NIO user (test)",
     [("words", 0, 12)]),
    ("EDIT", FILE_TEST + "M3FileRangeContract.java", "453e55a95707717dfff61ae7cabf294cb31faafb", "NIO user (test)",
     [("words", 0, 8)]),
    ("EDIT", FILE_TEST + "M3FileSourceContract.java", "0c935e55ba7b95d845296c929b12e751b65e1a0c", "NIO user (test)",
     [("words", 0, 4)]),
    ("EDIT", FILE_TEST + "M3FileStreamsContract.java", "f6f8aa82b22478e0f49a11b7132d04bd3162d2df", "NIO user (test)",
     [("words", 0, 2)]),
    ("EDIT", FILE_TEST + "M3ShadowStreamsContract.java", "5d655045f6438e3caa20ce52b348041f8e440292", "NIO user (test)",
     [("words", 0, 6)]),
    ("EDIT", NEW + "/verification/src/test/java/jdk/m3/file/test/M3FileDeltaContract.java",
     "2366def6b1e8288e2bdb24dc29d821fe1c3ec6de", "NIO user (verification contract)", [("words", 0, 83)]),
    ("EDIT", FS + "main/java/jdk/m3/fs/M3HostFileInventory.java", "4eb339154e1ee7514b48f3223ec220d599dab381",
     "consumer of the NIO pair (m3/fs)", [("words", 0, 16)]),
    ("EDIT", FS + "test/java/jdk/m3/fs/M3HostFileInventoryProof.java", "b5d8d81e1e58d4755833e69c9de565ad0950e9f9",
     "consumer of the NIO pair (m3/fs test)", [("words", 0, 10)]),
    ("EDIT", FS + "test/java/jdk/m3/fs/M3HostFileInventoryTest.java", "220ea2aed0ba663362f8e44e390f590920de8672",
     "consumer of the NIO pair (m3/fs test)", [("words", 0, 10)]),
    ("EDIT", ECLIPSE + "main/java/jdk/m3/eclipse/core/M3EclipseCore.java", "c7c9ee8f78b660d0194739cfbc7c7d7cfc5a6573",
     "consumer of the NIO pair (eclipse optional-jmod)", [("words", 0, 2)]),
    ("EDIT", ECLIPSE + "main/java/jdk/m3/eclipse/core/impl/M3EclipseCoreProvider.java",
     "ba8ba61fb20a0721d61107ffb0567a8119391d49", "consumer of the NIO pair (eclipse optional-jmod)", [("words", 0, 3)]),
    ("EDIT", ECLIPSE + "test/java/jdk/m3/eclipse/smoke/M3EclipseCoreSelfTest.java",
     "4791d60a0d3762a4b42cf3f42adb29d38c202f25", "consumer of the NIO pair (eclipse optional-jmod test)", [("words", 0, 2)]),
    ("EDIT", PARSER_IT + "M3ActualJavacCaptureContract.java", "c577ec2a48f77dac9c3613d32e11843dce5780fa",
     "consumer of the NIO pair next to jdk.m3.parser.M3File: qualified references only, plus the one prose mention", [
         ("qualified", 0, 2),
         ("sub", "over ONE original NIO M3File capture.", "over ONE original M3NioFile capture.")]),
    ("EDIT", PARSER_IT + "M3ProviderCaptureContract.java", "40a339f6d82cb76a6b36bcbd067f23bc1f868274",
     "consumer of the NIO pair next to jdk.m3.parser.M3File: qualified references only", [("qualified", 0, 5)]),
    ("EDIT", NATIVE_API + "verification/src/test/java/NativeApiContract.java", "3a05dd91c8f65bb5f468f0a779fe1dd94f4146a1",
     "consumer of the NIO pair; module name of the unified module", [
         ("words", 0, 27),
         ("sub", 'getName().equals("jdk.m3.file")', 'getName().equals("org.m3jdk.file")'),
         ("sub", '"--add-modules", "jdk.m3.file",', '"--add-modules", "org.m3jdk.file",')]),
    ("EDIT", NATIVE_API + "build.py", "168c8ef465b5b58563dc81a4e951d4141c8bd168",
     "module-name references: org.m3jdk.file (and its jmod file name)", [
         ("subn", '"jdk.m3.file"', '"org.m3jdk.file"', 4),
         ("sub", '"jdk.m3.file.jmod"', '"org.m3jdk.file.jmod"')]),
    ("EDIT", ".github/workflows/m3-file-shadow-stream-20261020.yml", "de70a94b5327ce10ef45ba234323e8a6aee91a69",
     "path of the renamed NIO boundary", [
         ("sub", "            m3/file/src/main/java/jdk/m3/file/M3File.java \\\n",
          "            m3/file/src/main/java/jdk/m3/file/M3NioFile.java \\\n")]),
    ("EDIT", ".github/workflows/m3-native-shadow-stream-progress-20261020.yml", "9601f534a3dae1111eef0c9d2a616618df49ee23",
     "path of the renamed NIO boundary and the module name org.m3jdk.file", [
         ("sub", "            m3/file/src/main/java/jdk/m3/file/M3File.java \\\n",
          "            m3/file/src/main/java/jdk/m3/file/M3NioFile.java \\\n"),
         ("subn", "-m jdk.m3.file/jdk.m3.file.", "-m org.m3jdk.file/jdk.m3.file.", 4),
         ("sub", "build/m3-shadow-jmods/jdk.m3.file.jmod", "build/m3-shadow-jmods/org.m3jdk.file.jmod"),
         ("sub", "--add-modules jdk.m3.file --output", "--add-modules org.m3jdk.file --output")]),
    ("EDIT", ".github/workflows/m3-actual-javac-capture-20261027.yml", "d11373e3a2444cfe3272c3405ff38a20b2a8a431",
     "path of the renamed NIO boundary", [
         ("sub", '            "$file/M3File.java" \\\n', '            "$file/M3NioFile.java" \\\n')]),
    ("EDIT", ".github/workflows/m3-original-provider-parser-capture-20261026.yml",
     "58e1bed5a1447cee2519d4c0b9e442b45b0a3889", "path of the renamed NIO boundary", [
         ("sub", '            "$file/M3File.java" \\\n', '            "$file/M3NioFile.java" \\\n')]),
    ("APPEND", "m3/tooling/jdk-naming/RENAME_MAP.tsv", "3c4f0155fa606e178208f0478dd1f4b977dd9d88",
     "the two path moves of the decided name collision",
     NEW_SRC + "M3File.java\t" + NEW_SRC + "M3NioFile.java\n" + NEW_SRC + "M3FileDelta.java\t" + NEW_SRC
     + "M3NioFileDelta.java\n"),
    ("APPEND", "m3/tooling/jdk-naming/RECIPE_MEMORY.tsv", "1611c8eceaf433c3da367d1c1cb959e605b0f326",
     "pattern row SPLIT_PACKAGE_NAME_COLLISION",
     "2026-11-06T12:49:41Z\tjdk_naming.py\tPATTERN:SPLIT_PACKAGE_NAME_COLLISION\tthe rename fe35ea98841 moved the"
     " m3/ports/file package into jdk.m3.file, which m3/file already exported; jdk.m3.file.M3File and"
     " jdk.m3.file.M3FileDelta then named two different types in two modules (the NIO read boundary adapted from"
     " MIndexResourceAccess, and the MIndexFile token file with its record), unseen until the modules were unified"
     " (M3jdk21 #2545 held both pairs)\tnaming owner decision (lane session 45): the side whose name is"
     " donor-mapped keeps it (MIndexFile -> M3File, the record M3FileDelta); the other side takes a role prefix"
     " (M3NioFile, M3NioFileDelta); two RENAME_MAP.tsv rows\t" + RECIPE_DIR_REL + "/unify.py v2 (RENAME rows plus"
     " counted token rows for every user)\tunify.py refuses a move onto an existing path unless a RENAME row vacates"
     " it first, and pins the rename count per token and file\twhen a naming pass maps two modules' types onto one"
     " package+name, the side whose name is not donor-mapped takes a role prefix\n"),
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
    """(old, new, blob, kind) for every file of m3/ports/file."""
    moves = []
    for path, blob in base_tree(root, OLD):
        rel = path[len(OLD) + 1:]
        moves.append((path, ABSORBED[rel], blob, "ABSORBED") if rel in ABSORBED else (path, target_of(rel), blob, "MOVE"))
    return moves


def eol_of(data: bytes) -> bytes:
    return b"\r\n" if b"\r\n" in data else b"\n"


def with_eol(text: str, eol: bytes) -> str:
    return text.replace("\n", eol.decode())


def apply_edit(text: str, edit: tuple, eol: bytes, path: str) -> str:
    mode, *args = edit
    if mode in ("words", "qualified"):
        for (pattern, new), expected in zip(WORDS if mode == "words" else QUALIFIED, args):
            text, count = pattern.subn(new, text)
            if count != expected:
                raise Refused("%s rename of %s matched %d times (need %d) in %s" % (mode, pattern.pattern, count,
                                                                                 expected, path))
        return text
    old, new, *rest = args
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
    text = pre.decode("utf-8")
    if kind == "APPEND":
        if not text.endswith(eol.decode()):
            text += eol.decode()
        return (text + with_eol(payload, eol)).encode("utf-8")
    edits = payload[1] if kind == "RENAME" else payload
    for edit in edits:
        text = apply_edit(text, edit, eol, path)
    return text.encode("utf-8")


def read(root: Path, rel: str) -> bytes | None:
    target = root / rel
    return target.read_bytes() if target.is_file() else None


def check_keep(root: Path) -> None:
    """A KEEP file may name the NIO pair only on comment lines (its bytes are pinned by a receipt)."""
    for rel, receipt in KEEP:
        data = read(root, rel)
        if data is None:
            raise Refused("missing KEEP file %s" % rel)
        for number, line in enumerate(data.decode("utf-8").splitlines(), 1):
            if any(pattern.search(line) for pattern, _ in WORDS) and not PROSE_LINE.match(line):
                raise Refused("KEEP file %s names the renamed pair in code at line %d (receipt %s)" % (rel, number, receipt))


def run_renames(root: Path, check: bool) -> tuple[list, list, int, dict]:
    """RENAME rows first: they vacate the paths the donor-mapped pair moves into."""
    move_rows, edit_rows, changed, vacated = [], [], 0, {}
    for kind, new, blob, note, payload in RULES:
        if kind != "RENAME":
            continue
        old = payload[0]
        vacated[old] = blob
        pre = git_blob(root, blob)
        post = postimage(kind, pre, payload, new)
        on_new, on_old = read(root, new), read(root, old)
        if on_new is not None:
            if on_new != post:
                raise Refused("drifted rename target %s: on disk %s" % (new, blob_sha(on_new)))
            state = "UNCHANGED"
        elif on_old is not None and blob_sha(on_old) == blob:
            state = "RENAMED"
            if not check:
                git(root, "mv", "--", old, new)
                (root / new).write_bytes(post)
                git(root, "add", "--", new)
        else:
            raise Refused("missing rename preimage %s (pinned blob %s)" % (old, blob))
        changed += state != "UNCHANGED"
        move_rows.append((old, new, blob, blob_sha(post), "RENAME"))
        edit_rows.append((new, blob, sha256(pre), sha256(post), "RENAME: " + note))
        print("RENAME %s %s -> %s" % (state, old, new))
    return move_rows, edit_rows, changed, vacated


def run_moves(root: Path, moves, edited: dict, vacated: dict, check: bool) -> tuple[list, int]:
    rows, changed = [], 0
    for old, new, blob, kind in moves:
        on_old = read(root, old)
        on_new = read(root, new)
        if kind == "ABSORBED":
            if on_old is not None and blob_sha(on_old) != blob:
                raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (old, blob_sha(on_old), blob))
            state = "UNCHANGED" if on_old is None else "ABSORBED"
            rows.append((old, new, blob, "-", kind))
            changed += state != "UNCHANGED"
            print("%s %s %s -> %s" % (kind, state, old, new))
            continue
        if on_old is not None:
            if blob_sha(on_old) != blob:
                raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (old, blob_sha(on_old), blob))
            if on_new is not None and not (check and vacated.get(new) == blob_sha(on_new)):
                raise Refused("both %s and %s exist; the move cannot be applied" % (old, new))
            if not check:
                (root / new).parent.mkdir(parents=True, exist_ok=True)
                git(root, "mv", "--", old, new)
            state = "MOVED"
        elif on_new is not None:
            if blob_sha(on_new) != blob and new not in edited:
                raise Refused("drifted target %s: on disk %s, pinned blob %s" % (new, blob_sha(on_new), blob))
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
        if kind == "RENAME":
            continue
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
    check_keep(root)
    moves = plan_moves(root)
    rename_rows, rename_edit_rows, renamed, vacated = run_renames(root, check)
    edited = {rel: None for kind, rel, *_ in RULES if kind != "RENAME"}
    move_rows, moved = run_moves(root, moves, edited, vacated, check)
    edit_rows, changed, edited = run_edits(root, moves, check)
    for index, (old, new, before, after, kind) in enumerate(move_rows):
        if new in edited and kind == "MOVE":
            move_rows[index] = (old, new, before, edited[new], "MOVE+EDIT")
    leftovers = prune(root, {old for old, *_ in moves}, check)
    for path in leftovers:
        print("LEFTOVER %s" % path)
    all_moves = rename_rows + move_rows
    ledgers = write_ledger(MOVES_NAME, MOVES_COLUMNS, all_moves, check)
    ledgers |= write_ledger(EDITS_NAME, EDITS_COLUMNS, rename_edit_rows + edit_rows, check)
    kinds = [row[4] for row in all_moves]
    print("UNIFY recipe=%s %s base=%s moves=%d renames=%d absorbed=%d edits=%d keep=%d changed=%d leftovers=%d"
          " ledgers=%s" % (RECIPE, VERSION, BASE_COMMIT[:12], sum(kind.startswith("MOVE") for kind in kinds),
                           kinds.count("RENAME"), kinds.count("ABSORBED"), len(edit_rows), len(KEEP),
                           renamed + moved + changed, len(leftovers), "CHANGED" if ledgers else "UNCHANGED"))
    if leftovers and not check:
        return 2
    return 1 if check and (renamed or moved or changed or ledgers or leftovers) else 0


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
