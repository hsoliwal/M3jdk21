<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia JAXP delegation recipe — authoring tool mirror

This is a byte-preserving, tool-only mirror of the reusable Synexia source recipe and its regression
driver. It is not production runtime code and adds no dependency from Java/HotSpot/JNI to Synexia.

## Provenance

- Donor repository: `hsoliwal/com.synexia`
- Donor revision: `08da2f86a8e758e0c1a2b48f282d41aadd599afa`
- Donor recipe: `synexia-openrewrite-recipes/verification/ph-isorelax-jaxp-delegation-fix-20261012/JaxpDelegationRecipe.java`
- Donor recipe Git blob: `578e4a6e5ba364c602a0b837ae11649a253129a4`
- Donor regression driver: `synexia-openrewrite-recipes/verification/ph-isorelax-jaxp-delegation-fix-20261012/JaxpDelegationRecipeTest.java`
- Donor test Git blob: `e0855d1a3b2958aee4cdd0f0df64a0a39a0617d0`
- License: Apache-2.0, subject to the exact source headers and Synexia's per-file provenance rules.
- Target mode: `EXTERNAL_RECIPE_TOOL`; no runtime dependency and no direct copy into OpenJDK-owned `src/**`.

The recipe is pinned to the exact upstream `ValidatingDocumentBuilderFactory.java` Git blob
`6d251eda4e40ae78be8134d61f4606cf932a6b84`. It applies only to a separate staged source copy,
adds the standard plural JAXP getter/setter delegation overrides, preserves the singular aliases,
and refuses source drift. Its fixed-point check inverse-reconstructs the exact preimage before
accepting a second pass.

## Qualification boundary

The donor's previous behavior run had 46/47 checks passing. This mirror is source custody and
authoring-tool reuse only. The target must run the recipe, compile the generated candidate, and
execute the JAXP entity-reference differential before any receiver promotion. Maven, OSGi,
Java 8 runtime, Windows, full reactor, JNI and M3JDK21 receiver acceptance remain separate gates.
