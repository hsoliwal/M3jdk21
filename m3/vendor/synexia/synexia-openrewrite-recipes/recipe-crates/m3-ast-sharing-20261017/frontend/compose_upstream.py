#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compose reviewed observer atoms with the current compiler owner; seal recipes, never product files."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
UPSTREAM = "4e29dc9d4141261e8763fbe9ffd6a20e017b1aff"
COMPILER = "src/jdk.compiler/share/classes/"
PATHS = [COMPILER + path for path in (
    "com/sun/source/util/M3AST.java",
    "com/sun/tools/javac/m3/M3ASTPC.java",
    "com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java",
    "com/sun/tools/javac/main/JavaCompiler.java")]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError("reviewed source seam is not unique: " + before[:90])
    return text.replace(before, after, 1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True, type=Path)
    parser.add_argument("--synexia", required=True, type=Path)
    args = parser.parse_args()
    pins_path = HERE / "UPSTREAM_COMPILER_COMPOSITION.json"
    if pins_path.exists():
        raise ValueError("composition already sealed; refine its reviewed templates and rerun the Maven recipes")
    originals = {path: subprocess.check_output(["git", "show", UPSTREAM + ":" + path], cwd=args.target)
                 for path in PATHS}
    history = HERE / "evidence/history/before-upstream-composition"
    history.mkdir(parents=True, exist_ok=True)
    old_seam = (HERE / "templates/M3ASTPC.java.txt").read_text()
    old_compiler_manifest = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-compiler-20261017/manifest.tsv"
    (history / "compiler-manifest.tsv").write_bytes(old_compiler_manifest.read_bytes())
    for name in ("M3ASTPC.java", "M3PrimitiveImageCompiler.java", "JavaCompiler.java", "M3ASTPlugin.java"):
        (history / (name + ".txt")).write_bytes((HERE / "templates" / (name + ".txt")).read_bytes())

    # Retain every existing public M3ASTPC method and add the independently scoped observer atom.
    seam = originals[COMPILER + "com/sun/tools/javac/m3/M3ASTPC.java"].decode()
    seam = replace_once(seam, "import java.util.Objects;", "import java.util.Objects;\nimport java.util.List;\nimport java.util.function.BiConsumer;\nimport com.sun.tools.javac.code.Source;")
    body = old_seam[old_seam.index("    private static final class State {"):old_seam.rfind("}")]
    body = body.replace("context.get(KEY)", "context.get(OBSERVER_KEY)").replace("context.put(KEY, state)", "context.put(OBSERVER_KEY, state)")
    seam = seam[:seam.rfind("}")] + "\n    // Independent optional immutable-index observers preserve the existing M3AST session contract.\n    private static final Context.Key<State> OBSERVER_KEY = new Context.Key<>();\n\n" + body + "}\n"
    (HERE / "templates/M3ASTPC.java.txt").write_text(seam)

    compiler = originals[COMPILER + "com/sun/tools/javac/main/JavaCompiler.java"].decode()
    compiler = replace_once(compiler, "        tree.sourcefile = filename;\n", "        tree.sourcefile = filename;\n        if (!silent) {\n            com.sun.tools.javac.m3.M3ASTPC.parsed(context, tree, content);\n        }\n")
    (HERE / "templates/JavaCompiler.java.txt").write_text(compiler)

    primitive = originals[COMPILER + "com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java"].decode()
    primitive = replace_once(primitive, "    public static void main(String[] args) throws Exception {\n", "    public static void main(String[] args) throws Exception {\n        run(args, task -> { });\n    }\n\n    /** Run the validated command with optional consumers attached before the same task parses. */\n    public static void run(String[] args, java.util.function.Consumer<? super JavacTask> beforeParse)\n            throws Exception {\n        Objects.requireNonNull(beforeParse, \"beforeParse\");\n")
    primitive = replace_once(primitive, "        emit(out, slots, sources, foldExpressions);", "        emit(out, slots, sources, foldExpressions, beforeParse);")
    primitive = replace_once(primitive, "                             boolean foldExpressions) throws Exception {", "                             boolean foldExpressions,\n                             java.util.function.Consumer<? super JavacTask> beforeParse) throws Exception {")
    primitive = replace_once(primitive, "            List<CompilationUnitTree> parsed = new ArrayList<>();", "            beforeParse.accept(task);\n            List<CompilationUnitTree> parsed = new ArrayList<>();")
    (HERE / "templates/M3PrimitiveImageCompiler.java.txt").write_text(primitive)
    (HERE / "templates/M3AST.java.jdk.txt").write_bytes(originals[PATHS[0]])

    plugin_path = HERE / "templates/M3ASTPlugin.java.txt"
    plugin = plugin_path.read_text().replace('return "M3AST";', 'return "M3ASTIndex";')
    plugin = plugin.replace('M3AST out=', 'M3ASTIndex out=').replace('"M3AST output ', '"M3ASTIndex output ')
    plugin_path.write_text(plugin)

    # Primary compiler recipes now admit the exact current upstream preimages.
    old_rows = {row.split("\t")[0]: row.split("\t") for row in old_compiler_manifest.read_text().splitlines()}
    for path, data in originals.items():
        preimage = HERE / "preimages/compiler" / path
        preimage.parent.mkdir(parents=True, exist_ok=True)
        preimage.write_bytes(data)
        if path in old_rows:
            old_rows[path][1] = digest(data)
    old_compiler_manifest.write_text("\n".join("\t".join(row) for row in old_rows.values()) + "\n")

    ast = "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/"
    recovery = {
        "canonical-recovery": (args.synexia, [ast + "M3ASTPlugin.java"]),
        "receiver-recovery": (args.target, PATHS + [
            "m3/vendor/synexia/" + ast + "M3ASTPlugin.java",
            "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/M3ASTPlugin.java"]),
    }
    for label, (root, paths) in recovery.items():
        for path in paths:
            if (root / path).exists():
                preimage = HERE / "preimages" / label / path
                preimage.parent.mkdir(parents=True, exist_ok=True)
                preimage.write_bytes((root / path).read_bytes())
    pins_path.write_text(json.dumps({"schema": "M3_UPSTREAM_COMPILER_COMPOSITION_V1",
        "upstream_revision": UPSTREAM, "plugin_name": "M3ASTIndex",
        "preserved_builtin_plugin_name": "M3AST", "source_pins": [{"path": path,
        "git_blob_sha1": hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest(),
        "sha256": digest(data)} for path, data in originals.items()],
        "recovery_paths": {label: paths for label, (_, paths) in recovery.items()},
        "policy": "Preserve existing public M3AST, Plugin/service behavior, exact parser retention and primitive consumer; add observer and command callback atoms. Module service registration remains owned by upstream module-info, outside the auxiliary patch jar."}, indent=2) + "\n")
    print("Composed reviewed upstream compiler templates and sealed recovery preimages; no product sources written")


if __name__ == "__main__":
    main()
