<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 language donor profiles

The [86 original capability M3JEP drafts](INDEX.md) remain stable, source-sealed
and counted separately. Donor profiles describe how one language contributes
to multiple existing capability proposals without duplicating feature IDs or
claiming that an upstream language runtime has been installed.

| Donor profile | Distinctive contribution | State |
| --- | --- | --- |
| [M3JEP-GROOVY-001](proposals/M3JEP-GROOVY-001.md) | Groovy dynamic MOP, closure/DSL dispatch, static compilation and GDK convergence | Draft / HOLD |

No donor profile is an assigned OpenJDK JEP. The M3JDK21 receiving order is
String -> arrays -> collections -> AST/compiler -> remaining. Reusable recipes,
templates and qualification evidence remain in Synexia; product capabilities
are accepted only after target compiler/runtime/JNI/jtreg gates.
