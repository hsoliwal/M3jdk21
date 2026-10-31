// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

/** Typed client-request failure mapped to an OpenAI-style HTTP error response. */
public final class ChromeMlRequestException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int status;

    public ChromeMlRequestException(int status, String message) {
        super(message);
        if (status < 400 || status > 499) {
            throw new IllegalArgumentException("status");
        }
        this.status = status;
    }

    public int status() {
        return status;
    }
}
