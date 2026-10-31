// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

/** Supported orchestration/admission projections of the canonical M3 recipe DAG. */
public enum M3DagProjectionFormat {
    CAMEL_JAVA("M3BackportRoutes.java"),
    AIRFLOW_PYTHON("m3_backport_dag.py"),
    DROOLS_DRL("m3_backport_rules.drl");

    private final String fileName;

    M3DagProjectionFormat(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
