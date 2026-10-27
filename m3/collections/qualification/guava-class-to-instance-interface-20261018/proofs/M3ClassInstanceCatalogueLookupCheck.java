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
import com.google.common.collect.ClassToInstanceMap;
import com.m3.collections.M3CollectionSdkCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Check every emitted row and invoke the registered existing factory. */
public final class M3ClassInstanceCatalogueLookupCheck {
  private M3ClassInstanceCatalogueLookupCheck() { }
  public static void main(String[] arguments) throws Exception {
    if (arguments.length != 4) throw new IllegalArgumentException("catalogue, output, owner-output, artifact required");
    var lines=Files.readAllLines(Path.of(arguments[0]),StandardCharsets.UTF_8);
    require(lines.size()==108 && M3CollectionSdkCatalog.size()==107,"107 families");
    var families=M3CollectionSdkCatalog.families();
    for(int i=0;i<107;i++) {
      String[] c=lines.get(i+1).split("\t",-1);
      require(c.length==6,"six columns");
      var f=families.get(i);
      require(f.upstreamName().equals(c[0]) && f.vendor().name().equals(c[1]),"provider at "+i);
      require(f.existingM3Owner().equals(c[2].equals("-")?"":c[2]),"owner at "+i);
      require(f.candidateRole().equals(c[3]) && f.disposition().name().equals(c[4]),"role/state at "+i);
      require(f.semanticContract().equals(c[5]),"contract at "+i);
      require(M3CollectionSdkCatalog.find(c[0]).orElseThrow().equals(f),"exact lookup at "+i);
    }
    var registered=M3CollectionSdkCatalog.find("com.google.common.collect.ClassToInstanceMap").orElseThrow();
    require(registered.existingM3Owner().equals("com.m3.collections.interop.M3GuavaClassToInstanceMaps"),"existing factory");
    require(registered.disposition()==M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER,"optional adapter");
    Class<?> factory=Class.forName(registered.existingM3Owner());
    Object created=factory.getMethod("create").invoke(null);
    require(created instanceof ClassToInstanceMap<?>,"factory creates original interface");
    var bimap=M3CollectionSdkCatalog.find("com.google.common.collect.BiMap").orElseThrow();
    require(bimap.existingM3Owner().equals("com.m3.collections.interop.M3BiMap")
      && bimap.disposition()==M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER,"previous BiMap preserved");
    require(M3CollectionSdkCatalog.find("com.google.common.collect.ArrayListMultimap").orElseThrow()
      .disposition()==M3CollectionSdkCatalog.Disposition.PENDING,"native-fork qualifier stays separate");
    location(M3CollectionSdkCatalog.class,Path.of(arguments[1]));
    location(factory,Path.of(arguments[2]));
    location(ClassToInstanceMap.class,Path.of(arguments[3]));
    location(created.getClass(),Path.of(arguments[3]));
    System.out.println("CLASS_INSTANCE_CATALOGUE_LOOKUP_OK rows=107 factory_interface_verified=true previous_bimap_preserved=true");
  }
  private static void location(Class<?> type,Path expected) throws Exception {
    Path actual=Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    require(actual.equals(expected.toRealPath()),type.getName()+" code source "+actual);
  }
  private static void require(boolean condition,String message) {
    if(!condition)throw new AssertionError(message);
  }
}

