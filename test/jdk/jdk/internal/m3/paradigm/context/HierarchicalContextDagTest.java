// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HierarchicalContextDagTest {
    @Test
    void expandsDagOfDagsWithDeterministicDependencies() {
        ContextSkillInventory inventory = new ContextSkillInventory(List.of(
                skill("scan"), skill("classify"), skill("emit")));

        HierarchicalContextDag.Dag child = new HierarchicalContextDag.Dag(
                "child", Set.of("phase"),
                List.of(
                        HierarchicalContextDag.Node.skill("scan", "scan", Set.of()),
                        HierarchicalContextDag.Node.skill("classify", "classify", Set.of("scan"))));

        HierarchicalContextDag.Dag parent = new HierarchicalContextDag.Dag(
                "parent", Set.of("workflow"),
                List.of(
                        HierarchicalContextDag.Node.dag("phase", "child", Set.of()),
                        HierarchicalContextDag.Node.skill("emit", "emit", Set.of("phase"))));

        HierarchicalContextDag graph = new HierarchicalContextDag(inventory, List.of(child, parent));
        HierarchicalContextDag.Plan plan = graph.expand("parent", 8, 32);

        assertEquals(List.of("parent.phase.scan", "parent.phase.classify", "parent.emit"),
                plan.steps().stream().map(HierarchicalContextDag.Step::id).toList());
        assertEquals(Set.of("parent.phase.scan"), plan.roots());
        assertEquals(Set.of("parent.emit"), plan.sinks());
        assertEquals(Set.of("parent.phase.classify"), plan.steps().get(2).dependsOn());
    }

    @Test
    void rejectsRecursiveDagReferences() {
        ContextSkillInventory inventory = new ContextSkillInventory(List.of(skill("noop")));
        HierarchicalContextDag.Dag left = new HierarchicalContextDag.Dag(
                "left", Set.of(), List.of(HierarchicalContextDag.Node.dag("r", "right", Set.of())));
        HierarchicalContextDag.Dag right = new HierarchicalContextDag.Dag(
                "right", Set.of(), List.of(HierarchicalContextDag.Node.dag("l", "left", Set.of())));
        HierarchicalContextDag graph = new HierarchicalContextDag(inventory, List.of(left, right));

        assertThrows(IllegalArgumentException.class, () -> graph.expand("left", 8, 32));
    }

    private static ContextSkillInventory.Skill skill(String id) {
        return new ContextSkillInventory.Skill(
                id, id, "direct:" + id, Set.of(id), 10_000, 1, 1,
                0, 1_000_000, 1024, 10_000, 1, true, true);
    }
}
