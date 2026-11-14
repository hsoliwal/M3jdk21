// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// CPU reference copies only the pinned receiver test's constants/check/assertion block.
import jdk.m3.text.M3PrecomputePayload;
import java.util.*;
final class M3PayloadReference {
    private static final String LEGACY_PAYLOAD = "{\"langdex_concept_id\":701,"
            + "\"langdex_confidence_permille\":1000,"
            + "\"langdex_evidence_mask\":1073741825,"
            + "\"langdex_feature_bits\":2147483647,"
            + "\"langdex_flags\":65535,"
            + "\"langdex_frequency\":123456789,"
            + "\"langdex_lexical_class_mask\":4611686018427387905,"
            + "\"langdex_semantic_class_mask\":2305843009213693953,"
            + "\"langdex_subject_id\":17,"
            + "\"langdex_target_lexeme_id\":70002}";
    private static final String PAYLOAD = "{\"langdex_concept_id\":701,"
            + "\"langdex_confidence_permille\":1000,"
            + "\"langdex_evidence_mask\":1073741825,"
            + "\"langdex_feature_bits\":2147483647,"
            + "\"langdex_flags\":65535,"
            + "\"langdex_frequency\":123456789,"
            + "\"langdex_lexical_class_mask\":4611686018427387905,"
            + "\"langdex_lexical_profile_confidence_permille\":735,"
            + "\"langdex_lexical_profile_evidence_mask\":17,"
            + "\"langdex_lexical_profile_feature_bits\":4660,"
            + "\"langdex_lexical_profile_lexical_class_mask\":12,"
            + "\"langdex_semantic_class_mask\":2305843009213693953,"
            + "\"langdex_subject_id\":17,"
            + "\"langdex_target_lexeme_id\":70002}";
    private static final List<String> REQUIRED_FIELDS = List.of(
            "langdex_concept_id", "langdex_confidence_permille", "langdex_evidence_mask",
            "langdex_feature_bits", "langdex_flags", "langdex_frequency",
            "langdex_lexical_class_mask", "langdex_semantic_class_mask",
            "langdex_subject_id", "langdex_target_lexeme_id");
    private static final List<String> EXPECTED_FIELDS = List.of(
            "langdex_concept_id", "langdex_confidence_permille", "langdex_evidence_mask",
            "langdex_feature_bits", "langdex_flags", "langdex_frequency",
            "langdex_lexical_class_mask", "langdex_lexical_profile_confidence_permille",
            "langdex_lexical_profile_evidence_mask", "langdex_lexical_profile_feature_bits",
            "langdex_lexical_profile_lexical_class_mask", "langdex_semantic_class_mask",
            "langdex_subject_id", "langdex_target_lexeme_id");
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    static void reset() { checks = 0; }
    static int checks() { return checks; }
    static void verify(M3PrecomputePayload payload) {
        check(payload.fieldNames().equals(new TreeSet<>(EXPECTED_FIELDS)));
        check(payload.canonicalJson().equals(PAYLOAD));
        check(payload.requireLong("langdex_concept_id") == 701L);
        check(payload.requireInt("langdex_confidence_permille") == 1000);
        check(payload.requireInt("langdex_evidence_mask") == 1073741825);
        check(payload.requireInt("langdex_feature_bits") == Integer.MAX_VALUE);
        check(payload.requireInt("langdex_flags") == 65535);
        check(payload.requireLong("langdex_frequency") == 123456789L);
        check(payload.requireLong("langdex_lexical_class_mask") == 4611686018427387905L);
        check(payload.requireInt("langdex_lexical_profile_confidence_permille") == 735);
        check(payload.requireInt("langdex_lexical_profile_evidence_mask") == 17);
        check(payload.requireInt("langdex_lexical_profile_feature_bits") == 4660);
        check(payload.requireLong("langdex_lexical_profile_lexical_class_mask") == 12L);
        check(payload.requireLong("langdex_semantic_class_mask") == 2305843009213693953L);
        M3PrecomputePayload legacy = M3PrecomputePayload.parse(LEGACY_PAYLOAD);
        check(legacy.fieldNames().equals(new TreeSet<>(REQUIRED_FIELDS)));
        check(legacy.field("langdex_lexical_profile_feature_bits").isEmpty());
        check(payload.requireInt("langdex_subject_id") == 17);
        check(payload.requireLong("langdex_target_lexeme_id") == 70002L);
    }
}
