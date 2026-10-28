#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact-source verification; not an OpenRewrite execution proof."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-indexed-live-views-20261021"
PRE = Path(__file__).resolve().parent / "pre"

# Historical six-row manifest is immutable; later, separately accepted product
# implementations are admitted only through this exact, Git-blob-sealed source
# ancestry. This is verification data, NOT a second naming registry.
HISTORY_PATH = Path(
    "m3/collections/qualification/arraylist-dual-word-20261023/SOURCE_HISTORY_DAG_20261025.md"
)
HISTORY_BLOB_SHA1 = "d1765e37afb90175de5df76df63c1e7f799f9628"
SUCCESSOR_CHAINS = {
    "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java": (
        "4f38bc4801a488c9b3cf1c2ec848a3293ec6b89c91bf01e6f6a9334197bbc0fa",
        "9833b3d8d36287fe38025d9b7c38fb09e495f94f27c611af85433130f1e72ac2",
        "884c9078022f64eb3c1f4aa8c306d852521be6089164c7e99b08e17c318d8282",
        "2168db105fe700ea1a8022ad362ee41d8f181180deba8404ea87d9c70f46d134",
    ),
    "m3/collections/src/main/java/com/m3/collections/M3IndexedListSnapshot.java": (
        "c59c2aaa40ef12712b7a5c3ab8af0173a63ae6b995fd4b71695bd61e312d7bff",
        "09594e9b2116889f9ca4065fabadf4d7dc4e2d9bae8a506d277f49fdc5ea362f",
        "43e9eb81a6828137668e597accbe347a315146d278f423e91fab5754af7f1431",
    ),
    "m3/collections/src/main/java/com/m3/collections/M3IndexedLiveView.java": (
        "84e9437c27f59060543cd4728a5222b489fe98f7e39068e66f4431b7ee00867f",
        "2e5c00296e26186c071fc47d2e3bcd16315825c8404bf271eafa8cd597c268af",
    ),
}


def require_sealed_history() -> str:
    raw = inside_repo(ROOT / HISTORY_PATH).read_bytes()
    expected = hashlib.sha1(
        b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw
    ).hexdigest()
    if expected != HISTORY_BLOB_SHA1:
        raise RuntimeError("unreviewed source-history DAG mutation")
    return raw.decode("utf-8")


def admit_historical_successor(path: str, original: str,
                               candidate: str, history: str) -> None:
    chain = SUCCESSOR_CHAINS.get(path)
    if chain is None or chain[0] != original or chain[-1] != candidate:
        raise RuntimeError(f"receiver != qualified historical source: {path}")
    if not all(pin in history for pin in chain):
        raise RuntimeError(f"incomplete source-history chain: {path}")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def inside_repo(path: Path) -> Path:
    resolved = path.resolve()
    if not resolved.is_relative_to(ROOT) or path.is_symlink():
        raise RuntimeError(f"unsafe source path: {path}")
    return path


def main() -> None:
    rows = [row.split("\t") for row in (CRATE / "manifest.tsv")
            .read_text(encoding="utf-8").splitlines()
            if row.strip() and not row.startswith("#")]
    if len(rows) != 6 or any(len(row) != 4 for row in rows):
        raise RuntimeError("six exact recipe rows required")
    if [row[0] for row in rows] != sorted({row[0] for row in rows}):
        raise RuntimeError("manifest paths must be strictly increasing")
    history = None
    successors = 0
    for path, before, after, template in rows:
        if not path.startswith("m3/collections/src/") or not template.endswith(".java.txt"):
            raise RuntimeError(f"invalid receiver path: {path}")
        recipe_file = inside_repo(CRATE / template)
        if not recipe_file.is_file() or digest(recipe_file.read_bytes()) != after:
            raise RuntimeError(f"recipe template drift: {template}")
        receiver = inside_repo(ROOT / path)
        if not receiver.is_file():
            raise RuntimeError(f"missing source receiver: {path}")
        current = digest(receiver.read_bytes())
        if current != after:
            if history is None:
                history = require_sealed_history()
            admit_historical_successor(path, after, current, history)
            successors += 1
        if before != "ABSENT":
            prior = inside_repo(PRE / (Path(path).name + ".txt"))
            if not prior.is_file() or digest(prior.read_bytes()) != before:
                raise RuntimeError(f"source preimage drift: {path}")
        mode = "qualified-successor" if current != after else "original-postimage"
        print(f"PIN_PASS {path} before={before} after={after} current={current} mode={mode}")
    print(f"M3_INDEXED_LIVE_RECIPE_PIN_PASS files={len(rows)} qualified_successors={successors}")


if __name__ == "__main__":
    main()
