#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact M3 native stream postimage and public-erasure compatibility receipt.

Run from any directory: python3 m3/collections/qualification/stream-current-compatible-20261118/verify.py
Does not claim to replace the actual JDK module compiler / JUnit or OpenRewrite engine.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
# Groovy collate successor: retain the six prior receipts and bind both new owners.
FILES = {
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStream.java":
        "9202ef14279f4ce140497d658130ccad2533843351940f317aa001dd38fd3251",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamAdvancedKernels.java":
        "29810419f0125147d7bdbc0d33794b113dcd07e85f0ba9937e5aabd806e6103d",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLoopKernels.java":
        "b7f5f9ba4f9a82b6a14a349a27ab10b43cc9b2f93ce224a3916c8a30989f1813",
    "test/jdk/jdk/m3/collections/ObjectStreamAdvancedContractTest.java":
        "da888b9b9ff96dc106bdad4d21e5de47f181f889477846367d537047263f53eb",
    "test/jdk/jdk/m3/collections/ObjectStreamLoopContractTest.java":
        "8c910a3d4fe8a10d8315686eeb21131051f067dd0f1bd8c7a0134c8430e19d7e",
    "test/jdk/jdk/m3/collections/ObjectStreamCompatibilityGateTest.java":
        "56b64efee5f246baea4a0c274f8ef684286bbbf9d7de79be725ff5a8cd764a90",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLanguageKernels.java":
        "d3167b2189bf303cabfc557ce8474b1df75af0d586e0c8adbe4377b6de569609",
    "test/jdk/jdk/m3/collections/ObjectStreamCollateTest.java":
        "8cfb6c344d69dab1957e499fdfb0cc5b8b33fa61708e1c5595a5c7dc47c08f2f",
}

MUST_RETAIN = (
    "arrayWindow(", "mapAdjacent(", "distinctAdjacent(", "foldByKey(",
    "pairwise(", "mapIndexed(", "mapIndexedOrdinalFirst(", "mapWhile(",
    "mapWhileNullable(", "zipLongest(", "topKBy(", "groupMapReduce(",
    "mergeSorted(", "partitionMap(", "collate(", "countByMany(",
)
UNIQUE = (
    "public interface IndexedMapper<",
    "public interface OrdinalFirstMapper<",
    "public <R> ObjectStream<R> mapIndexed(",
    "public <R> ObjectStream<R> mapIndexedOrdinalFirst(",
    "public <R> ObjectStream<R> mapWhile(",
    "public <R> ObjectStream<R> mapWhileNullable(",
    "public <K> Map<K, Long> countByMany(",
    "public ObjectStream<List<T>> collate(int width)",
    "public ObjectStream<List<T>> collate(int width, int step)",
    "public ObjectStream<List<T>> collate(int width, int step, TailPolicy tailPolicy)",
)
INTEGRATION = (
    "import java.util.stream.Streams;",
    "Streams.pairwise(input, mapper)",
    "Streams.mapWhile(input, mapper)",
    "ObjectStreamLanguageKernels.collate(stream(), width, step, tailPolicy)",
)


def verify() -> None:
    for path, expected in FILES.items():
        content = (ROOT / path).read_bytes()
        actual = hashlib.sha256(content).hexdigest()
        if actual != expected:
            raise RuntimeError(f"postimage SHA-256 drift: {path}: {actual} != {expected}")
        print(f"SOURCE_OK {path} {actual}")
    source = (ROOT / next(iter(FILES))).read_text(encoding="utf-8")
    verify_api(source)
    print("M3_STREAM_CURRENT_SOURCE_RECEIPT_OK")


def verify_api(source: str) -> None:
    """Retain native API, source-erasure, and delegation contracts after identity checks."""
    for needle in MUST_RETAIN + INTEGRATION:
        if needle not in source:
            raise RuntimeError(f"lost native API or JDK delegate: {needle}")
    for needle in UNIQUE:
        if source.count(needle) != 1:
            raise RuntimeError(f"duplicate/missing source erasure: {needle} ({source.count(needle)})")
    if "public <R> ObjectStream<R> mapWhile(Function<? super T, ? extends R>" in source:
        raise RuntimeError("null-sentinel mapWhile collides with Optional-based method erasure")


if __name__ == "__main__":
    verify()
