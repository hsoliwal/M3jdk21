# M3 atomize/patternize receiver

M3JDK21 is a **public target repository**. Reusable atomize/patternize/mastery machinery is
canonically developed in Synexia; this directory is the public receiver and verification surface.

## Public mode — default

A fresh public clone can verify the checked-in Apache-2.0 receiver packet without access to
Synexia, a private Maven repository, or OpenRewrite:

```sh
bash .m3/atomize-patternize/run.sh
```

This dispatches to `public-verify.sh`, which:

1. compiles the existing JDK-only `m3/synexia-import` verifier with Java 21 warnings-as-errors;
2. parses and recomputes the root of `m3/synexia-import/synexia-seed-export.tsv`;
3. verifies every checked-in `m3/vendor/synexia/**` file against its declared SHA-256;
4. emits `.m3/target/atomize-patternize/public-receiver/receipt.tsv`.

Public verification proves integrity/provenance of the checked-in receiver packet. It does **not**
claim the private/current Synexia mastery campaign was rerun against this checkout.

## Maintainer refresh mode

Maintainers with the exact reviewed Synexia recipe artifact may run the full content-bound mastery
analysis explicitly:

```sh
M3_ATOM_PATTERN_MODE=maintainer \
SYNEXIA_RECIPE_VERSION=<reviewed-version> \
bash .m3/atomize-patternize/run.sh
```

Maintainer mode defaults to:

`com.synexia.m3.EveryModuleAtomPatternMastery`

That composite performs strict FILE -> PACKAGE -> MODULE -> PROJECT -> REPOSITORY atom/pattern
coverage and reuses the deterministic regex/String permutation campaign, compiler/JUnit/Java-JNI
mastery evidence, current mastery fan-in and heavy-campaign binding.

For the lighter inventory-only lane:

```sh
export SYNEXIA_ATOM_PATTERN_RECIPE=com.synexia.m3.EveryModuleAtomPatternApplication
```

## Bootstrap a maintainer analysis snapshot

Maintainer mode is content-bound. When a rollout branch intentionally has no `inventory.tsv`, bind
the current ordinary Java/POM bytes first:

```sh
bash .m3/atomize-patternize/bootstrap.sh
```

Rebinding is explicit:

```sh
SYNEXIA_REBIND_INVENTORY=1 bash .m3/atomize-patternize/bootstrap.sh
```

The bootstrap does not invoke Maven or OpenRewrite. Generated evidence stays under `.m3/target`.

## Authority boundary

Neither mode grants source-copy, replacement, merge, semantic-equivalence or promotion authority.

- Synexia owns reusable recipes, donor catalogues, static precompute observers, regex/String
  permutation mastery and Java/JNI mastery machinery.
- M3JDK21 owns its public product source, JDK-specific adapters/backports, public Apache receiver
  snapshots, OpenJDK build/jtreg/runtime gates, and promotion.
- Repeatable M3JDK21 defects are fed back into Synexia recipes first, then received through pinned
  public-safe outputs or thin target receivers.

OpenJDK product promotion remains a separate `configure -> make -> jtreg -> runtime/benchmark`
decision.
