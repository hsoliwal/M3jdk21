// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceInventoryTest {
  private SourceInventoryTest() {}
  public static void main(String[] args) throws Exception {
    Path root = Files.createTempDirectory("m3-inventory-");
    Files.writeString(root.resolve("Owner.java"), "package p; public class Owner { public char value(int i) { return 0; } static class Inner {} }");
    Files.writeString(root.resolve("nonprefix-resource.txt"), "resource");
    var first = SourceInventory.scan(root);
    if (!Json.write(first).equals(Json.write(SourceInventory.scan(root)))) throw new AssertionError("nondeterministic inventory");
    if (Json.array(first.get("files")).size() != 2) throw new AssertionError("resource omitted");
    String text = Json.write(first);
    if (!text.contains("p.Owner.Inner") || !text.contains("value")) throw new AssertionError("declarations missing");
    Files.writeString(root.resolve("Bad.java"), "class Bad { void broken( }");
    if (!Json.write(SourceInventory.scan(root)).contains("blocked_syntax_diagnostics")) throw new AssertionError("syntax error hidden");
    Path outside = Files.createTempDirectory("m3-inventory-link-");
    Files.createSymbolicLink(root.resolve("linked-dir"), outside);
    boolean refused = false;
    try { SourceInventory.scan(root); } catch (IllegalArgumentException expected) { refused = true; }
    if (!refused) throw new AssertionError("directory symlink was silently omitted");
    System.out.println("SOURCE_INVENTORY 5 checks passed");
  }
}
