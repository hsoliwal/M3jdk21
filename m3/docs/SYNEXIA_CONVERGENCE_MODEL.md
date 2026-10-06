# Synexia donor-convergence / public-target model

Status: M3JDK21 receiving authority.

## Model

`hsoliwal/com.synexia` is the donor-convergence, recipe-mastery and public-code-polish workspace.
M3JDK21 is a receiving public target.

A capability should be invented/adapted/proved once in Synexia, then delivered to M3JDK21 as an
exact qualified atom plus its recipe and evidence. M3JDK21 should not create a second implementation
when an already-qualified Synexia owner/recipe satisfies the target contract.

```text
external donors / problem catalogues / upstream projects
                       |
                       v
              Synexia convergence workspace
 inventory -> provenance -> recipe -> atom/pattern/IOP -> compiler/tests -> fixed point -> polish
                       |
                       v
          source-pinned qualified handoff packet
                       |
                       v
                   M3JDK21
 preimage/scope -> license policy -> build/API/ABI -> runtime/jtreg -> promotion
```

## Apache-2.0 fast lane

First-party or independently authored Synexia source that is qualified as `Apache-2.0` is reusable
by M3JDK21, including Java, JNI/C/C++, OpenRewrite recipes, tests, documentation, manifests and
generated configuration.

The receiving rule is **preserve the license, do not relabel it**:

- Apache-2.0 Synexia tooling/recipes/ports remain Apache-2.0.
- Apache-2.0 Synexia components carried in an OpenJDK module retain their Apache-2.0 copyright,
  modification and legal/NOTICE record. `src/java.base/share/legal/synexia.md` is the current
  product legal aggregation point for admitted `java.base` components.
- Existing OpenJDK-derived source retains its existing OpenJDK license/header. Inclusion of Synexia
  code does not turn OpenJDK source into Apache-2.0.
- Source-license acceptance never substitutes for JDK compile, jtreg, native, GC/JIT/CDS/JNI or
  platform acceptance.

## Third-party donor boundary

Synexia is allowed to study and qualify donors under many licenses, but that does not make donor
source Apache-2.0.

The default Synexia -> M3JDK21 public-target packet is restricted to
`SYNEXIA_FIRST_PARTY_APACHE2_V1`. Copied/modified third-party bodies, source-available material,
GPL-only material, Commons-Clause material or any non-Apache payload require a separate
artifact-specific license lane and receiver decision. Reference-only donor ideas/problem statements
carry no source-copy authority.

## Transfer unit

Every target-ready transfer binds:

- exact Synexia Git revision;
- source path and SHA-256;
- `Apache-2.0` source-license assertion on the default lane;
- producing Maven/OpenRewrite recipe ID;
- contract/equivalence evidence root;
- compiler/test/convergence evidence root;
- exact destination path and destination preimage;
- target-side acceptance receipt.

Recipes are transferable assets, not internal implementation detail. Recurring fixes should improve
the Synexia recipe/mastery corpus first, then export new exact outputs to every public target.

## Machine policy

`m3/compatibility/synexia-public-target-policy.tsv` is the target-side license/disposition table.
`m3/compatibility/check_synexia_public_target.py` validates it and can validate an exported
`SYNEXIA_M3_HANDOFF_V1` packet before receiver application.

Missing/unknown license classification fails closed. This policy complements, and does not replace,
the existing source hash, target preimage, scope, compiler/test and runtime gates.


## Canonical-home invariant

Reusable code flows one way with respect to ownership:

```text
donors / target discoveries
        |
        v
hsoliwal/com.synexia
canonical Apache-2.0 recipe + module owner
        |
        | exact revision + SHA-256 + license + proof roots
        v
hsoliwal/M3jdk21
receiver / adapter / JDK-specific backport / product verification
```

M3JDK21 does not become a second canonical owner merely because a Synexia recipe or M3Index
component is materialized into `m3/**` or `.m3/**`. Reusable recipe changes, semantic-hash
mechanics, atom/pattern/IOP machinery and M3Index DB/JDK-bridge improvements belong in Synexia.
The target repository retains only product compatibility, OpenJDK licensing, build/jtreg/runtime
and final promotion authority.


## Canonical reusable recipe ownership invariant

Reusable Maven/OpenRewrite mechanics are canonically owned and mastered in `hsoliwal/com.synexia`.
M3JDK21 is a consumer, not an independent owner, for reusable atomization, patternization/IOP,
documentation, scope, convergence, donor-review and mastery recipes.

M3JDK21 may retain historical `com.m3.rewrite.*` implementations temporarily as compatibility
mirrors while callers migrate, but those files are frozen: semantic changes must be made first in
Synexia and then re-imported as an exact source-pinned Apache-2.0 mirror.

M3JDK21 remains canonical only for target-specific recipes whose semantics depend on the exact
OpenJDK tree, JDK API/ABI, native/runtime boundaries, jtreg, or a source-sealed backport preimage.

The initial moved family is the pure-int FILE convergence stack. Its canonical namespace is
`com.synexia.rewrite.atom`; the exact Synexia source revision and Git blob identities are bound in
`m3/compatibility/synexia-recipe-ownership.tsv`.

A borrowed canonical recipe may execute in M3JDK21, but target promotion still requires M3JDK21's
scope, license, compile, API/ABI, runtime/jtreg and fixed-point gates. Borrowing never transfers
promotion authority back to Synexia.


## Nebula proving-ground transfer

The reusable FILE convergence family was independently proved and operationally exercised first in
`hsoliwal/nebula`, then transferred into Synexia canonical ownership and finally consumed by
M3JDK21.

The custody chain is machine-readable at:

`m3/compatibility/nebula-proven-recipe-transfer.tsv`

The required semantic path is:

```text
Nebula proving ground
  inventory
  -> atomization
  -> patternization / IOP
  -> documentation
  -> fixed point
  -> 99% semantic recipe proof
  -> Camel / Airflow / Drools projections
        |
        v
Synexia canonical recipe home
        |
        | exact source-pinned Apache-2.0 recipe family
        v
M3JDK21 consumer
        |
        v
JDK-specific compatibility/backport DAG
  -> compile
  -> jtreg/runtime
  -> serial promotion
```

The transfer does not grant source-mutation or promotion authority to Nebula, Synexia, Camel,
Airflow, or Drools. M3JDK21 remains the OpenJDK product/backport verification authority.

Nebula PR #80 is intentionally recorded as
`HOSTED_ACTIONS_STARTUP_BLOCKED_NO_JOBS`: its two relevant hosted M3 workflow runs created zero
jobs. Therefore the custody/proving architecture is accepted from merged repository evidence, but
no hosted green-build claim is inferred from that run.
