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
import java.nio.file.Path;

/** Bind existing interface tests to the qualified native artifact and actual receiver owner. */
public final class M3BiMapQualificationLauncher {
  private M3BiMapQualificationLauncher() { }

  public static void main(String[] arguments) throws Exception {
    if (arguments.length != 2) throw new IllegalArgumentException("output-directory and native-jar required");
    Path output = Path.of(arguments[0]).toRealPath();
    Path jar = Path.of(arguments[1]).toRealPath();
    location("com.m3.collections.interop.M3BiMap", output);
    location("com.m3.collections.interop.M3BiMaps", output);
    location("com.google.common.collect.HashBiMap", jar);
    location("com.google.common.collect.ArrayListMultimap", jar);
    location("com.m3.collections.M3LinkedHashMap", jar);
    location("com.m3.collections.M3ArrayList", jar);
    CollectionTestLauncher.main(new String[] {"com.m3.collections.interop.M3BiMapTest", "22"});
    System.out.println("BIMAP_EXISTING_OWNER_QUALIFIED original_interface=Guava33.4.8 native_artifact_verified=true");
  }

  private static void location(String name, Path expected) throws Exception {
    Class<?> type = Class.forName(name);
    Path actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    if (!expected.equals(actual)) throw new AssertionError(name + " loaded from " + actual);
    System.out.println("BIMAP_CODE_SOURCE " + name + " " + actual);
  }
}
