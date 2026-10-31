<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Web widget concepts: harvest coverage

`CONCEPTS.tsv` holds 104 concepts. `COMPONENT_CONCEPTS.tsv` maps every harvested component name to a concept:

| tier | rule | names |
|---|---|---|
| T1_ALIAS | the harvest's own alias table (`universe_concepts.py`, verbatim) | see the `tier` column |
| T2_WORD | exact-word aliases added in `tools/concepts.py` (`EXTRA`) | |
| T5_PATH | a concept word in the component's directory path | |
| T3_NON_WIDGET | infrastructure names (events, bindings, converters, hooks) | |
| UNMAPPED | none of the above; still open | |

Run `python tools/concepts.py <inputs> spec/MEMBERS.tsv <out>`; it prints the counts per tier.

## Major kits checked against the harvest (2026-10-30)

| kit | in the 69-kit harvest | action |
|---|---|---|
| MUI | yes (mui/material-ui, mui/base-ui) | none |
| Ant Design | yes (+ ant-design-mobile) | none |
| Chakra UI | yes | none |
| Mantine | yes | none |
| shadcn/ui, Radix | yes (shadcn-ui/ui, radix-ui/primitives, radix-ui/themes) | none |
| Headless UI | yes (tailwindlabs/headlessui) | none |
| Bootstrap | yes (twbs/bootstrap, react-bootstrap) | none |
| PrimeReact / PrimeVue | yes | none |
| Vuetify | yes | none |
| Quasar | yes | none |
| Element Plus | yes | none |
| Fluent UI | **no** | added: microsoft/fluentui `941ed35fd897` (MIT), tree-only clone, `HARVEST_ADDENDUM.tsv` |
| Carbon | yes | none |
| Spectrum | yes (adobe/react-spectrum) | none |
| Blueprint | yes | none |
| Semantic UI | yes (Semantic-UI-React) | none |
| Kendo UI | **no** | added: telerik/kendo-ui-core `b97766b5ab65` (Apache-2.0 core only; the commercial suite is not harvested) |
| Syncfusion | **no** | not harvested: Syncfusion licence is not an open-source licence (licence gate) |
| DevExtreme | **no** | not harvested: DevExpress licence is not an open-source licence (licence gate) |
| AG Grid | **no** | added: ag-grid/ag-grid `6315ba233760` (MIT community features) |
| Ionic | yes | none |
| Onsen UI | **no** | added: OnsenUI/OnsenUI `82c5ff8ce1b8` (Apache-2.0) |
| Framework7 | yes | none |
| Flutter Material/Cupertino | yes (flutter/flutter) | none |
| Jetpack Compose | partly (OTHER_UI_KITS_INVENTORY.tsv techniques; material-components-android names) | names of androidx compose material3 not harvested yet |
| SwiftUI | **no** | not harvestable: closed source; API names only from Apple documentation (not done) |
| WinUI | yes (microsoft/microsoft-ui-xaml) | none |

The addendum adds 478 names (Fluent UI 364, Kendo core 62, AG Grid 41, Onsen 11) from tree listings only (no file contents read).
