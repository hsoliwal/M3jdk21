// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiletime.fixture;

import jdk.m3.residency.M3BehaviourOnly;
import jdk.m3.residency.M3Retains;

/** Parent fixture that hands out transient items. */
public class FakeTable {
    public FakeItem getItem(int index) {
        return new FakeItem();
    }

    public FakeItem[] getItems() {
        return new FakeItem[] {new FakeItem()};
    }

    @M3BehaviourOnly
    public int indexOfItem(FakeItem item) {
        return 0;
    }

    @M3Retains
    public void remember(FakeItem item) {
    }
}
