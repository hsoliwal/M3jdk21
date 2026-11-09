/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Prepared common regex classes preserve public String.matches semantics and exact owner/range facts
 * @modules java.base/java.lang:open java.management jdk.management
 * @build M3CommonRegexProductTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CommonRegexProductTest candidate acceptance
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3CommonRegexProductTest candidate acceptance
 */
