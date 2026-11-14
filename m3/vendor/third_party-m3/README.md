<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/vendor/third_party-m3 — M3 refactor layer over the vendored third-party modules

`m3/vendor/third_party` holds the third-party libraries as **unchanged published sources**, each one gate-proven against
its published jar. This directory holds what M3 adds on top of them. It never edits the upstream layer.

- `descriptors/<module>/module-info.java`: provenance kind `m3-descriptor-for-jmod`. These are explicit module descriptors
  for libraries that publish none (or only an `Automatic-Module-Name`), so that every vendored library can be packaged
  as a JMOD and linked by `jlink`.
  - Facts come from `jdeps --generate-module-info` over the gate-proven jar.
  - The files are written by the OpenRewrite recipe `com.m3.rewrite.jmod.M3ModuleDescriptorRecipe`
    (`m3/tooling/jmod-recipes`), from a hash-pinned manifest (`m3/tooling/jmod-recipes/evidence/DESCRIPTOR_MANIFEST.tsv`).
  - Do not edit them by hand: the recipe refuses to overwrite a drifted descriptor.

The descriptors are M3 packaging metadata, licensed Apache-2.0 (see NOTICE). The libraries they describe keep their own
licences in `m3/vendor/third_party/<module>/`.
