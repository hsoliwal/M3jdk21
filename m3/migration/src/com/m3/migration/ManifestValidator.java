// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Semantic gates for the existing name-map; does not infer a port from a file's existence. */
public final class ManifestValidator {
  private ManifestValidator() {}
  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <repository-root> [--complete]");
    if (args.length == 2 && !args[1].equals("--complete")) throw new IllegalArgumentException("unknown option");
    validate(Path.of(args[0]), args.length == 2);
    System.out.println("MANIFEST valid for declared partial scope; this is not runtime acceptance");
  }
  public static void validate(Path root, boolean complete) throws Exception {
    validateDocument(root, Json.object(Json.parse(Files.readString(root.resolve("m3/docs/name-mapping.json")))), complete);
  }
  static void validateDocument(Path root, Map<String, Object> doc, boolean complete) throws Exception {
    root = root.toRealPath();
    require(Json.integer(doc.get("schema")) == 1, "unsupported root schema");
    Map<String, Object> config = Json.object(doc.get("migration"));
    require(Json.integer(config.get("schema_version")) == 1, "unsupported migration extension");
    require(Json.string(config.get("source_commit")).matches("[0-9a-f]{40}"), "source commit not pinned");
    require(Json.string(config.get("target_base_commit")).matches("[0-9a-f]{40}"), "target baseline not pinned");
    List<Object> mappings = Json.array(doc.get("mappings"));
    Set<String> ids = new HashSet<>(), targets = new HashSet<>();
    for (Object value : mappings) require(ids.add(Json.string(Json.object(value).get("id"))), "duplicate mapping ID");
    for (Object value : mappings) {
      Map<String, Object> mapping = Json.object(value);
      String status = Json.string(mapping.get("status"));
      for (Object dependency : Json.array(mapping.getOrDefault("dependencies", List.of())))
        require(ids.contains(Json.string(dependency)), "unmapped dependency: " + dependency);
      if (mapping.get("recipe") == null) {
        require(!status.equals("implemented_tested"), "port claim without executable recipe");
        if (complete) require(status.equals("intentionally_excluded") && mapping.containsKey("rationale"), "pending or legacy mapping");
        continue;
      }
      for (String field : List.of("canonical_owner", "identity_rules", "compatibility", "bootstrap", "port_direction"))
        require(!Json.string(mapping.get(field)).isBlank(), "missing " + field);
      Map<String, Object> recipe = Json.object(mapping.get("recipe"));
      require(Set.of("port", "generate").contains(Json.string(recipe.get("mode"))), "unsupported recipe mode");
      require(!Json.string(recipe.get("version")).isBlank(), "missing recipe version");
      for (Object reference : Json.array(mapping.get("source_refs"))) {
        Map<String, Object> ref = Json.object(reference);
        require(Json.string(ref.get("commit")).equals(config.get("source_commit")), "stale source pin");
        safe(root, Json.string(ref.get("path")), false);
        require(Json.string(ref.get("sha256")).matches("[0-9a-f]{64}"), "invalid source hash");
        require(!Json.string(ref.get("symbol")).isBlank(), "source symbol missing");
      }
      for (Object reference : Json.array(mapping.get("target_refs"))) {
        Map<String, Object> ref = Json.object(reference);
        String relative = Json.string(ref.get("path"));
        require(targets.add(relative), "duplicate executable target path");
        verifyHash(root, relative, Json.string(ref.get("sha256")));
        require(!Json.string(ref.get("symbol")).isBlank(), "target symbol missing");
        if (complete) require(ref.get("commit") instanceof String pin && pin.matches("[0-9a-f]{40}"), "target revision unresolved");
      }
      for (String resource : List.of("patch", "template")) if (recipe.get(resource + "_path") != null)
        verifyHash(root, Json.string(recipe.get(resource + "_path")), Json.string(recipe.get(resource + "_sha256")));
      if (status.equals("implemented_tested")) {
        List<Object> receipts = Json.array(mapping.get("evidence"));
        require(!receipts.isEmpty(), "tested claim has no receipt");
        for (Object receiptValue : receipts) {
          Map<String, Object> receiptRef = Json.object(receiptValue);
          Path file = verifyHash(root, Json.string(receiptRef.get("path")), Json.string(receiptRef.get("sha256")));
          Map<String, Object> receipt = Json.object(Json.parse(Files.readString(file)));
          require("pass".equals(receipt.get("result")), "receipt is not passing");
          Map<String, Object> covered = Json.object(receipt.get("target_hashes"));
          for (Object targetValue : Json.array(mapping.get("target_refs"))) {
            Map<String, Object> target = Json.object(targetValue);
            require(target.get("sha256").equals(covered.get(Json.string(target.get("path")))), "receipt does not cover this exact target");
          }
        }
      } else if (complete) require(false, "unverified executable mapping");
      if (complete) require(Json.array(mapping.getOrDefault("pending", List.of())).isEmpty(), "mandatory mapping gates remain");
    }
    // A new production file must acquire lineage in this same manifest, even outside known prefixes.
    Path port = root.resolve("m3/ports/indexstring/src");
    if (Files.exists(port)) try (var files = Files.walk(port)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        String name = root.relativize(file).toString().replace('\\', '/');
        require(targets.contains(name), "unmapped production addition: " + name);
      }
    }
    if (complete) {
      require("complete".equals(config.get("source_inventory_coverage")), "global inventory is incomplete");
      require("complete".equals(config.get("completion")), "three-route acceptance remains open");
    }
  }
  private static Path verifyHash(Path root, String name, String expected) throws Exception {
    require(expected.matches("[0-9a-f]{64}"), "invalid SHA-256");
    Path file = safe(root, name, true);
    require(Files.size(file) <= 16L * 1024 * 1024, "file exceeds validation budget");
    require(M3PortRecipe.hash(Files.readAllBytes(file)).equals(expected), "content drift: " + name);
    return file;
  }
  static Path safe(Path root, String name, boolean mustExist) throws Exception {
    require(!name.isEmpty() && !name.startsWith("/") && !name.contains("\\") && !name.contains(":"), "unsafe path");
    Path file = root;
    for (String part : name.split("/", -1)) {
      require(!part.isEmpty() && !Set.of(".", "..", ".git").contains(part), "unsafe component");
      file = file.resolve(part); require(!Files.isSymbolicLink(file), "symlink refused");
    }
    if (mustExist) require(Files.isRegularFile(file), "missing file: " + name);
    return file;
  }
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}
