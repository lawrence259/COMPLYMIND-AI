package com.complymind;

import com.complymind.evidence.service.DocumentParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTests {

    private final DocumentParserService parserService = new DocumentParserService();

    @Test
    @DisplayName("Document Parser: Compute deterministic SHA-256 checksum")
    void testSha256Checksum() {
        byte[] content = "AICTE Approval Process Handbook Compliance Evidence".getBytes(StandardCharsets.UTF_8);
        String checksum = parserService.computeSha256(content);

        assertNotNull(checksum);
        assertEquals(64, checksum.length(), "SHA-256 hex string should be 64 characters");

        // Idempotency: exact same data yields exact same checksum
        String checksum2 = parserService.computeSha256(content);
        assertEquals(checksum, checksum2);
    }

    @Test
    @DisplayName("Apache Tika: Extract text from plain text input")
    void testTikaTextExtraction() {
        String testDoc = "National Institute of Science & Technology - Land Title Registry Deed No. 4567";
        byte[] bytes = testDoc.getBytes(StandardCharsets.UTF_8);

        DocumentParserService.ParseResult result = parserService.extractTextAndMetadata(
                new ByteArrayInputStream(bytes), "test_deed.txt", bytes
        );

        assertTrue(result.isSuccess(), "Tika extraction should succeed");
        assertTrue(result.getText().contains("National Institute of Science"), "Extracted text should contain institution name");
        assertTrue(result.getText().contains("Land Title Registry"), "Extracted text should contain title keywords");
    }

    @Test
    @DisplayName("Rule Check: Match required statutory keywords")
    void testKeywordMatching() {
        String docText = "Government of Maharashtra. Registered Sale Deed No. 1234. Clear land title possession granted.";
        assertTrue(parserService.containsRequiredKeywords(docText, new String[]{"Sale Deed", "possession"}));
        assertFalse(parserService.containsRequiredKeywords(docText, new String[]{"Fire NOC", "Occupancy"}));
    }
}
