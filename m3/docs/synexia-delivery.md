# Synexia convergence -> M3JDK21 delivery

## Role split

Synexia (hsoliwal/com.synexia) is the donor-convergence workspace.

M3JDK21 is a delivery/product target.

M3JDK21 may reuse, modify and redistribute Synexia material that is actually admitted under
Apache License 2.0, subject to the Apache license/NOTICE conditions. This delivery lane does not
relicense OpenJDK and does not automatically admit third-party donor source merely because it is
present somewhere in the Synexia repository.

## Automatic COPY lane

Automatic imports must satisfy all of the following:

1. exact Synexia source revision;
2. exact source path;
3. exact target path below m3/vendor/synexia/;
4. exact SHA-256;
5. license identity exactly Apache-2.0;
6. import mode APACHE_SOURCE or APACHE_RECIPE_RESOURCE;
7. independent M3JDK21 manifest-root verification;
8. exact target snapshot verification after materialization.

Non-Apache or separately conditioned material remains evidence-only unless a separate licensing
decision explicitly admits it.

Examples that are not automatically copied by this lane include Commons-Clause source,
GPL/OpenJDK-derived source, CC-BY/CC-BY-SA datasets, share-alike corpora and unresolved-license
donor material.

## Seed snapshot

The initial checked-in snapshot is pinned to Synexia:

    62aea466cf2f2bb4668f38aad9343d59525965b5

Manifest:

    m3/synexia-import/synexia-seed-export.tsv

Manifest root:

    ca570e874586c327f98e3982bf9e62a404e87e266b3195df69be06a0b5111ef6

The seed includes the machine-readable M3 invariant ledger, scope/multipass recipe atoms,
hash-pinned/verbatim/OpenRewrite recipe source, atom-hash-database recipe source, Java/JNI
algorithm-kernel source and parity tests, plus the Synexia LICENSE and NOTICE.

These snapshots are intentionally outside the normal Maven source roots. They are verified donor
inputs, not automatically compiled product code.

## M3Index family receiver

The full M3Index family uses the same automatic Apache custody lane; it does not use a second
database, recipe repository, or migration framework.

Receiver classification is checked in at:

    m3/synexia-import/m3index-family-receiver.tsv

Admitted family prefixes include the M3Index alias reactor, IndexString, MIndex compiler/AST,
data-structure, precompute-api, DB, OpenRewrite recipes, convergence kernels and M3 recipe owners.
Each automatic target path must be exactly:

    m3/vendor/synexia/<original Synexia source path>

This mirror rule makes the vendored snapshot evidence/custody only. Java classes under the vendor
tree are not automatically compiled into the JDK and do not supersede the canonical Synexia
implementation.

The existing 15-row seed snapshot remains a historical bounded seed. Expanding the full family
requires a new exact Synexia revision and generated manifest; do not mutate the old seed/root to
pretend a later import already happened.

## Receiver

The Java-21 receiver lives at m3/synexia-import.

Verify the checked-in target snapshot through JUnit:

    mvn -B -ntp -f m3/synexia-import/pom.xml verify

With a local Synexia checkout and the manifest, the receiver supports:

    SynexiaImportCli verify <manifest> <synexia-root> <m3jdk-root>
    SynexiaImportCli materialize <manifest> <synexia-root> <m3jdk-root>
    SynexiaImportCli verify-target <manifest> <synexia-root> <m3jdk-root>

Materialization refuses source drift and refuses overwriting a different existing target.

## Recipe-first use

com.m3.rewrite.M3SynexiaImportInventory exposes the pinned delivery rows through OpenRewrite
DataTables.

The intended next step for any imported capability is:

    vendor snapshot / pinned Synexia handoff
      -> inventory
      -> select exact canonical Synexia atom/recipe
      -> improve the reusable recipe in Synexia first
      -> receive only a thin M3JDK21 adapter or JDK-specific backport/receiver
      -> JUnit / compiler / jtreg / runtime proof
      -> target-specific materialization
      -> fixed point

The vendor tree itself is not a shortcut around M3 scope, contract or verification gates.

### Canonical recipe ownership guard

M3JDK21 is not a second reusable recipe home.

The append-only receiver intake at
`m3/synexia-import/intakes/recipe-ownership-20261007/` pins the Synexia ownership/borrowing
ledgers to an exact Synexia revision. The Java receiver policy
`SynexiaRecipeOwnershipPolicy` audits the current
`m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/**` tree.

Every local recipe source must resolve to one of:

- `SYNEXIA_CANONICAL` — compatibility/proof residue only; reusable evolution is forbidden locally;
- `TARGET_ADAPTER_ONLY` — thin target intake/compatibility surface; reusable evolution is forbidden locally;
- `JDK_TARGET_SPECIFIC` — OpenJDK/JEP/backport/receiver logic whose product contract is target-local.

An unclassified local reusable recipe fails closed. A Synexia-owned residue cannot become
independently evolvable merely because it is still present in M3JDK21. New generic
atomization/patternization/IOP, semantic hash, M3Index, algorithm/search, donor-convergence,
recipe-mastery, or Java/JNI mechanics must be implemented and proved in Synexia first.

M3JDK21 remains authoritative for exact OpenJDK preimages/postimages, JEP/backport application,
HotSpot/JIT/GC/CDS/JVMTI/JNI integration, jtreg/runtime qualification and final product promotion.


## License boundary

Synexia's imported Apache license and NOTICE are retained at:

    m3/vendor/synexia/LICENSE
    m3/vendor/synexia/NOTICE

OpenJDK source outside the independent Apache-owned m3/ subtree retains its existing license and
notice regime. Moving or adapting imported Apache code into OpenJDK-owned files requires a
separate compatibility/provenance review; this automatic import contract does not perform that
step.
