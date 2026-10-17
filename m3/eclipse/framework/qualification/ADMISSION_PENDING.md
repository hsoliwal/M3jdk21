<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verified boundaries of this forwarding wave
Five Java21 stubbed-interface tests executed locally with
`javac --release 21 -Xlint:all -Werror`:
- OSGi ID/state handles, id=0, Long.MAX_VALUE
- duplicate and illegal OSGi states
- Eclipse extension snapshot validity and live lookup
- IResourceDelta BFS parent-index and primitive kind filters
- resource delta budget and duplicate refusal; depth 2049 without recursive stack

**Stubbed test success is not real Eclipse JUnit, OSGi launch, JVM runtime or JNI
proof.** Source postimages are canonical SHA-256-exact recipe templates;
target Maven/OpenRewrite and 5 JUnit cases must execute at the PR exact head.
Leave OSGi/Equinox, SWT/Nebula, JFace, EMF, PDE, Workbench/JDT migrations
unqualified until their own native and public-API gates pass.
