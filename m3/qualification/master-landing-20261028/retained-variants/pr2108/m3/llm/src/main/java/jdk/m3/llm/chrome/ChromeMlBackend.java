// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.util.Locale;

/** Explicit requested backend, never device attestation or an automatic fallback. */
public enum ChromeMlBackend {
    GPU, CPU, APU;

    public static ChromeMlBackend parse(String value) {
        return value == null || value.isBlank() ? GPU : valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
