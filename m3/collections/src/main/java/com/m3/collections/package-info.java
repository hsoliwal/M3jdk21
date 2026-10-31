// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
/**
 * Indexed storage implementations of Java collection interfaces. Payloads and control
 * lanes are arrays; ordering, topology and reclamation use primitive indices. Explicit
 * views, snapshots, Map.Entry return values, weak references and synchronization wait
 * records remain boundary allocations. Concurrent alternatives specify blocking locks.
 * Existing native Synexia shapes and factories retain their contracts.
 */
package com.m3.collections;
