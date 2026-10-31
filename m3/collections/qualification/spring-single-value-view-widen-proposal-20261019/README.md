<!-- SPDX-License-Identifier: Apache-2.0 -->
# Proposal: widen `M3SingleValueMapView` to any `Map<K, List<V>>`, 2026-10-19

This is a proposal for the owner of the Spring LinkedMultiValueMap row (`implementations/spring`) to review.

## Change

`M3SingleValueMapView` only reads the list-valued `Map` protocol of its target: size, get, put, remove, the
views, forEach, hash and text. Its field and constructor were typed `M3MultiValueMap<K, V>`; they are now
`Map<K, List<V>>`.

- The only caller, `M3MultiValueMap.asSingleValueMap()`, compiles unchanged.
- Source compatibility holds. Binary compatibility does not, for code compiled against the old constructor
  descriptor; nothing in this repository is.
- Serialization: `serialVersionUID` is unchanged. Object streams assign object fields by name and check the
  type at runtime, so an old stream whose target is an `M3MultiValueMap` still deserializes.

The receiving recipe is `recipe/spring-single-value-view-widen-20261019`. It moves the master preimage
`6f33bb30...` (the postimage of `owned-spring-multivalue-map-20261017`) to the widened postimage `f79e3637...`
(2 of 2 tests pass).

## Evidence

- `M3MultiValueMapSelfTest` passes, with output identical before and after: 142,077 checks against Spring 6.2.0.
- `SingleValueViewWideningDiscriminator.java` runs the widened view over four target kinds, 8 seeds x 2,000
  operations each, against Spring 6.2's own first-value view over the provider counterpart. The four target
  kinds are an `M3MultiValueMap`, an `M3MultiValueMapAdapter`, a Spring `LinkedMultiValueMap` and a plain
  `LinkedHashMap<K, List<V>>`.

`widening-results.tsv` gives the per-operation counts:

| target | operations | protocol equal | equal including list class |
|---|---|---|---|
| `M3MultiValueMap` | 16,000 | 16,000 | 13,838 |
| `M3MultiValueMapAdapter` | 16,000 | 16,000 | 13,838 |
| `LinkedMultiValueMap` | 16,000 | 16,000 | 13,838 |
| `LinkedHashMap<K, List<V>>` | 16,000 | 16,000 | 13,838 |

Every protocol outcome and every target text is equal. The only difference is the class of the list that
`put`/`putAll` install:
- The owned view installs its private `SingleElementList`, a deliberate delta of the master owner.
- Spring installs `Collections.singletonList`.

## Why the adapter keeps its private view

`M3MultiValueMapAdapter` (#1469) is qualified with the installed list class as part of its exact
differential comparison. Switching it to this view would trade that exactness for one shared body.

After the widening, the choice is one constructor argument. Possible follow-ups (owner's choice):
- Let `M3SingleValueMapView` take a list factory, `SingleElementList` by default. The adapter would then pass
  `Collections::singletonList` and delete its private view (about 150 lines).
- Keep both, as now.

## Reproduce

Compile `implementations/spring/src/main/java/com/m3/collections/spring/*.java`, the adapter
(`implementations/spring-multi-value-map-adapter/.../M3MultiValueMapAdapter.java`) and this discriminator
against the core m3-collections classes and `spring-core-6.2.0.jar`. Then run `SingleValueViewWideningDiscriminator`.
Output goes through `java.util.logging`; the log has SHA-256 `c9aad769...`.

This was run locally on Windows with JDK 21.0.9 only; hosted CI was not run.
