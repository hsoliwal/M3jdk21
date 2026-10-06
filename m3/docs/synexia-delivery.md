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

    vendor snapshot
      -> inventory
      -> select exact atom/recipe
      -> create or improve M3JDK21 OpenRewrite port recipe
      -> JUnit / compiler / runtime proof
      -> target-specific materialization
      -> fixed point

The vendor tree itself is not a shortcut around M3 scope, contract or verification gates.

## License boundary

Synexia's imported Apache license and NOTICE are retained at:

    m3/vendor/synexia/LICENSE
    m3/vendor/synexia/NOTICE

OpenJDK source outside the independent Apache-owned m3/ subtree retains its existing license and
notice regime. Moving or adapting imported Apache code into OpenJDK-owned files requires a
separate compatibility/provenance review; this automatic import contract does not perform that
step.
