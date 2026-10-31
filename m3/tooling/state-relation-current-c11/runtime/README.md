<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current StateRelation C11/JNI qualification inputs

Status: SOURCE_AUTHORED_NOT_EXECUTED. The driver, classpath route and current-C11 proof have not run.

This runtime adapter reuses the complete existing M3RelationPortProof, without modifying its cases or implementing another numeric algorithm. It reads exactly five existing files under an explicit M3_SOURCE_ROOT. INPUTS.json pins each original path, Git blob, SHA-256 and byte count: 28,948 bytes total. The files remain in their original receiving repository locations. No java.lang/String patch, declaration stub, copied owner or automatic native dispatch is introduced.

The pre-code foundation is private hsoliwal/com.synexia commit baa99d4a2ab1dd71fe05706bae4e7444ade70e30, task ADMISSION.json blob 36a7443169195a3075b16d23e3c1373b192fd582. The receiving baseline is public hsoliwal/M3jdk21 commit adc10e96af417655a7f1fc61b07c0ca8789c83da. Source binding and the Maven KEEP recipe SDK are independent mandatory hosted prerequisites, owned by the outer workflow. This driver does not claim either prerequisite has executed.

## Caller-owned execution boundary

Use provisioned Linux JDK21, gcc, libubsan and nm. No installation, download or fallback is attempted. The caller must provision an absolute canonical output root Q, separate from the source checkout, with real directories Q/artifacts, Q/tmp, Q/home and Q/cache, and set:

- M3_SOURCE_ROOT to the canonical actual receiving Git checkout.
- M3_QUALIFICATION_ROOT to Q and M3_QUALIFICATION_ID to a fresh 16–128 character safe identifier.
- HOME=Q/home, XDG_CACHE_HOME=Q/cache and TMPDIR=TEMP=TMP=Q/tmp.
- JAVA_HOME and PATH to the same provisioned JDK21; CC may name one provisioned gcc executable.

The existing outer supervisor must own the process group and continuously count the complete checkout, output/cache/M2/tmp tree. Preserve the 512 MiB aggregate cap, 8 GiB free reserve, outer deadline, group cleanup and bounded flat artifact upload. The driver checks reserve before/after commands and log size after completion; it does not pretend to enforce an aggregate cap while children run. All inner commands inherit the outer process group; none starts a nested session. The outer supervisor owns descendant cleanup if an inner timeout kills a compiler/JVM parent.

With that caller-owned boundary, run the existing Python interpreter without cache writes:

~~~sh
python3 -I -S -B runtime/qualify.py --output-root "$M3_QUALIFICATION_ROOT" --inputs-only
python3 -I -S -B runtime/qualify.py --output-root "$M3_QUALIFICATION_ROOT" --run
~~~

Use the actual runtime/qualify.py repository path; the snippets assume its module directory as the current working directory. Inputs-only reads and checks the five source files and emits no PASS execution receipt. --run requires fresh output paths and refuses reused logs/receipts.

## Exact execution and evidence

The driver compiles the three real Java sources with --release 21 -proc:none -Xlint:all -Werror, explicit empty -classpath/-sourcepath and javac -h. No ambient compiler classpath or source discovery is admitted. It requires exactly ten major-65 class files and one real generated JNI header with both authentic composeNative/repeatNative signatures. The original current C is compiled as a complete translation unit with -std=c11, fatal warning flags including -Wconversion/-Wsign-conversion, -fPIC/-shared, fatal UBSan, the authentic kernel header and forced javac-generated JNI header. nm must show precisely the two expected JNI exports and both real kernel symbols.

Fresh JVMs run the unchanged proof first in Java mode and then with an absolute System.load library path under -Xcheck:jni. Both use the plain classpath. No silent --patch-module fallback is permitted: inability to load/execute the classpath route is a qualification failure. Exact output must be the original reuse line plus the complete proof line:

~~~text
REUSE logicalUnits=2097152 reads=2 summaries=21 joins=20 hits=20
M3_RELATION_PORT cases=71748 checks=74829 native=false PASS
M3_RELATION_PORT cases=71748 checks=149658 native=true PASS
~~~

The Java JVM must produce the first and second lines; the native JVM must produce the first and third. A fresh missing-library JVM must exit 1 with UnsatisfiedLinkError naming the absent absolute path and no proof marker. There is no native skip or fallback acceptance.

Six complete command logs live directly in Q/artifacts:

- state-relation-java-compile.log
- state-relation-java-proof.log
- state-relation-c11-compile.log
- state-relation-native-symbols.log
- state-relation-native-proof.log
- state-relation-missing-library-refusal.log

After all checks succeed, source and manifest hashes are rechecked, classes/library/generated header and command logs must remain unchanged, and the driver writes state-relation-source-custody.json followed by state-relation-execution.json. The latter uses schema M3_STATE_RELATION_CURRENT_C11_EXECUTION_V1, a fresh qualification ID, exact counts, all six command/log hashes, tool version observations, ten class hashes, library/header hashes and a linked custody receipt. The generated header is retained as state-relation-generated-jni.h. Binaries remain under Q/state-relation-native and are not export artifacts. Failed attempts retain already-written full command logs but never receive the final PASS execution receipt. The caller's existing artifact fence controls failure uploads.

## Historical proof and selected gap

Canonical recipe m3-regex-receiver-20261014 already executed a numeric C17/UBSan proof with the same Java/proof files and an older C template 5b30f71d8ed878b3199225b9512b75a090ff896c. Its qualification blob 90c166890a6f265473bf5471776ca0be596f2876 and Java/JNI logs 31290aaaad02936d581da02f4625353337eab0e5 / e009eb8e32ecf615fd67eaa24728b6bca25c7613 establish historical results only.

Current C610ca8 changes the compose JNI transfer/staging body to reuse one transfer lane and use lhs as kernel output. The kernel stages validated rows before writing output. The selected new proof directly executes this current alias/staging path and repeat, preserving the original full corpus. Historical numeric module-patch execution does not qualify this current C, C11 or plain classpath route.

The original corpus covers states1–64, dense/repeated frontiers, repeat0–11/Long.MAX_VALUE, input immutability, invalid shapes/bits/counts, order, DAG reuse, slices/empty seams, dot terminators, surrogate refusal, node/work budgets, cancellation and provider isolation. These are existing bounded discriminator cases, not a proof for arbitrary regex inputs.

M3StateRelation exposes composeNative and repeatNative. advance0 is a distinct StringDP native owner. This task does not qualify advance0, direct-C partial-overlap cases, complete libjava/native/CMake linkage, production String dispatch, a matched JDK image, the original private source-fork reactor, Windows, performance or retention. Runtime application and migration-phase advancement remain false.

See THIRD_PARTY_NOTICES.md for source custody and licenses.
