<!-- SPDX-License-Identifier: Apache-2.0 -->
Copyright 2026 Hitesh Soliwal and contributors.

Exact admitted v7 regex vendor receiver candidate. This is a custody packet; target runtime admission is pending.

export.tsv uses the existing SynexiaImportManifest M3_DELIVERY_EXPORT_V1 schema at M3 commit 6cb27fcc02c72f93d6ae7bdf6edb680ec5563468. Its 24 entries retain original paths/packages: 22 production sources plus original LICENSE/NOTICE at admitted Synexia 80d288951148548ef6aa4c95df9cb31a438aac16. Every production body also matches exact current 24d0a4e20826c627e12c2a4addb16d52479a22b4 Contents readback.

Importer source root is templates/m3/vendor/synexia. Target root must be a separate isolated M3 fixture. Use the existing com.m3.synexia.importer.SynexiaImportCli verify/materialize/verify-target and existing recipe owner. Existing importer snapshots under existing-intake/ are review context, not target changes. No new engine is included.

The original f005a6648371d84b4b317152ade12cb61c9c0814ee8524d2cd87abd61f821dc6 proof compiled 22 production +3 test sources into86 new classes while consuming2440 inherited classes and64 jars. DEPENDENCY_FRONTIER.json keeps those limits and explicit unresolved edges. No JVM, build, runtime or full-family completion is claimed here. Refused V8 bodies were excluded.

Existing M3 vendor LICENSE/NOTICE equal admitted80d originals. Current Synexia NOTICE has a1906-byte larger successor with an edited heading plus insertions; readback is retained for a separately sealed metadata reconciliation, without automatic vendor overwrite.
