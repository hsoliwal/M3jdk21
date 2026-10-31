// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiletime.fixture;

import jdk.m3.residency.M3Transient;

/** Transient handle fixture. */
@M3Transient
public class FakeItem {
    public String getText() {
        return "t";
    }

    public void setChecked(boolean checked) {
    }
}
