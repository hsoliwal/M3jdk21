<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent 13-target metadata follow-up review

**Source review passes: the new metadata correction matches the existing Guava factory, and all 11 Java test postimages are byte-identical to the prior review.** Runtime requalification remains pending in this review; no Maven or shared-output writes were performed.

The complete candidate now contains 13 files: the previously reviewed 11 Java tests, the overview, and the newly included binding TSV. Exact before/after SHA-256 values are recorded in `CANDIDATE_REVIEW_13_METADATA_20261020.json`. The earlier 12-file review and its receipt were read and hash-checked without modification.

## Exact delta from the prior review

- `sdk-family-bindings.tsv` changes exactly one cell: the `m3_owners` field of `GUAVA / guavaListMultimap` changes from `M3LinkedHashMap;M3LinkedList` to `M3LinkedHashMap;M3ArrayList`, retaining the full class names. All 25 rows, their order, remaining fields, factory signatures, adapter names, versions, and family counts are unchanged.
- `SDK_FAMILIES.md` changes only the same Guava value-owner name relative to its previously sealed postimage. Reversing this one exact row substitution reproduces the previous overview SHA-256.
- All 11 Java test preimages and postimages exactly match the previous review seals. No production Java or POM target was added.

## Source and packaging witness

The factory and loader bytes match the source witness, the isolated module, the current target files, and Git source parent `2779aba3f98f31c324be43f731be7914e1a05935`. The factory returns `Multimaps.newListMultimap(new M3LinkedHashMap<>(), M3ArrayList::new)`. The unchanged loader reads `/META-INF/m3/sdk-family-bindings.tsv`; the existing POM already packages the top-level TSV at that resource path. The corrected metadata therefore reaches the existing searchable inventory without introducing a Java implementation or another metadata loader.

The count remains JDK 6, Guava 7, Eclipse Collections 5, Spring 4, Apache Commons Collections 3. The previously reported Guava owner metadata discrepancy is resolved in this candidate. This source review does not claim broader storage-contract qualification from an inventory count.

The parent is rerunning the owning Java/native suite and standalone binding main for this expanded closure. Their actual results, canonical recipe qualification, and target receiving evidence determine admission.

Candidate-set SHA-256 (sorted postimage SHA-256, two spaces, relative path, newline): `05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb`.
