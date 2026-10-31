// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.internal;

public final class M3ContractException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public M3ContractException(String message) { super(message); }
    public M3ContractException(String message, Throwable cause) { super(message, cause); }
}
