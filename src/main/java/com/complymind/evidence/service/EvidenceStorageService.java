package com.complymind.evidence.service;

import com.complymind.common.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class EvidenceStorageService {

    private final Path rootLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".pdf", ".docx", ".xlsx", ".doc", ".xls", ".txt", ".csv"
    );

    public EvidenceStorageService(@Value("${app.storage.upload-dir:./uploads/evidence}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize evidence storage directory: " + uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Cannot upload an empty document");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessValidationException("Invalid document filename");
        }

        String cleanFilename = Paths.get(originalFilename).getFileName().toString();
        String lower = cleanFilename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!validExt) {
            throw new BusinessValidationException("Unsupported document format. Allowed extensions: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Validate File Signature / Magic Bytes
        validateFileSignature(file, lower);

        String extension = cleanFilename.contains(".") ? cleanFilename.substring(cleanFilename.lastIndexOf(".")) : "";
        String storedFilename = UUID.randomUUID().toString() + extension;

        try {
            Path destinationFile = this.rootLocation.resolve(storedFilename).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BusinessValidationException("Security Alert: Path traversal attempt detected");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + cleanFilename, e);
        }
    }

    public Path load(String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new BusinessValidationException("Security Alert: Invalid stored filename");
        }
        Path file = rootLocation.resolve(storedFilename).normalize().toAbsolutePath();
        if (!file.startsWith(rootLocation)) {
            throw new BusinessValidationException("Security Alert: Path traversal detected on document load");
        }
        return file;
    }

    /**
     * Checks magic bytes / file signature to verify that file matches claimed extension.
     */
    private void validateFileSignature(MultipartFile file, String lowerFilename) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessValidationException("Document is too small or corrupted (incomplete header)");
            }

            if (lowerFilename.endsWith(".pdf")) {
                // PDF magic bytes: %PDF (0x25 0x50 0x44 0x46)
                if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                    throw new BusinessValidationException("Invalid PDF document: File header signature does not match PDF format");
                }
            } else if (lowerFilename.endsWith(".docx") || lowerFilename.endsWith(".xlsx")) {
                // ZIP-based Office documents (PK.. : 0x50 0x4B 0x03 0x04)
                if (header[0] != 0x50 || header[1] != 0x4B) {
                    throw new BusinessValidationException("Invalid Office OpenXML document: Header signature does not match ZIP container format");
                }
            }
        } catch (IOException e) {
            throw new BusinessValidationException("Failed to inspect document file signature: " + e.getMessage());
        }
    }
}
