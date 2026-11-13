// SPDX-License-Identifier: Apache-2.0
package jdk.m3.cb;

public class VmFixture implements Runnable {
    static { System.setProperty("m3.vm.fixture.initialized", "yes"); }
    public int number;
    @Override public void run() { number++; }
}
