/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package fixture.probe;

import java.io.IOException;
import java.util.Map;

import fixture.spi.CustomProvider;
import opennlp.tools.util.ext.ProviderSpec;
import opennlp.tools.util.ext.Providers;

/** Qualifies the generic Providers seam against the actual named donor module. */
public final class CustomSpiProbe {
  private CustomSpiProbe() {
  }

  public static void main(final String[] args) throws IOException {
    final Module owner = Providers.class.getModule();
    check(owner.isNamed() && "org.apache.opennlp.tools".equals(owner.getName()),
        "the donor must be the linked named module");
    check(!owner.getDescriptor().uses().contains(CustomProvider.class.getName()),
        "the fixture SPI must be absent from the donor descriptor");
    check(!owner.canUse(CustomProvider.class), "the fixture SPI must begin unadmitted");
    final ClassLoader loader = CustomProvider.class.getClassLoader();
    final ProviderSpec spec = ProviderSpec.of(Map.of("value", "module"));

    final Providers<CustomProvider> providers =
        Providers.of(CustomProvider.class, loader, key -> null);
    check(providers.installed().size() == 1, "one resolved provider must be installed");
    check("custom:module".equals(providers.select(spec).create(spec)),
        "the real generic resolver must invoke the fixture provider");
    check(providers.byName("custom-echo").isPresent(), "name resolution must work");
    check(owner.canUse(CustomProvider.class), "the owner must admit the runtime SPI");
    check(!owner.getDescriptor().uses().contains(CustomProvider.class.getName()),
        "runtime admission must leave the descriptor unchanged");

    final Providers<CustomProvider> repeated =
        Providers.of(CustomProvider.class, loader, key -> null);
    check("custom:module".equals(repeated.select(spec).create(spec)),
        "lookup must also work after admission");

    final Providers<CustomProvider> untrusted =
        Providers.of(CustomProvider.class, loader, key -> null, type -> false);
    check(untrusted.installed().isEmpty(), "the existing trust filter must still apply");
    final String disabledKey = Providers.CONFIGURATION_KEY_PREFIX
        + CustomProvider.class.getName() + Providers.DISABLED_KEY_SUFFIX;
    final Providers<CustomProvider> disabled = Providers.of(CustomProvider.class, loader,
        key -> disabledKey.equals(key) ? "fixture.provider.CustomEchoProvider" : null);
    check(disabled.installed().isEmpty(), "the existing disabled-class filter must still apply");
    System.out.println("OPENNLP_CUSTOM_SPI_MODULE_PASS");
  }

  private static void check(final boolean condition, final String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
