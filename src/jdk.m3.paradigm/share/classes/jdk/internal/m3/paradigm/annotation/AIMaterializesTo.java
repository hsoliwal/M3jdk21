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
 * AIMaterializesTo - Declares where AI-generated artifacts are stored.
 *
 * <h2>IOP v1 Materialization Model</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AIMaterializesTo - EXPLICIT OUTPUT DECLARATION                          ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "AI output must materialize to a declared target.                        ║
 * ║   No invisible side effects. No hidden artifacts."                        ║
 * ║                                                                           ║
 * ║  This annotation declares where generated code/artifacts will be stored.  ║
 * ║  AI MUST NOT write to any location not declared in this annotation.       ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Source code only
 * 	&#64;AIMaterializesTo(MaterializationTarget.SOURCE)
 * 	&#64;AIGenClass("User service")
 * 	interface IUserService {
 *
 * 		User findById(long id);
 * 	}
 *
 * 	// Source + tests
 * 	&#64;AIMaterializesTo({MaterializationTarget.SOURCE, MaterializationTarget.TEST})
 * 	&#64;AIGenClass("Tested service")
 * 	interface ITestedService {
 *
 * 		void process();
 * 	}
 *
 * 	// Source + documentation
 * 	&#64;AIMaterializesTo({MaterializationTarget.SOURCE, MaterializationTarget.DOCS})
 * 	&#64;AIGenClass("Documented service")
 * 	interface IDocumentedService {
 *
 * 		void serve();
 * 	}
 *
 * 	// Memory only (dry-run/preview)
 * 	&#64;AIMaterializesTo(MaterializationTarget.MEMORY)
 * 	&#64;AIGenClass("Preview service")
 * 	interface IPreviewService {
 *
 * 		void preview();
 * 	}
 * }
 * </pre>
 *
 * <h2>Target Combinations</h2>
 *
 * <pre>
 * Common patterns:
 *
 * Standard generation:
 *   &#64;AIMaterializesTo(SOURCE)
 *
 * With tests:
 *   &#64;AIMaterializesTo({SOURCE, TEST})
 *
 * Full documentation:
 *   &#64;AIMaterializesTo({SOURCE, TEST, DOCS})
 *
 * Resource generation:
 *   &#64;AIMaterializesTo({SOURCE, RESOURCE})
 *
 * Dry-run (no files):
 *   &#64;AIMaterializesTo(MEMORY)
 * </pre>
 *
 * <h2>Security</h2>
 * <p>
 * This annotation acts as a security boundary. AI cannot write outside the
 * declared targets. This prevents:
 * </p>
 * <ul>
 * <li>Accidental modification of unrelated files</li>
 * <li>Writing to protected directories</li>
 * <li>Hidden side effects</li>
 * </ul>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see MaterializationTarget
 * @see IOPOperation
 * @see AIGenClass
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIMaterializesTo.java}
 * blob {@code 487be83459c36630cad4b292a107d727cd6f789f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIMaterializesTo {

    /**
     * The materialization targets for generated artifacts.
     *
     * <p>
     * AI MUST write only to these targets. Writing to any other location is a
     * security violation.
     * </p>
     *
     * @return array of materialization targets
     */
    MaterializationTarget[] value();

    /**
     * Whether to inherit parent's target list.
     *
     * <p>
     * If true, method-level @AIMaterializesTo adds to class-level list. If false,
     * method-level @AIMaterializesTo replaces class-level list.
     * </p>
     *
     * @return true to inherit (default: true)
     */
    boolean inherit() default true;

    /**
     * Whether to create backup before overwriting existing files.
     *
     * @return true to enable backup (default: true)
     */
    boolean backup() default true;

    /**
     * Backup directory relative to project root.
     *
     * @return backup directory (default: ".iop-backup")
     */
    String backupDir() default ".iop-backup";

    /**
     * Whether to fail if target file already exists.
     *
     * <p>
     * If true, AI will fail rather than overwrite. If false, AI will overwrite
     * (with backup if enabled).
     * </p>
     *
     * @return true to prevent overwrite (default: false)
     */
    boolean preventOverwrite() default false;

    /**
     * Custom output directory (overrides default from target).
     *
     * <p>
     * If specified, files are written to this directory instead of the default
     * directory for the target type.
     * </p>
     *
     * @return custom output directory (empty = use default)
     */
    String outputDir() default "";

    /**
     * File naming pattern for generated files.
     *
     * <p>
     * Supports placeholders:
     * </p>
     * <ul>
     * <li>{interface} - Interface name (e.g., "IUserService")</li>
     * <li>{class} - Generated class name (e.g., "UserServiceImpl")</li>
     * <li>{package} - Package path (e.g., "com/example/service")</li>
     * <li>{timestamp} - ISO timestamp</li>
     * <li>{hash} - Short spec hash</li>
     * </ul>
     *
     * @return naming pattern (empty = default convention)
     */
    String namingPattern() default "";

    /**
     * Whether to add generation metadata to output files.
     *
     * <p>
     * If true, generated files include:
     * </p>
     * <ul>
     * <li>@AIGenerated annotation</li>
     * <li>@SpecHash annotation</li>
     * <li>Generation timestamp in JavaDoc</li>
     * </ul>
     *
     * @return true to include metadata (default: true)
     */
    boolean includeMetadata() default true;

    /**
     * Maximum file size in bytes for generated output.
     *
     * <p>
     * Prevents runaway generation. 0 means no limit.
     * </p>
     *
     * @return max file size (default: 1MB)
     */
    int maxFileSize() default 1_048_576;

    /**
     * Whether to verify output after writing.
     *
     * <p>
     * If true, verifies:
     * </p>
     * <ul>
     * <li>File exists and is readable</li>
     * <li>Content matches what was generated</li>
     * <li>File compiles (for SOURCE target)</li>
     * </ul>
     *
     * @return true to verify (default: true)
     */
    boolean verifyOutput() default true;
}
