<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK21 selected implementation source namespace pass

Copyright 2026 Hitesh Soliwal and contributors. Authored receiving metadata is Apache-2.0; imported source retains its original rights.

The package-first pass imports complete source bodies into [the implementation directory](../../implementations/jdk21-package-renamed/). FULL_SOURCE_BINDINGS.tsv records each original/target path, mode, Git blob and raw SHA-256. File counts describe this finite inventory, not implemented collection contracts.

All59 selected sources have exact raw inverses and preserved headers. Full compilation fails with78 errors and one warning on internal APIs/package-private dependencies. Only four helpers compile in the finite strict profile; no collection runtime was initialized or validated.

TARGET_DISPOSITION.json records PENDING_RUNTIME_PORT. This independent source directory is not added to the M3 reactor or java.base. Public JDK names and the receiving sequence are unchanged. Runtime behavior, full dependency closure, array/index/offset/mask conversion, stored-entry elimination and JNI remain open.

SOURCE_QUALIFICATION.json records executed proof and limits. TRANSFORMATION_CATALOGUE.json records recipe selection and rejected alternatives for reuse in later serial per-file passes. Qualification helpers are not presented as a runnable checkout workflow.
