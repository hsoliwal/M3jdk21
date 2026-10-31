# Public A3 recipe artifact boundary

## Purpose

Make the public M3JDK21 control reactor buildable without a private Synexia Maven repository while
preserving Synexia as the canonical reusable-recipe owner.

The existing A3 consumer already pins the canonical export:

- source repository: `hsoliwal/com.synexia`
- source revision: `a062661e91e769a00c2d027e1f04a7a0c8f9dc28`
- source PR: `9599`
- artifact: `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT`
- export manifest Git blob: `8a3e3d6e802e95dbcc7b0bf83a347887b02d6717`

Those canonical source blobs are unchanged on current Synexia.

## Public mirror

The exact Apache-2.0 export source is mirrored under:

`m3/vendor/synexia/synexia-openrewrite-recipes/`

The target-local module:

`m3/tooling/synexia-jdk-a3-recipes`

contains build/proof glue only. Its Maven source directory points at the mirrored Synexia source and
compiles only the eight classes listed by the canonical export manifest.

No reusable recipe implementation is authored or evolved in M3JDK21.

## Verification

The target wrapper must fail closed unless:

1. the checked-in export manifest is byte-identical to the canonical Synexia export manifest;
2. every mirrored source listed by that manifest has the declared Git blob SHA-1;
3. no additional Java source is compiled into the slim artifact;
4. the resulting artifact coordinate remains exactly
   `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT`;
5. Java 21 strict compilation succeeds.

The module is inserted immediately before `tooling/a3` in the M3 Maven reactor so the existing A3
dependency resolves reactor-locally.

## Ownership

Synexia remains the canonical owner of:

- reusable Atomize/Patternize recipes;
- recipe mastery, regex/String permutations and precompute observers;
- donor catalogues and Java/JNI mastery machinery.

M3JDK21 remains the owner of:

- the public mirror and its integrity proof;
- JDK-specific adapters/backports;
- OpenJDK configure/make/jtreg/runtime gates;
- final product promotion.

Any reusable improvement discovered in M3JDK21 is fixed in Synexia first and then received again as
a newly pinned public-safe export.
