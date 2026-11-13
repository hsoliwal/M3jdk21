#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe INTEROP_BINDINGS_GATING v1: JDK-name the SDK binding inventory and gate the interop JNI tests.

1. m3/collections/interop/sdk-family-bindings.tsv still names com.m3.collections owners: JDK_NAMING_RENAME
   (fe35ea98841) classifies every .tsv as documentation/lineage (jdk_naming.py DOC_SUFFIXES -> is_lineage ->
   in_scope False), but this TSV is a runtime resource (packaged as META-INF/m3/sdk-family-bindings.tsv, loaded by
   M3LibraryBindings, which refuses any owner outside jdk.m3.collections.M3*). The rule applies the tool's own
   rewrite_text (rules NAMING_RULES.tsv, PKG_COM_M3) to this one file; the postimage sha256 is pinned.
2. M3SpringCaseInsensitiveSdkFactoryTest pins the SDK factory count like M3SdkFamiliesTest: 25 -> 30, completing
   mergeloss-01-15-20261106 (#2638), which raised the inventory to 30 while this pin still errored on step 1.
3. m3/collections/interop/pom.xml: core's JNI test gating (m3/collections/pom.xml, jni-test-gating-20261106 /
   #2577) copied: default surefire <excludes> of the m3_arrays JNI-dependent classes, generated from EXCLUDED.tsv
   (pinned sha256; enumerated from surefire XML by make/m3/collections/qualification/jni-test-gating-20261106/tools/
   failing_classes.py), and the profile m3-collections-native re-including them with m3.arrays.native.path.

Every rule names its preimage blob; a target must equal the preimage or the postimage, else the recipe refuses
(exit 2) without writing. Idempotent; EDITS.tsv is a fixed point; --check writes nothing, exit 1 if a run would
change a file. usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import subprocess
import sys
from pathlib import Path

RECIPE, VERSION = "apply.py", "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "7e14ce2d8f0"  # #2638 head (mergeloss-01-15-20261106) on master e6e5b59569f
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "rule")
INTEROP = "m3/collections/interop/"
TSV = INTEROP + "sdk-family-bindings.tsv"
POM = INTEROP + "pom.xml"
TEST = INTEROP + "src/test/java/jdk/m3/collections/interop/M3SpringCaseInsensitiveSdkFactoryTest.java"
TOOL = "make/m3/tooling/jdk-naming/jdk_naming.py"
TSV_PRE_BLOB = "b387cf78bd30f9d4a521c58e40aac7a00b2c9a91"
TSV_POST_SHA256 = "97654ada7e6d7e386f8e42927ccba0c97ae6b17f7071f61cd99cf08d2f3fbaad"
POM_PRE_BLOB = "3cc5eff7ceb22f89d79eff6ea3902504878a7aba"
TEST_PRE_BLOB = "4cec6718324f96d96ff5e183087569d8503758ce"
EXCLUDED_SHA256 = "87a91aa52d35b7c75ca3ff570215caeb4e119519703599b7eff7322d2135554d"
ANCHOR = "FLLDCIM: MRL - interop bindings/gating, recipe qualification/bindings-gating-20261107"


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git_blob(root: Path, blob: str) -> bytes:
    try:
        data = subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("pinned blob %s is not in this repository (%s)" % (blob, failure)) from failure
    if blob_sha(data) != blob:
        raise Refused("blob %s does not hash to itself" % blob)
    return data


def substitute(pre: bytes, edits, path: str) -> bytes:
    text = pre.decode("utf-8")
    for old, new in edits:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def tsv_post(root: Path, pre: bytes) -> bytes:
    spec = importlib.util.spec_from_file_location("jdk_naming", root / TOOL)
    tool = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(tool)
    post = tool.rewrite_text(pre.decode("utf-8"))[0].encode("utf-8")
    if sha256(post) != TSV_POST_SHA256:
        raise Refused("JDK naming rules give %s for %s, pinned %s" % (sha256(post), TSV, TSV_POST_SHA256))
    return post


def excluded_classes() -> list[str]:
    data = (RECIPE_DIR / "EXCLUDED.tsv").read_bytes()
    if sha256(data) != EXCLUDED_SHA256:
        raise Refused("EXCLUDED.tsv sha256 %s, pinned %s" % (sha256(data), EXCLUDED_SHA256))
    rows = data.decode("utf-8").splitlines()[1:]
    return sorted(row.split("\t", 1)[0].rsplit(".", 1)[1] for row in rows if row.strip())


def pom_edits():
    excludes = "".join("            <exclude>**/%s.java</exclude>\n" % name for name in excluded_classes())
    return [
        ("          <useModulePath>false</useModulePath>\n          <failIfNoTests>true</failIfNoTests>\n"
         "        </configuration>\n",
         "          <useModulePath>false</useModulePath>\n          <failIfNoTests>true</failIfNoTests>\n"
         "          <!-- " + ANCHOR + ". JNI test gating (m3/collections/pom.xml pattern,\n"
         "               jni-test-gating-20261106): the m3_arrays JNI-dependent classes (EXCLUDED.tsv, enumerated from\n"
         "               surefire XML) are excluded from the default build and re-included by -Pm3-collections-native. -->\n"
         "          <excludes>\n" + excludes + "          </excludes>\n        </configuration>\n"),
        ("  </build>\n</project>\n",
         "  </build>\n  <profiles>\n    <profile>\n"
         "      <!-- Native tests: build m3/arrays/src/main/native with CMake first, then -Pm3-collections-native"
         " -Dm3.collections.native.dir=<dir>. -->\n"
         "      <id>m3-collections-native</id>\n      <build>\n        <plugins>\n          <plugin>\n"
         "            <groupId>org.apache.maven.plugins</groupId>\n"
         "            <artifactId>maven-surefire-plugin</artifactId>\n            <version>3.2.5</version>\n"
         "            <configuration>\n              <useModulePath>false</useModulePath>\n"
         "              <excludes combine.self=\"override\"/>\n              <systemPropertyVariables>\n"
         "                <m3.arrays.native.path>${m3.collections.native.dir}/${m3.collections.native.prefix}m3_arrays"
         "${m3.collections.native.suffix}</m3.arrays.native.path>\n"
         "              </systemPropertyVariables>\n            </configuration>\n          </plugin>\n"
         "        </plugins>\n      </build>\n      <properties>\n"
         "        <m3.collections.native.prefix>lib</m3.collections.native.prefix>\n"
         "        <m3.collections.native.suffix>.so</m3.collections.native.suffix>\n      </properties>\n"
         "    </profile>\n  </profiles>\n</project>\n"),
    ]


TEST_EDITS = [
    ("        // The canonical SDK table now declares 25 actual first-party factories.\n"
     "        assertEquals(25, M3LibraryBindings.all().size());\n"
     "        assertEquals(25, M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));\n",
     "        //" + ANCHOR + ": SDK factory pin 25 -> 30 as in M3SdkFamiliesTest (mergeloss-01-15-20261106)\n"
     "        // The canonical SDK table now declares 30 actual first-party factories.\n"
     "        assertEquals(30, M3LibraryBindings.all().size());\n"
     "        assertEquals(30, M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));\n"),
]


def rules(root: Path):
    yield TSV, TSV_PRE_BLOB, lambda pre: tsv_post(root, pre), (
        "JDK naming of the runtime SDK inventory: jdk_naming.rewrite_text (PKG_COM_M3, 45 tokens in 25 rows); the tool"
        " itself skips .tsv as lineage (DOC_SUFFIXES/is_lineage), this file is a runtime resource")
    yield TEST, TEST_PRE_BLOB, lambda pre: substitute(pre, TEST_EDITS, TEST), (
        "SDK factory pin 25 -> 30 (the second pin of the inventory, next to M3SdkFamiliesTest)")
    yield POM, POM_PRE_BLOB, lambda pre: substitute(pre, pom_edits(), POM), (
        "JNI test gating copied from m3/collections/pom.xml: <excludes> generated from EXCLUDED.tsv (%d classes)"
        " + profile m3-collections-native" % len(excluded_classes()))


def run(root: Path, check: bool) -> int:
    rows, writes = [], []
    for rel, blob, make, note in rules(root):
        pre = git_blob(root, blob)
        post = make(pre)
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        if on_disk not in (pre, post):
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (
                rel, blob_sha(on_disk) if on_disk is not None else "ABSENT", blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED":
            writes.append((target, post))
        rows.append((rel, sha256(pre), sha256(post), note))
        print("FILE %s %s" % (state, rel))
    if not check:  # every preimage verified before the first write
        for target, data in writes:
            target.write_bytes(data)
    table = ("\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(r) for r in rows]) + "\n").encode("utf-8")
    edits_path = RECIPE_DIR / EDITS_NAME
    table_changed = not edits_path.exists() or edits_path.read_bytes() != table
    if table_changed and not check:
        edits_path.write_bytes(table)
    print("INTEROP_BINDINGS_GATING recipe=%s %s base=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, len(rows), len(writes), "CHANGED" if table_changed else "UNCHANGED"))
    return 1 if check and (writes or table_changed) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: five up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
