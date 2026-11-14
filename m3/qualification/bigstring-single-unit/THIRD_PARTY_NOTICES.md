Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

Production reuses existing M3BigString parallel-array chunk metadata/Bloom summaries and invokes Java21 String.indexOf(int,int,int) on an existing immutable String leaf. OpenJDK jdk-21+35 String.java is identified by commit890adb6410dab4606a4f26a942aed02fb2f55387/blob5c234d4dfacac9e642578483640d4e6b41488beb as a reviewed platform API surface. No OpenJDK GPL source is copied or redistributed in this Apache-2.0 slice. The existing M3 source header's Swift Collections conceptual attribution is preserved; no new Swift code is copied.

The JGit merge qualifier invokes the pre-existing org.eclipse.jgit6.9.0.202403050737-r artifact (SHA2566209ac6691be2a06320f02000d89bed73edf05f45d65ef9a155147511ab77f3c). Source release tagv6.9.0.202403050737-r resolves to d764bc34fc15079379a71bb638e7c1b5836e061e. Full Eclipse Distribution License v1.0 and the actual JAR about.html are retained in donor/. No JGit JAR or JGit implementation is bundled; it is an existing qualification-only dependency, not a production dependency.

The reference BigString is a mechanically relocated copy of the pinned original M3 class: package declaration plus one M3StringPiece import only. The public BigString contract program is unchanged. The streaming adapter retains exact pinned check/Windows/ropeWindows source segments and runs only the in-memory rope-window subset. Its filesystem/shard portion is not invoked or claimed. Their Hitesh Soliwal/Synexia Apache-2.0 notices are retained.

Canonical Maven/OpenRewrite sources and the RAM compiler adapter retain their existing notices. One exact owner admission is qualified; no generic source root is widened. The pending R234 source is a pinned merge-test input, not proof that its PR is merged. New authored maintenance and CPU qualification retain Hitesh Soliwal/Synexia Apache-2.0 attribution.

No implementation from LeetCode, HackerRank or GeeksforGeeks is copied or assigned a license; catalogue categories guide discovery and the existing/platform owners supply this implementation.
