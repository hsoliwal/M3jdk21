<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Notices

This optional provider and its compiler integration tests are Copyright 2026
Hitesh Soliwal and contributors, licensed under Apache License 2.0.

The provider compiles against the independently owned Gradle fork's published
`M3JavacASTProvider` API. That API remains Copyright 2026 Gradle and contributors,
licensed under Apache License 2.0; its source and classes are not redistributed
in the provider JAR. The exact source compatibility pin is
`hsoliwal/gradle@419a77cf61af3924f60678510ed2440230047671`, source blob
`f2a08ee8074d6f6faec69543032c9176ad62ef11`.

The public `com.sun.source.util.M3AST` API and compiler implementation remain in
the selected M3JDK compiler module with their original licensing and ownership.
They are not copied into this provider artifact. JSpecify is a provided compile
dependency and JUnit is a test dependency; neither is bundled in this JAR.
