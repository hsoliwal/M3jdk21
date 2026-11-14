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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IMapperSpec - Object mapping specification for AI code generation.
 *
 * <h2>Mapping Patterns</h2>
 *
 * <pre>
 * Mapping libraries:
 *   - MapStruct (compile-time)
 *   - ModelMapper (runtime)
 *   - Dozer
 *   - Orika
 *   - JMapper
 *
 * &#64;IMapperSpec specifies object mapping in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IMapperSpec(
 *     source = UserEntity.class,
 *     target = UserDTO.class,
 *     mappings = {
 *         &#64;IMapperSpec.Mapping(source = "id", target = "userId"),
 * &#64;IMapperSpec.Mapping(source = "createdAt", target = "created", format = "yyyy-MM-dd")})
 * public interface IUserMapper&lt;Self extends IUserMapper&lt;Self>> extends IContext&lt;Self> {
 *
 * 	UserDTO toDto(UserEntity entity);
 *
 * 	UserEntity toEntity(UserDTO dto);
 *
 * 	List&lt;UserDTO> toDtoList(List&lt;UserEntity> entities);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Mapper Specification
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares source/target types, embedded field mappings, null and collection handling and generation options; it constructs no mapper and transforms no object.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IMapperSpec.java}
 * blob {@code 0e9df88ef176da0aff4449e6d2403e5f6fcbcb3c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IMapperSpec {

    // --- SOURCE & TARGET ---

    /**
     * Requested source object type; Void.class is preserved as the unspecified sentinel.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#source intent: Requested source object type; Void.class is preserved as the unspecified sentinel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> source() default Void.class;

    /**
     * Requested target object type; Void.class is preserved as the unspecified sentinel.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#target intent: Requested target object type; Void.class is preserved as the unspecified sentinel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> target() default Void.class;

    // --- FIELD MAPPINGS ---

    /**
     * Field-mapping specifications in supplied order.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#mappings intent: Field-mapping specifications in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Mapping[]}; default expression {@code {}}.
     */
    Mapping[] mappings() default {};

    /**
     * Embedded source-to-target mapping with formatting, default, ignore and converter hints; it transforms no value and retains implicit CLASS retention without explicit Target restriction.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Embedded source-to-target mapping with formatting, default, ignore and converter hints; it transforms no value and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Mapping {

        /**
         * Source field or property selector.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#source intent: Source field or property selector. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String source();

        /**
         * Target field or property selector.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#target intent: Target field or property selector. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String target();

        /**
         * Requested formatting string; no format language is defined by the schema.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#format intent: Requested formatting string; no format language is defined by the schema. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String format() default "";

        /**
         * Requested default value encoded as a string.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#defaultValue intent: Requested default value encoded as a string. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String defaultValue() default "";

        /**
         * Whether this mapping should be ignored.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#ignore intent: Whether this mapping should be ignored. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean ignore() default false;

        /**
         * Requested converter type; the Class wildcard imposes no converter interface at compile time.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IMapperSpec.Mapping#converter intent: Requested converter type; the Class wildcard imposes no converter interface at compile time. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
         */
        Class<?> converter() default Void.class;
    }

    // --- OPTIONS ---

    /**
     * Requested behavior for null source values.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#nullStrategy intent: Requested behavior for null source values. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code NullStrategy}; default expression {@code NullStrategy.SET_NULL}.
     */
    NullStrategy nullStrategy() default NullStrategy.SET_NULL;

    /**
     * Null handling choices; the enum performs no mapping.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IMapperSpec.NullStrategy choices described as Null handling choices; the enum performs no mapping. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum NullStrategy {
        /**
         * Requested assigning null; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.NullStrategy#SET_NULL policy choice: Requested assigning null; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.NullStrategy; its declared order and identity are preserved.
         */
        SET_NULL,
        /**
         * Requested ignoring null values; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.NullStrategy#IGNORE policy choice: Requested ignoring null values; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.NullStrategy; its declared order and identity are preserved.
         */
        IGNORE,
        /**
         * Requested throwing for null values; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.NullStrategy#THROW policy choice: Requested throwing for null values; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.NullStrategy; its declared order and identity are preserved.
         */
        THROW
    }

    /**
     * Requested collection-access/update style.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#collectionStrategy intent: Requested collection-access/update style. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CollectionStrategy}; default expression {@code CollectionStrategy.ACCESSOR}.
     */
    CollectionStrategy collectionStrategy() default CollectionStrategy.ACCESSOR;

    /**
     * Collection update choices; the enum performs no mapping.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IMapperSpec.CollectionStrategy choices described as Collection update choices; the enum performs no mapping. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum CollectionStrategy {
        /**
         * Requested collection accessor use; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.CollectionStrategy#ACCESSOR policy choice: Requested collection accessor use; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.CollectionStrategy; its declared order and identity are preserved.
         */
        ACCESSOR,
        /**
         * Requested element adder use; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.CollectionStrategy#ADDER policy choice: Requested element adder use; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.CollectionStrategy; its declared order and identity are preserved.
         */
        ADDER,
        /**
         * Requested collection setter use; consuming tooling defines the implementation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMapperSpec.CollectionStrategy#SETTER policy choice: Requested collection setter use; consuming tooling defines the implementation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMapperSpec.CollectionStrategy; its declared order and identity are preserved.
         */
        SETTER
    }

    // --- GENERATION ---

    /**
     * Whether inverse mapping generation is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#generateInverse intent: Whether inverse mapping generation is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean generateInverse() default true;

    /**
     * Whether list mapping methods are requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#generateListMethods intent: Whether list mapping methods are requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean generateListMethods() default true;

    /**
     * Requested component-model identifier; no container integration is established by this string.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#componentModel intent: Requested component-model identifier; no container integration is established by this string. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "default"}.
     */
    String componentModel() default "default";

    // --- DESCRIPTION ---

    /**
     * Natural-language object-mapping specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#value intent: Natural-language object-mapping specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMapperSpec#model intent: Preferred generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
