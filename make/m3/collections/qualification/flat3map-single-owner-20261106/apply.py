#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FLAT3MAP_SINGLE_OWNER v1 (census leaf L075): one Flat3 map implementation in m3/collections.

Owner decision (the user, relayed by the coordinating session 63, 2026-11-06): the M3-native jdk.m3.collections.M3Flat3Map
of the core module is the single implementation; jdk.m3.collections.apache.M3Flat3Map (implementations/
apache-flat3-map) stays only as a thin Commons-compatible facade that delegates to it, proven by a Commons
differential. README.md holds the decision, the hunk verdicts and the facade design; GATES.tsv the proof.

Rule kinds. VERIFY: the target must equal its pinned preimage and stays unchanged. EDIT: anchored substitutions, every
anchor exactly once. TEMPLATE: the body is replaced wholesale by a file under templates/. ADD: a new file (preimage
ABSENT) from templates/. TEMPLATE and ADD pin their postimage sha256 as well, so a drifted template is refused.
Crate identity: a rule may name the sha256 of the receiving crate's postimage (m3jdk21-flat3map-20261029, pinned in
README.md); the pinned preimage must reproduce it once the module package is spelled the crate's way again (the
2026-10-31 JDK naming rename is the only difference), so --check proves the receive mechanically.

Hash-pinned: a target must equal the preimage (the rule applies) or the postimage (already applied, no-op); anything
else is drift and the recipe refuses (exit 2) without writing. Once applied, the preimage is read back with git
cat-file, so --check recomputes every postimage from the pinned bytes. Payloads are written with LF and re-joined with
the preimage's line ending. Idempotent: a second run reports changed=0 and EDITS.tsv (file, kind, before blob, before
sha256, after sha256, crate sha256, rule) is a fixed point; the per-run state (CHANGED, ADDED, UNCHANGED) is printed.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "make/m3/collections/qualification/flat3map-single-owner-20261106"
BASE_COMMIT = "e65f0f664fa334b4ad60b1ebcb2f1436380aace1"
OWNER = "jdk.m3.collections.M3Flat3Map"
FACADE = "jdk.m3.collections.apache.M3Flat3Map"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "crate_sha256", "rule")
ABSENT = "ABSENT"
MODULE_PACKAGE = "jdk.m3.collections"
# the crate's package, spelled in parts: the naming recipe rewrites .py files and would turn a literal into the module's
CRATE_PACKAGE = ".".join(("com", "m3", "collections"))
CORE = "m3/collections/src/main/java/jdk/m3/collections/"
CORE_TEST = "m3/collections/src/test/java/jdk/m3/collections/"
APACHE = "m3/collections/implementations/apache-flat3-map/"
APACHE_TEST = APACHE + "src/test/java/jdk/m3/collections/apache/"
INVENTORY = "make/m3/collections/qualification/five-provider-inventory-20261018/INVENTORY.tsv"
BRANCH = "m3/collections-flat3map-single-owner-20261106a"


class Refused(Exception):
    pass


CORE_EDITS = [
    ("sub",
     "    private static final long[] EMPTY_STATES = new long[0];\n",
     "    private static final long[] EMPTY_STATES = new long[0];\n"
     "    private static final String NOT_LIVE = \"M3Flat3Map: slot is not live\";\n"),
    ("sub",
     "        V prior = valueAt(slot);\n"
     "        if (hashed) {\n"
     "            keyLane[slot] = null;\n"
     "            valueLane[slot] = null;\n"
     "            setState(stateMaskWords, slot, PACKED_DELETED);\n"
     "            size--;\n"
     "            if (size == 0) reset();\n"
     "        } else {\n"
     "            int moved = size - slot - 1;\n"
     "            if (moved > 0) {\n"
     "                System.arraycopy(keyLane, slot + 1, keyLane, slot, moved);\n"
     "                System.arraycopy(valueLane, slot + 1, valueLane, slot, moved);\n"
     "            }\n"
     "            --size;\n"
     "            keyLane[size] = null;\n"
     "            valueLane[size] = null;\n"
     "        }\n"
     "        return prior;\n"
     "    }\n",
     "        V prior = valueAt(slot);\n"
     "        if (hashed) removeHashed(slot); else removeInline(slot);\n"
     "        return prior;\n"
     "    }\n"
     "\n"
     "    //FLLDCIM: MRL - L075 method split: remove(Object) and removeSlot(int) share the two removal bodies\n"
     "    private void removeHashed(int slot) {\n"
     "        keyLane[slot] = null;\n"
     "        valueLane[slot] = null;\n"
     "        setState(stateMaskWords, slot, PACKED_DELETED);\n"
     "        size--;\n"
     "        if (size == 0) reset();\n"
     "    }\n"
     "\n"
     "    private void removeInline(int slot) {\n"
     "        int moved = size - slot - 1;\n"
     "        if (moved > 0) {\n"
     "            System.arraycopy(keyLane, slot + 1, keyLane, slot, moved);\n"
     "            System.arraycopy(valueLane, slot + 1, valueLane, slot, moved);\n"
     "        }\n"
     "        --size;\n"
     "        keyLane[size] = null;\n"
     "        valueLane[size] = null;\n"
     "    }\n"),
    ("sub",
     "    public void clear() { reset(); }\n",
     "    public void clear() { reset(); }\n"
     "\n"
     "    //FLLDCIM: MRL - L075 single owner: slot cursor for the Commons facade jdk.m3.collections.apache.M3Flat3Map\n"
     "    /**\n"
     "     * Returns the first live slot at or after {@code from}, or -1 when there is none. Ascending slot\n"
     "     * order is this map's iteration order: insertion order while inline (a removal shifts the later\n"
     "     * slots down), table order once hashed. A cursor stays valid across {@link #setSlotValue} and\n"
     "     * {@link #removeSlot}; any other structural change ends it.\n"
     "     */\n"
     "    public int nextSlot(int from) {\n"
     "        int end = hashed ? keyLane.length : size;\n"
     "        for (int slot = Math.max(from, 0); slot < end; slot++) {\n"
     "            if (!hashed || state(stateMaskWords, slot) == PACKED_USED) return slot;\n"
     "        }\n"
     "        return -1;\n"
     "    }\n"
     "\n"
     "    /** Returns the key of live slot {@code slot}; IllegalStateException when the slot is not live. */\n"
     "    @SuppressWarnings(\"unchecked\")\n"
     "    public K slotKey(int slot) { return (K) keyLane[liveSlot(slot)]; }\n"
     "\n"
     "    /** Returns the value of live slot {@code slot}; IllegalStateException when the slot is not live. */\n"
     "    public V slotValue(int slot) { return valueAt(liveSlot(slot)); }\n"
     "\n"
     "    /** Replaces the value of live slot {@code slot} without a structural change; returns the old value. */\n"
     "    public V setSlotValue(int slot, V value) {\n"
     "        V previous = valueAt(liveSlot(slot));\n"
     "        valueLane[slot] = value;\n"
     "        return previous;\n"
     "    }\n"
     "\n"
     "    /**\n"
     "     * Removes the mapping of live slot {@code slot} without a key lookup and returns the slot a cursor\n"
     "     * resumes from: the same slot while inline (the later slots shift down), the next one once hashed.\n"
     "     */\n"
     "    public int removeSlot(int slot) {\n"
     "        if (!hashed) {\n"
     "            removeInline(liveSlot(slot));\n"
     "            return slot;\n"
     "        }\n"
     "        removeHashed(liveSlot(slot));\n"
     "        return slot + 1;\n"
     "    }\n"
     "\n"
     "    /** Returns an independent map with the same mappings, regime and slot (iteration) order. */\n"
     "    public M3Flat3Map<K, V> copy() {\n"
     "        M3Flat3Map<K, V> copy = new M3Flat3Map<>();\n"
     "        copy.keyLane = keyLane.clone();\n"
     "        copy.valueLane = valueLane.clone();\n"
     "        copy.stateMaskWords = stateMaskWords.clone();\n"
     "        copy.size = size;\n"
     "        copy.occupied = occupied;\n"
     "        copy.hashed = hashed;\n"
     "        return copy;\n"
     "    }\n"
     "\n"
     "    private int liveSlot(int slot) {\n"
     "        if (slot < 0 || slot >= (hashed ? keyLane.length : size)\n"
     "                || hashed && state(stateMaskWords, slot) != PACKED_USED) {\n"
     "            throw new IllegalStateException(NOT_LIVE);\n"
     "        }\n"
     "        return slot;\n"
     "    }\n"),
]

APACHE_POM_EDITS = [
    ("sub",
     "<!-- GENERATED by qualification/five-provider-inventory-20261018/M3FiveProviderScaffold.java from "
     "scaffold-template/module-pom.xml; regenerate, do not hand-edit. -->\n",
     "<!-- Owned since census leaf L075 (" + RECIPE_DIR_REL + "/apply.py): no longer generated by the five-provider "
     "scaffold; edit through that recipe. -->\n"),
    ("sub",
     "  <name>M3 scaffold: org.apache.commons.collections4.map.Flat3Map</name>\n"
     "  <description>SCAFFOLD, not owned: M3Flat3Map mirrors the public surface of "
     "org.apache.commons.collections4.map.Flat3Map; every body throws UnsupportedOperationException.</description>\n",
     "  <name>M3 Commons facade: org.apache.commons.collections4.map.Flat3Map</name>\n"
     "  <description>Commons-compatible facade: M3Flat3Map keeps the public surface of "
     "org.apache.commons.collections4.map.Flat3Map and delegates every operation to the single implementation "
     + OWNER + " (com.m3:m3-collections).</description>\n"),
    ("sub",
     "  <dependencies>\n"
     "    <dependency>\n"
     "      <groupId>org.apache.commons</groupId>\n"
     "      <artifactId>commons-collections4</artifactId>\n"
     "      <version>4.5.0</version>\n"
     "    </dependency>\n"
     "  </dependencies>\n",
     "  <dependencies>\n"
     "    <dependency>\n"
     "      <groupId>com.m3</groupId>\n"
     "      <artifactId>m3-collections</artifactId>\n"
     "      <version>0.1.0-SNAPSHOT</version>\n"
     "    </dependency>\n"
     "    <dependency>\n"
     "      <groupId>org.apache.commons</groupId>\n"
     "      <artifactId>commons-collections4</artifactId>\n"
     "      <version>4.5.0</version>\n"
     "    </dependency>\n"
     "    <dependency>\n"
     "      <groupId>org.junit.jupiter</groupId>\n"
     "      <artifactId>junit-jupiter</artifactId>\n"
     "      <version>5.10.2</version>\n"
     "      <scope>test</scope>\n"
     "    </dependency>\n"
     "  </dependencies>\n"),
    ("sub",
     "            <arg>-Werror</arg>\n"
     "          </compilerArgs>\n"
     "        </configuration>\n"
     "      </plugin>\n"
     "    </plugins>\n"
     "  </build>\n",
     "            <arg>-Werror</arg>\n"
     "          </compilerArgs>\n"
     "        </configuration>\n"
     "      </plugin>\n"
     "      <plugin>\n"
     "        <groupId>org.apache.maven.plugins</groupId>\n"
     "        <artifactId>maven-surefire-plugin</artifactId>\n"
     "        <version>3.2.5</version>\n"
     "      </plugin>\n"
     "    </plugins>\n"
     "  </build>\n"),
]

APACHE_NOTICE_EDITS = [
    ("sub", "M3 collections scaffold: M3Flat3Map\n",
     "M3 collections Commons facade: M3Flat3Map (delegates to " + OWNER + ")\n"),
]

INVENTORY_EDITS = [
    ("re",
     r"^(org\.apache\.commons\.collections4\.map\.Flat3Map\tapache\t4\.5\.0\tclass\t07\t)SCAFFOLD\t([^\t\n]*)\t"
     r"scaffold module m3/collections/implementations/apache-flat3-map\t([^\t\n]*)(\t[^\n]*?)(\r?)$",
     r"\1DONE " + BRANCH + r"\t\2\tfacade m3/collections/implementations/apache-flat3-map over the single owner "
     + OWNER + " (census L075, " + RECIPE_DIR_REL + r")\tsingle owner " + OWNER + r"; \3\4\5"),
]

AGGREGATOR_EDITS = [
    ("sub", "  <description>123 SCAFFOLD modules generated", "  <description>122 SCAFFOLD modules generated"),
    ("sub", "\n    <module>apache-flat3-map</module>", ""),
]

# kind, path, preimage blob (ABSENT for a new file), crate sha256 or "-", note, payload, postimage sha256 or "-"
RULES = (
    # --- commit 1: the core and its crate tests already carry the crate postimage (receive abc8b3aea44, renamed by
    # fe35ea98841); the 11 hunks of the historical prepacked candidate are superseded (HUNKS.tsv, NEC.tsv) ----------
    ("EDIT", CORE + "M3Flat3Map.java", "f2de0a79c1cf4c2413b8b61b2f3538812068eddf",
     "28fb1d0ac39c3e1836750694c729349d01dd4477d4d3d711c82242055263710e",
     "crate postimage (packed two-bit states) verified on the preimage; slot cursor nextSlot/slotKey/slotValue/"
     "setSlotValue/removeSlot/copy for the facade, remove(Object) split into removeHashed/removeInline",
     CORE_EDITS, "-"),
    ("VERIFY", CORE + "M3SlotArrays.java", "ab4663c2c76108dad0de96252ec294006e7f629c",
     "59da94e994bbbf3decc17fb1c217e42b126496361f28e8a4dcbedf1baedf54d5",
     "crate's unchanged hash/capacity kernel", None, "-"),
    ("VERIFY", CORE_TEST + "M3Flat3MapContract.java", "b5f0740db5ecc8ff0fc4ff17e69dfabf262e6506",
     "7378bc357e20e8e3b5fb53167248ddc9748c9c004d2eaa50c376ac36bb15e2a4",
     "crate oracle: 1,951,504 assertions", None, "-"),
    ("VERIFY", CORE_TEST + "M3Flat3MapTest.java", "0e67769c40ffda50b31178211cf6b6b9fd652122",
     "04f7b82e6a18c9870d87ab3370e5f2bd77d7191d36ff882d2b2ce9c79e12394a",
     "crate Jupiter wrapper of the oracle", None, "-"),
    ("VERIFY", "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3Flat3MapProviderTest.java",
     "254d85294724b1cdcaa26a2c526da0fa17d623be",
     "508f91d8852c460c27cd4b81004496c58af08f5bab14432228fff2ebe33970ba",
     "crate provider test (Commons and account providers)", None, "-"),
    # --- commit 2: the apache class becomes a facade over the owner ---------------------------------------------------
    ("ADD", CORE_TEST + "M3Flat3MapSlotCursorTest.java", ABSENT, "-",
     "JUnit: slot cursor and copy against a HashMap model", "M3Flat3MapSlotCursorTest.java",
     "7b95f4e2806f5438fdb3774e6a434f605844008f6732322fa25893bf6573c8ef"),
    ("TEMPLATE", APACHE + "src/main/java/jdk/m3/collections/apache/M3Flat3Map.java",
     "e8125b543daf9bbaee312b07e9fe5c21b578db3d", "-",
     "scaffold (every body threw UnsupportedOperationException) -> facade delegating to " + OWNER
     + "; javap -public identical", FACADE + ".java",
     "5d52a632f4003cf55ec039a26568226d10ef356203d175e5fe6b092a1ef08009"),
    ("ADD", APACHE_TEST + "M3Flat3MapDifferential.java", ABSENT, "-",
     "seeded lockstep differential driver: Commons Flat3Map 4.5.0 vs facade (full) and vs core (subset)",
     "M3Flat3MapDifferential.java", "e779e6bb6533fdac0a06151f48625be6be27ecf9fba0d5312cd1fd6ecb0d3bd4"),
    ("ADD", APACHE_TEST + "M3Flat3MapCommonsDifferentialTest.java", ABSENT, "-",
     "JUnit wrapper of the differential (1,000,000 ops per mode)", "M3Flat3MapCommonsDifferentialTest.java",
     "688c37897736d855419e25d5376fa9452382a39ba6f34121d4a89d9df2d16b3d"),
    ("EDIT", APACHE + "pom.xml", "961b2cdd07fef1ec810a335c91a94465b1fdbdc7", "-",
     "owned header, name/description, dependency com.m3:m3-collections, junit-jupiter test, surefire 3.2.5",
     APACHE_POM_EDITS, "-"),
    ("TEMPLATE", APACHE + "README.md", "d07e24d82fcc402cd867580acad8224aa859680d", "-",
     "scaffold README -> facade README", "apache-flat3-map.README.md",
     "cd845729575f40e202808f53cb58674937e606ef3fad22f479401e0e0f425cdb"),
    ("EDIT", APACHE + "NOTICE.txt", "efd7875003abfe8bb042c3ff6270e51949aae5a1", "-", "first line names the facade",
     APACHE_NOTICE_EDITS, "-"),
    ("EDIT", INVENTORY, "44af4e1ecd9a50588cb7fdf4b70f4c15f78ca85c", "-",
     "row Flat3Map: SCAFFOLD -> DONE " + BRANCH + ", evidence and reuse owner name the facade and the owner",
     INVENTORY_EDITS, "-"),
    ("EDIT", "m3/collections/implementations/pom-five-provider-scaffold.xml",
     "45e28a103cfe1ed997dd73bc57727350adf7dca4",
     "-", "apache-flat3-map leaves the scaffold aggregator (its row is no longer SCAFFOLD; it needs m3-collections)",
     AGGREGATOR_EDITS, "-"),
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


def template(name: str, eol: bytes) -> bytes:
    lines = (RECIPE_DIR / "templates" / name).read_text(encoding="utf-8").splitlines()
    return (eol.decode().join(lines) + eol.decode()).encode("utf-8")


def postimage(kind: str, pre: bytes | None, payload, path: str) -> bytes:
    """The bytes a rule produces from its pinned preimage."""
    if kind == "VERIFY":
        return pre
    if kind == "ADD":
        return template(payload, b"\n")
    eol = eol_of(pre)
    if kind == "TEMPLATE":
        return template(payload, eol)
    text = pre.decode("utf-8")
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


def preimage(root: Path, rel: str, blob: str, on_disk: bytes | None) -> bytes | None:
    if blob == ABSENT:
        return None
    if on_disk is not None and blob_sha(on_disk) == blob:
        return on_disk
    return git_blob(root, blob)


def crate_check(rel: str, pre: bytes | None, crate: str) -> None:
    """The pinned preimage spelled with the crate's package must be the crate postimage, byte for byte."""
    if crate == "-":
        return
    spelled = pre.replace(MODULE_PACKAGE.encode(), CRATE_PACKAGE.encode())
    if sha256(spelled) != crate:
        raise Refused("crate identity: %s with the crate package is %s, crate postimage %s"
                      % (rel, sha256(spelled), crate))


def run_rule(root: Path, rule: tuple, check: bool) -> tuple:
    kind, rel, blob, crate, note, payload, pinned_post = rule
    target = root / rel
    on_disk = target.read_bytes() if target.exists() else None
    pre = preimage(root, rel, blob, on_disk)
    crate_check(rel, pre, crate)
    post = postimage(kind, pre, payload, rel)
    if pinned_post != "-" and sha256(post) != pinned_post:
        raise Refused("drifted template for %s: %s, pinned %s" % (rel, sha256(post), pinned_post))
    if on_disk is not None and on_disk != post and (pre is None or blob_sha(on_disk) != blob):
        raise Refused("drifted target %s: on disk blob %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
    if on_disk is None and pre is not None:
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
    state = "UNCHANGED"
    if on_disk != post:
        state = "ADDED" if on_disk is None else "CHANGED"
        if not check:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(post)
    print("%s %s %s" % (kind, state, rel))
    row = (rel, kind, blob, ABSENT if pre is None else sha256(pre), sha256(post), crate, note)
    return state, row


def run(root: Path, check: bool) -> int:
    rows, changed = [], 0
    for rule in RULES:
        state, row = run_rule(root, rule, check)
        changed += state != "UNCHANGED"
        rows.append(row)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    edits_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if edits_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    counts = {kind: sum(rule[0] == kind for rule in RULES) for kind in ("VERIFY", "EDIT", "TEMPLATE", "ADD")}
    print("FLAT3MAP_SINGLE_OWNER recipe=%s %s base=%s owner=%s rules=%d changed=%d verify=%d edits=%d templates=%d"
          " adds=%d edits_tsv=%s" % (RECIPE, VERSION, BASE_COMMIT[:12], OWNER, len(rows), changed, counts["VERIFY"],
                                     counts["EDIT"], counts["TEMPLATE"], counts["ADD"],
                                     "CHANGED" if edits_state else "UNCHANGED"))
    return 1 if check and (changed or edits_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
