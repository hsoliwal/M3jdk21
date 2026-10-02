# Recipe reuse provenance

The three-way replay enhancement adapts the existing M3 foundation recipe at
`hsoliwal/M3jdk21@8bb6215372e07712f1fdf5a0cb912af495007b19` and its runtime
recipe mechanism at `3776d6e674d6c9b04539ca24aca2504aa88d4a57`. Their copyright
and Apache-2.0 SPDX notices remain in the recipe sources. The runtime recipe is
used as a mechanism reference; its Java/HotSpot production patch is not copied.

Git's installed command-line interfaces provide binary diff and complete-patch
preflight/application. No Git source is copied. Git is distributed separately
under GPL-2.0; invoking its executable does not relicense the M3 recipe.

The OpenClaw harvested catalogue was checked before implementation and did not
fit this source-pinned recipe capability. No OpenClaw or private Synexia source
is copied into this enhancement. OpenJDK source files are only hash-validated
as runtime fences; their upstream licenses and notices remain unchanged.
# JSON Schema validation

`mapping-check.py` wraps the installed `jsonschema.Draft202012Validator` (MIT,
https://github.com/python-jsonschema/jsonschema). No implementation source is
copied. This is a maintainer/CI dependency and is outside the runtime kernel.
