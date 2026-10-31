#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Canonical payload producer, never called by the receiving product builder.

Source application belongs exclusively to the canonical Synexia OpenRewrite
snapshot recipe. This review-time producer emits exact preimages/postimages and
seals; it does not edit the donor checkout.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re

PIN = "d1b7235f1ba1c1eea5b04a5b7e6554595a90719e"
CRATE = "m3-lucene-jdk21-current-fork-20261018"
SKIP = {"org.apache.lucene.core.tests.main", "org.apache.lucene.test_framework",
        "org.apache.lucene.benchmark", "org.apache.lucene.benchmark.jmh",
        "org.apache.lucene.demo"}

# Match non-code tokens first. Standalone underscore identifiers in code are
# the only tokens renamed. No comments, strings, chars or text blocks change.
TOKENS = re.compile(
    r'//[^\r\n]*|/\*[\s\S]*?\*/|"""(?:\\[\s\S]|(?!""")[\s\S])*"""'
    r'|"(?:\\[\s\S]|[^"\\])*"|\'(?:\\[\s\S]|[^\'\\])*\''
    r'|(?<![\w$])_(?![\w$])'
)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def rename_unnamed(source):
    counter = 0
    def replace(match):
        nonlocal counter
        if match.group() != "_":
            return match.group()
        while True:
            counter += 1
            name = f"m3Unused{counter:04d}"
            if not re.search(r"(?<![\w$])" + name + r"(?![\w$])", source):
                return name
    return TOKENS.sub(replace, source)

def replace_exact(source, before, after, expected=1):
    count = source.count(before)
    if count != expected:
        raise ValueError(f"Expected {expected} exact occurrences, found {count}: {before!r}")
    return source.replace(before, after)

def transform(path, source):
    result = rename_unnamed(source)
    if path.endswith("/internal/vectorization/VectorizationProvider.java"):
        result = replace_exact(result, "Set.of(Option.DROP_METHOD_INFO)", "Set.of()")
        result = replace_exact(
            result,
            "assert runtimeVersion >= 25;\n    if (runtimeVersion <= UPPER_JAVA_FEATURE_VERSION)",
            "assert runtimeVersion >= 21;\n"
            "    // The existing scalar implementation is authoritative below the Java 25 SIMD lane.\n"
            "    if (runtimeVersion < 25) {\n"
            "      return new DefaultVectorizationProvider();\n"
            "    }\n"
            "    if (runtimeVersion <= UPPER_JAVA_FEATURE_VERSION)")
    elif path.endswith("/internal/tests/TestSecrets.java"):
        result = replace_exact(
            result, "Set.of(Option.DROP_METHOD_INFO, Option.RETAIN_CLASS_REFERENCE)",
            "Set.of(Option.RETAIN_CLASS_REFERENCE)")
        result = replace_exact(result, "Set.of(Option.DROP_METHOD_INFO)", "Set.of()")
    elif path.endswith("/sandbox/codecs/dedup/DedupVectorValues.java"):
        result = replace_exact(
            result,
            "      RandomAccessInput slice =\n"
            "          vectorData.randomAccessSlice(fieldOrdToGroupOrdOffset, fieldOrdToGroupOrdSize);\n"
            "      LongValues values = DirectReader.getInstance(slice, fieldOrdToGroupOrdBitsPerValue);\n"
            "      this(\n"
            "          vectorData,\n"
            "          fieldOrdToGroupOrdOffset,\n"
            "          fieldOrdToGroupOrdSize,\n"
            "          fieldOrdToGroupOrdBitsPerValue,\n"
            "          values);",
            "      this(\n"
            "          vectorData,\n"
            "          fieldOrdToGroupOrdOffset,\n"
            "          fieldOrdToGroupOrdSize,\n"
            "          fieldOrdToGroupOrdBitsPerValue,\n"
            "          DirectReader.getInstance(\n"
            "              vectorData.randomAccessSlice(fieldOrdToGroupOrdOffset, fieldOrdToGroupOrdSize),\n"
            "              fieldOrdToGroupOrdBitsPerValue));")
    elif path.endswith("/sandbox/codecs/faiss/FaissNativeWrapper.java"):
        result = replace_exact(
            result,
            "SymbolLookup.loaderLookup().findOrThrow(functionName)",
            "SymbolLookup.loaderLookup().find(functionName).orElseThrow(\n"
            "            () -> new java.util.NoSuchElementException(\"Symbol not found: \" + functionName))")
        result = replace_exact(result, ".getString(0)", ".getUtf8String(0)")
    elif path.endswith("/sandbox/codecs/faiss/FaissLibraryNativeImpl.java"):
        for before, after in (
            ("temp.allocateFrom(description)", "temp.allocateUtf8String(description)"),
            ("temp.allocateFrom(indexParams)", "temp.allocateUtf8String(indexParams)"),
            ("temp.allocateFrom(JAVA_FLOAT, query)", "temp.allocateArray(JAVA_FLOAT, query)"),
            ("temp.allocateFrom(JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN), bits)",
             "temp.allocateArray(JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN), bits)"),
        ):
            result = replace_exact(result, before, after)
    elif path.endswith("/expressions/js/JavascriptCompiler.java"):
        result = replace_exact(
            result, "import java.lang.classfile.", "import io.github.dmlloyd.classfile.", expected=12)
        result = replace_exact(
            result,
            "    builder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL).withSuperclass(CD_Expression);",
            "    // The source-built Java 21 lane must emit classes loadable by its target VM.\n"
            "    builder.withVersion(ClassFile.JAVA_21_VERSION, 0);\n"
            "    builder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL).withSuperclass(CD_Expression);")
    elif path == "lucene/expressions/src/java/module-info.java":
        result = replace_exact(
            result, "  requires org.antlr.antlr4.runtime;",
            "  requires org.antlr.antlr4.runtime;\n  requires io.github.dmlloyd.classfile;")
    return result

def produce(source, destination):
    resource = destination / "resources/com/synexia/rewrite/hash-pinned-text" / CRATE
    templates = resource
    originals = destination / "preimages"
    templates.mkdir(parents=True, exist_ok=True)
    originals.mkdir(parents=True, exist_ok=True)
    manifest = []
    locks = []
    modules = []
    for descriptor in sorted(source.glob("lucene/**/src/java/module-info.java")):
        content = descriptor.read_text()
        name = re.search(r"^module\s+(\S+)\s+\{", content, re.M).group(1)
        if name in SKIP:
            continue
        module_root = descriptor.parent.parent.parent
        roots = [descriptor.parent]
        resources = descriptor.parent.parent / "resources"
        if resources.is_dir():
            roots.append(resources)
        modules.append({
            "name": name, "directory": str(module_root.relative_to(source)),
            "source_roots": [str(p.relative_to(source)) for p in roots],
            "requires": re.findall(r"\brequires\s+(?:(?:static|transitive)\s+)*([\w.]+);", content),
        })
        for root in roots:
            for path in sorted(root.rglob("*")):
                if not path.is_file():
                    continue
                if path.is_symlink():
                    raise ValueError("Symlink source refused: " + str(path))
                relative = path.relative_to(source).as_posix()
                before = path.read_bytes()
                after = transform(relative, before.decode("utf-8")).encode("utf-8") if path.suffix == ".java" else before
                locks.append({"path": relative, "before": sha(before), "after": sha(after)})
                if before != after:
                    basename = relative.replace("/", "__") + ".template"
                    (templates / basename).write_bytes(after)
                    original = originals / relative
                    original.parent.mkdir(parents=True, exist_ok=True)
                    original.write_bytes(before)
                    manifest.append((relative, sha(before), sha(after), basename))
    if len(manifest) > 256:
        raise ValueError("Canonical snapshot target bound exceeded")
    legal = [source / "LICENSE.txt", source / "NOTICE.txt", *sorted((source / "lucene/licenses").glob("*.txt"))]
    for path in legal:
        name = path.relative_to(source).as_posix()
        locks.append({"path": name, "before": sha(path.read_bytes()), "after": sha(path.read_bytes())})
    rows = "".join("\t".join(row) + "\n" for row in sorted(manifest))
    (resource / "manifest.tsv").write_text(rows)
    lock = {"source": "https://github.com/hsoliwal/lucene", "revision": PIN,
            "recipe": CRATE, "manifest_sha256": sha(rows.encode()),
            "java_release": 21, "preview_required": True,
            "production_modules": sorted(modules, key=lambda m: m["name"]),
            "files": sorted(locks, key=lambda f: f["path"])}
    (destination.parent / "source-lock.json").write_text(json.dumps(lock, indent=2) + "\n")
    print(json.dumps({"modules": len(modules), "source_files": len(locks),
                      "transformed_files": len(manifest), "manifest_sha256": lock["manifest_sha256"],
                      "resources": str(destination / "resources")}, indent=2))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=Path(__file__).resolve().parent)
    args = parser.parse_args()
    produce(args.source.resolve(), args.out.resolve())

