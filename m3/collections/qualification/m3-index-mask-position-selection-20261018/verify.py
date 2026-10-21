#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from hashlib import sha256
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FILES = {
    ROOT / "src/main/java/com/m3/collections/M3CountedBag.java":
        "2334edd988b5512c9ef6b7092a8ab5ac99e36df09629661ae3779f7090c5bc54",
    ROOT / "src/main/java/com/m3/collections/M3IndexMaskCompactor.java":
        "477135e13481942fef3b95b96a08d39c9c86dd92a649959c6d035b19b5784031",
    ROOT / "src/main/native/collections/m3_index_mask_compactor.c":
        "8610e9fcedcc8eb698acbf3161bca8ea7d16194e61c66915f9e25fe47aae27f7",
}
for path, expected in FILES.items():
    data = path.read_bytes()
    actual = sha256(data).hexdigest()
    assert actual == expected, f"{path}: SHA-256 drift {actual}"

bag = (ROOT / "src/main/java/com/m3/collections/M3CountedBag.java").read_text()
compactor = (ROOT / "src/main/java/com/m3/collections/M3IndexMaskCompactor.java").read_text()
native = (ROOT / "src/main/native/collections/m3_index_mask_compactor.c").read_text()

assert "rowIds = new int[keys.length]" not in bag
assert "M3IndexMaskCompactor.selectPositions(states, required, backend)" in bag
assert "public static int[] selectPositions(" in compactor
assert "selectPositionsJava(rowStates, requiredMask)" in compactor
assert "NativePositions.AVAILABLE" in compactor
assert "selectNative(identityPositions(rowStates.length), rowStates, requiredMask)" in compactor
assert "Java_com_m3_collections_M3IndexMaskCompactor_positionsAbiNative" in native
assert "Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative" in native
assert "positions[accepted++] = offset + i" in native
assert "GetPrimitiveArrayCritical" not in native
print("M3_INDEX_MASK_POSITION_SELECTION_SOURCE_PASS")
