/* ******************************************************************************
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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIDelegate - AnOP annotation for delegate injection point in decorators.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Delegate → injection point for decorated object ║
 * ║ AnOP: @AIDelegate → compile-time delegate declaration ║
 * ║ ║
 * ║ CDI says: "Inject the decorated instance here" ║
 * ║ AnOP says: "This field holds the delegate" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT inject delegates. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIDecorator(decorates = IUserService.class)
 *     public class CachingUserServiceDecorator implements IUserService {
 *
 *         &#64;AIDelegate
 *         &#64;AIInject
 *         private IUserService delegate; // The actual service
 *
 *         private final Map&lt;String, User> cache = new ConcurrentHashMap&lt;>();
 *
 *         &#64;Override
 *         public User findById(String id) {
 *             return cache.computeIfAbsent(id, delegate::findById);
 *         }
 *
 *         &#64;Override
 *         public void save(User user) {
 *             cache.remove(user.getId());
 *             delegate.save(user); // Delegate to actual service
 *         }
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Delegate Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @see AIDecorator
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIDelegate.java}
 * blob {@code 186696ad0eb80e7c03ac40632a0cc39e97d4c131} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIDelegate {}
