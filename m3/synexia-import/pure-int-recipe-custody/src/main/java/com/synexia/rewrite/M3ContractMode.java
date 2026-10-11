// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/**
 * Contract authority carried by one M3 recipe or pass.
 *
 * <p>This vocabulary is reusable across targets. Target repositories may keep local policy and
 * registry adapters, but they must not redefine these contract meanings.</p>
 */
public enum M3ContractMode {
    /** Public/external contract and observable behavior remain locked. */
    BEHAVIOR_AND_CONTRACT_PRESERVING,

    /** Contract change is intentional and requires explicit LIBRARY_API authority. */
    EXPLICIT_CONTRACT_CHANGE
}
