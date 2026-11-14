# Portable compiled-regex receiver

Status: isolated proof receiver; no java.base promotion.

Canonical recipe/source ownership stays in `hsoliwal/com.synexia`.

The source bundle is produced from the exact Synexia regex-program lineage pinned by the full-family
migration invariant. It is compiled here first because the target has no existing compiled-regex
program owner beyond the already-integrated `M3TQ` candidate filter.

This module must prove `matches`, `find`, `lookingAt`, unsupported-syntax refusal and binary
image round-trip behavior against Java 21 before the implementation is renamed/promoted under
`jdk.internal.mindex`.

The proof bundle deliberately drops MIndex-specific fast-fact shortcuts and uses generic
`CharSequence` scans instead. That is a performance adaptation only. It must never turn an
unsupported expression into an accepted approximation.

First-party source remains:

`Copyright 2026 Hitesh Soliwal and contributors`

under Apache-2.0. The later OpenJDK product integration retains separate Apache legal attribution;
existing OpenJDK source keeps its original license.

No Pattern.java/Matcher.java edit is authorized by this packet.
