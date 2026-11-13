# Synexia M3IndexDB V1 fingerprint compatibility receiver

Status: target-side compatibility handoff on M3JDK21.

## Canonical compatibility source

Repository: `hsoliwal/com.synexia`

Pending canonical PR: #9637

Pinned candidate revision:
`4c5705bcef78b4489c5c695c665db2bca4d2b40a`

Canonical compatibility owner:
`com.synexia.mindex.db.M3IndexDbFingerprintV1`

Canonical source Git blob:
`64318cf995164aee065e07dc6315b60630e377f5`

Historical M3JDK21 owner:
`com.m3.indexdb.M3IndexDbSemanticFingerprint`

Historical source Git blob:
`428658eb8565f77f8e450a98e24e90a3837a2215`

## Compatibility rule

The historical V1 fingerprint bytes/hashes are a persisted compatibility contract. M3JDK21 keeps
its old class frozen while importing the Synexia V1 compatibility owner under its canonical package.

The newer Synexia `com.synexia.mindex.precompute.M3SemanticFingerprint` is intentionally a
different identity plane. No automatic equivalence or silent migration is allowed.

A later caller migration requires explicit translation or dual-read/dual-write evidence.

## Proof

The target must prove old-vs-canonical parity for:

- leaf exact/structural/logic SHA-256;
- structural/logic 64-bit hashes;
- SimHash;
- normalized composition;
- ordered parent composition;
- Hamming distance;
- empty and non-empty feature domains;
- deterministic generated corpus.

The parity suite compares exact outputs. It does not grant semantic-equivalence or promotion
authority.

## Ownership

This compatibility algorithm is reusable and canonical in Synexia.

M3JDK21 remains owner of its product/module wiring, existing persisted-artifact compatibility,
build/runtime validation and final promotion.

The global OpenRewrite recipe-home pin is not reused for this DB handoff; this persisted-format
compatibility surface has its own exact source pin.
