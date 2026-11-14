Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

Existing M3/Synexia source, test and recipe headers are preserved in the exact fixtures/templates.

ICU4J UTF16.StringComparator.compareCaseSensitive was reviewed at unicode-org/icu commit049e0d6a420629ac7db77256987d083a563287b5, path icu4j/main/core/src/main/java/com/ibm/icu/text/UTF16.java, blob6326a83b56b0e21c345c8ec03cd384ca0d6d03f2. Its code-point-order approach informed candidate evaluation. The method body is not copied: the replacement composes JDK String.codePointAt/Character.charCount with the original encoder's '?' replacement contract. ICU direct malformed-surrogate ordering is not substituted. The exact upstream license notice is retained in LICENSES/ICU-LICENSE.txt (Unicode License V3 and its included notices).

The actual JDK UTF-8 encoder is the differential oracle. Existing offline Maven/OpenRewrite/JUnit/Jackson dependencies retain their own licenses; no binaries are vendored here. No dataset payloads or interview-solution text are copied.
