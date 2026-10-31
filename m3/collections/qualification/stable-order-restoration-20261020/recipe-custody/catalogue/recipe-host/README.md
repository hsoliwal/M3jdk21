<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Additive catalogue receiving passes

This task reuses the existing serial Maven/OpenRewrite proof host without changing
its test or engine implementations. Select one catalogue pass with -Dm3.pass and
set -Drecipe.owner.root to the enclosing synexia-openrewrite-recipes module.
The nested location makes that explicit owner override mandatory. Each pass
contains one ABSENT text target, a sealed template and the existing named recipe.
The parent CONTRACT.md admits only the four appended knowledge files.

Run manifest/format lint, test-compile -DskipTests, then surefire:test. The ordered
SDK proof emits actual Result output only after replay/fixed-point/order/refusal
checks pass. Target admission verifies the emitted source hash before copying.
These decisions describe separate completed Java/JNI qualifications; generating
the catalogue itself does not establish runtime correctness or performance.
