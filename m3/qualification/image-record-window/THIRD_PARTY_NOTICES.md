Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

Production reuses existing SharedLexiconImage and qualified R237 constructor-local SHA256/scratch copy. Original Hitesh Soliwal/Synexia and Guava Authors comparison notices remain;actual M3 Apache-2.0 licence is included in donor/M3-APACHE-LICENSE. All non-constructor comparison/lookup/copy methods remain bytecode-identical.

Reviewed OpenJDK jdk-21+35 commit890adb6410dab4606a4f26a942aed02fb2f55387 Buffer/ByteBuffer-template/MessageDigest/MessageDigestSpi/JCAUtil source identities are provenance only. Production invokes standard asReadOnlyBuffer/rewind/limit/position/update APIs. No OpenJDK GPL implementation is copied. Clear and rewind variants invoke platform APIs;their source and measurements are distinguished in prior/ and ALGORITHM_CATALOGUE.json.

Reference is the exact pinned original M3 class with self-type identifier renamed inside its original package;the pinned incumbent test source is unchanged. Only image/seal/provider helpers execute,not its filesystem main. Qualification provider wrappers use installed JCA/SUN implementations;no digest implementation/JAR is bundled. Recipe/Maven/RAM compiler sources retain existing notices.

No implementation from LeetCode/HackerRank/GeeksforGeeks is copied or assigned a guessed licence. Category discovery reuses existing M3/platform mechanisms. Existing R237 resources and the unadmitted direct-table ownership counterexample remain preserved.
