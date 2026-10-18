#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from hashlib import sha256
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SOURCE = ROOT / "implementations/apache-circular-fifo/src/main/java/com/m3/collections/apache/M3CircularFifoQueue.java"
EXPECTED = "3d337f1123ce5c8f977a395a37e515198a72fce967953d59e8a9a379463d8186"
OLD = "final int idx = (start + index) % maxElements;"
NEW1 = "final int remaining = maxElements - start;"
NEW2 = "final int idx = index < remaining ? start + index : index - remaining;"

data = SOURCE.read_bytes()
actual = sha256(data).hexdigest()
assert actual == EXPECTED, f"source SHA-256 drift: {actual}"
text = data.decode("utf-8")
assert OLD not in text, "old modulo atom remains"
assert text.count(NEW1) == 1, "remaining atom missing/duplicated"
assert text.count(NEW2) == 1, "single-wrap atom missing/duplicated"
assert "% maxElements" not in text[text.index("public E get("):text.index("private int increment(")]
print("M3_CIRCULAR_FIFO_INDEX_WRAP_SOURCE_PASS", actual)
