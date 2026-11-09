// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

/** CLI proof entry point for the canonical M3JDK 21 backport DAG. */
public final class BackportDagMain {
    private BackportDagMain() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("no arguments expected");
        }
        RecipeDag dag = RecipeDag.canonical();
        System.out.println("M3_BACKPORT_DAG_OK nodes=" + dag.size());
        int ordinal = 0;
        for (var layer : dag.layers()) {
            System.out.println(
                    "layer=" + ordinal++
                            + " nodes="
                            + layer.stream().map(DagNode::id).toList());
        }
    }
}
