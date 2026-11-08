# tests.sed: the donor test (PROVENANCE.tsv row PrimitiveRadixTest, blob a582f393d450) becomes
# m3/algorithms/src/test/java/jdk/m3/algorithm/RadixSortLanesTest.java: package, test class name, the module's cancelable
# monitor helper, one RELOCATE anchor; the member references (IntRadix.*, LongRadix.*, IProgressMonitor.noop()) are rewritten
# by the REFS rows of FOLD_MAP.tsv in the same run.
s/^package com\.synexia\.algorithms\.collections;/package jdk.m3.algorithm;/
/^import com\.synexia\.job\.DefaultProgressMonitor;$/d
/^import com\.synexia\.job\.IProgressMonitor;$/d
s/\bPrimitiveRadixTest\b/RadixSortLanesTest/g
s/\bDefaultProgressMonitor\b/CancelableProgressMonitor/g
s|^final class RadixSortLanesTest {|//FLLDCIM: RELOCATE - donor blob a582f393d450 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv); recipe m3/algorithms/recipe/radix-lanes-fold (tests.sed, FOLD_MAP.tsv REFS)\nfinal class RadixSortLanesTest {|
