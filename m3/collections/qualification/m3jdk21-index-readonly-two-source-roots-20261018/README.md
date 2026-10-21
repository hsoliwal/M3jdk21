<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact-source path correction for indexed immutable list receiver

The first corrected M3JDK21 #1260 workflow (run 39100632685)
failed at actual javac 21 with `file should be on source path, or on patch path
for module` for the two explicit test Java files. It had corrected the main
root but omitted `m3/collections/src/test/java`.

This existing canonical TEXT OpenRewrite host is used on the exact YAML file
preimage SHA256 d396321f55c421386a551d191d25881dee5aa28dbf93592f36dcdda68ec717f1
to produce only the corrected sourcepath. Full source/test input lists,
`--release 21`, `-Werror`, JNI and Maven/source-vendored JUnit remain
unchanged. No package-incorrect Java TEXT transform or hand production patch.
The revised receiver still needs actual CI proof.
