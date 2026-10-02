// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

/** Contract authority declared by an M3 recipe. */
public enum M3ContractMode {
    /** Public/external contract and observable behavior remain locked. */
    BEHAVIOR_AND_CONTRACT_PRESERVING,

    /** Contract change is intentional and requires explicit LIBRARY_API authority. */
    EXPLICIT_CONTRACT_CHANGE
}
