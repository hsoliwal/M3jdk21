// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reflection adapter over real compiled classes, not a new Java parser or equivalence proof. */
public final class SurfaceInventory {
  private SurfaceInventory() { }
  public static void main(String[] names) throws ClassNotFoundException {
    System.out.println("symbol\tkind\tsignature\tbridge\tsynthetic");
    for (String name : names) {
      Class<?> type = Class.forName(name, false, SurfaceInventory.class.getClassLoader());
      List<String[]> entries = new ArrayList<>();
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())) {
          entries.add(new String[]{name,"constructor",constructor.toGenericString(),"false",Boolean.toString(constructor.isSynthetic())});
        }
      }
      for (Method method : type.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
          entries.add(new String[]{name,"method",method.toGenericString(),Boolean.toString(method.isBridge()),Boolean.toString(method.isSynthetic())});
        }
      }
      entries.sort(Comparator.comparing(row -> row[2]));
      for (String[] entry : entries) System.out.println(String.join("\t",entry));
    }
  }
}
