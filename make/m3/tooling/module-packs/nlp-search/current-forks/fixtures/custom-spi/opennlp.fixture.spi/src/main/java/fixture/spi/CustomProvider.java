/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package fixture.spi;

import opennlp.tools.util.ext.Provider;

/** A caller-defined SPI, unknown to the donor module descriptor. */
public interface CustomProvider extends Provider<String> {
}
