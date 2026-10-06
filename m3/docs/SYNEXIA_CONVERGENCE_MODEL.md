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
`m3/compatibility/check-synexia-public-target.py` validates it and can validate an exported
`SYNEXIA_M3_HANDOFF_V1` packet before receiver application.

Missing/unknown license classification fails closed. This policy complements, and does not replace,
the existing source hash, target preimage, scope, compiler/test and runtime gates.
