Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

The fixed-alphabet/nibble-emission pattern is adapted from FasterXML/jackson-core@249716c8c2b141e4cab646f9d161fda0da97ae03 (jackson-core-2.16.1): JsonStringEncoder._appendByte and CharTypes.HClower/copyHexChars(false). Both complete pinned source files and the original Apache-2.0 LICENSE are retained under donor/. Jackson's full serializer and uppercase/general escaping policy are not transplanted. The M3 code keeps its own exact lowercase four-digit UTF16-unit policy and uses a local immutable16-character String alphabet; no new array owner, field or runtime dependency.

The pinned pre-existing Jackson2.16.1 JAR is a qualification input only (SHA256 f5f8ef90609e64fec82eb908e497dc7d81b2eb983fe509b870292a193cde4dfb). Its META-INF/NOTICE and FastDoubleParser-NOTICE are preserved verbatim under donor/, with provenance. No JAR or FastDoubleParser code is redistributed/adapted in this slice. Source-tree root and src/main/resources/META-INF NOTICE probes were absent; the JAR notices supply the actual attribution, not an invented upstream notice.

Existing Synexia canonical Maven/OpenRewrite sources, the RAM compiler adapter, the payload source and all three incumbent test programs retain their original Hitesh Soliwal/Synexia Apache-2.0 notices. Their before/after hashes bind the executed qualification.

LeetCode/HackerRank/GeeksforGeeks are catalogue-discovery categories only; no code from those sites is copied or assigned a license. New source generation was not invoked: this is bounded maintenance of an existing private atom and deterministic recipe replay.
