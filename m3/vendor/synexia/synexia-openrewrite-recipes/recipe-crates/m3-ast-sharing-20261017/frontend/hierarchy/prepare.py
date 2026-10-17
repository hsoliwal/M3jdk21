#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Prepare sealed recipe resources only. Java SDK results alone may write product paths."""
import argparse
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
CANONICAL = HERE.parents[4]
WORK = CANONICAL.parent.parent
TARGET = (CANONICAL.parents[2] if CANONICAL.as_posix().endswith("/m3/vendor/synexia")
          else CANONICAL.parent / "M3jdk21")
AST = "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/"
TEST = "synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/ast/"
RUNTIME = "synexia-mindex/runtime/src/main/java/com/synexia/mindex/ast/"
M3 = "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/"
M3_TEST = "m3/ports/compiletime/src/test/java/com/m3/compiletime/ast/"
DONORS = {
    "MIndexPartialAstAdapter.java": "f79c0b23729e697b9ecf61db858bd3c39caf8d14",
    "MIndexPartialAstAttempt.java": "e6600ad2c84699c06ffc13b899ae04bfd903dc5a",
    "MIndexPartialAstCache.java": "4b49d33287efae36ec161a0cc0276ca9f394500c",
    "MIndexPartialAstKey.java": "d1cec2707d31c9b37efc4d751e9c2c93427cb855",
    "MIndexPartialAstMaster.java": "e631494426316fb67e3c8a60fb3f9c1451247cd0",
    "MIndexPartialAstMode.java": "8f63217433cd569a8915c121ebb60c146e5f1d8d",
    "MIndexPartialAstPressure.java": "0566f1ad6f9fce3d81b94fb3fd85804558594383",
    "MIndexPartialAstProjection.java": "eb42483bdf55b2b110287737bdc82f8dcb291f9c",
    "MIndexPartialAstScope.java": "a8f9e141610dfeda7b55cf003b2b0bd7a8c198e8",
}
OWNERS = {
    "M3HashPinnedJavaSnapshotRecipe.java": "67f7b81126e4736c7b61b444fa6545153a61e23c012975ca796fcee623ab6625",
    "M3HashPinnedTextSnapshotRecipe.java": "fbf1ea9efad784afef5ac201ff7fc324551f77a099ebfe2f79b70c3a17c731ef",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def mapped(data):
    text = data.decode().replace("package com.synexia.mindex.compiler.ast;",
                                "package com.m3.compiletime.ast;\n\n"
                                "import com.synexia.mindex.compiler.ast.MIndexASTDocument;\n"
                                "import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;\n"
                                "import com.synexia.mindex.compiler.ast.MIndexJavaAST;")
    return text.replace("MIndexASTHierarchy", "M3ASTHierarchy").encode()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--without-tests", action="store_true")
    parser.add_argument("--canonical", type=Path, default=CANONICAL,
                        help="Canonical checkout or public target m3/vendor/synexia source mirror")
    parser.add_argument("--target", type=Path, default=TARGET)
    args = parser.parse_args()
    source_owners = args.canonical / "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite"
    for name, expected in OWNERS.items():
        if sha((source_owners / name).read_bytes()) != expected:
            raise ValueError("shared exact recipe owner drift: " + name)
    donors = {}
    lineage = []
    for name, expected in sorted(DONORS.items()):
        path = RUNTIME + "partial/" + name
        original = HERE / "preimages/canonical" / path
        data = (original if original.exists() else args.canonical / path).read_bytes()
        if blob(data) != expected:
            raise ValueError("exact partial owner drift: " + path)
        donors[path] = data
        lineage.append(dict(path=path, git_blob_sha1=expected, sha256=sha(data)))
    snapshot_path = RUNTIME + "MIndexAST.java"
    baseline = HERE / "preimages/canonical" / snapshot_path
    if not baseline.exists():
        data = (args.canonical / snapshot_path).read_bytes()
        if blob(data) != "716f126c6faa70f8cc85aaad0be9b37fc9267fb6":
            raise ValueError("existing attributed AST owner preimage drift")
        baseline.parent.mkdir(parents=True, exist_ok=True)
        baseline.write_bytes(data)
    before = baseline.read_bytes()
    if blob(before) != "716f126c6faa70f8cc85aaad0be9b37fc9267fb6":
        raise ValueError("retained AST owner preimage drift")
    anchor = b"    public int contentIndex() { row(); return id; }\n"
    addition = b"""    /**
     * Immutable attributed coordinate prefix through this root, sharing existing immutable rows.
     * Later pool appends cannot affect its size, symbols or numeric coordinates. Earlier prefix
     * rows remain necessary to preserve IDs. This copies reference tables, never AST row payloads.
     */
    public MIndexASTSnapshot snapshot() {
        return snapshot(Integer.MAX_VALUE);
    }
    /** Admission checks the coordinate-prefix budget before allocating snapshot reference tables. */
    public MIndexASTSnapshot snapshot(int maxNodes) {
        row();
        if (maxNodes < 1 || id >= maxNodes) throw new IllegalArgumentException("AST snapshot prefix node budget");
        if (store instanceof MIndexASTSnapshot snapshot && snapshot.size() == id + 1) return snapshot;
        if (!(store instanceof MIndexASTPool) && !(store instanceof MIndexASTSnapshot))
            throw new IllegalStateException("unknown attributed AST owner");
        synchronized (store) {
            var rows = new java.util.ArrayList<MIndexASTNode>(id + 1);
            int lastSymbol = -1;
            for (int coordinate = 0; coordinate <= id; coordinate++) {
                MIndexASTNode node = store.node(coordinate);
                rows.add(node);
                lastSymbol = Math.max(lastSymbol, node.kind);
                for (int symbol : node.attributes) lastSymbol = Math.max(lastSymbol, symbol);
                for (int symbol : node.roles) lastSymbol = Math.max(lastSymbol, symbol);
            }
            var symbols = new java.util.ArrayList<com.synexia.mindex.experiment.MIndexString>(lastSymbol + 1);
            for (int symbol = 0; symbol <= lastSymbol; symbol++) symbols.add(store.symbol(symbol));
            return new MIndexASTSnapshot(rows, symbols);
        }
    }
"""
    if before.count(anchor) != 1:
        raise ValueError("exact attributed coordinate insertion seam drift")
    changed_ast = before.replace(anchor, anchor + addition)
    (HERE / "templates/canonical/MIndexAST.java.txt").write_bytes(changed_ast)
    canonical = {snapshot_path: changed_ast}
    receiver = {"m3/vendor/synexia/" + path: data for path, data in donors.items()}
    receiver["m3/vendor/synexia/" + snapshot_path] = changed_ast
    partial_cache_path = RUNTIME + "partial/MIndexPartialAstCache.java"
    changed_cache = (HERE / "templates/canonical/MIndexPartialAstCache.java.txt").read_bytes()
    canonical[partial_cache_path] = changed_cache
    receiver["m3/vendor/synexia/" + partial_cache_path] = changed_cache
    for name in ("MIndexASTHierarchy", "MIndexASTHierarchyFacts"):
        data = (HERE / "templates/canonical" / (name + ".java.txt")).read_bytes()
        canonical[AST + name + ".java"] = data
        receiver["m3/vendor/synexia/" + AST + name + ".java"] = data
        receiver[M3 + name.replace("MIndex", "M3", 1) + ".java"] = mapped(data)
    if not args.without_tests:
        for name in ("MIndexASTHierarchyTest", "MIndexASTHierarchyFactsTest"):
            data = (HERE / "templates/tests" / (name + ".java.txt")).read_bytes()
            canonical[TEST + name + ".java"] = data
            receiver[M3_TEST + name.replace("MIndex", "M3", 1) + ".java"] = mapped(data)
    manifests = {}
    for label, outputs, root in (("canonical", canonical, args.canonical), ("receiver", receiver, args.target)):
        resources = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / ("m3-ast-hierarchy-" + label + "-20261017")
        resources.mkdir(parents=True, exist_ok=True)
        manifest = resources / "manifest.tsv"
        prior = {line.split("\t")[0]: line.split("\t")[1] for line in manifest.read_text().splitlines()
                 if line and not line.startswith("#")} if manifest.exists() else {}
        rows = []
        for index, (path, data) in enumerate(sorted(outputs.items())):
            template = f"{index:03d}-" + Path(path).name + ".txt"
            (resources / template).write_bytes(data)
            preimage = HERE / "preimages" / label / path
            if path.endswith("/ast/MIndexAST.java") and not preimage.exists():
                actual = (root / path).read_bytes()
                if actual != before:
                    raise ValueError("existing target AST preimage drift: " + path)
                preimage.parent.mkdir(parents=True, exist_ok=True)
                preimage.write_bytes(actual)
            expected = prior.get(path, sha(preimage.read_bytes()) if preimage.exists() else "ABSENT")
            if expected == "ABSENT" and preimage.exists():
                raise ValueError("additive preimage drift: " + path)
            if expected != "ABSENT" and (not preimage.exists() or sha(preimage.read_bytes()) != expected):
                raise ValueError("preimage custody drift: " + path)
            rows.append("\t".join((path, expected, sha(data), template)))
        manifest.write_text("\n".join(rows) + "\n")
        referenced = {row.split("\t")[3] for row in rows}
        for obsolete in resources.glob("[0-9]*-*.java.txt"):
            if obsolete.name not in referenced:
                obsolete.unlink()
        manifests[label] = dict(path=str(manifest.relative_to(HERE)), sha256=sha(manifest.read_bytes()), count=len(rows))
    (HERE / "SOURCE_LINEAGE.json").write_text(json.dumps({
        "schema": "M3_AST_HIERARCHY_SOURCE_LINEAGE_V1",
        "donor_commit": "e244d68fdee88d4a406be48e4ff1d31b60a17988",
        "integration_base": "3aa8e45d93f5fed721fdb64579b655f4a432a308",
        "source_receipt": "m3/compiler/ast-sharing-20261017/handoff.json",
        "recipe_owners": OWNERS, "partial_owner_preimages": lineage,
        "existing_ast": {"path": snapshot_path, "before_sha256": sha(before), "after_sha256": sha(changed_ast)},
        "existing_partial_cache": {"path": partial_cache_path, "before_sha256": sha(donors[partial_cache_path]),
                                   "after_sha256": sha(changed_cache)},
        "manifests": manifests,
        "mapping": "Exact package rename and MIndexASTHierarchy→M3ASTHierarchy; all underlying AST/partial/swap owners remain canonical.",
        "levels": ["L0 per-file facts and atoms", "L1 file summary", "L2 package", "L3 module", "L4 project", "L5 repository"],
    }, indent=2) + "\n")
    print(json.dumps({"manifests": manifests, "existing_ast_after": sha(changed_ast)}))


if __name__ == "__main__":
    main()
