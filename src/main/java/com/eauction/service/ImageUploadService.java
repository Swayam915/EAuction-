package com.eauction.service;

import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles secure image uploads for auction products.
 *
 * Images are stored under  {appRealPath}/uploads/products/
 * and served via the URL   /EAuction/uploads/products/{filename}
 *
 * Security:
 *  - Whitelist of allowed MIME types
 *  - Filename is a random UUID (no user-controlled filenames on disk)
 *  - Max file size: 5 MB
 */
public class ImageUploadService {

    private static final Logger LOG = Logger.getLogger(ImageUploadService.class.getName());

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;   // 5 MB
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif")
    );

    private final String uploadDir;   // absolute path on disk

    public ImageUploadService(String appRealPath) {
        this.uploadDir = appRealPath + File.separator + "uploads" + File.separator + "products";
    }

    /**
     * Saves the uploaded Part to disk.
     *
     * @param part  the multipart form part
     * @return relative URL path  (e.g. "uploads/products/abc123.jpg")
     *         or null on failure / no file
     */
    public String saveImage(Part part) {
        if (part == null || part.getSize() == 0) return null;

        String contentType = part.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            LOG.warning("ImageUploadService: rejected content-type=" + contentType);
            return null;
        }

        if (part.getSize() > MAX_SIZE_BYTES) {
            LOG.warning("ImageUploadService: file too large (" + part.getSize() + " bytes)");
            return null;
        }

        String extension = extensionFor(contentType);
        String filename   = UUID.randomUUID().toString() + extension;
        Path targetDir    = Paths.get(uploadDir);
        Path targetFile   = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return "uploads/products/" + filename;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "ImageUploadService: failed to save image", e);
            return null;
        }
    }

    /** Deletes a previously uploaded image by its relative path. */
    public void deleteImage(String relativePath, String appRealPath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path p = Paths.get(appRealPath, relativePath);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "ImageUploadService: could not delete " + relativePath, e);
        }
    }

    private String extensionFor(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "image/png":  return ".png";
            case "image/webp": return ".webp";
            case "image/gif":  return ".gif";
            default:           return ".jpg";
        }
    }
}
