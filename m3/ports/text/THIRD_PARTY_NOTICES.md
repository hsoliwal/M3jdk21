# Retained text provenance

The seven owner files under `../indexstring/src/main/java/com/synexia/indexstring`
are imported from the user-owned
`hsoliwal/com.synexia` at `6df9df8d8f42111239013ee941723ec37f97ba6e`. Their
Apache-2.0 SPDX notices and original public names are retained. The Apache license
is preserved in `LICENSE`. Source hashes and target adaptations are in
`provenance-consolidated.json`; `provenance.json` and `donor-adaptations.patch`
retain the earlier f07 selected source plan. Only this selected dependency closure is authorized for the
public migration; unrelated private repository contents are not imported.

`FrozenByteInterner` gains exact UTF16BE String probes and cache clearing;
`FrozenChars` gains an immutable byte-owner projection. The M3 facade composes
these owners. The existing `com.m3.text.compat.M3Text` facade and the three
`com.m3.indexstring` facade sources share this one physical owner closure. Their
admission, equality and search policies remain separate public API contracts.
The consolidation retains incoming `MIndexJoinedChars.concat` and `copyTo`
alongside the selected owner projection and uncached compaction APIs. It does
not replace structural `MIndexAtomStore` or resolver IDs.

Stock JDK regex, charset and I/O APIs are invoked through their public interfaces;
their source is not copied or relicensed. OpenJDK retains its existing licenses.
The OpenClaw harvest was checked first; its non-Java lifecycle/packaging tools do
not supply this storage contract. No OpenClaw source is included.
