/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
module opennlp.fixture.provider {
  requires opennlp.fixture.spi;
  provides fixture.spi.CustomProvider with fixture.provider.CustomEchoProvider;
}
