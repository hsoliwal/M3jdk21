M3TreeMap sorted bulk loading
-----------------------------
The private readSorted subtree split is adapted from fastutil drv/AVLTreeMap.drv
readTree, lines1851-1896, commit e70f36a137c349efa50f346818d16917a9f91986.
Upstream: https://github.com/vigna/fastutil/blob/e70f36a137c349efa50f346818d16917a9f91986/drv/AVLTreeMap.drv
Copyright (C) 2002-2026 Sebastiano Vigna. Licensed under Apache License 2.0:
https://www.apache.org/licenses/LICENSE-2.0
The source license is retained under LICENSE.fastutil. No fastutil Entry/thread
storage or GNU libavl-derived clone implementation is copied.
Modification: consume Map.Entry input into M3 Object[] payloads, int[] links
and byte[] heights; publish only after the destination structural epoch check.
Existing M3 rotations, deletion and views remain unchanged.
