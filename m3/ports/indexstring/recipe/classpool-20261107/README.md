<!-- SPDX-License-Identifier: Apache-2.0 -->
# classpool-20261107: the class-metadata pool in M3JDK (census2 P2-099)

`MIndexClassPool` is an append-only canonical arena of Java class metadata. It holds no `Class`, `Method`, `Field` or
collection objects after admission, only:
- primitive columns for kinds, modifiers, super and interface handles and members;
- five 64-bit hashes plus SHA-256 per class and per member;
- a topology snapshot (subtypes, depth, topological order).

In the donor, names live in a MatIndexStringPool of MIndexString tokens. Here they live in the existing
`jdk.m3.intern.M3InternSpace`, whose atom ids are dense and fit the pool's int handle lanes. Name accessors return
`String`. Hashes are unchanged because the donor hashed names as `String.hashCode` over the same units
(RELOCATION_MAP.tsv).

- NEC with `jdk.m3.intern.M3ClassSpace` (#2646): class-file bytes there, class metadata here; nothing is merged.
- Gates: GATES.tsv; the differential is 1,087,504 checks with 0 mismatches.
