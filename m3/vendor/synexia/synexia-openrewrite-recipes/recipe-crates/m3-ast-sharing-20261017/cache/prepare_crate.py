# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Seal reviewed cache templates and exact donor custody; does not write product sources."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[3]
RESOURCES = ROOT / "src/main/resources/com/synexia/rewrite"
PIN = "e244d68fdee88d4a406be48e4ff1d31b60a17988"
DONORS = [
    "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/TieredSwapCache.java",
    "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/SwapStore.java",
    "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/SwapBudgetPlane.java",
    "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/SqliteSwapStore.java",
    "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/SwapNativeLibrary.java",
    "synexia-native-interop/src/main/java/com/synexia/nativeinterop/JniLibraryLoader.java",
    "synexia-native-interop/src/main/java/com/synexia/nativeinterop/NativeLibrarySpec.java",
    "synexia-mindex/runtime/native/swap/mindex_swap.c",
    "synexia-mindex/runtime/native/swap/CMakeLists.txt",
]
DONOR_BLOBS = {
    "TieredSwapCache.java": "22fe5017f55d39bba1a10d588ecf1b12fad17726",
    "SwapStore.java": "47eee325cec2f97c09d22cdb37e9f7a91abb80d5",
    "SwapBudgetPlane.java": "600754efed3306213dac1908ef4fbca8a865f6f4",
    "SqliteSwapStore.java": "f74acfb24e0a3061f542aaf8b8ba9b7a646604ee",
    "SwapNativeLibrary.java": "ae51e7d3e0fcdabeb3725c5a834adf95ba7f6ff0",
    "JniLibraryLoader.java": "da5d6391af12d53a71c5f1e0f3736fa125659b64",
    "NativeLibrarySpec.java": "39b9f04e46411de9bf7b8063608241efb2819aca",
    "mindex_swap.c": "e55b0c4c6e78b789e570c6c002a6157ad73fbfb5",
    "CMakeLists.txt": "2ec59294a9d14f0420cdbc4a44645c776dae3b3f",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def write_crate(kind, name, sources):
    dest = RESOURCES / kind / name
    dest.mkdir(parents=True, exist_ok=True)
    manifest = []
    for index, (path, data) in enumerate(sorted(sources.items())):
        resource = f"{index:02d}-{Path(path).name}.txt"
        (dest / resource).write_bytes(data)
        manifest.append(f"{path}\tABSENT\t{sha(data)}\t{resource}\n")
    (dest / "manifest.tsv").write_text("".join(manifest), encoding="utf-8")


canonical = {}
target = {}
native = {}
lineage = []
for name in ("M3FileSourceFacts", "M3ASTWorkspaceCache"):
    data = (ROOT / "templates" / f"{name}.java.txt").read_bytes()
    path = f"synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/{name}.java"
    canonical[path] = data
    mapped = data.replace(b"package com.synexia.mindex.compiler.ast;", b"package com.m3.compiletime.ast;")
    target[f"m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/{name}.java"] = mapped
    lineage.append({"source": path, "sourceSha256": sha(data), "targetSha256": sha(mapped),
                    "mode": "PACKAGE_ONLY_ADAPTER_MAPPING", "license": "Apache-2.0"})

for path in DONORS:
    data = (REPO / path).read_bytes()
    (target if path.endswith(".java") else native)["m3/vendor/synexia/" + path] = data
    git_blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    if git_blob != DONOR_BLOBS[Path(path).name]:
        raise ValueError("canonical donor drift from pinned revision: " + path)
    lineage.append({"source": path, "revision": PIN, "gitBlob": git_blob,
                    "sourceSha256": sha(data), "mode": "EXACT_VENDOR_CUSTODY", "license": "Apache-2.0"})

for test in sorted((ROOT / "runtime-tests").glob("*.java")):
    data = test.read_bytes()
    canonical[f"synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/ast/{test.name}"] = data
    target[f"m3/ports/compiletime/src/test/java/com/m3/compiletime/ast/{test.name}"] = data.replace(
        b"com.synexia.mindex.compiler.ast", b"com.m3.compiletime.ast")

write_crate("hash-pinned-java", "m3-ast-cache-canonical-20261017", canonical)
write_crate("hash-pinned-java", "m3-ast-cache-target-20261017", target)
write_crate("hash-pinned-text", "m3-ast-cache-native-20261017", native)
(ROOT / "SOURCE_LINEAGE.json").write_text(json.dumps({"sourceRevision": PIN, "files": lineage}, indent=2) + "\n")
print(f"Sealed canonical={len(canonical)}, target Java={len(target)}, native={len(native)} postimages")
