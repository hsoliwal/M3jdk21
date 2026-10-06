# Exact-ref FILE crate generation

## Purpose

Released-GA tree comparison is useful for the complete backport denominator, but an individual
JEP/JBS packet must often absorb one exact implementation commit plus a small dependency/fix closure.

Generating FILE atoms from the full GA tree can accidentally pull unrelated changes into a packet.

The existing lower-level `file_delta_inventory.compare_refs(...)` already supports exact Git refs.
This pass exposes that capability through `generate_recipe_crates.py`.

## CLI

Historical behavior remains unchanged:

```text
generate_recipe_crates.py --release 22 ...
```

uses:

```text
baseline = jdk-21+35
donor    = jdk-22+36
```

An exact packet may instead supply both:

```text
--baseline-ref <exact-ref>
--donor-ref <exact-ref>
```

The arguments are atomic: supplying only one is rejected.

## Evidence

`CRATES.tsv` records the exact baseline and donor refs actually used. This prevents a generated
crate from claiming commit-pinned provenance while its receipt still names a GA tag.

A deterministic regression fixture creates:

```text
baseline -> feature -> later unrelated change
```

and proves that generation pinned to `feature` contains the feature bytes, not the later bytes.

## JEP 423 use

JEP 423 requires a three-commit closure (initial region pinning, pin-count widening, region pin
cache). Exact-ref generation allows each upstream delta/closure checkpoint to be inventoried and
materialized as FILE atoms without treating all of JDK 22 GA as the donor.

Native/JNI files continue to use the existing source-sealed native/text lane; Java files continue to
use the OpenRewrite Java lane. This change grants no compatibility or promotion authority.

## Recipe-first ownership

The change to the generator and its test is itself replayed by:

`M3Jdk21HashPinnedTextSnapshotRecipe("exact-ref-recipe-crates")`

because Python tooling is OpenRewrite PlainText, not a Java LST. The recipe pins the exact current
preimages and must reach second-pass fixed point.
