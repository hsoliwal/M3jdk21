// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ManifestValidatorTest {
  private static int checks;
  private ManifestValidatorTest() {}
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    ManifestValidator.validate(root, false);
    checks++;
    expect(() -> ManifestValidator.validate(root, true));
    var original = Json.object(Json.parse(Files.readString(root.resolve("m3/docs/name-mapping.json"))));
    var duplicate = Json.object(Json.parse(Json.write(original)));
    var duplicates = Json.array(duplicate.get("mappings")); duplicates.add(duplicates.get(0)); duplicate.put("mappings", duplicates);
    expect(() -> ManifestValidator.validateDocument(root, duplicate, false));
    var claimed = Json.object(Json.parse(Json.write(original)));
    change(claimed, mapping -> { mapping.put("status", "implemented_tested"); mapping.put("evidence", List.of()); });
    expect(() -> ManifestValidator.validateDocument(root, claimed, false));
    var broken = Json.object(Json.parse(Json.write(original)));
    change(broken, mapping -> mapping.put("dependencies", List.of("missing-owner")));
    expect(() -> ManifestValidator.validateDocument(root, broken, false));
    var hash = Json.object(Json.parse(Json.write(original)));
    change(hash, mapping -> { var ref = Json.object(Json.array(mapping.get("target_refs")).get(0)); ref.put("sha256", "0".repeat(64)); mapping.put("target_refs", List.of(ref)); });
    expect(() -> ManifestValidator.validateDocument(root, hash, false));
    var traversal = Json.object(Json.parse(Json.write(original)));
    change(traversal, mapping -> { var ref = Json.object(Json.array(mapping.get("target_refs")).get(0)); ref.put("path", "../outside.java"); mapping.put("target_refs", List.of(ref)); });
    expect(() -> ManifestValidator.validateDocument(root, traversal, false));
    check(EnhancementPlan.classify("a", "b", "a", "a").equals("source_changed"));
    check(EnhancementPlan.classify("a", "a", "b", "c").equals("target_adapted"));
    check(EnhancementPlan.classify("a", "b", "c", "d").equals("conflict_review_required"));
    check(EnhancementPlan.classify("a", null, "b", "b").equals("source_deleted_tombstone_required"));
    check(EnhancementPlan.classify("a", "a", "b", null).equals("target_deleted_review_required"));
    check(EnhancementPlan.classify("a", "a", "b", "b").equals("unchanged"));
    var closure = EnhancementPlan.dependentClosure(List.of(
        Map.of("id", "a", "dependencies", List.of()),
        Map.of("id", "b", "dependencies", List.of("a")),
        Map.of("id", "c", "dependencies", List.of("b"))), List.of("a"));
    check(closure.equals(List.of("a", "b", "c")));
    System.out.println("MANIFEST_TOOLS " + checks + " checks passed");
  }
  private static void change(Map<String, Object> doc, java.util.function.Consumer<Map<String, Object>> edit) {
    var mappings = Json.array(doc.get("mappings")); var mapping = Json.object(mappings.get(4)); edit.accept(mapping); mappings.set(4, mapping); doc.put("mappings", mappings);
  }
  private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError(); }
  private static void expect(Throwing action) throws Exception {
    checks++;
    try { action.run(); } catch (IllegalArgumentException expected) { return; }
    throw new AssertionError("expected refusal");
  }
  @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
