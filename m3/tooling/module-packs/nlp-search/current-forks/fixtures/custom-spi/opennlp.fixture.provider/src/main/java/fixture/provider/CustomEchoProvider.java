/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package fixture.provider;

import fixture.spi.CustomProvider;
import opennlp.tools.util.ext.ProviderSpec;

/** An application provider used only to qualify generic module service lookup. */
public final class CustomEchoProvider implements CustomProvider {
  public CustomEchoProvider() {
  }

  @Override
  public String name() {
    return "custom-echo";
  }

  @Override
  public boolean supports(final ProviderSpec spec) {
    if (spec == null) {
      throw new IllegalArgumentException("spec must not be null");
    }
    return spec.location().isEmpty() && spec.hasOnlyOptions("value");
  }

  @Override
  public String create(final ProviderSpec spec) {
    if (!supports(spec)) {
      throw new IllegalArgumentException("unsupported fixture spec");
    }
    return "custom:" + spec.option("value", "default");
  }
}
