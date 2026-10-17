/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;

/** Execute with org.swinglabs.swingx:swingx-all:1.6.5-1 on the class path. */
public final class M3SwingXLinkageProof {
    private M3SwingXLinkageProof() { }
    public static void main(String[] args) throws Exception {
        same(JList.class, "org.jdesktop.swingx.JXList");
        same(JTable.class, "org.jdesktop.swingx.JXTable");
        same(JTree.class, "org.jdesktop.swingx.JXTree");
        same(JTable.class, "org.jdesktop.swingx.JXTreeTable");
        System.out.println("M3_SWINGX_SUBTYPE_PROOF_PASS=4");
    }
    private static void same(Class<?> api, String donor) throws Exception {
        Class<?> type = Class.forName(donor);
        if (!api.isAssignableFrom(type)) throw new AssertionError(donor + " not assignable to " + api);
    }
}
