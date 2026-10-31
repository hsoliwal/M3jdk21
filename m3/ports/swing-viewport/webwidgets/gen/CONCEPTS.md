<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Web widget concepts: harvest coverage

`CONCEPTS.tsv` holds 107 concepts (104 from the harvest + 3 new). `COMPONENT_CONCEPTS.tsv` maps every harvested component name to a concept:

| tier | rule | names |
|---|---|---|
| T1_ALIAS | the harvest's own alias table (`universe_concepts.py`, verbatim) | 4,774 |
| T2_WORD | exact-word aliases added in `tools/concepts.py` (`EXTRA`) | 831 |
| T5_PATH | a concept word in the component's directory path | 58 |
| T6_CLUSTER | clusters from a CPU token census of the remaining names (`CLUSTER`; substrings for all-lowercase GTK/wx names) | 211 |
| T7_FINAL | every remaining name, hand-checked cluster by cluster (`FINAL`), including 3 new concepts | 179 |
| T3_NON_WIDGET | support types: events, bindings, converters, hooks, GTK private/builder/css/gesture types, printing, settings (`NON_WIDGET`, `NON_WIDGET_SUB`, `SUPPORT`) | 748 |
| UNMAPPED | none | 0 |

Total 6,801 names (6,323 harvested + 478 addendum): 6,053 map to one of 107 concepts, 748 are support types, 0 unmapped.
New concepts from the last pass (generated on both sides with existing family patterns): emoji-picker (PICKER, cascade of
category and emoji), signature-pad (EMBED, drawing surface; TODO M3P-EMBED-1 stroke capture), splash-screen (OVERLAY, centred
modal plane).

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
