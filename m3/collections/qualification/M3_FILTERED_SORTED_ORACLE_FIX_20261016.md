<!-- SPDX-License-Identifier: Apache-2.0 -->
# Corrected filtered/sorted test oracle source-sealed receiving

First hosted focused Java21 workflow `38924935838` verified the four
postimage digests and compiled the existing M3 production module but
rejected a new test fixture's assumption about `List.remove(value)`
with duplicate values.

Corrected canonical [Synexia #11911](https://github.com/hsoliwal/com.synexia/pull/11911)
postimage `7a0fe770960515d6e134329c640eb4c085df0f25`
removes the known freshly appended element by **position**. The original
array-backed List behavior (remove first matching occurrence by value)
is preserved. This commit receives that exact corrected fixture and
the updated sealed SHA256 receipt. No production Java, JNI or public
method change. Retain the original failed CI receipt and require a
fresh passing Java/JNI invocation before promoting this draft.
