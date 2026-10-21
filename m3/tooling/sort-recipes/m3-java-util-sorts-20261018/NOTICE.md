<!-- SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors -->
# Notices and source custody

The Maven/OpenRewrite recipe composition, receiver, recipe tests, authored
verification tools and documentation in this crate are first-party Synexia work
under Apache License 2.0, except where a file retains a different explicit notice.
LICENSE contains the Apache terms. provenance/SYNEXIA-NOTICE.txt preserves the
original Synexia notice.

Four Java engine sources are exact copies of their existing canonical Synexia
owners. Their original headers remain intact. provenance/engine-sources.tsv
records each canonical repository path, source revision, Git blob and SHA-256
digest. Their canonical ownership stays with those existing engines.

The Java payload candidates under payload/java retain their declared OpenJDK
GPL-2.0-only WITH Classpath-exception-2.0 terms. These source fixtures and their
generated product files are not relicensed by the Apache-licensed recipe.
M3TimSort is mechanically specialized from the exact receiving JDK's TimSort
source, with its Oracle and Google copyright notices preserved. The original
donor, OpenJDK license and additional license information remain under
provenance/donors; provenance/TimSort-derivation.tsv records the exact lineage.
Other algorithm implementations retain their own original per-file attribution.

The sorting catalogue records pinned donor references, implementation versus
reference roles, and review outcomes. Challenge sites provide category and
evaluation evidence; no challenge statement, editorial or submitted solution body
is copied by this packet. Referenced Apache libraries contribute comparison
evidence only where the catalogue says reference; a URL does not grant source
copying or change a payload's license.

OpenRewrite is a pinned authoring and qualification dependency. The generated
M3JDK21 runtime sources acquire no Synexia or OpenRewrite runtime dependency.
