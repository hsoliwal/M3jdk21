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
 * TLOUOOIACI Course Classifier
 *
 * Maps university courses to the Crystalline-Amorphous spectrum.
 * Based on data from: D:\COGNIX_SYLLABUS\US_Universities
 *
 * The ordering is FIXED and INVARIANT:
 *   Mathematics → Physics → Chemistry → Biology → ... → Philosophy
 */
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.annotation.ValidationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
/**
 * CourseClassifier - Maps university courses to the Crystalline-Amorphous
 * spectrum.
 *
 * <p>
 * Uses TLOUOOIACI (The Law Of Universals Order Of Informational And
 * Constructional Invariance) to classify courses by their level of determinism.
 * </p>
 *
 * <h2>Classification Principle</h2>
 *
 * <pre>
 * CRYSTALLINE (C=1.0) ────────────────────► AMORPHOUS (C=0.0)
 *
 * Course 18 (Math) → Course 8 (Physics) → Course 5 (Chemistry) →
 * Course 7 (Biology) → Course 9 (Cognition) → Course 14 (Economics) →
 * Course 17 (Political Science) → Course 24 (Philosophy)
 * </pre>
 *
 * <h2>Data Sources</h2>
 * <ul>
 * <li>MIT: Course numbers 1-24</li>
 * <li>Harvard: Department codes</li>
 * <li>Berkeley: Department abbreviations</li>
 * <li>Stanford: Subject codes</li>
 * </ul>
 *
 * @see OrdinalSpine for the domain ordering
 * @m3.pattern synexia:bop
 * @m3.algorithm Groups declared fields CRYSTALLINE_THRESHOLD, COURSE_CODE_SPLIT_FIRST_PART, REGEX_SPLIT_DOT, NEWLINE, MIT_COURSE_MAP, SUBJECT_PATTERNS, MIT_SOURCE and member operations &lt;init&gt;, classifyMIT, classifyMIT, isValidCourseCode, parseCourseNumber, crystallinityMIT, classifyByText, crystallinityByText, classify, classifyBySource, classifyMITWithFallback, printUniversalLaws, printUniversalLawsHeader, printUniversalLawsBody, printUniversalLawsFooter, printMITClassification, main. Concrete bodies and inherited behavior remain owned by their member and parent contracts.
 * @m3.dataStructures Declared class owner with double CRYSTALLINE_THRESHOLD, int COURSE_CODE_SPLIT_FIRST_PART, String REGEX_SPLIT_DOT, String NEWLINE, Map of Integer, OrdinalSpine.Domain MIT_COURSE_MAP, Map of Pattern, OrdinalSpine.Domain SUBJECT_PATTERNS, String MIT_SOURCE. Nested types and inherited members retain their own declarations.
 * @m3.member {@code INITIALIZER:<clinit>#1} pattern=synexia:bop; algorithm=Populates the shared MIT_COURSE_MAP HashMap through the checked-in put calls in declaration order. Existing entries at a repeated key would be replaced and HashMap traversal has no declared classification priority.; data.structures=Shared HashMap MIT_COURSE_MAP mapping integer keys to OrdinalSpine.Domain values.; description=Initializes shared state of CourseClassifier.
 * @m3.member {@code INITIALIZER:<clinit>#2} pattern=synexia:bop; algorithm=Populates the shared SUBJECT_PATTERNS HashMap through the checked-in put calls in declaration order. Existing entries at a repeated key would be replaced and HashMap traversal has no declared classification priority.; data.structures=Shared HashMap SUBJECT_PATTERNS mapping regex Pattern references to OrdinalSpine.Domain values.; description=Initializes shared state of CourseClassifier.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/CourseClassifier.java}
 * blob {@code 5aa6424e8bd0111376cdcb76f3f1ad7af4d9ce35} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CourseClassifier {
    private static final PlaceholderLogger log = PlaceholderLogger.of(CourseClassifier.class);


    /**
     * Stored CRYSTALLINE_THRESHOLD member of CourseClassifier.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes CRYSTALLINE_THRESHOLD by evaluating the checked-in expression 0.7. The field is shared by the declared owner.
     * @m3.dataStructures One double declared field named CRYSTALLINE_THRESHOLD.
     */
    private static final double CRYSTALLINE_THRESHOLD = 0.7;
    /**
     * Stored COURSE_CODE_SPLIT_FIRST_PART member of CourseClassifier.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes COURSE_CODE_SPLIT_FIRST_PART by evaluating the checked-in expression 0. The field is shared by the declared owner.
     * @m3.dataStructures One int declared field named COURSE_CODE_SPLIT_FIRST_PART.
     */
    private static final int COURSE_CODE_SPLIT_FIRST_PART = 0;
    /**
     * Stored REGEX_SPLIT_DOT member of CourseClassifier.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes REGEX_SPLIT_DOT by evaluating the checked-in expression "\\.". The field is shared by the declared owner.
     * @m3.dataStructures One String declared field named REGEX_SPLIT_DOT.
     */
    private static final String REGEX_SPLIT_DOT = "\\.";
    /**
     * Stored NEWLINE member of CourseClassifier.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes NEWLINE by evaluating the checked-in expression "\n". The field is shared by the declared owner.
     * @m3.dataStructures One String declared field named NEWLINE.
     */
    private static final String NEWLINE = "\n";

    /**
     * Initializes an instance of CourseClassifier.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty declared constructor body after implicit superclass initialization.
     * @m3.dataStructures Declared constructor with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    private CourseClassifier() {
        // Utility class - no instantiation
    }

    // ════════════════════════════════════════════════════════════════════════
    // MIT COURSE CLASSIFICATION
    // ════════════════════════════════════════════════════════════════════════

    /** MIT course number to domain mapping.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes MIT_COURSE_MAP by evaluating the checked-in expression new HashMap&lt;&gt;(). The field is shared by the declared owner.
     * @m3.dataStructures One Map of Integer, OrdinalSpine.Domain declared field named MIT_COURSE_MAP.
     */
    private static final Map<Integer, OrdinalSpine.Domain> MIT_COURSE_MAP = new HashMap<>();

    static {
        // PURE CRYSTALLINE (C=1.0)
        MIT_COURSE_MAP.put(18, OrdinalSpine.Domain.MATHEMATICS);

        // HIGH CRYSTALLINE (C=0.90-0.99)
        MIT_COURSE_MAP.put(8, OrdinalSpine.Domain.PHYSICS);
        MIT_COURSE_MAP.put(5, OrdinalSpine.Domain.CHEMISTRY);
        MIT_COURSE_MAP.put(12, OrdinalSpine.Domain.EARTH_SCIENCES);

        // SEMI-CRYSTALLINE - Engineering (C=0.85)
        MIT_COURSE_MAP.put(1, OrdinalSpine.Domain.ENGINEERING); // Civil
        MIT_COURSE_MAP.put(2, OrdinalSpine.Domain.ENGINEERING); // Mechanical
        MIT_COURSE_MAP.put(3, OrdinalSpine.Domain.ENGINEERING); // Materials
        MIT_COURSE_MAP.put(10, OrdinalSpine.Domain.ENGINEERING); // Chemical Eng
        MIT_COURSE_MAP.put(16, OrdinalSpine.Domain.ENGINEERING); // Aero/Astro
        MIT_COURSE_MAP.put(22, OrdinalSpine.Domain.ENGINEERING); // Nuclear

        // SEMI-CRYSTALLINE - Life Sciences (C=0.75-0.80)
        MIT_COURSE_MAP.put(7, OrdinalSpine.Domain.BIOLOGY);
        MIT_COURSE_MAP.put(20, OrdinalSpine.Domain.BIOLOGY); // Biological Eng

        // TRANSITIONAL (C=0.50-0.69)
        MIT_COURSE_MAP.put(6, OrdinalSpine.Domain.COMPUTER_SCIENCE); // EECS
        MIT_COURSE_MAP.put(9, OrdinalSpine.Domain.COGNITIVE_SCIENCE); // Brain/Cog
        MIT_COURSE_MAP.put(14, OrdinalSpine.Domain.ECONOMICS);
        MIT_COURSE_MAP.put(15, OrdinalSpine.Domain.ECONOMICS); // Management

        // SEMI-AMORPHOUS (C=0.30-0.49)
        MIT_COURSE_MAP.put(17, OrdinalSpine.Domain.POLITICAL_SCIENCE);

        // HIGH AMORPHOUS (C=0.10-0.29)
        MIT_COURSE_MAP.put(4, OrdinalSpine.Domain.ARCHITECTURE);
        MIT_COURSE_MAP.put(11, OrdinalSpine.Domain.ARCHITECTURE); // Urban Planning
        MIT_COURSE_MAP.put(21, OrdinalSpine.Domain.LITERATURE); // Humanities

        // PURE AMORPHOUS (C=0.0-0.09)
        MIT_COURSE_MAP.put(24, OrdinalSpine.Domain.PHILOSOPHY); // Linguistics & Phil
    }

    /**
     * Classifies an MIT course by its course number.
     *
     * @param courseNumber
     *            MIT course number (1-24)
     * @return the domain classification
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates and returns Optional.ofNullable(MIT_COURSE_MAP.get(courseNumber)). Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
     * @m3.dataStructures Declared return type Optional of OrdinalSpine.Domain with formal parameters int courseNumber. Input references and declared collections follow the member body or implementation contract.
     */
    public static Optional<OrdinalSpine.Domain> classifyMIT(int courseNumber) {
        return Optional.ofNullable(MIT_COURSE_MAP.get(courseNumber));
    }

    /**
     * Classifies an MIT course by its code string (e.g., "18.01", "8.01").
     *
     * @param courseCode
     *            MIT course code
     * @return the domain classification
     * @m3.pattern synexia:bop
     * @m3.algorithm Rejects an invalid string with Optional.empty, otherwise parses its first dot-separated segment as an int and delegates to numeric classification. NumberFormatException is logged and converted to empty, with no fuzzy title lookup in this overload.
     * @m3.dataStructures Declared return type Optional of OrdinalSpine.Domain with formal parameters final String courseCode. Input references and declared collections follow the member body or implementation contract.
     */
    public static Optional<OrdinalSpine.Domain> classifyMIT(final String courseCode) {
        log.debug("Classifying MIT course code: {}", courseCode);

        if (!isValidCourseCode(courseCode)) {
            log.debug("Empty or null course code provided");
            return Optional.empty();
        }

        try {
            final int courseNumber = parseCourseNumber(courseCode);
            return classifyMIT(courseNumber);
        } catch (final NumberFormatException e) {
            log.warn("Invalid MIT course code format: {}", courseCode, e);
            return Optional.empty();
        }
    }

    /**
     * Validates course code is not null or empty.
     *
     * @param courseCode
     *            course code to validate
     * @return true if valid
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates and returns ValidationUtils.isValidString(courseCode). Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
     * @m3.dataStructures Declared return type boolean with formal parameters final String courseCode. Input references and declared collections follow the member body or implementation contract.
     */
    private static boolean isValidCourseCode(final String courseCode) {
        return ValidationUtils.isValidString(courseCode);
    }

    /**
     * Parses course number from course code.
     *
     * @param courseCode
     *            course code string
     * @return course number
     * @throws NumberFormatException
     *             if parsing fails
     * @m3.pattern synexia:bop
     * @m3.algorithm Splits courseCode with the checked-in dot regular expression and parses its first array element using Integer.parseInt. Null input, an absent element or a noninteger first segment fails under those called operations without validation here.
     * @m3.dataStructures Declared return type int with formal parameters final String courseCode. Input references and declared collections follow the member body or implementation contract.
     */
    private static int parseCourseNumber(final String courseCode) {
        final String[] parts = courseCode.split(REGEX_SPLIT_DOT);
        return Integer.parseInt(parts[COURSE_CODE_SPLIT_FIRST_PART]);
    }

    /**
     * Returns the crystallinity coefficient for an MIT course.
     *
     * @param courseNumber
     *            MIT course number
     * @return crystallinity [0.0, 1.0] or -1 if not found
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates and returns classifyMIT(courseNumber).map((d)-&gt;d.crystallinity).orElse(-1.0). Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
     * @m3.dataStructures Declared return type double with formal parameters int courseNumber. Input references and declared collections follow the member body or implementation contract.
     */
    public static double crystallinityMIT(int courseNumber) {
        return classifyMIT(courseNumber).map(d -> d.crystallinity).orElse(-1.0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UNIVERSAL SUBJECT CLASSIFICATION
    // ════════════════════════════════════════════════════════════════════════

    /** Subject name patterns to domain mapping.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes SUBJECT_PATTERNS by evaluating the checked-in expression new HashMap&lt;&gt;(). The field is shared by the declared owner.
     * @m3.dataStructures One Map of Pattern, OrdinalSpine.Domain declared field named SUBJECT_PATTERNS.
     */
    private static final Map<Pattern, OrdinalSpine.Domain> SUBJECT_PATTERNS = new HashMap<>();

    static {
        // PURE CRYSTALLINE
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)math|calculus|algebra|geometry|topology|analysis"),
                OrdinalSpine.Domain.MATHEMATICS);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)statistics|probability"), OrdinalSpine.Domain.MATHEMATICS);

        // HIGH CRYSTALLINE
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)physics|mechanics|quantum|relativity|thermodynamics"),
                OrdinalSpine.Domain.PHYSICS);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)chemistry|organic|inorganic|biochem"), OrdinalSpine.Domain.CHEMISTRY);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)geology|earth|planetary|atmospheric|ocean"), OrdinalSpine.Domain.EARTH_SCIENCES);

        // SEMI-CRYSTALLINE - Engineering
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)engineering|mechanical|civil|electrical|materials"),
                OrdinalSpine.Domain.ENGINEERING);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)aerospace|aeronautic|nuclear"), OrdinalSpine.Domain.ENGINEERING);

        // SEMI-CRYSTALLINE - Biology
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)biology|molecular|genetics|evolution|ecology"), OrdinalSpine.Domain.BIOLOGY);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)neuroscience|brain|neural"), OrdinalSpine.Domain.COGNITIVE_SCIENCE);

        // TRANSITIONAL
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)computer|algorithm|software|programming|data structure"),
                OrdinalSpine.Domain.COMPUTER_SCIENCE);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)cognitive|cognition|perception|memory"), OrdinalSpine.Domain.COGNITIVE_SCIENCE);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)economics|finance|market|microecon|macroecon"), OrdinalSpine.Domain.ECONOMICS);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)information theory|entropy|coding theory"),
                OrdinalSpine.Domain.INFORMATION_THEORY);

        // SEMI-AMORPHOUS
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)psychology|behavior|developmental|clinical"), OrdinalSpine.Domain.PSYCHOLOGY);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)political|government|policy|international relation"),
                OrdinalSpine.Domain.POLITICAL_SCIENCE);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)anthropology|cultural|ethnograph"), OrdinalSpine.Domain.ANTHROPOLOGY);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)linguistics|language|phonology|syntax|semantic"), OrdinalSpine.Domain.LINGUISTICS);

        // HIGH AMORPHOUS
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)history|historical|ancient|medieval|modern"), OrdinalSpine.Domain.HISTORY);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)literature|poetry|fiction|novel|drama|narrative"), OrdinalSpine.Domain.LITERATURE);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)art|music|theater|film|visual|aesthetic"), OrdinalSpine.Domain.ARTS);
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)architecture|urban|design|planning"), OrdinalSpine.Domain.ARCHITECTURE);

        // PURE AMORPHOUS
        SUBJECT_PATTERNS.put(
                Pattern.compile("(?i)philosophy|ethics|epistemology|metaphysics|logic"),
                OrdinalSpine.Domain.PHILOSOPHY);
        SUBJECT_PATTERNS.put(Pattern.compile("(?i)theology|religion|spiritual|faith"), OrdinalSpine.Domain.THEOLOGY);
    }

    /**
     * Classifies a course by its title or description.
     *
     * @param text
     *            course title or description
     * @return the most likely domain classification
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns Optional.empty for a string rejected by ValidationUtils.isValidString, otherwise streams the existing HashMap entries and returns the domain from the first regex matcher.find success. The regular expressions are case insensitive and HashMap traversal supplies no category priority.
     * @m3.dataStructures Declared return type Optional of OrdinalSpine.Domain with formal parameters String text. Input references and declared collections follow the member body or implementation contract.
     */
    public static Optional<OrdinalSpine.Domain> classifyByText(String text) {
        if (!ValidationUtils.isValidString(text)) {
            return Optional.empty();
        }

        return SUBJECT_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(text).find())
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Returns the crystallinity coefficient for a course by text.
     *
     * @param text
     *            course title or description
     * @return crystallinity [0.0, 1.0] or -1 if not classified
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates and returns classifyByText(text).map((d)-&gt;d.crystallinity).orElse(-1.0). Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
     * @m3.dataStructures Declared return type double with formal parameters String text. Input references and declared collections follow the member body or implementation contract.
     */
    public static double crystallinityByText(String text) {
        return classifyByText(text).map(d -> d.crystallinity).orElse(-1.0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // COURSE RECORD FOR CLASSIFICATION RESULTS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Classified course record.
     *
     * @param source
     *            university source (MIT, Harvard, etc.)
     * @param code
     *            course code
     * @param title
     *            course title
     * @param domain
     *            classified domain
     * @param crystallinity
     *            crystallinity coefficient
     * @param tier
     *            classification tier
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence source, code, title, domain, crystallinity, tier. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with String source, String code, String title, OrdinalSpine.Domain domain, double crystallinity, OrdinalSpine.Tier tier. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:source} pattern=synexia:iop; algorithm=Stores the caller-supplied source component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named source.; description=Stored source member of CourseClassifier.ClassifiedCourse.
     * @m3.member {@code FIELD:code} pattern=synexia:iop; algorithm=Stores the caller-supplied code component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named code.; description=Stored code member of CourseClassifier.ClassifiedCourse.
     * @m3.member {@code FIELD:title} pattern=synexia:iop; algorithm=Stores the caller-supplied title component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named title.; description=Stored title member of CourseClassifier.ClassifiedCourse.
     * @m3.member {@code FIELD:domain} pattern=synexia:iop; algorithm=Stores the caller-supplied domain component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One OrdinalSpine.Domain record component field named domain.; description=Stored domain member of CourseClassifier.ClassifiedCourse.
     * @m3.member {@code FIELD:crystallinity} pattern=synexia:iop; algorithm=Stores the caller-supplied crystallinity component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One double record component field named crystallinity.; description=Stored crystallinity member of CourseClassifier.ClassifiedCourse.
     * @m3.member {@code FIELD:tier} pattern=synexia:iop; algorithm=Stores the caller-supplied tier component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One OrdinalSpine.Tier record component field named tier.; description=Stored tier member of CourseClassifier.ClassifiedCourse.
     */
    public record ClassifiedCourse(
            String source,
            String code,
            String title,
            OrdinalSpine.Domain domain,
            double crystallinity,
            OrdinalSpine.Tier tier) {

        /**
         * Creates a classified course from raw data.
         *
         * @param source
         *            university
         * @param code
         *            course code
         * @param title
         *            course title
         * @param domain
         *            classified domain
         * @return classified course record
         * @m3.pattern synexia:bop
         * @m3.algorithm Reads crystallinity and tier directly from domain and constructs a ClassifiedCourse retaining the supplied source, code, title and domain references. Null domain fails during field access before construction.
         * @m3.dataStructures Declared return type ClassifiedCourse with formal parameters String source,String code,String title,OrdinalSpine.Domain domain. Input references and declared collections follow the member body or implementation contract.
         */
        public static ClassifiedCourse of(String source, String code, String title, OrdinalSpine.Domain domain) {
            return new ClassifiedCourse(source, code, title, domain, domain.crystallinity, domain.tier);
        }

        /**
         * Returns true if this course is more crystalline than another.
         *
         * @param other
         *            course to compare
         * @return true if this is more crystalline
         * @m3.pattern synexia:bop
         * @m3.algorithm Compares this stored double crystallinity with the other record value using Java greater-than. Null other fails on dereference and NaN compares false.
         * @m3.dataStructures Declared return type boolean with formal parameters ClassifiedCourse other. Input references and declared collections follow the member body or implementation contract.
         */
        public boolean isMoreCrystallineThan(ClassifiedCourse other) {
            return this.crystallinity > other.crystallinity;
        }

        /**
         * Returns true if this course is a prerequisite for another.
         *
         * @param other
         *            course to check
         * @return true if this should be taken before other
         * @m3.pattern synexia:bop
         * @m3.algorithm Calls this domain.isPrerequisiteFor with the other record domain. Null other or this domain fails on dereference and the domain relation remains owned by OrdinalSpine.Domain.
         * @m3.dataStructures Declared return type boolean with formal parameters ClassifiedCourse other. Input references and declared collections follow the member body or implementation contract.
         */
        public boolean isPrerequisiteFor(ClassifiedCourse other) {
            return this.domain.isPrerequisiteFor(other.domain);
        }
    }

    /**
     * Classifies and creates a course record.
     *
     *            university
     *            course code
     *            course title
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes MIT_SOURCE by evaluating the checked-in expression "MIT". The field is shared by the declared owner.
     * @m3.dataStructures One String declared field named MIT_SOURCE.
     */
    private static final String MIT_SOURCE = "MIT";

    /**
     * Classifies and creates a course record.
     *
     * @param source
     *            university
     * @param code
     *            course code
     * @param title
     *            course title
     * @return classified course or empty if not classifiable
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the declared statement sequence { final Optional&lt;OrdinalSpine.Domain&gt; domain = classifyBySource(source, code, title); return domain.map((d)-&gt;ClassifiedCourse.of(source, code, title, d)); }. Java evaluation order, called-operation failures and the declared catch clauses remain those shown in this sequence.
     * @m3.dataStructures Declared return type Optional of ClassifiedCourse with formal parameters final String source,final String code,final String title. Input references and declared collections follow the member body or implementation contract.
     */
    public static Optional<ClassifiedCourse> classify(final String source, final String code, final String title) {
        final Optional<OrdinalSpine.Domain> domain = classifyBySource(source, code, title);
        return domain.map(d -> ClassifiedCourse.of(source, code, title, d));
    }

    /**
     * Classifies course by source university.
     *
     * @param source
     *            university source
     * @param code
     *            course code
     * @param title
     *            course title
     * @return domain classification
     * @m3.pattern synexia:bop
     * @m3.algorithm Uses case-insensitive comparison against the stored MIT source label to select code classification with title fallback, otherwise classifies title alone. Null source follows the text branch because comparison is invoked on the stored label.
     * @m3.dataStructures Declared return type Optional of OrdinalSpine.Domain with formal parameters final String source,final String code,final String title. Input references and declared collections follow the member body or implementation contract.
     */
    private static Optional<OrdinalSpine.Domain> classifyBySource(
            final String source, final String code, final String title) {
        if (MIT_SOURCE.equalsIgnoreCase(source)) {
            return classifyMITWithFallback(code, title);
        }
        return classifyByText(title);
    }

    /**
     * Classifies MIT course with text fallback.
     *
     * @param code
     *            course code
     * @param title
     *            course title
     * @return domain classification
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls classifyMIT with code, returns that Optional when present, otherwise classifies title by text. It does not combine multiple classifications or validate a successful code against the title.
     * @m3.dataStructures Declared return type Optional of OrdinalSpine.Domain with formal parameters final String code,final String title. Input references and declared collections follow the member body or implementation contract.
     */
    private static Optional<OrdinalSpine.Domain> classifyMITWithFallback(final String code, final String title) {
        final Optional<OrdinalSpine.Domain> mitDomain = classifyMIT(code);
        return mitDomain.isPresent() ? mitDomain : classifyByText(title);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UNIVERSAL LAWS CLASSIFICATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Universal Laws - Crystalline laws that apply to everyone without exception.
     *
     * <p>
     * These are the invariant laws of physics, mathematics, and chemistry. They are
     * singleton contexts that apply universally.
     * </p>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enum singleton values in source order with declared fields ARITHMETIC, PYTHAGOREAN, EULER_IDENTITY, CALCULUS_FUNDAMENTAL, NEWTON_SECOND, MASS_ENERGY, GRAVITATION, SPEED_OF_LIGHT, PLANCK_CONSTANT, MAXWELL_EQUATIONS, ENERGY_CONSERVATION, MOMENTUM_CONSERVATION, IDEAL_GAS, GIBBS_ENERGY, THERMO_FIRST, THERMO_SECOND, ARRHENIUS, SHANNON_ENTROPY, CHANNEL_CAPACITY, formula, domain, description and member operations &lt;init&gt;, crystallinity, isCrystalline.
     * @m3.dataStructures Declared enum owner with UniversalLaw ARITHMETIC, UniversalLaw PYTHAGOREAN, UniversalLaw EULER_IDENTITY, UniversalLaw CALCULUS_FUNDAMENTAL, UniversalLaw NEWTON_SECOND, UniversalLaw MASS_ENERGY, UniversalLaw GRAVITATION, UniversalLaw SPEED_OF_LIGHT, UniversalLaw PLANCK_CONSTANT, UniversalLaw MAXWELL_EQUATIONS, UniversalLaw ENERGY_CONSERVATION, UniversalLaw MOMENTUM_CONSERVATION, UniversalLaw IDEAL_GAS, UniversalLaw GIBBS_ENERGY, UniversalLaw THERMO_FIRST, UniversalLaw THERMO_SECOND, UniversalLaw ARRHENIUS, UniversalLaw SHANNON_ENTROPY, UniversalLaw CHANNEL_CAPACITY, String formula, OrdinalSpine.Domain domain, String description. Nested types and inherited members retain their own declarations.
     */
    public enum UniversalLaw {
        // ══════════════════════════════════════════════════════════════════
        // MATHEMATICAL LAWS (C=1.0) - Pure Logic
        // ══════════════════════════════════════════════════════════════════

        /** 2 + 2 = 4 (Arithmetic)
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes ARITHMETIC by evaluating the checked-in expression new UniversalLaw("2 + 2 = 4", OrdinalSpine.Domain.MATHEMATICS, "Basic arithmetic - axiomatic, no exceptions"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named ARITHMETIC.
         */
        ARITHMETIC("2 + 2 = 4", OrdinalSpine.Domain.MATHEMATICS, "Basic arithmetic - axiomatic, no exceptions"),

        /** Pythagorean theorem: a² + b² = c²
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes PYTHAGOREAN by evaluating the checked-in expression new UniversalLaw("a² + b² = c²", OrdinalSpine.Domain.MATHEMATICS, "Geometric relationship in Euclidean space"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named PYTHAGOREAN.
         */
        PYTHAGOREAN("a² + b² = c²", OrdinalSpine.Domain.MATHEMATICS, "Geometric relationship in Euclidean space"),

        /** Euler's identity: e^(iπ) + 1 = 0
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes EULER_IDENTITY by evaluating the checked-in expression new UniversalLaw("e^(iπ) + 1 = 0", OrdinalSpine.Domain.MATHEMATICS, "Most beautiful equation - connects e, i, π, 1, 0"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named EULER_IDENTITY.
         */
        EULER_IDENTITY(
                "e^(iπ) + 1 = 0", OrdinalSpine.Domain.MATHEMATICS, "Most beautiful equation - connects e, i, π, 1, 0"),

        /** Fundamental theorem of calculus
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes CALCULUS_FUNDAMENTAL by evaluating the checked-in expression new UniversalLaw("∫f\'(x)dx = f(b) - f(a)", OrdinalSpine.Domain.MATHEMATICS, "Integration and differentiation are inverses"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named CALCULUS_FUNDAMENTAL.
         */
        CALCULUS_FUNDAMENTAL(
                "∫f'(x)dx = f(b) - f(a)",
                OrdinalSpine.Domain.MATHEMATICS,
                "Integration and differentiation are inverses"),

        // ══════════════════════════════════════════════════════════════════
        // PHYSICAL LAWS (C=0.98) - Universal Constants
        // ══════════════════════════════════════════════════════════════════

        /** Newton's Second Law: F = ma
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes NEWTON_SECOND by evaluating the checked-in expression new UniversalLaw("F = ma", OrdinalSpine.Domain.PHYSICS, "Force equals mass times acceleration"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named NEWTON_SECOND.
         */
        NEWTON_SECOND("F = ma", OrdinalSpine.Domain.PHYSICS, "Force equals mass times acceleration"),

        /** Einstein's mass-energy equivalence: E = mc²
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes MASS_ENERGY by evaluating the checked-in expression new UniversalLaw("E = mc²", OrdinalSpine.Domain.PHYSICS, "Energy equals mass times speed of light squared"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named MASS_ENERGY.
         */
        MASS_ENERGY("E = mc²", OrdinalSpine.Domain.PHYSICS, "Energy equals mass times speed of light squared"),

        /** Universal gravitation: F = Gm₁m₂/r²
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes GRAVITATION by evaluating the checked-in expression new UniversalLaw("F = Gm₁m₂/r²", OrdinalSpine.Domain.PHYSICS, "Gravitational force between two masses"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named GRAVITATION.
         */
        GRAVITATION("F = Gm₁m₂/r²", OrdinalSpine.Domain.PHYSICS, "Gravitational force between two masses"),

        /** Speed of light: c = 299,792,458 m/s
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes SPEED_OF_LIGHT by evaluating the checked-in expression new UniversalLaw("c = 299,792,458 m/s", OrdinalSpine.Domain.PHYSICS, "Universal speed limit - constant in all frames"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named SPEED_OF_LIGHT.
         */
        SPEED_OF_LIGHT(
                "c = 299,792,458 m/s", OrdinalSpine.Domain.PHYSICS, "Universal speed limit - constant in all frames"),

        /** Planck's constant: h = 6.626×10⁻³⁴ J·s
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes PLANCK_CONSTANT by evaluating the checked-in expression new UniversalLaw("h = 6.626×10⁻³⁴ J·s", OrdinalSpine.Domain.PHYSICS, "Quantum of action"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named PLANCK_CONSTANT.
         */
        PLANCK_CONSTANT("h = 6.626×10⁻³⁴ J·s", OrdinalSpine.Domain.PHYSICS, "Quantum of action"),

        /** Maxwell's equations
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes MAXWELL_EQUATIONS by evaluating the checked-in expression new UniversalLaw("∇·E = ρ/ε₀, ∇×B = μ₀J + μ₀ε₀∂E/∂t", OrdinalSpine.Domain.PHYSICS, "Electromagnetic field equations"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named MAXWELL_EQUATIONS.
         */
        MAXWELL_EQUATIONS(
                "∇·E = ρ/ε₀, ∇×B = μ₀J + μ₀ε₀∂E/∂t", OrdinalSpine.Domain.PHYSICS, "Electromagnetic field equations"),

        /** Conservation of energy
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes ENERGY_CONSERVATION by evaluating the checked-in expression new UniversalLaw("ΔE = 0 (closed system)", OrdinalSpine.Domain.PHYSICS, "Energy cannot be created or destroyed"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named ENERGY_CONSERVATION.
         */
        ENERGY_CONSERVATION(
                "ΔE = 0 (closed system)", OrdinalSpine.Domain.PHYSICS, "Energy cannot be created or destroyed"),

        /** Conservation of momentum
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes MOMENTUM_CONSERVATION by evaluating the checked-in expression new UniversalLaw("Δp = 0 (closed system)", OrdinalSpine.Domain.PHYSICS, "Momentum is conserved in closed systems"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named MOMENTUM_CONSERVATION.
         */
        MOMENTUM_CONSERVATION(
                "Δp = 0 (closed system)", OrdinalSpine.Domain.PHYSICS, "Momentum is conserved in closed systems"),

        // ══════════════════════════════════════════════════════════════════
        // CHEMICAL LAWS (C=0.95) - Thermodynamics
        // ══════════════════════════════════════════════════════════════════

        /** Ideal gas law: PV = nRT
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes IDEAL_GAS by evaluating the checked-in expression new UniversalLaw("PV = nRT", OrdinalSpine.Domain.CHEMISTRY, "Relationship between pressure, volume, and temperature"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named IDEAL_GAS.
         */
        IDEAL_GAS("PV = nRT", OrdinalSpine.Domain.CHEMISTRY, "Relationship between pressure, volume, and temperature"),

        /** Gibbs free energy: ΔG = ΔH - TΔS
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes GIBBS_ENERGY by evaluating the checked-in expression new UniversalLaw("ΔG = ΔH - TΔS", OrdinalSpine.Domain.CHEMISTRY, "Spontaneity of chemical reactions"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named GIBBS_ENERGY.
         */
        GIBBS_ENERGY("ΔG = ΔH - TΔS", OrdinalSpine.Domain.CHEMISTRY, "Spontaneity of chemical reactions"),

        /** First law of thermodynamics: ΔU = Q - W
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes THERMO_FIRST by evaluating the checked-in expression new UniversalLaw("ΔU = Q - W", OrdinalSpine.Domain.CHEMISTRY, "Energy conservation in thermodynamics"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named THERMO_FIRST.
         */
        THERMO_FIRST("ΔU = Q - W", OrdinalSpine.Domain.CHEMISTRY, "Energy conservation in thermodynamics"),

        /** Second law of thermodynamics: ΔS ≥ 0
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes THERMO_SECOND by evaluating the checked-in expression new UniversalLaw("ΔS ≥ 0 (universe)", OrdinalSpine.Domain.CHEMISTRY, "Entropy of universe always increases"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named THERMO_SECOND.
         */
        THERMO_SECOND("ΔS ≥ 0 (universe)", OrdinalSpine.Domain.CHEMISTRY, "Entropy of universe always increases"),

        /** Arrhenius equation: k = A·e^(-Ea/RT)
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes ARRHENIUS by evaluating the checked-in expression new UniversalLaw("k = A·e^(-Ea/RT)", OrdinalSpine.Domain.CHEMISTRY, "Temperature dependence of reaction rates"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named ARRHENIUS.
         */
        ARRHENIUS("k = A·e^(-Ea/RT)", OrdinalSpine.Domain.CHEMISTRY, "Temperature dependence of reaction rates"),

        // ══════════════════════════════════════════════════════════════════
        // INFORMATION LAWS (C=0.80) - Shannon
        // ══════════════════════════════════════════════════════════════════

        /** Shannon entropy: H = -Σp log p
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes SHANNON_ENTROPY by evaluating the checked-in expression new UniversalLaw("H = -Σp log p", OrdinalSpine.Domain.INFORMATION_THEORY, "Information content of a probability distribution"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named SHANNON_ENTROPY.
         */
        SHANNON_ENTROPY(
                "H = -Σp log p",
                OrdinalSpine.Domain.INFORMATION_THEORY,
                "Information content of a probability distribution"),

        /** Channel capacity: C = max I(X;Y)
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes CHANNEL_CAPACITY by evaluating the checked-in expression new UniversalLaw("C = max I(X;Y)", OrdinalSpine.Domain.INFORMATION_THEORY, "Maximum rate of reliable communication"). The field is shared by the declared owner.
         * @m3.dataStructures One UniversalLaw declared field named CHANNEL_CAPACITY.
         */
        CHANNEL_CAPACITY(
                "C = max I(X;Y)", OrdinalSpine.Domain.INFORMATION_THEORY, "Maximum rate of reliable communication");

        /** The law formula.
         * @m3.pattern synexia:iop
         * @m3.algorithm Has no inline initializer. A declaring constructor may assign it, otherwise Java default initialization applies. This declaration performs no validation or copying.
         * @m3.dataStructures One String declared field named formula.
         */
        public final String formula;
        /** The domain this law belongs to.
         * @m3.pattern synexia:iop
         * @m3.algorithm Has no inline initializer. A declaring constructor may assign it, otherwise Java default initialization applies. This declaration performs no validation or copying.
         * @m3.dataStructures One OrdinalSpine.Domain declared field named domain.
         */
        public final OrdinalSpine.Domain domain;
        /** Description of the law.
         * @m3.pattern synexia:iop
         * @m3.algorithm Has no inline initializer. A declaring constructor may assign it, otherwise Java default initialization applies. This declaration performs no validation or copying.
         * @m3.dataStructures One String declared field named description.
         */
        public final String description;

        /**
         * Initializes an instance of CourseClassifier.UniversalLaw.
         * @m3.pattern synexia:bop
         * @m3.algorithm Executes the declared statement sequence { this.formula = formula; this.domain = domain; this.description = description; }. Java evaluation order, called-operation failures and the declared catch clauses remain those shown in this sequence.
         * @m3.dataStructures Declared constructor with formal parameters String formula,OrdinalSpine.Domain domain,String description. Input references and declared collections follow the member body or implementation contract.
         */
        UniversalLaw(String formula, OrdinalSpine.Domain domain, String description) {
            this.formula = formula;
            this.domain = domain;
            this.description = description;
        }

        /**
         * Returns the crystallinity of this law.
         *
         * @return crystallinity coefficient [0.0, 1.0]
         * @m3.pattern synexia:bop
         * @m3.algorithm Evaluates and returns domain.crystallinity. Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
         * @m3.dataStructures Declared return type double with no formal parameters. Input references and declared collections follow the member body or implementation contract.
         */
        public double crystallinity() {
            return domain.crystallinity;
        }

        /**
         * Returns true if this is a crystalline (deterministic) law.
         *
         * @return true if crystallinity >= 0.7
         * @m3.pattern synexia:bop
         * @m3.algorithm Evaluates and returns domain.crystallinity &gt;= CRYSTALLINE_THRESHOLD. Arguments and calls follow Java expression evaluation order without an added copy or validation layer.
         * @m3.dataStructures Declared return type boolean with no formal parameters. Input references and declared collections follow the member body or implementation contract.
         */
        public boolean isCrystalline() {
            return domain.crystallinity >= CRYSTALLINE_THRESHOLD;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRINT UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /** Prints all universal laws ordered by crystallinity.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: printUniversalLawsHeader, printUniversalLawsBody, printUniversalLawsFooter. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    public static void printUniversalLaws() {
        printUniversalLawsHeader();
        printUniversalLawsBody();
        printUniversalLawsFooter();
    }

    /**
     * printUniversalLawsHeader operation declared by CourseClassifier.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: log.info, log.info, log.info, log.info. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    private static void printUniversalLawsHeader() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  UNIVERSAL LAWS - TLOUOOIACI Ordering");
        log.info("  (Crystalline → Amorphous)");
        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * printUniversalLawsBody operation declared by CourseClassifier.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: java.util.Arrays.stream, UniversalLaw.values, .forEach, currentDomain.get, currentDomain.set, log.info, log.info. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    private static void printUniversalLawsBody() {
        java.util.concurrent.atomic.AtomicReference<OrdinalSpine.Domain> currentDomain =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.Arrays.stream(UniversalLaw.values()).forEach(law -> {
            if (law.domain != currentDomain.get()) {
                currentDomain.set(law.domain);
                log.info("── {} (C={}) ──", law.domain.name, law.domain.crystallinity);
            }
            log.info("  {} {}", law.formula, law.description);
        });
    }

    /**
     * printUniversalLawsFooter operation declared by CourseClassifier.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: log.info, log.info, UniversalLaw.values, java.util.Arrays.stream, UniversalLaw.values, .allMatch, log.info. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    private static void printUniversalLawsFooter() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info(
                "  Total Laws: {} | All Crystalline: {}",
                UniversalLaw.values().length,
                java.util.Arrays.stream(UniversalLaw.values()).allMatch(UniversalLaw::isCrystalline));
        log.info("═══════════════════════════════════════════════════════════════");
    }

    /** Prints MIT course classification.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: log.info, log.info, log.info, MIT_COURSE_MAP.entrySet, .stream, .sorted, Double.compare, b.getValue, a.getValue, .forEach, log.info, String.format, e.getKey, String.format, e.getValue, String.format, e.getValue, e.getValue, log.info. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with no formal parameters. Input references and declared collections follow the member body or implementation contract.
     */
    public static void printMITClassification() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  MIT COURSE CLASSIFICATION - TLOUOOIACI Ordering");
        log.info("═══════════════════════════════════════════════════════════════");

        MIT_COURSE_MAP.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().crystallinity, a.getValue().crystallinity))
                .forEach(e -> log.info(
                        "  Course {} → {} (C={}) {}",
                        String.format("%2d", e.getKey()),
                        String.format("%-20s", e.getValue().name),
                        String.format("%.2f", e.getValue().crystallinity),
                        e.getValue().tier));

        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Main method for testing.
     *
     * @param args
     *            command line arguments (ignored)
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes the checked-in logging and helper calls in displayed source order: printMITClassification, printUniversalLaws. Any loop or stream retains the ordering guarantees of its declared input, and this helper adds no classification or database mutation.
     * @m3.dataStructures Declared return type void with formal parameters String[] args. Input references and declared collections follow the member body or implementation contract.
     */
    public static void main(String[] args) {
        printMITClassification();

        printUniversalLaws();
    }
}
