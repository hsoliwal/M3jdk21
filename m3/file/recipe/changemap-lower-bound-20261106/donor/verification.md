<!-- SPDX-License-Identifier: Apache-2.0 -->
# Donor receipt: verification.txt of crate m3-file-changemap-lower-bound-20261029

Verbatim copy of `synexia-openrewrite-recipes/crates/m3-file-changemap-lower-bound-20261029/verification.txt` at hsoliwal/com.synexia 4a2e6452a7a (blob eacf9fdf58211da0dd01d5f18f893cb42ef9322c). Kept as Markdown so the JDK naming recipe leaves the donor names untouched.

```
# M3_FILE_CHANGEMAP_LOWER_BOUND_SOURCE_TEST_RECEIPT_V1
# Single Linux container, 2026-10-29, OpenJDK 21.0.12.1, Java21 release mode
status=EXECUTED_LOCAL_JAVA21_COMPILE_AND_SMOKE_PASS
remote_proof_git_blob=6c38591926d215e0ac3b62c360c8ded5e768a1df
local_exact_proof_git_blob=6c38591926d215e0ac3b62c360c8ded5e768a1df
compiler_command=javac --release 21 -proc:none -Xlint:all -Werror -d classes M3FileChangeMapBinarySearchProof.java
runtime_command=java -cp classes com.synexia.m3.verification.M3FileChangeMapBinarySearchProof
runtime_output=PASS M3_FILE_CHANGEMAP_LOWER_BOUND_V1 maps=50001 checks=9341740
donor_M3JDK21_commit=0ab28ea86ccb2b7c1997e662d1555ece229c05de
donor_M3JDK21_git_blob=87dd9d8293de46d8843c09b2109063b11e682b54
donor_source_sha256=7612ebe067c2602e4c3efcf6df2824e173ca4d858e85f5395abd0e5e60b890b9
candidate_source_sha256=6329ee71cc7921d6480d83b80a9e5d528d834adbb37f9a2e24349ad24e11f52d
maven_verify=NOT_RUN_MAVEN_UNAVAILABLE
openrewrite_sdk_junit=NOT_RUN_DEPENDENCIES_UNAVAILABLE
actual_product_source_compilation=NOT_RUN
m3jdk21_runtime_jtreg=NOT_RUN
jni_build_and_parity=NOT_APPLICABLE_SCALAR_JAVA_CANDIDATE
performance_benchmark=NOT_RUN
product_promotion=HOLD
```
