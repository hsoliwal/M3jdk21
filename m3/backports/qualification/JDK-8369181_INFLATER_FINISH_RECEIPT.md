# JDK-8369181 — source-locked Java21 candidate

Target source: `src/java.base/share/classes/java/util/zip/InflaterOutputStream.java`
Original M3JDK21 master: `315cd18d0092f70c212223bb6af1f4366591ed1b`
Original Git blob: `7b08d5b45385caaecec8d14ceee6c3eb3ec3a4f9`
Canonical recipe: [Synexia #14996](https://github.com/hsoliwal/com.synexia/pull/14996),
commit `aa71f3d668f569e689dbd5dc67bb94b4f18ead10`.
Donor: openjdk/jdk@e58e576f80cd35728bac6738474c4090737c16a8, GPLv2+Classpath Exception.

The candidate inserts only the internal default-owned Inflater closed state, records
it after `finish()`, and throws `IOException("Inflater closed")` before
attempting further writes. All public methods, signatures, and JNI symbols stay
unchanged. Caller-owned Inflaters retain their original lifecycle.

Executed on reference OpenJDK21 with exact source patched via `--patch-module`:
strict Java21 lint/compile PASS; stock baseline RED; corrected normal, -Xint,
-Xcomp 27 checks each PASS, including decompressed output and stream ownership.
This is not source-built M3JDK21 proof. Canonical Maven/OpenRewrite SDK,
source-built target jtreg, product JIT, API/ABI, JNI and cross-platform gates
remain NOT_RUN. HOLD: do not merge until qualification evidence is attached.
