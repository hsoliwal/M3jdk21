// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContextSkillInventoryTest {
    @Test
    void ranksFuzzySkillsUnderOperationalConstraints() {
        ContextSkillInventory.Skill grep = new ContextSkillInventory.Skill(
                "grep-index", "search.text", "direct:grep", Set.of("search", "index", "text"),
                9800, 2, 40, 0, 1_000_000, 512_000, 9990, 8, true, true);
        ContextSkillInventory.Skill llm = new ContextSkillInventory.Skill(
                "nano-llm", "reason.text", "direct:llm", Set.of("reasoning", "language"),
                7500, 40, 5000, 0, 1_000_000, 2_000_000_000L, 9700, 1, false, true);
        ContextSkillInventory inventory = new ContextSkillInventory(List.of(llm, grep));

        ContextSkillInventory.Query query = new ContextSkillInventory.Query(
                "serch indx text", Set.of("search"), 10, 1000, 4096, 10_000_000,
                9900, 9000, 2, true, true, 4);

        List<ContextSkillInventory.Match> matches = inventory.rank(query);
        assertEquals(1, matches.size());
        assertEquals("grep-index", matches.get(0).skill().id());
    }

    @Test
    void rejectsInvalidMetrics() {
        assertThrows(IllegalArgumentException.class, () -> new ContextSkillInventory.Skill(
                "bad", "bad", "direct:bad", Set.of(), 10_001, 0, 0,
                0, 1, 0, 10_000, 1, true, true));
    }
}
