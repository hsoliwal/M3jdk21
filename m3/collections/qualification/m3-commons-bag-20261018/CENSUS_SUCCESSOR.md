<!-- SPDX-License-Identifier: Apache-2.0 -->
# 36-owner current M3 five-provider census source-sealed successor
- Preserve 34 previous source records and immutable original historical five-file receipt.
- Add precisely `M3ApacheBag.java` / `M3ApacheBagContractTest.java`, both SHA256 and Git blob pinned.
- Source base before/after: `master@4b24824c3aa09d802121447f8970d442a855f064`; no other approved owner mutated.
- Regenerate all 4 census text owners from this named OpenRewrite recipe; do not hand-patch `verify.py` or JUnit outputs.
- Strict tracked-file cardinality rises 34→36; installed source names and hashes remain fail-closed.
- Real Maven/OpenRewrite JUnit/CI must prove fixed-point, refused missing/drift and complete source check.
- Original Java Apache Bag/HashBag license Apache-2.0; first-party generated work Apache-2.0.
