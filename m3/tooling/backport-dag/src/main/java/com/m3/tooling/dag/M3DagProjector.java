// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic Camel/Airflow/Drools projections of one canonical M3 recipe DAG.
 *
 * <p>The validated {@link M3RecipeDag} remains the semantic authority. Airflow and Drools preserve
 * exact direct dependencies. Camel executes topological layers behind barriers; this may strengthen
 * ordering but can never relax a dependency. Projections carry no edit-scope or promotion authority.</p>
 */
public final class M3DagProjector {
    private M3DagProjector() {}

    public static Map<M3DagProjectionFormat, M3DagProjection> all(
            M3RecipeDag dag, String projectionId) {
        EnumMap<M3DagProjectionFormat, M3DagProjection> result =
                new EnumMap<>(M3DagProjectionFormat.class);
        for (M3DagProjectionFormat format : M3DagProjectionFormat.values()) {
            M3DagProjection projection = project(dag, projectionId, format);
            result.put(format, projection);
        }
        return Map.copyOf(result);
    }

    public static M3DagProjection project(
            M3RecipeDag dag,
            String projectionId,
            M3DagProjectionFormat format) {
        M3RecipeDag checkedDag = Objects.requireNonNull(dag, "dag");
        String checkedId = projectionId(projectionId);
        M3DagProjectionFormat checkedFormat = Objects.requireNonNull(format, "format");
        String root = M3DagSemanticRoot.of(checkedDag);
        String content =
                switch (checkedFormat) {
                    case CAMEL_JAVA -> camel(checkedDag, checkedId, root);
                    case AIRFLOW_PYTHON -> airflow(checkedDag, checkedId, root);
                    case DROOLS_DRL -> drools(checkedDag, checkedId, root);
                };
        return new M3DagProjection(checkedFormat, root, content);
    }

    private static String camel(M3RecipeDag dag, String projectionId, String root) {
        StringBuilder out = new StringBuilder();
        out.append("// Generated from canonical M3RecipeDag; do not hand-edit.\n")
                .append("package com.m3.generated;\n\n")
                .append("import org.apache.camel.builder.RouteBuilder;\n\n")
                .append("public final class M3BackportRoutes extends RouteBuilder {\n")
                .append("    public static final String PROJECTION_ID = \"")
                .append(javaString(projectionId))
                .append("\";\n")
                .append("    public static final String DAG_ROOT = \"")
                .append(root)
                .append("\";\n\n")
                .append("    @Override\n")
                .append("    public void configure() {\n")
                .append("        from(\"direct:m3-start\").routeId(\"m3-start\")\n")
                .append("                .setHeader(\"M3ProjectionId\").constant(PROJECTION_ID)\n")
                .append("                .setHeader(\"M3DagRoot\").constant(DAG_ROOT)\n")
                .append("                .to(\"direct:m3-layer-0\");\n\n");

        List<List<M3DagNode>> layers = dag.layers();
        for (int index = 0; index < layers.size(); index++) {
            List<M3DagNode> layer = layers.get(index);
            out.append("        from(\"direct:m3-layer-")
                    .append(index)
                    .append("\").routeId(\"m3-layer-")
                    .append(index)
                    .append("\")\n")
                    .append("                .multicast().parallelProcessing().stopOnException()\n");
            for (M3DagNode node : layer) {
                out.append("                .to(\"direct:m3-node-")
                        .append(endpoint(node.id()))
                        .append("\")\n");
            }
            out.append("                .end()");
            if (index + 1 < layers.size()) {
                out.append("\n                .to(\"direct:m3-layer-")
                        .append(index + 1)
                        .append("\")");
            }
            out.append(";\n\n");
        }

        for (M3DagNode node : dag.topologicalOrder()) {
            out.append("        from(\"direct:m3-node-")
                    .append(endpoint(node.id()))
                    .append("\").routeId(\"m3-node-")
                    .append(endpoint(node.id()))
                    .append("\")\n")
                    .append("                .setHeader(\"M3NodeId\").constant(\"")
                    .append(javaString(node.id()))
                    .append("\")\n")
                    .append("                .setHeader(\"M3Kind\").constant(\"")
                    .append(node.kind().name())
                    .append("\")\n")
                    .append("                .setHeader(\"M3Scope\").constant(\"")
                    .append(node.scope().name())
                    .append("\")\n")
                    .append("                .setHeader(\"M3Mutating\").constant(")
                    .append(node.mutating())
                    .append(")\n")
                    .append("                .setHeader(\"M3SerialPromotion\").constant(")
                    .append(node.serialPromotion())
                    .append(")\n")
                    .append("                .setHeader(\"M3ScopePromotionApproved\").constant(")
                    .append(node.scopePromotionApproved())
                    .append(")\n")
                    .append("                .setHeader(\"M3WorkRef\").constant(\"")
                    .append(javaString(node.workRef()))
                    .append("\")\n")
                    .append("                .to(\"direct:m3-dispatch\");\n\n");
        }
        out.append("    }\n}\n");
        return out.toString();
    }

    private static String airflow(M3RecipeDag dag, String projectionId, String root) {
        StringBuilder out = new StringBuilder();
        out.append("# Generated from canonical M3RecipeDag; do not hand-edit.\n")
                .append("from datetime import datetime\n")
                .append("from airflow import DAG\n")
                .append("from airflow.operators.empty import EmptyOperator\n\n")
                .append("PROJECTION_ID = '")
                .append(pythonString(projectionId))
                .append("'\n")
                .append("DAG_ROOT = '")
                .append(root)
                .append("'\n\n")
                .append("with DAG(\n")
                .append("    dag_id='m3_")
                .append(pythonIdentifier(projectionId))
                .append("',\n")
                .append("    start_date=datetime(2026, 1, 1),\n")
                .append("    schedule=None,\n")
                .append("    catchup=False,\n")
                .append("    tags=['m3', 'jdk21', 'backport'],\n")
                .append(") as dag:\n");

        for (M3DagNode node : dag.topologicalOrder()) {
            out.append("    ")
                    .append(pythonIdentifier(node.id()))
                    .append(" = EmptyOperator(\n")
                    .append("        task_id='")
                    .append(pythonString(node.id()))
                    .append("',\n")
                    .append("        params={\n")
                    .append("            'projection_id': PROJECTION_ID,\n")
                    .append("            'dag_root': DAG_ROOT,\n")
                    .append("            'kind': '")
                    .append(node.kind().name())
                    .append("',\n")
                    .append("            'scope': '")
                    .append(node.scope().name())
                    .append("',\n")
                    .append("            'mutating': ")
                    .append(pythonBoolean(node.mutating()))
                    .append(",\n")
                    .append("            'serial_promotion': ")
                    .append(pythonBoolean(node.serialPromotion()))
                    .append(",\n")
                    .append("            'scope_promotion_approved': ")
                    .append(pythonBoolean(node.scopePromotionApproved()))
                    .append(",\n")
                    .append("            'work_ref': '")
                    .append(pythonString(node.workRef()))
                    .append("',\n")
                    .append("        },\n")
                    .append("    )\n");
        }
        out.append("\n");
        for (M3DagNode node : dag.topologicalOrder()) {
            for (String dependency : node.dependsOn().stream().sorted().toList()) {
                out.append("    ")
                        .append(pythonIdentifier(dependency))
                        .append(" >> ")
                        .append(pythonIdentifier(node.id()))
                        .append("\n");
            }
        }
        return out.toString();
    }

    private static String drools(M3RecipeDag dag, String projectionId, String root) {
        StringBuilder out = new StringBuilder();
        out.append("// Generated from canonical M3RecipeDag; do not hand-edit.\n")
                .append("package com.m3.generated\n\n")
                .append("global java.lang.String m3ProjectionId;\n")
                .append("global java.lang.String m3DagRoot;\n\n")
                .append("// expected projection_id=")
                .append(drlComment(projectionId))
                .append(" dag_root=")
                .append(root)
                .append("\n\n")
                .append("declare M3NodeState\n")
                .append("    id : String\n")
                .append("    status : String\n")
                .append("end\n\n");

        for (M3DagNode node : dag.topologicalOrder()) {
            out.append("// node=")
                    .append(drlComment(node.id()))
                    .append(" kind=")
                    .append(node.kind().name())
                    .append(" scope=")
                    .append(node.scope().name())
                    .append(" mutating=")
                    .append(node.mutating())
                    .append(" scopePromotionApproved=")
                    .append(node.scopePromotionApproved())
                    .append(" workRef=")
                    .append(drlComment(node.workRef()))
                    .append("\n")
                    .append("rule \"ready-")
                    .append(drlString(node.id()))
                    .append("\"\n")
                    .append("when\n")
                    .append("    $self : M3NodeState(id == \"")
                    .append(drlString(node.id()))
                    .append("\", status == \"PENDING\")\n")
                    .append("    eval(m3ProjectionId.equals(\"")
                    .append(drlString(projectionId))
                    .append("\"))\n")
                    .append("    eval(m3DagRoot.equals(\"")
                    .append(root)
                    .append("\"))\n");
            for (String dependency : node.dependsOn().stream().sorted().toList()) {
                out.append("    M3NodeState(id == \"")
                        .append(drlString(dependency))
                        .append("\", status == \"DONE\")\n");
            }
            out.append("then\n")
                    .append("    modify($self) { setStatus(\"READY\") };\n")
                    .append("end\n\n");
        }
        return out.toString();
    }

    private static String projectionId(String value) {
        String checked = Objects.requireNonNull(value, "projectionId").strip();
        if (!checked.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,119}")) {
            throw new IllegalArgumentException("invalid projection id: " + checked);
        }
        return checked;
    }

    private static String endpoint(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "-");
    }

    private static String pythonIdentifier(String value) {
        return "n_" + value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String javaString(String value) {
        return escape(value);
    }

    private static String drlString(String value) {
        return escape(value);
    }

    private static String drlComment(String value) {
        return value.replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String pythonString(String value) {
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String pythonBoolean(boolean value) {
        return value ? "True" : "False";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }
}
