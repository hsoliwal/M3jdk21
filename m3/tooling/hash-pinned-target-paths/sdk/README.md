# Canonical targetPaths API compatibility recipe

SPDX-License-Identifier: Apache-2.0

Pre-code admission7948eaa7c2e739aaab4906cfc68c61236811b281 selects only the existing canonical targetPaths accessor. Two actual broader CI jobs failed at migration-recipes test compilation because the unchanged object-slot snapshot test invokes this missing receiving API. Current defaultd6d76b5 still has providerd58ca50d without the accessor.

This reversible source-sealed Maven/OpenRewrite proposal inserts only the canonical accessor and preserves all other receiving-provider bytes, including its PlainText/source-custody mode. Six exact KEEP inputs retain the two-method owning test and its manifest/four payloads. It does not replace the complete31KB canonical provider or import unrelated scope/parser changes.

Recipe names: com.synexia.m3.HashPinnedTargetPathsCompat and com.synexia.m3.HashPinnedTargetPathsCompat.Inverse. Three named recipe tests cover both before/after states, both orders, fixed points, full inverse recovery, all seven owners' drift/missing/duplicate refusals and the exact leaf-only delta with unchanged owning assertions. Expected actual evidence:3methods/16successfulreplays/42refusals/2authenticprovideroutputs. SDK31files/30nonselfpins. Existing providers and POM are unchanged.

Actual Maven is NOT_RUN and no product provider has been applied. After qualifying this recipe and the actual emitted provider, compile the whole original receiving migration-recipes test source set in its authentic source-built Maven model, execute the unchanged object-slot Java-LST tests, and rerun both affected consumers. Source-only replay does not qualify the generated Java provider, the private parser/model, runtime semantics or the complete JDK.
