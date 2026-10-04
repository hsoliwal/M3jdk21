# UTF-8 and payload bounds for the existing semantic codec

## Authority and scope
Continue the database/parser work at the existing `M3IndexDbSemanticCodec` owner.
Do not add a database, parser, public API or long type name. The JDK charset
encoder/decoder does Unicode validation. Existing functions and private methods
express the reusable atoms. This is the semantic snapshot binary parser, NOT
javac/OpenRewrite source parsing or SQL.

Base: PR79 `9d9c9370e4cebbd6a87cb6ac60bf2917c4ea6f64`.
One production file, additive tests and an existing-recipe crate. V1 valid UTF-8
bytes remain identical. Intentional invalid-input tightening needs MODULE-level
contract review despite the implementation edit being file-local.

## Defects to reproduce before repair
1. Malformed UTF-8 in persisted source-path metadata is silently replaced and
   accepted; the decoder's re-encoded bytes differ from the supplied payload.
2. An unpaired Java surrogate is silently replaced by the encoder; source metadata
   cannot round-trip. V1 uses UTF-8, not an arbitrary UTF-16 code-unit representation.
3. A 24-byte header can request huge string/node/edge ArrayList capacities before
   payload truncation is checked, exhausting a small JVM heap.

## Change
Use standard `CharsetEncoder`/`CharsetDecoder` in REPORT mode. Reject ill-formed
UTF-8 or unpaired surrogates rather than inventing replacement text. Valid U+FFFD,
supplementary characters and non-ASCII text remain legal and byte-compatible.
Never normalize Unicode or change strings to achieve deduplication.

Before count-dependent allocations, prove the minimum record bytes fit the
remaining supplied payload with long arithmetic. V1 has four-byte string lengths,
173-byte node records and 16-byte edges. Before each text allocation reserve
remaining string prefixes and all fixed records. Existing row and byte limits
remain; this is not a full configurable memory quota or hostile concurrent-input
guarantee. Never catch OutOfMemoryError and call it validation.

Encode/decode byte limits must agree. Invalid persisted payloads retain the
existing IOException translation at M3IndexDB.requireSemanticIndex. Rejected
encoding must occur before publishing any new database artifact.

## Semantic atoms and roles
- requireBytes: Specification, payload-budget precondition.
- utf8: JDK CharsetEncoder Adapter, lossless valid-text encoding.
- readText: JDK CharsetDecoder Adapter, bounded strict decoding.
- CodecTest/CodecProbe: ContractProbe, real persisted bytes and small-heap process.
No new functional interface and no replacement of standard APIs.

## Proof
Record old behavior first; run strict Java21 compilation. Compare old/new bytes
for fixed and generated valid snapshots across separate JVMs. Exercise malformed
Unicode, truncated/fabricated counts, database error propagation and persisted
reopen. Run real JUnit and retain the unchanged0.99 line/branch gate.
Recipe uses the existing hash-pinned Java-LST mechanism, module-relative paths
and separate replay/refusal tests. Source identity is not semantic proof.
