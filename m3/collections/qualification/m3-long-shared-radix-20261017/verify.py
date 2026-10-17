#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from hashlib import sha256
from pathlib import Path
root=Path(__file__).resolve().parents[4]
rows={
"m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java":"57acf6174c2e2f15931fb1e43cdc6026501a8bbb8789d57a5e143a2ec24714b3",
"m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java":"01a27d87732081d6c0ada76e1ed4258b25fec0a558caa6826167918cc956a60f",
"m3/collections/qualification/m3-long-shared-radix-20261017/M3LongSharedRadixProof.java":"6d2e63f078f6f0a6b268144dcdcbc15c574694db330aa3ac2c4b58aebd262959",
}
for name,expected in rows.items():
  path=root/name
  if not path.is_file() or path.is_symlink() or sha256(path.read_bytes()).hexdigest()!=expected:
    raise SystemExit("M3_SHARED_LONG_RADIX_RECEIVER_FAIL source="+name)
projection=(root/"m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java").read_text()
signal=(root/"m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java").read_text()
for atom in ("M3SignalOrder.sortUnsignedKeyRows(keys, rowIds);",
             "M3SignalOrderNative.sort(keys, rowIds);",
             "M3LongAddressKernel.loadPhysicalUnchecked(values, rowIds[index]);",
             "M3IndexPathMasks.select(indices, indices,", "if (admitRadix(size))"):
    if atom not in projection: raise SystemExit("M3_SHARED_LONG_RADIX_RECEIVER_FAIL lost="+atom)
if "sortJava(keys, rowIds);" not in signal: raise SystemExit("M3_SHARED_LONG_RADIX_RECEIVER_FAIL no-shared-executor")
print("M3_SHARED_LONG_RADIX_RECEIVER_PASS files=3 no_new_jni=true exact_recipe=true")
