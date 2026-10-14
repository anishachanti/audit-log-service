package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedactionSignatureKeyRotationTests {

    @Test
    void rotatedKeyRingVerifiesOldAndNewRedactionSignatures() {
        String legacyKey = testKey(1);
        String nextKey = testKey(2);

        RedactionKeyProperties legacyProperties = new RedactionKeyProperties();
        legacyProperties.setKeyId("legacy-v1");
        legacyProperties.setKeys(Map.of("legacy-v1", legacyKey));

        RedactionSignatureService beforeRotation = new RedactionSignatureService(
                legacyProperties,
                new ObjectMapper());

        Instant redactedAt = Instant.parse("2026-10-14T00:00:00Z");
        String oldCanonicalRecord = beforeRotation.canonicalize(
                7L,
                "a".repeat(64),
                "b".repeat(64),
                List.of("email"),
                redactedAt,
                "legacy-v1");
        String oldSignature = beforeRotation.sign(oldCanonicalRecord);

        RedactionKeyProperties rotatedProperties = new RedactionKeyProperties();
        rotatedProperties.setKeyId("key-v2");
        rotatedProperties.setKeys(Map.of(
                "legacy-v1", legacyKey,
                "key-v2", nextKey));

        RedactionSignatureService afterRotation = new RedactionSignatureService(
                rotatedProperties,
                new ObjectMapper());

        assertTrue(afterRotation.verify(
                "legacy-v1", oldCanonicalRecord, oldSignature),
                "A retained old key should verify records signed before rotation");

        String newCanonicalRecord = afterRotation.canonicalize(
                8L,
                "c".repeat(64),
                "d".repeat(64),
                List.of("phone"),
                redactedAt,
                "key-v2");
        String newSignature = afterRotation.sign(newCanonicalRecord);

        assertTrue(afterRotation.verify(
                "key-v2", newCanonicalRecord, newSignature),
                "The active key should sign and verify new records");
        assertFalse(afterRotation.verify(
                "unknown-key", newCanonicalRecord, newSignature),
                "An unknown key ID must not verify");
    }

    private String testKey(int firstByte) {
        byte[] bytes = new byte[32];
        bytes[0] = (byte) firstByte;
        return Base64.getEncoder().encodeToString(bytes);
    }
}
