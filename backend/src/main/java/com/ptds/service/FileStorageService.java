package com.ptds.service;

import com.ptds.config.StorageProperties;
import com.ptds.exception.FileValidationException;
import com.ptds.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Local-disk implementation of the storage backend. Files are addressed by
 * an opaque "storage key" (a UUID-based relative path) so the public API
 * never exposes real filesystem paths. A MinIO-backed implementation would
 * satisfy the same contract behind an interface in a later phase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageProperties storageProperties;

    /** Validates the upload (extension + size) and persists it, returning storage metadata. */
    public StoredFile store(MultipartFile multipartFile) {
        String originalName = sanitizeFilename(multipartFile.getOriginalFilename());
        String extension = extractExtension(originalName);

        if (!storageProperties.getAllowedExtensions().contains(extension.toLowerCase(Locale.ROOT))) {
            throw new FileValidationException("File type ." + extension + " is not permitted for distribution");
        }
        if (multipartFile.getSize() <= 0) {
            throw new FileValidationException("Uploaded file is empty");
        }
        if (multipartFile.getSize() > storageProperties.getMaxSizeBytes()) {
            throw new FileValidationException("File exceeds the maximum allowed upload size");
        }

        try {
            Path root = Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize();
            Files.createDirectories(root);

            String storageKey = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path destination = root.resolve(storageKey).normalize();
            if (!destination.startsWith(root)) {
                throw new FileValidationException("Invalid file path");
            }

            String checksum;
            try (InputStream in = multipartFile.getInputStream()) {
                checksum = copyAndHash(in, destination);
            }

            return new StoredFile(storageKey, originalName, multipartFile.getSize(),
                    multipartFile.getContentType(), checksum);
        } catch (IOException e) {
            throw new StorageException("Failed to store uploaded file", e);
        }
    }

    /** Loads a previously stored file as a streaming {@link Resource} for download. */
    public Resource load(String storageKey) {
        try {
            Path root = Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize();
            Path file = root.resolve(storageKey).normalize();
            if (!file.startsWith(root) || !Files.exists(file)) {
                throw new StorageException("Stored file is missing: " + storageKey);
            }
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new StorageException("Unable to resolve stored file", e);
        }
    }

    public Path resolvePath(String storageKey) {
        return Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize().resolve(storageKey).normalize();
    }

    private String copyAndHash(InputStream in, Path destination) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var digestStream = new java.security.DigestInputStream(in, digest)) {
                Files.copy(digestStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-256 algorithm unavailable", e);
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        String cleaned = Paths.get(name).getFileName().toString();
        return cleaned.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 ? filename.substring(dot + 1) : "";
    }

    public record StoredFile(String storageKey, String originalName, long sizeBytes,
                              String mimeType, String checksumSha256) {}
}
