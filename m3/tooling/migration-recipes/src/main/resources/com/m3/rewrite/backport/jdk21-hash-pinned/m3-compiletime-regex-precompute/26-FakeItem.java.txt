// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime.fixture;

import com.m3.residency.M3Transient;

/** Transient handle fixture. */
@M3Transient
public class FakeItem {
    public String getText() {
        return "t";
    }

    public void setChecked(boolean checked) {
    }
}
