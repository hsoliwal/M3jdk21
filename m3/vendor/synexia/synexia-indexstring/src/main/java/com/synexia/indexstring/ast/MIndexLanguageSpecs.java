// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

/** Process-level registry of frozen language specification images. */
public final class MIndexLanguageSpecs {
  private MIndexLanguageSpecs() {}

  public static MIndexLanguageSpec java21() {
    return MIndexJava21LanguageSpec.INSTANCE;
  }

  public static MIndexLanguageSpec forLanguage(String language, int release) {
    if ("java".equalsIgnoreCase(language) && release == 21) return java21();
    throw new IllegalArgumentException("unsupported frozen language spec: " + language + "-" + release);
  }
}
