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

/**
 * IIOPTarget - Union of Materialization Targets.
 *
 * <h2>Target Hierarchy</h2>
 *
 * <pre>
 * IIOPTarget
 * ├── SourceTarget     - .java source files
 * ├── BytecodeTarget   - .class compiled files
 * ├── ModuleTarget     - module-info.java
 * ├── ResourceTarget   - resources (properties, xml, yaml)
 * ├── TestTarget       - test source files
 * ├── ConfigTarget     - configuration files
 * ├── DocsTarget       - documentation files
 * └── TransientTarget  - memory/stdout (no persistence)
 * </pre>
 *
 * <h2>IOP v1 Materialization Contract</h2> <blockquote> "AI output must
 * materialize to a declared target. No invisible side effects. No hidden
 * artifacts." </blockquote>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPTarget metadata variants and the local signatures/defaults category, producesFiles, shouldVersionControl, getBaseDirectory, getFilePattern. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPTarget.java}
 * blob {@code 817174221c519bbf29623c7a909adae86e936abd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPTarget extends IIOPConcept
        permits IIOPTarget.SourceTarget,
                IIOPTarget.BytecodeTarget,
                IIOPTarget.ModuleTarget,
                IIOPTarget.ResourceTarget,
                IIOPTarget.TestTarget,
                IIOPTarget.ConfigTarget,
                IIOPTarget.DocsTarget,
                IIOPTarget.TransientTarget {

    /**
     * category metadata operation of IIOPTarget.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.TARGET</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.TARGET;
    }

    /**
     * Check if this target produces files.
     *
     * @return true if target creates files
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract producesFiles interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-1949
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean producesFiles();

    /**
     * Check if this target should be version controlled.
     *
     * @return true if should be committed to git
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract shouldVersionControl interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-2110
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean shouldVersionControl();

    /**
     * Get the base directory for this target.
     *
     * @param moduleName
     *            module name (null for root)
     * @return base directory path
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract getBaseDirectory interface signature declares a String result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-2323
     * @m3.dataStructures Abstract interface signature returning String. Implementations supply the result and its ownership behavior.
     */
    String getBaseDirectory(String moduleName);

    /**
     * Get file pattern for this target.
     *
     * @return file pattern (e.g., "*.java")
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract getFilePattern interface signature declares a String result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-2481
     * @m3.dataStructures Abstract interface signature returning String. Implementations supply the result and its ownership behavior.
     */
    String getFilePattern();

    // --- SOURCE TARGET ---

    /**
     * SourceTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied SourceTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String packagePath, String filename. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:packagePath} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The packagePath declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-2561; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored packagePath metadata in SourceTarget.
     * @m3.member {@code FIELD:filename} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The filename declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-2581; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored filename metadata in SourceTarget.
     */
    record SourceTarget(String packagePath, String filename) implements IIOPTarget {

        /**
         * name metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"SOURCE"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "SOURCE";
        }

        /**
         * description metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Java source file: " + packagePath + "/" + filename</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Java source file: " + packagePath + "/" + filename;
        }

        /**
         * producesFiles metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Uses src/ when the supplied module argument is null and src- plus the module argument plus / otherwise, then appends main/java. Empty names and separators are accepted without path validation.
         * @m3.dataStructures String module argument, one local prefix String and the concatenated directory result.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            String prefix = moduleName != null ? "src-" + moduleName + "/" : "src/";
            return prefix + "main/java";
        }

        /**
         * getFilePattern metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"*.java"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return "*.java";
        }

        /**
         * of metadata operation of SourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new SourceTarget(packagePath, filename)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared SourceTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static SourceTarget of(String packagePath, String filename) {
            return new SourceTarget(packagePath, filename);
        }
    }

    // --- BYTECODE TARGET ---

    /**
     * BytecodeTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied BytecodeTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String packagePath, String filename. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:packagePath} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The packagePath declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-3574; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored packagePath metadata in BytecodeTarget.
     * @m3.member {@code FIELD:filename} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The filename declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-3594; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored filename metadata in BytecodeTarget.
     */
    record BytecodeTarget(String packagePath, String filename) implements IIOPTarget {

        /**
         * name metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"BYTECODE"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "BYTECODE";
        }

        /**
         * description metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Compiled class: " + packagePath + "/" + filename</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Compiled class: " + packagePath + "/" + filename;
        }

        /**
         * producesFiles metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return false;
        }

        /**
         * getBaseDirectory metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"target/classes"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            return "target/classes";
        }

        /**
         * getFilePattern metadata operation of BytecodeTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"*.class"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return "*.class";
        }
    }

    // --- MODULE TARGET ---

    /**
     * ModuleTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ModuleTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String moduleName. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:moduleName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The moduleName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-4348; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored moduleName metadata in ModuleTarget.
     */
    record ModuleTarget(String moduleName) implements IIOPTarget {

        /**
         * name metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"MODULE"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "MODULE";
        }

        /**
         * description metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Module descriptor for: " + moduleName</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Module descriptor for: " + moduleName;
        }

        /**
         * producesFiles metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Uses src/ when the supplied module argument is null and src- plus the module argument plus / otherwise, then appends main/java. Empty names and separators are accepted without path validation.
         * @m3.dataStructures String module argument, one local prefix String and the concatenated directory result.
         */
        @Override
        public String getBaseDirectory(String mod) {
            String prefix = mod != null ? "src-" + mod + "/" : "src/";
            return prefix + "main/java";
        }

        /**
         * getFilePattern metadata operation of ModuleTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"module-info.java"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return "module-info.java";
        }
    }

    // --- RESOURCE TARGET ---

    /**
     * ResourceTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ResourceTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String path, ResourceType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:path} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The path declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-5171; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored path metadata in ResourceTarget.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored ResourceType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-5184; data.structures=One stored ResourceType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in ResourceTarget.
     */
    record ResourceTarget(String path, ResourceType type) implements IIOPTarget {
        /**
         * name metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"RESOURCE"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "RESOURCE";
        }

        /**
         * description metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Resource file: " + path + " (" + type + ")"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Resource file: " + path + " (" + type + ")";
        }

        /**
         * producesFiles metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Uses src/ when the supplied module argument is null and src- plus the module argument plus / otherwise, then appends main/resources. Empty names and separators are accepted without path validation.
         * @m3.dataStructures String module argument, one local prefix String and the concatenated directory result.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            String prefix = moduleName != null ? "src-" + moduleName + "/" : "src/";
            return prefix + "main/resources";
        }

        /**
         * getFilePattern metadata operation of ResourceTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.pattern()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return type.pattern();
        }

        /**
         * ResourceType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 6 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: PROPERTIES, XML, YAML, JSON, SQL, OTHER.
         * @m3.member {@code FIELD:PROPERTIES} pattern=synexia:iop; algorithm=Initializes PROPERTIES from the existing expression <code>new ResourceType("*.properties")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored PROPERTIES metadata in ResourceType.
         * @m3.member {@code FIELD:XML} pattern=synexia:iop; algorithm=Initializes XML from the existing expression <code>new ResourceType("*.xml")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored XML metadata in ResourceType.
         * @m3.member {@code FIELD:YAML} pattern=synexia:iop; algorithm=Initializes YAML from the existing expression <code>new ResourceType("*.{yml,yaml}")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored YAML metadata in ResourceType.
         * @m3.member {@code FIELD:JSON} pattern=synexia:iop; algorithm=Initializes JSON from the existing expression <code>new ResourceType("*.json")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored JSON metadata in ResourceType.
         * @m3.member {@code FIELD:SQL} pattern=synexia:iop; algorithm=Initializes SQL from the existing expression <code>new ResourceType("*.sql")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored SQL metadata in ResourceType.
         * @m3.member {@code FIELD:OTHER} pattern=synexia:iop; algorithm=Initializes OTHER from the existing expression <code>new ResourceType("*")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ResourceType field with its declared initializer.; description=Stored OTHER metadata in ResourceType.
         * @m3.member {@code FIELD:pattern} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The pattern declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-6198; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored pattern metadata in ResourceType.
         */
        public enum ResourceType {
            PROPERTIES("*.properties"),
            XML("*.xml"),
            YAML("*.{yml,yaml}"),
            JSON("*.json"),
            SQL("*.sql"),
            OTHER("*");

            private final String pattern;

            /**
             * ResourceType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied pattern references to pattern fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: pattern.
             */
            ResourceType(String pattern) {
                this.pattern = pattern;
            }

            /**
             * pattern metadata operation of ResourceType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored pattern field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String pattern() {
                return pattern;
            }
        }
    }

    // --- TEST TARGET ---

    /**
     * TestTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied TestTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String packagePath, String filename, boolean isResource. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:packagePath} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The packagePath declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-6478; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored packagePath metadata in TestTarget.
     * @m3.member {@code FIELD:filename} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The filename declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-6498; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored filename metadata in TestTarget.
     * @m3.member {@code FIELD:isResource} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The isResource declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-6515; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored isResource metadata in TestTarget.
     */
    record TestTarget(String packagePath, String filename, boolean isResource) implements IIOPTarget {

        /**
         * name metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the condition in <code>isResource ? "TEST_RESOURCE" : "TEST"</code> and returns the selected branch result, retaining the existing boolean selection and literal values.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return isResource ? "TEST_RESOURCE" : "TEST";
        }

        /**
         * description metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the condition in <code>"Test " + (isResource ? "resource" : "source") + ": " + packagePath + "/" + filename</code> and returns the selected branch result, retaining the existing boolean selection and literal values.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Test " + (isResource ? "resource" : "source") + ": " + packagePath + "/" + filename;
        }

        /**
         * producesFiles metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Uses src/ when the supplied module argument is null and src- plus the module argument plus / otherwise, then appends test/ and resources or java according to isResource.
         * @m3.dataStructures String module argument, one local prefix String and the concatenated directory result.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            String prefix = moduleName != null ? "src-" + moduleName + "/" : "src/";
            return prefix + "test/" + (isResource ? "resources" : "java");
        }

        /**
         * getFilePattern metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the condition in <code>isResource ? "*" : "*Test.java"</code> and returns the selected branch result, retaining the existing boolean selection and literal values.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return isResource ? "*" : "*Test.java";
        }

        /**
         * source metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new TestTarget(packagePath, filename, false)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared TestTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static TestTarget source(String packagePath, String filename) {
            return new TestTarget(packagePath, filename, false);
        }

        /**
         * resource metadata operation of TestTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new TestTarget(path, filename, true)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared TestTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static TestTarget resource(String path, String filename) {
            return new TestTarget(path, filename, true);
        }
    }

    // --- CONFIG TARGET ---

    /**
     * ConfigTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ConfigTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String filename, ConfigType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:filename} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The filename declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-7775; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored filename metadata in ConfigTarget.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored ConfigType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-7792; data.structures=One stored ConfigType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in ConfigTarget.
     */
    record ConfigTarget(String filename, ConfigType type) implements IIOPTarget {
        /**
         * name metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"CONFIG"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "CONFIG";
        }

        /**
         * description metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Configuration: " + filename + " (" + type + ")"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Configuration: " + filename + " (" + type + ")";
        }

        /**
         * producesFiles metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"."</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            return ".";
        }

        /**
         * getFilePattern metadata operation of ConfigTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.pattern()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return type.pattern();
        }

        /**
         * ConfigType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 5 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: MAVEN, GRADLE, APPLICATION, LOGGING, OTHER.
         * @m3.member {@code FIELD:MAVEN} pattern=synexia:iop; algorithm=Initializes MAVEN from the existing expression <code>new ConfigType("pom.xml")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ConfigType field with its declared initializer.; description=Stored MAVEN metadata in ConfigType.
         * @m3.member {@code FIELD:GRADLE} pattern=synexia:iop; algorithm=Initializes GRADLE from the existing expression <code>new ConfigType("*.gradle")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ConfigType field with its declared initializer.; description=Stored GRADLE metadata in ConfigType.
         * @m3.member {@code FIELD:APPLICATION} pattern=synexia:iop; algorithm=Initializes APPLICATION from the existing expression <code>new ConfigType("application.{yml,yaml,properties}")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ConfigType field with its declared initializer.; description=Stored APPLICATION metadata in ConfigType.
         * @m3.member {@code FIELD:LOGGING} pattern=synexia:iop; algorithm=Initializes LOGGING from the existing expression <code>new ConfigType("log*.xml")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ConfigType field with its declared initializer.; description=Stored LOGGING metadata in ConfigType.
         * @m3.member {@code FIELD:OTHER} pattern=synexia:iop; algorithm=Initializes OTHER from the existing expression <code>new ConfigType("*")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ConfigType field with its declared initializer.; description=Stored OTHER metadata in ConfigType.
         * @m3.member {@code FIELD:pattern} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The pattern declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-8700; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored pattern metadata in ConfigType.
         */
        public enum ConfigType {
            MAVEN("pom.xml"),
            GRADLE("*.gradle"),
            APPLICATION("application.{yml,yaml,properties}"),
            LOGGING("log*.xml"),
            OTHER("*");

            private final String pattern;

            /**
             * ConfigType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied pattern references to pattern fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: pattern.
             */
            ConfigType(String pattern) {
                this.pattern = pattern;
            }

            /**
             * pattern metadata operation of ConfigType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored pattern field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String pattern() {
                return pattern;
            }
        }
    }

    // --- DOCS TARGET ---

    /**
     * DocsTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied DocsTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String path, DocsType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:path} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The path declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-8978; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored path metadata in DocsTarget.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored DocsType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-8991; data.structures=One stored DocsType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in DocsTarget.
     */
    record DocsTarget(String path, DocsType type) implements IIOPTarget {
        /**
         * name metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"DOCS"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "DOCS";
        }

        /**
         * description metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Documentation: " + path + " (" + type + ")"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Documentation: " + path + " (" + type + ")";
        }

        /**
         * producesFiles metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return true;
        }

        /**
         * shouldVersionControl metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return true;
        }

        /**
         * getBaseDirectory metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"docs"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            return "docs";
        }

        /**
         * getFilePattern metadata operation of DocsTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.pattern()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return type.pattern();
        }

        /**
         * DocsType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 5 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: MARKDOWN, JAVADOC, PLANTUML, MERMAID, OTHER.
         * @m3.member {@code FIELD:MARKDOWN} pattern=synexia:iop; algorithm=Initializes MARKDOWN from the existing expression <code>new DocsType("*.md")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DocsType field with its declared initializer.; description=Stored MARKDOWN metadata in DocsType.
         * @m3.member {@code FIELD:JAVADOC} pattern=synexia:iop; algorithm=Constructs the JAVADOC enum singleton using the original recursive double-asterisk slash asterisk dot-java wildcard literal, preserving that stored pattern reference.; data.structures=One DocsType field with its declared initializer.; description=Stored JAVADOC metadata in DocsType.
         * @m3.member {@code FIELD:PLANTUML} pattern=synexia:iop; algorithm=Initializes PLANTUML from the existing expression <code>new DocsType("*.puml")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DocsType field with its declared initializer.; description=Stored PLANTUML metadata in DocsType.
         * @m3.member {@code FIELD:MERMAID} pattern=synexia:iop; algorithm=Initializes MERMAID from the existing expression <code>new DocsType("*.mmd")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DocsType field with its declared initializer.; description=Stored MERMAID metadata in DocsType.
         * @m3.member {@code FIELD:OTHER} pattern=synexia:iop; algorithm=Initializes OTHER from the existing expression <code>new DocsType("*")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DocsType field with its declared initializer.; description=Stored OTHER metadata in DocsType.
         * @m3.member {@code FIELD:pattern} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The pattern declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-9861; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored pattern metadata in DocsType.
         */
        public enum DocsType {
            MARKDOWN("*.md"),
            JAVADOC("**/*.java"),
            PLANTUML("*.puml"),
            MERMAID("*.mmd"),
            OTHER("*");

            private final String pattern;

            /**
             * DocsType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied pattern references to pattern fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: pattern.
             */
            DocsType(String pattern) {
                this.pattern = pattern;
            }

            /**
             * pattern metadata operation of DocsType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored pattern field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String pattern() {
                return pattern;
            }
        }
    }

    // --- TRANSIENT TARGET ---

    /**
     * TransientTarget metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied TransientTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: TransientType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored TransientType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPTarget-10147; data.structures=One stored TransientType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in TransientTarget.
     */
    record TransientTarget(TransientType type) implements IIOPTarget {
        /**
         * name metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Transient target: " + type</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Transient target: " + type;
        }

        /**
         * producesFiles metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean producesFiles() {
            return false;
        }

        /**
         * shouldVersionControl metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public boolean shouldVersionControl() {
            return false;
        }

        // TODO: return null — @Override interface method
        /**
         * getBaseDirectory metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns null directly, retaining the transient target no-path/no-pattern contract.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getBaseDirectory(String moduleName) {
            return null;
        }

        // TODO: return null — @Override interface method
        /**
         * getFilePattern metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns null directly, retaining the transient target no-path/no-pattern contract.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String getFilePattern() {
            return null;
        }

        /**
         * TransientType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 3 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: MEMORY, STDOUT, REGISTRY.
         * @m3.member {@code FIELD:MEMORY} pattern=synexia:iop; algorithm=Initializes MEMORY from the existing expression <code>new TransientType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TransientType field with its declared initializer.; description=Stored MEMORY metadata in TransientType.
         * @m3.member {@code FIELD:STDOUT} pattern=synexia:iop; algorithm=Initializes STDOUT from the existing expression <code>new TransientType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TransientType field with its declared initializer.; description=Stored STDOUT metadata in TransientType.
         * @m3.member {@code FIELD:REGISTRY} pattern=synexia:iop; algorithm=Initializes REGISTRY from the existing expression <code>new TransientType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TransientType field with its declared initializer.; description=Stored REGISTRY metadata in TransientType.
         */
        public enum TransientType {
            MEMORY,
            STDOUT,
            REGISTRY
        }

        /**
         * memory metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new TransientTarget(TransientType.MEMORY)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared TransientTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static TransientTarget memory() {
            return new TransientTarget(TransientType.MEMORY);
        }

        /**
         * stdout metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new TransientTarget(TransientType.STDOUT)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared TransientTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static TransientTarget stdout() {
            return new TransientTarget(TransientType.STDOUT);
        }

        /**
         * registry metadata operation of TransientTarget.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new TransientTarget(TransientType.REGISTRY)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared TransientTarget result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static TransientTarget registry() {
            return new TransientTarget(TransientType.REGISTRY);
        }
    }
}
