package com.complymind.evidence.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);
    private final Tika tika = new Tika();

    public static class ParseResult {
        private final String text;
        private final Map<String, String> metadata;
        private final String checksum;
        private final boolean success;
        private final String errorMessage;

        public ParseResult(String text, Map<String, String> metadata, String checksum, boolean success, String errorMessage) {
            this.text = text;
            this.metadata = metadata;
            this.checksum = checksum;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getText() {
            return text;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public String getChecksum() {
            return checksum;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute SHA-256", e);
            return "UNKNOWN_CHECKSUM";
        }
    }

    public ParseResult extractTextAndMetadata(InputStream inputStream, String filename, byte[] rawBytes) {
        String checksum = computeSha256(rawBytes);
        Map<String, String> metaMap = new HashMap<>();
        String extractedText = "";

        try {
            String lower = filename.toLowerCase();

            // Specialized POI handling for docx and xlsx where relevant
            if (lower.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(rawBytes))) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph p : doc.getParagraphs()) {
                        sb.append(p.getText()).append("\n");
                    }
                    extractedText = sb.toString().trim();
                    metaMap.put("DocumentType", "Microsoft Word OpenXML");
                }
            } else if (lower.endsWith(".xlsx")) {
                try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(rawBytes))) {
                    StringBuilder sb = new StringBuilder();
                    for (Sheet sheet : workbook) {
                        sb.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---\n");
                        for (Row row : sheet) {
                            for (Cell cell : row) {
                                sb.append(cell.toString()).append("\t");
                            }
                            sb.append("\n");
                        }
                    }
                    extractedText = sb.toString().trim();
                    metaMap.put("DocumentType", "Microsoft Excel OpenXML");
                }
            }

            // If empty or PDF / generic, use Apache Tika
            if (extractedText.isEmpty()) {
                BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024); // 10MB limit
                Metadata metadata = new Metadata();
                AutoDetectParser parser = new AutoDetectParser();
                ParseContext context = new ParseContext();

                try (InputStream is = new java.io.ByteArrayInputStream(rawBytes)) {
                    parser.parse(is, handler, metadata, context);
                    extractedText = handler.toString().trim();

                    for (String name : metadata.names()) {
                        metaMap.put(name, metadata.get(name));
                    }
                }
            }

            return new ParseResult(extractedText, metaMap, checksum, true, null);

        } catch (Exception e) {
            log.warn("Document extraction encountered an issue for {}: {}", filename, e.getMessage());
            return new ParseResult(extractedText, metaMap, checksum, false, "Extraction partial or failed: " + e.getMessage());
        }
    }

    public boolean containsRequiredKeywords(String extractedText, String[] keywords) {
        if (extractedText == null || keywords == null || keywords.length == 0) {
            return false;
        }
        String lower = extractedText.toLowerCase();
        for (String kw : keywords) {
            if (!lower.contains(kw.toLowerCase().trim())) {
                return false;
            }
        }
        return true;
    }
}
