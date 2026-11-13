# Review-before-search admission correction

The original private PmemGramCampaignAdmission previously invoked its trusted
PmemCatalogueWorkOrders review gate before validating a malformed search hit
budget, overlength pattern or an index associated with a different source root.
These invalid requests now refuse before any trusted callback, native search,
ledger claim or model call. The original successful path and all existing public
Java/JNI ABI, KMP, exact 2-gram filters, source hashes, search result ordering
and recipe/source-writing authority remain unchanged.

The existing M3HashPinnedJavaSnapshotRecipe owns PRE->POST exact Java/JUnit
source transformations. Compare both frozen inputs, apply the typed recipe,
verify the resulting exact SHA, fixed point, drift/absent refusal, and then run
actual Maven JUnit/FILE/POSIX JNI before marking READY.

Local Java 21 strict compiler and original 20k synthetic fixture passed (846
assertions, identical JIT and interpreted results); four selected JUnit-shaped
methods passed with test-only API stubs including the new no-review-before-
search-admission regression. Real native Maven/OpenRewrite/JUnit, full physical
20k, Chrome inference and repository-wide provenance are NOT_RUN / HOLD.
