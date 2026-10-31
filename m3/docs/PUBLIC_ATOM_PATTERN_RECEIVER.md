# Public atom/pattern receiver boundary

M3JDK21 is public. Synexia remains the canonical recipe/mastery workspace.

Therefore M3JDK21 has two deliberately separate modes.

## Public verification mode

Default command:

```sh
bash .m3/atomize-patternize/run.sh
```

A fresh public clone must not require access to `hsoliwal/com.synexia` or a private Maven
repository. Public mode verifies the checked-in Apache-2.0 Synexia receiver packet:

```text
m3/synexia-import/synexia-seed-export.tsv
        -> SynexiaImportManifest root verification
        -> SynexiaImporter.verifyTargetSnapshot
        -> exact SHA-256 check of every m3/vendor/synexia target
```

The public verifier does not mutate source and does not claim that the private/current Synexia
mastery campaign was rerun on the checkout.

## Maintainer refresh mode

Maintainers who have the exact reviewed Synexia recipe artifact may run:

```sh
M3_ATOM_PATTERN_MODE=maintainer \
SYNEXIA_RECIPE_VERSION=<reviewed-version> \
bash .m3/atomize-patternize/run.sh
```

That retains the existing content-bound OpenRewrite `dryRunNoFork` path and defaults to:

`com.synexia.m3.EveryModuleAtomPatternMastery`

The lighter inventory-only Synexia composite remains selectable with
`SYNEXIA_ATOM_PATTERN_RECIPE`.

## Ownership

- Synexia owns reusable atomize/patternize/mastery recipes, generated regex/String campaigns,
  static precompute observers, donor catalogues, and Java/JNI mastery machinery.
- M3JDK21 owns public product source, JDK-specific adapters/backports, OpenJDK build/jtreg/runtime
  gates, public Apache receiver snapshots, and promotion.
- Public M3JDK21 must not silently grow a competing reusable recipe implementation.
- New reusable defects discovered in M3JDK21 are fed back to Synexia first, then received through a
  pinned public-safe packet or thin receiver.

## Proof boundary

Public packet verification establishes provenance/integrity of the checked-in receiver snapshot.
It does not establish semantic equivalence of an arbitrary JDK transformation.

Product promotion remains:

```text
candidate
  -> diff
  -> compile / configure
  -> make
  -> jtreg
  -> runtime / benchmark as required
  -> explicit promotion
```
