package com.rag.engine.service.ingestion;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class FileHashService {

    public String calculateHash(MultipartFile file, String algorithm) throws IOException, NoSuchAlgorithmException {
        try (InputStream inputStream = file.getInputStream()) {
            return calculateHash(inputStream, algorithm);
        }
    }

    // Hash a local file on disk
    public String calculateHash(Path filePath, String algorithm) throws IOException, NoSuchAlgorithmException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return calculateHash(inputStream, algorithm);
        }
    }

    // Core streaming hash method
    private String calculateHash(InputStream inputStream, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        // DigestInputStream automatically updates the digest as bytes are read
        try (DigestInputStream dis = new DigestInputStream(inputStream, digest)) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            while (dis.read(buffer) != -1) {
                // Reading the stream updates the MessageDigest
            }
        }

        byte[] hashBytes = digest.digest();
        return HexFormat.of().formatHex(hashBytes); // Java 17+ hex conversion
    }
}
