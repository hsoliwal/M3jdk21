// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

/** CLI proof entry point for the canonical M3JDK21 backport DAG. */
public final class M3BackportDagMain {
    private M3BackportDagMain() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("no arguments expected");
        }
        M3RecipeDag dag = M3RecipeDag.canonical();
        System.out.println("M3_BACKPORT_DAG_OK nodes=" + dag.size());
        int ordinal = 0;
        for (var layer : dag.layers()) {
            System.out.println(
                    "layer=" + ordinal++
                            + " nodes="
                            + layer.stream().map(M3DagNode::id).toList());
        }
    }
}
