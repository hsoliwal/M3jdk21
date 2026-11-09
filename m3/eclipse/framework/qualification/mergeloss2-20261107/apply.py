#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_RESTORE v1: restore merge-loss audit phase-2 leaf 97 (m3/eclipse/framework).

Same engine as m3/collections/qualification/mergeloss2-20261107/apply.py (derived from
m3/collections/qualification/census-small-batch-20261106/apply.py v1): anchored "sub" edits with a declared anchor
count, LF payloads re-joined with the preimage's line ending. The rule pins the git blob sha of its preimage on master;
the target must equal the preimage (the rule is applied) or the postimage (already applied, no-op); anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and
EDITS.tsv is a fixed point. --check writes nothing and exits 1 when a run would change a file.

The rule cites the origin commit of the lost lines, the merge that dropped them, and the 3-way merge it was checked
against: base = origin commit's parent blob, theirs = origin commit's blob (both through m3/tooling/jdk-naming
rewrite_text), ours = the pinned master blob. The 3-way conflict is resolved by ADD rows on master's owner: master's
always-captured flag lane stays authoritative and the twin's work-state lane is added beside it.

usage: python -I apply.py [--root DIR] [--check] [--leaf 97]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "04fae52d11dd590d6640377caa96ebac8e2d004a"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "threeway",
                 "rule")
FW = "m3/eclipse/framework/src/main/java/jdk/m3/eclipse/framework/"


class Refused(Exception):
    pass


# leaf, path, preimage blob, origin commit, dropping merge, 3-way record, rule note, edits [(old, new, count)]
RULES = (
    ("97", FW + "M3EclipseResourceDeltaSnapshot.java", "5e5de8a08972f3dffde1736d338e5aa1c961cc8e",
     "dce32109366d03e9ecbe749e00eaec17eaf828a0", "b11532eb12e1e0b3dbb29a077ca878d222ae9db5",
     "base=316c2856c2a2 theirs=06a56e699661 ours=5e5de8a08972 git-merge-file=CONFLICT:1 (flags field: master keeps a"
     " full-width M3LongArrayList lane captured by every capture(); the twin opts in through an int[]) plus duplicate"
     " capturedFlags/selectByFlags in the clean output; resolved ADD: master's always-captured flag lane and its"
     " capturedFlags/selectByFlags stay, the twin's work-state lane is added on top and reads flags from that lane",
     "RESTORE: origin dce32109366 'Apply 39 exact outputs of the qualified canonical per-file recipes' added opt-in"
     " build-work nominations (retained-variants/pr1637 copy 622e3b04cca5, its qualification resource-change-20261021"
     " and M3EclipseResourceChangeSnapshotTest); #2547 CONFLICTS.tsv pr1637 row: 1 conflict, owner review", [
         ("import jdk.m3.collections.M3Collections;\n",
          "import jdk.m3.collections.M3Collections;\nimport jdk.m3.collections.M3IndexMaskCompactor;\n", 1),
         ("import org.eclipse.core.resources.IResourceDelta;\n",
          "import org.eclipse.core.resources.IResource;\nimport org.eclipse.core.resources.IResourceDelta;\n", 1),
         (" * on each original IResourceDelta, rather than a lossy M3 reimplementation.</p>\n",
          " * on each original IResourceDelta, rather than a lossy M3 reimplementation.\n"
          " * {@link #captureChanges} additionally derives conservative build-work nominations;\n"
          " * resource paths and operations remain on the original Eclipse handles.</p>\n", 1),
         ("public final class M3EclipseResourceDeltaSnapshot {\n",
          "public final class M3EclipseResourceDeltaSnapshot {\n"
          "    //FLLDCIM: MRL - merge-loss leaf 97 (origin dce32109366, dropped by retain merge b11532eb12e1): work-state"
          " nominations of the pr1637 twin over master's always-captured flag lane\n"
          "    /** Case-sensitive Java source filename, including module-info.java. */\n"
          "    public static final int JAVA_SOURCE = 1;\n"
          "    /** Case-sensitive .class filename. */\n"
          "    public static final int CLASS_FILE = 2;\n"
          "    /** A recognized build or project descriptor filename. */\n"
          "    public static final int BUILD_DESCRIPTOR = 4;\n"
          "    /** Workspace root, project, or folder in the captured resource handle. */\n"
          "    public static final int CONTAINER = 8;\n"
          "    /** Content must be reconsidered; this does not prove that bytes changed. */\n"
          "    public static final int CONTENT_DIRTY = 16;\n"
          "    /** Removed resource or removed phantom. */\n"
          "    public static final int REMOVED = 32;\n"
          "    /** Build context must be reconsidered. */\n"
          "    public static final int CONTEXT_DIRTY = 64;\n"
          "    /** Descendant context may be invalid, including descendants absent from this delta. */\n"
          "    public static final int DESCENDANTS_DIRTY = 128;\n"
          "\n", 1),
         ("    private final M3LongArrayList flags = M3Collections.longArrayList();\n",
          "    private final M3LongArrayList flags = M3Collections.longArrayList();\n"
          "    /** Unsigned work-state byte per row; null unless {@link #captureChanges} built it. */\n"
          "    private byte[] workStates;\n", 1),
         ("    private static int kindMask(IResourceDelta delta) {\n",
          "    /**\n"
          "     * Captures the existing bounded breadth-first projection, including its full flag\n"
          "     * words, then reads each node's resource handle once in ordinal order. Resource type\n"
          "     * is read once; filename is read once for files only. Existing {@link #capture}\n"
          "     * callers do not incur these callbacks or the additional payload byte per row.\n"
          "     *\n"
          "     * <p>Call while Eclipse supplies a stable delta. Work\n"
          "     * state is a conservative nomination, not a dependency graph, content digest,\n"
          "     * workspace mutation, or proof that a compiler/build must execute. A descriptor\n"
          "     * content change nominates its parent subtree; inherited context propagates only\n"
          "     * through captured parent ordinals. Existing compiler and build owners resolve\n"
          "     * dependencies beyond this delta. No resource handle is passed to JNI.</p>\n"
          "     *\n"
          "     * <p>Descriptor names are case-sensitive: pom.xml, .classpath, .project,\n"
          "     * module-info.java, MANIFEST.MF, build.xml, build.gradle, build.gradle.kts,\n"
          "     * settings.gradle, and settings.gradle.kts. This list is a nomination aid and\n"
          "     * is not an exhaustive classification of supported build inputs.</p>\n"
          "     */\n"
          "    public static M3EclipseResourceDeltaSnapshot captureChanges(\n"
          "            IResourceDelta root, int maxNodes) {\n"
          "        M3EclipseResourceDeltaSnapshot snapshot = capture(root, maxNodes);\n"
          "        int count = snapshot.size();\n"
          "        byte[] states = new byte[count];\n"
          "        for (int ordinal = 0; ordinal < count; ordinal++) {\n"
          "            IResourceDelta delta = snapshot.at(ordinal);\n"
          "            IResource resource = Objects.requireNonNull(delta.getResource(), \"delta resource\");\n"
          "            states[ordinal] = (byte) workState(resource, snapshot.capturedKind(ordinal),\n"
          "                    snapshot.capturedFlags(ordinal));\n"
          "        }\n"
          "        // Descriptor changes affect sibling inputs too. Nominate the owning parent\n"
          "        // subtree before the single forward ancestry pass, independent of child order.\n"
          "        for (int ordinal = 0; ordinal < count; ordinal++) {\n"
          "            int state = Byte.toUnsignedInt(states[ordinal]);\n"
          "            if ((state & (BUILD_DESCRIPTOR | CONTENT_DIRTY))\n"
          "                    == (BUILD_DESCRIPTOR | CONTENT_DIRTY)) {\n"
          "                int parent = snapshot.parentOrdinal(ordinal);\n"
          "                if (parent >= 0) {\n"
          "                    states[parent] |= (byte) (CONTEXT_DIRTY | DESCENDANTS_DIRTY);\n"
          "                }\n"
          "            }\n"
          "        }\n"
          "        for (int ordinal = 1; ordinal < count; ordinal++) {\n"
          "            int parent = snapshot.parentOrdinal(ordinal);\n"
          "            if ((states[parent] & DESCENDANTS_DIRTY) != 0) {\n"
          "                states[ordinal] |= (byte) CONTEXT_DIRTY;\n"
          "                if ((states[ordinal] & CONTAINER) != 0) {\n"
          "                    states[ordinal] |= (byte) DESCENDANTS_DIRTY;\n"
          "                }\n"
          "            }\n"
          "        }\n"
          "        snapshot.workStates = states;\n"
          "        return snapshot;\n"
          "    }\n"
          "\n"
          "    private static int workState(IResource resource, int kind, int flags) {\n"
          "        int type = resource.getType();\n"
          "        int state = 0;\n"
          "        if (type == IResource.FILE) {\n"
          "            String name = Objects.requireNonNull(resource.getName(), \"resource name\");\n"
          "            if (name.endsWith(\".java\")) state |= JAVA_SOURCE;\n"
          "            if (name.endsWith(\".class\")) state |= CLASS_FILE;\n"
          "            if (isBuildDescriptor(name)) state |= BUILD_DESCRIPTOR;\n"
          "        } else if (type == IResource.ROOT || type == IResource.PROJECT || type == IResource.FOLDER) {\n"
          "            state |= CONTAINER;\n"
          "        }\n"
          "        boolean added = (kind & (IResourceDelta.ADDED | IResourceDelta.ADDED_PHANTOM)) != 0;\n"
          "        boolean removed = (kind & (IResourceDelta.REMOVED | IResourceDelta.REMOVED_PHANTOM)) != 0;\n"
          "        if (removed) state |= REMOVED;\n"
          "        int contentFlags = IResourceDelta.CONTENT | IResourceDelta.ENCODING\n"
          "                | IResourceDelta.REPLACED | IResourceDelta.TYPE | IResourceDelta.MOVED_FROM\n"
          "                | IResourceDelta.MOVED_TO | IResourceDelta.COPIED_FROM | IResourceDelta.LOCAL_CHANGED;\n"
          "        if (added || removed || (flags & contentFlags) != 0) state |= CONTENT_DIRTY;\n"
          "        int contextFlags = IResourceDelta.DESCRIPTION | IResourceDelta.OPEN | IResourceDelta.TYPE\n"
          "                | IResourceDelta.REPLACED | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO\n"
          "                | IResourceDelta.COPIED_FROM | IResourceDelta.ENCODING\n"
          "                | IResourceDelta.LOCAL_CHANGED | IResourceDelta.DERIVED_CHANGED;\n"
          "        if ((flags & contextFlags) != 0\n"
          "                || (state & (BUILD_DESCRIPTOR | CONTENT_DIRTY)) == (BUILD_DESCRIPTOR | CONTENT_DIRTY)\n"
          "                || ((state & CONTAINER) != 0 && (added || removed))) {\n"
          "            state |= CONTEXT_DIRTY;\n"
          "        }\n"
          "        if (((state & CONTAINER) != 0 && (state & (CONTENT_DIRTY | CONTEXT_DIRTY)) != 0)\n"
          "                || (flags & IResourceDelta.TYPE) != 0) {\n"
          "            // TYPE also covers folder -> file: the former descendants need invalidation.\n"
          "            state |= DESCENDANTS_DIRTY;\n"
          "        }\n"
          "        return state;\n"
          "    }\n"
          "\n"
          "    private static boolean isBuildDescriptor(String name) {\n"
          "        return switch (name) {\n"
          "            case \"pom.xml\", \".classpath\", \".project\", \"module-info.java\", \"MANIFEST.MF\",\n"
          "                    \"build.xml\", \"build.gradle\", \"build.gradle.kts\", \"settings.gradle\",\n"
          "                    \"settings.gradle.kts\" -> true;\n"
          "            default -> false;\n"
          "        };\n"
          "    }\n"
          "\n"
          "    /** Whether the work-state lane was requested during capture. */\n"
          "    public boolean hasCapturedChanges() { return workStates != null; }\n"
          "\n"
          "    /** Unsigned work-state byte; see {@link #captureChanges} for its limits. */\n"
          "    public int capturedWorkState(int ordinal) {\n"
          "        requireChanges();\n"
          "        return Byte.toUnsignedInt(workStates[Objects.checkIndex(ordinal, size())]);\n"
          "    }\n"
          "\n"
          "    /**\n"
          "     * Detached ascending ordinals selected through the existing primitive mask owner.\n"
          "     * Masks must fit an unsigned byte. NATIVE_REQUIRED preserves that owner's explicit\n"
          "     * failure when its compatible JNI library is unavailable; it never falls back to Java.\n"
          "     */\n"
          "    public int[] selectWorkOrdinals(int requiredState, int excludedState,\n"
          "            M3IndexMaskCompactor.Backend backend) {\n"
          "        requireChanges();\n"
          "        return M3IndexMaskCompactor.selectPositions(workStates, requiredState, excludedState, backend);\n"
          "    }\n"
          "\n"
          "    private void requireChanges() {\n"
          "        if (workStates == null) {\n"
          "            throw new IllegalStateException(\"work state requires captureChanges\");\n"
          "        }\n"
          "    }\n"
          "\n"
          "    private static int kindMask(IResourceDelta delta) {\n", 1),
     ]),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new, expected in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != expected:
            raise Refused("anchor occurs %d times (need %d) in %s: %r" % (count, expected, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, origin, merge, threeway, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, origin, merge, threeway, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS2_RESTORE recipe=%s %s base=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: five up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
