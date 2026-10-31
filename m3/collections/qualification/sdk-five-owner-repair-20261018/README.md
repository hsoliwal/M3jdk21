# Five-library M3 SDK direct owner receiving — source and ABI custody

Canonical reusable Synexia [PR #12216](https://github.com/hsoliwal/com.synexia/pull/12216); product M3JDK21 parent `master@4b24824c3aa09d802121447f8970d442a855f064`.

**FIX:** duplicate Java declarations of readOnlyFilteredSortedSet/readOnlyFilteredSortedMap in M3CollectionSdk.java. The product source was syntactically unbuildable before this correction. Preserve the *first* original method bodies, return types and overloads; delete their byte-identical second declarations. Add explicit M3 factories only:
- `m3ArrayList()`: M3ArrayList over one growable Object[].
- `indexedTable()`: M3IndexedTable with value Object[] + row/column int[] links and byte states.
- `indexedMultiValueMap()`: M3IndexedMultiValueMap with Object[] occurrences, int[] next offsets, byte[] state and one M3 key-group directory.
- `longRangeSet()`: M3LongRangeSet sorted long[] starts/ends and mask/JNI reuse.

External names `java.util.*`, `com.google.common.collect.*`, `org.eclipse.collections.*`, `org.springframework.util.*`, `org.apache.commons.collections4.*` remain authoritative, distinct. These four new factories return *M3 substrate types*, not external vendor interface clones. Existing five-provider interop adapters remain unchanged.

**SOURCE FIRST:** exact copied canonical `M3GpuToolJoinRecipe` + `M3GpuToolJoinManifest` in the product qualification-only crate, unchanged; the single custom recipe only redirects the resource root. OpenRewrite PlainText is used for the malformed original Java preimage; emitted source must pass actual JDK21 compilation. Three manifest entries (one exact MODIFY, two ABSENT tests), strict SHA-256 and Git-blob verification, JUnit replay/fixed point/refusal, and a full core Java21/differential/JNI gate.

`verify.py` is only byte/owner custody, **NOT evidence of Maven/JVM runtime correctness**. Hosted CI must execute `mvn -B -ntp -f m3/collections/qualification/sdk-five-owner-repair-20261018/recipe/pom.xml test` then compile the actual module, run new seeded and existing JDK/JNI oracles, and check ABI descriptions. Current local container has Java21 but no Maven/plugin cache or full live master checkout; no fabricated test results.

**Other currently known prerequisite:** the optional five-provider `M3FiveProviderFlagSelection` still refers to missing `M3ObjectCounts`, whereas the current Guava/Apache/Eclipse bag implementations use `M3CountedBag`/distinct stores. That mismatched live-share API is outside this bounded SDK factory atom. Its proper repair must reuse/adapt the admitted count owner, not restore a duplicate engine without provider proof.

Retain first-party Apache-2.0 attribution and donor/category reviews from Synexia. LeetCode, HackerRank and GeeksforGeeks are taxonomy references, no copied submissions. Full implementation of every five-provider class, and String→arrays→collections receiving promotion, remain HOLD pending independent evidence.
