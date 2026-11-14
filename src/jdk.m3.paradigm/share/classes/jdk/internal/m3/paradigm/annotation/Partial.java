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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Partial - Marks a method as a partial method in COP layer.
 *
 * <h2>ContextJ 2.0 - Partial Methods</h2>
 * <p>
 * A partial method provides behavior that applies only when its owning layer is
 * active. This is the core mechanism of COP.
 *
 * <h2>Execution Model</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PARTIAL METHOD EXECUTION                                               │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  @Partial(target = Service.class, method = "execute")                   │
 * │  public Result execute(IProceed&lt;Result> proceed, Request req) {          │
 * │      log("Before");           ← before advice                           │
 * │      Result r = proceed.call(); ← call next layer/base                  │
 * │      log("After");            ← after advice                            │
 * │      return r;                                                          │
 * │  }                                                                      │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class LoggingLayer extends AbstractLayer&lt;LoggingLayer> {
 *
 * 		&#64;Partial(target = Repository.class, method = "save")
 * 		public void save(IProceed&lt;Void> proceed, Entity entity) {
 * 			log.info("Saving: { }", entity);
 * 			proceed.call();
 * 			log.info("Saved: { }", entity);
 * 		}
 *
 * 		{@literal @}Partial(target = Repository.class, method = "find")
 * 		public Entity find(IProceed&lt;Entity> proceed, Long id) {
 * 			log.info("Finding: { }", id);
 * 			Entity result = proceed.call();
 * 			log.info("Found: { }", result);
 * 			return result;
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Method Signature</h2>
 * <p>
 * Partial methods must have:
 * <ol>
 * <li>First parameter: {@code IProceed<R>} for calling next layer</li>
 * <li>Remaining parameters: Same as target method</li>
 * <li>Return type: Same as target method</li>
 * </ol>
 *
 * <h2>AnOP Compliance</h2>
 * <p>
 * SOURCE retention - compile-time code generation creates the layer dispatch
 * wrapper. No runtime annotation processing.
 *
 * @since ContextOS 1.0 - ContextJ 2.0
 * See also: jdk.internal.m3.context.layer.ILayer (resolved in module that provides this type)
 * See also: jdk.internal.m3.context.layer.IPartialMethod (resolved in module that provides this type)
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Partial.java}
 * blob {@code 2220ea3262fb5e3385d3a7c9d5d553eecbdd5775} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Partial {

    /**
     * Target class this partial method applies to.
     *
     * @return target class
     */
    Class<?> target();

    /**
     * Target method name. Empty string means same as annotated method name.
     *
     * @return method name
     */
    String method() default "";

    /**
     * Priority within this layer (for multiple partials on same method).
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Condition expression (JEXL) for selective application. Empty string means
     * always apply.
     *
     * @return condition expression
     */
    String when() default "";
}
