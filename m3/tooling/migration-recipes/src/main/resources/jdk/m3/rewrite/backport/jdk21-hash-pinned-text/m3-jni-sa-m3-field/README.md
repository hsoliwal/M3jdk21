<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3-jni-sa-m3-field (text lane)

Startup-correctness crate. `src/java.base/share/native/libjava/jni_util.c` still cached
`String_mindex_ID = GetFieldID(String, "mindex", "Ljava/lang/MIndexString;")` inside
`InitializeEncoding`, which the VM runs at start-up; `java.lang.String` has no such field since the
consolidate commit (the canonical value is `private volatile M3String m3`). The lookup therefore
leaves a pending `NoSuchFieldError` on every launch.

Postimage: field id renamed to `String_m3_ID`, lookup `("m3", "Ljava/lang/M3String;")`, identical
control flow (an M3-backed String still takes the explicit charset boundary before any critical
pointer is acquired). The template keeps the OpenJDK GPLv2+CPE header; this crate relicenses nothing.

`00-jni_util.c.txt.before` is the exact master preimage retained for the fixed-point/drift test
(`M3JniSaM3FieldRecipeTest`). Wrapper: `com.m3.rewrite.backport.M3JniSaM3FieldRecipe`
(`com.m3.M3JniSaM3Field`). Companion Java lane: `jdk21-hash-pinned/m3-jni-sa-m3-field`.
