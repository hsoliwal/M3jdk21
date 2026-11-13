/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Verify array-owned order-statistic list against JDK ArrayList
 * @modules jdk.m3.collections/jdk.m3.collections.apache
 * @compile ArrayTreeListContract.java
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.m3.collections.apache=ALL-UNNAMED ArrayTreeListContract
 */
public final class M3TreeListJTReg {
    private M3TreeListJTReg() {
    }
}
