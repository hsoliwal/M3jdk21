# Regex current-owner recipe SDK hosted qualification

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project

SPDX-License-Identifier: Apache-2.0

This hosted lane reuses the successful TQ supervisor and the existing Maven/OpenRewrite SDK. It does not apply a recipe to the receiving JDK. The SDK retains its original three providers, Maven POM, dependency coordinates and three named JUnit methods. Its two emitted source candidates are the current regex DP owner and the hierarchical jtreg source; three helper/JNI inputs remain under exact KEEP custody.

The fresh sparse checkout contains the complete SDK and hosted inputs. The helper checks the exact pull-request head, every source-binding byte and all 21 tracked SDK files before running SDK code. The lane then runs the SDK's existing input and refusal checks, unchanged-POM Maven test with tests enabled, and the existing actual-report verifier. Maven resolves the declared Central OpenRewrite 8.17.1 dependencies in the task's isolated repository. There is no source-fork substitution or toolchain download fallback.

The generated hierarchical jtreg source is additionally compiled with the provisioned javac, release 21, no annotation processing and fatal warnings. Its class file must be nonempty, have Java 21 major version 65, and have a recorded hash. This is compile-only: the test is not run on stock Java, and matched-VM dispatch, complete jtreg and JNI execution remain open.

The report binds the actual three-method Surefire XML, source and compiled provider identities, both in-job output hashes, the SDK receipt and the three unchanged KEEP inputs. Product DP source is qualified only for source recipe replay; its Java compilation remains open. Generated sources and class files are checked in-job; the upload retains small logs and hash receipts, not binary or product recovery.

The inherited 512 MiB aggregate task cap, 8 GiB after-output reserve, explicit HOME/TEMP/cache/Maven paths, process-group cleanup and deadlines remain active. Command logs are created directly in the artifact directory before execution. Every upload, including failure evidence, requires at most 64 flat regular files and 8 MiB, with no symlinks or truncation.

The existing verifier deletes a Python temporary directory beneath SDK/target. Java tools may delete task-owned temporary directories beneath ROOT/tmp. During the corresponding commands only, an ENOENT for these exact temporary subtrees restarts the complete storage census, at most three attempts. Source, Maven repository and ordinary target directory errors remain fatal. Eleven mocked cases call the actual inline gate before SDK work; they do not simulate real disk exhaustion or execute product behavior.

Qualification is NOT_RUN until the exact hosted job and its actual receipts are inspected. Original private-model, product application, regex runtime, JNI, matched JDK, performance and retention qualification remain false. The complete migration and continual optimization goal remains active.
