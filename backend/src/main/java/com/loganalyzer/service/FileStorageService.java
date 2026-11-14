package com.loganalyzer.service;

import com.loganalyzer.exception.AppException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

@Service
public class FileStorageService {

    private final Path storageDirectory;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "log", "txt", "json", "csv", "out", "gz"
    );

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new AppException("Could not create upload storage directory", e);
        }
    }

    public StoredFile storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException("Failed to store empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new AppException("Invalid file path in uploaded filename: " + originalFilename);
        }

        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException("Unsupported file format: ." + extension + ". Allowed formats: " + ALLOWED_EXTENSIONS);
        }

        String storedFilename = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path targetLocation = this.storageDirectory.resolve(storedFilename);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                Files.copy(dis, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(md.digest());

            return new StoredFile(
                    storedFilename,
                    originalFilename,
                    file.getSize(),
                    extension,
                    targetLocation.toString(),
                    checksum
            );
        } catch (Exception e) {
            throw new AppException("Could not store file " + originalFilename + ". Please try again!", e);
        }
    }

    public BufferedReader getBufferedReader(String storagePath, String extension) throws IOException {
        File file = new File(storagePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Log file not found at " + storagePath);
        }

        InputStream fileStream = new FileInputStream(file);
        if ("gz".equalsIgnoreCase(extension)) {
            fileStream = new GZIPInputStream(fileStream);
        }
        return new BufferedReader(new InputStreamReader(fileStream));
    }

    public InputStream getInputStream(String storagePath, String extension) throws IOException {
        File file = new File(storagePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Log file not found at " + storagePath);
        }

        InputStream fileStream = new FileInputStream(file);
        if ("gz".equalsIgnoreCase(extension)) {
            return new GZIPInputStream(fileStream);
        }
        return fileStream;
    }

    public void deleteFile(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log and continue
        }
    }

    public record StoredFile(
            String storedFilename,
            String originalFilename,
            long size,
            String format,
            String absolutePath,
            String checksum
    ) {}
}
