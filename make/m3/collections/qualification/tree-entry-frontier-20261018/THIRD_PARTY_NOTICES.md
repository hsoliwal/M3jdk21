M3 attached-entry projection
The index projection pattern is adapted from fastutil drv/OpenHashMap.drv
MapEntry lines1360-1395, commit e70f36a137c349efa50f346818d16917a9f91986,
blob8c5a1ab10917d770f4628150e4805fe9349b9797.
Copyright (C) 2002-2026 Sebastiano Vigna. Apache License 2.0, retained in
LICENSE.fastutil. The selected pattern is used in M3 LiveEntry slot hints;
hash ownership, rehash and recycled fast entries are excluded.
Modified2026 by Hitesh Soliwal and Contributors to the Synexia Project:
keep existing entry projections and use an index only at the same structural
revision, with legacy key resolution after change. The owner stores no entry
objects. M3 core headers and prior sorted-bulk fastutil attribution remain.
