# Additional production recipe qualification

The `stable-order-groups` and `native-row-suffix` packets each passed lint, strict offline Java 21 test compilation of the unchanged four canonical engine owners plus the host test, and all five actual OpenRewrite SDK tests. The six stages executed serially. No host or engine repairs were made. These add ten passing SDK tests to the previous fifteen.

| Pass | Inputs bound | Generated postimage SHA-256 |
|---|---:|---|
| stable-order-groups | 343, including the real baseline parser classpath | `bf601d711667e1078ca30246da6fa89b43d7ed951a3179e0a161365526581959` |
| native-row-suffix | 42; no parser classpath | `3361cebc637103be73d7928e61b69ecc7fd6899abd4f8078019c39dfce6b3960` |

Source and compiled-input drift checks were empty for both passes. SDK_RESULTS, SDK_REFUSALS, SDK_INPUTS and actual emitted patches accompany the candidates under host target/candidate. Exact paths and hashes are retained in `QUALIFICATION_EXTENSION_09_14.json`. Receiving source edits and Java/C/JNI behavior, API, ABI or resource measurements remain separate parent tasks.

Six verification-only packet admissions were inventoried next. The existing host already supports ABSENT targets and conditions only missing-required-source refusal on a present preimage. A documented evidence runner and exact real classpath configuration were prepared and syntax/whitespace linted. No verification packet was frozen or passed to Maven before the parent requested an integration pause after reporting upstream movement. Original templates, source snapshots and receipts remain unchanged.
