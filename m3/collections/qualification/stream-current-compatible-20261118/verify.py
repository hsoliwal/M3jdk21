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
# Stream convergence successor: retain six prior seals and bind two qualified owners.
# Recipe: com.synexia.m3.StreamReceivingGuard20261122.
# The exact prior guard and seals remain in its canonical recipe beforeimage.
FILES = {
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStream.java":
        "aeb6c3f614aa223cfe37cc5ddbc991f9dd4d9106b4f5940fd707ff57e106a465",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamAdvancedKernels.java":
        "fc9a3d1a0c0ab7114b6f2765f0214915115272a2dc02d6e85168b600e0ad31d3",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLoopKernels.java":
        "02b56913bb682f90a3f7d2d04689c259a6e8c6ee68748ad920cfd1082937c024",
    "test/jdk/jdk/m3/collections/ObjectStreamAdvancedContractTest.java":
        "da888b9b9ff96dc106bdad4d21e5de47f181f889477846367d537047263f53eb",
    "test/jdk/jdk/m3/collections/ObjectStreamLoopContractTest.java":
        "8c910a3d4fe8a10d8315686eeb21131051f067dd0f1bd8c7a0134c8430e19d7e",
    "test/jdk/jdk/m3/collections/ObjectStreamCompatibilityGateTest.java":
        "56b64efee5f246baea4a0c274f8ef684286bbbf9d7de79be725ff5a8cd764a90",
    "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLanguageKernels.java":
        "fd565bea1630eddb3cadf5ce139e113bbf961602287922777b9c08865815fc23",
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
    "delegateStream(input -> ObjectStreamLanguageKernels.pairwise(input, mapper))",
    "delegateStream(input -> ObjectStreamLoopKernels.mapWhileOptional(",
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
