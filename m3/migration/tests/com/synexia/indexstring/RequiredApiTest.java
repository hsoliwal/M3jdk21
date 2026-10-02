// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

/** Red/green probe for additive APIs; compiles against the original source owners. */
public final class RequiredApiTest {
  private RequiredApiTest() {}
  public static void main(String[] args) throws Exception {
    require(FrozenChars.class, "fromString", String.class);
    require(MIndexJoinedChars.class, "ofRetained", FrozenChars[].class);
    require(MIndexJoinedChars.class, "retainedParts");
    require(MIndexJoinedChars.class, "stringHash");
    require(MIndexJoinedChars.class, "copyTo", int.class, char[].class, int.class, int.class);
    System.out.println("REQUIRED_API 5 additive APIs present");
  }
  private static void require(Class<?> owner, String name, Class<?>... parameters) {
    try { owner.getMethod(name, parameters); }
    catch (NoSuchMethodException missing) {
      throw new AssertionError("Missing required additive API: " + owner.getName() + "." + name, missing);
    }
  }
}
