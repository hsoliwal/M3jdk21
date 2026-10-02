// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Read-only three-way file-hash assessment. No semantic equivalence or automatic merge claims. */
public final class EnhancementPlan {
  private EnhancementPlan() {}
  public static void main(String[] args) throws Exception {
    if (args.length != 3) throw new IllegalArgumentException("usage: <baseline-manifest> <new-source-checkout> <target-checkout>");
    Map<String, Object> baseline = Json.object(Json.parse(Files.readString(Path.of(args[0]))));
    List<Object> mappings = Json.array(baseline.get("mappings"));
    List<String> changed = new ArrayList<>();
    List<Object> result = new ArrayList<>();
    for (Object value : mappings) {
      Map<String, Object> mapping = Json.object(value);
      List<Object> sourceRefs = Json.array(mapping.getOrDefault("source_refs", List.of()));
      List<Object> targetRefs = Json.array(mapping.getOrDefault("target_refs", List.of()));
      if (sourceRefs.size() != 1 || targetRefs.size() != 1) continue;
      Map<String, Object> source = Json.object(sourceRefs.get(0)), target = Json.object(targetRefs.get(0));
      String sourceNow = hash(Path.of(args[1]).toRealPath(), Json.string(source.get("path")));
      String targetNow = hash(Path.of(args[2]).toRealPath(), Json.string(target.get("path")));
      String status = classify(Json.string(source.get("sha256")), sourceNow, Json.string(target.get("sha256")), targetNow);
      String id = Json.string(mapping.get("id"));
      if (!status.equals("unchanged")) changed.add(id);
      result.add(Map.of("id", id, "status", status, "action", "review-only; do not overwrite"));
    }
    System.out.println(Json.write(Map.of("mappings", result, "affected_dependency_closure", dependentClosure(mappings, changed),
        "limitations", "File hashes only. Multi-source/target mappings and unmapped additions require inventory review. New checkout ancestry is not validated by this read-only assessment.")));
  }
  static String classify(String baselineSource, String sourceNow, String baselineTarget, String targetNow) {
    if (sourceNow == null) return "source_deleted_tombstone_required";
    if (targetNow == null) return "target_deleted_review_required";
    boolean sourceChanged = !Objects.equals(baselineSource, sourceNow), targetChanged = !Objects.equals(baselineTarget, targetNow);
    if (sourceChanged && targetChanged) return "conflict_review_required";
    return sourceChanged ? "source_changed" : targetChanged ? "target_adapted" : "unchanged";
  }
  static List<String> dependentClosure(List<?> mappings, List<String> seeds) {
    TreeSet<String> result = new TreeSet<>(seeds);
    boolean more;
    do {
      more = false;
      for (Object value : mappings) {
        Map<String, Object> mapping = Json.object(value);
        for (Object dependency : Json.array(mapping.getOrDefault("dependencies", List.of())))
          if (result.contains(Json.string(dependency))) more |= result.add(Json.string(mapping.get("id")));
      }
    } while (more);
    return List.copyOf(result);
  }
  private static String hash(Path root, String relative) throws Exception {
    Path path = ManifestValidator.safe(root, relative, false);
    if (!Files.exists(path)) return null;
    if (Files.size(path) > 16L * 1024 * 1024) throw new IllegalArgumentException("hash input exceeds budget");
    return M3PortRecipe.hash(Files.readAllBytes(path));
  }
}
