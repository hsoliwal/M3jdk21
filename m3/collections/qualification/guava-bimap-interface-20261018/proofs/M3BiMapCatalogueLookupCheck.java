/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
import com.google.common.collect.BiMap;
import com.m3.collections.M3CollectionSdkCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Compare the executed generated lookup with every source-data row and real interface owner. */
public final class M3BiMapCatalogueLookupCheck {
  private M3BiMapCatalogueLookupCheck() { }

  public static void main(String[] arguments) throws Exception {
    if (arguments.length != 4) throw new IllegalArgumentException("catalogue, output, owner-output, artifact required");
    var lines = Files.readAllLines(Path.of(arguments[0]), StandardCharsets.UTF_8);
    require(lines.size() == 108, "one header plus 107 source rows");
    require(M3CollectionSdkCatalog.size() == 107, "107 generated families");
    var families = M3CollectionSdkCatalog.families();
    for (int i = 0; i < 107; i++) {
      String[] columns = lines.get(i + 1).split("\t", -1);
      require(columns.length == 6, "six source columns");
      var family = families.get(i);
      require(family.upstreamName().equals(columns[0]), "provider name at " + i);
      require(family.vendor().name().equals(columns[1]), "vendor at " + i);
      require(family.existingM3Owner().equals(columns[2].equals("-") ? "" : columns[2]), "owner at " + i);
      require(family.candidateRole().equals(columns[3]), "candidate role at " + i);
      require(family.disposition().name().equals(columns[4]), "disposition at " + i);
      require(family.semanticContract().equals(columns[5]), "semantic contract at " + i);
      require(M3CollectionSdkCatalog.find(columns[0]).orElseThrow().equals(family), "exact lookup at " + i);
    }
    var registered = M3CollectionSdkCatalog.find("com.google.common.collect.BiMap").orElseThrow();
    require(registered.hasExistingM3Owner(), "existing owner is registered");
    require(registered.existingM3Owner().equals("com.m3.collections.interop.M3BiMap"), "reuse existing owner");
    require(registered.disposition() == M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER, "optional dependency classification");
    Class<?> owner = Class.forName(registered.existingM3Owner());
    require(BiMap.class.isAssignableFrom(owner), "actual owner implements the original BiMap interface");
    require(M3CollectionSdkCatalog.find("com.google.common.collect.ArrayListMultimap").orElseThrow()
        .disposition() == M3CollectionSdkCatalog.Disposition.PENDING, "native-fork qualifier stays separate");
    location(M3CollectionSdkCatalog.class, Path.of(arguments[1]));
    location(owner, Path.of(arguments[2]));
    location(BiMap.class, Path.of(arguments[3]));
    System.out.println("BIMAP_CATALOGUE_LOOKUP_OK rows=107 existing_owner_verified=true");
  }

  private static void location(Class<?> type, Path expected) throws Exception {
    Path actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    require(actual.equals(expected.toRealPath()), type.getName() + " code source " + actual);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
