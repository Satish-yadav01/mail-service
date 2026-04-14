package com.satish.mailservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GoogleDriveUtil {

    private static final String DRIVE_DOWNLOAD_URL = "https://drive.google.com/uc?export=download&id=";
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("/d/([a-zA-Z0-9_-]+)");
    private static final Pattern FILE_ID_PATTERN_2 = Pattern.compile("id=([a-zA-Z0-9_-]+)");

    /**
     * Extract file ID from Google Drive link
     * Supports formats:
     * - https://drive.google.com/file/d/FILE_ID/view
     * - https://drive.google.com/open?id=FILE_ID
     * - https://drive.google.com/uc?id=FILE_ID
     */
    public String extractFileId(String driveLink) {
        if (driveLink == null || driveLink.isEmpty()) {
            throw new IllegalArgumentException("Drive link cannot be empty");
        }

        // Try pattern 1: /d/FILE_ID
        Matcher matcher = FILE_ID_PATTERN.matcher(driveLink);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try pattern 2: id=FILE_ID
        matcher = FILE_ID_PATTERN_2.matcher(driveLink);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Invalid Google Drive link format. Could not extract file ID.");
    }

    /**
     * Get direct download URL from Google Drive link
     */
    public String getDirectDownloadUrl(String driveLink) {
        String fileId = extractFileId(driveLink);
        return DRIVE_DOWNLOAD_URL + fileId;
    }

    /**
     * Download file from Google Drive to temporary location
     * Returns the temporary file path
     */
    public File downloadFromDrive(String driveLink, String fileName) throws IOException {
        String downloadUrl = getDirectDownloadUrl(driveLink);
        
        // Create temp directory if not exists
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "mail-service-resumes");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        // Generate unique filename to avoid conflicts
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        File tempFile = tempDir.resolve(uniqueFileName).toFile();

        log.info("Downloading file from Google Drive: {}", driveLink);
        log.info("Download URL: {}", downloadUrl);
        log.info("Temp file: {}", tempFile.getAbsolutePath());

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);

            // Handle redirects manually for Google Drive
            int responseCode = connection.getResponseCode();
            
            // Google Drive may return 302/303 for large files
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                
                String newUrl = connection.getHeaderField("Location");
                connection.disconnect();
                
                url = new URL(newUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                responseCode = connection.getResponseCode();
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file. HTTP response code: " + responseCode);
            }

            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            log.info("Successfully downloaded {} bytes to {}", totalBytesRead, tempFile.getAbsolutePath());
            return tempFile;

        } catch (Exception e) {
            // Clean up temp file if download failed
            if (tempFile.exists()) {
                tempFile.delete();
            }
            log.error("Error downloading file from Google Drive", e);
            throw new IOException("Failed to download file from Google Drive: " + e.getMessage(), e);
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Validate if the link is a valid Google Drive link
     */
    public boolean isValidDriveLink(String driveLink) {
        if (driveLink == null || driveLink.isEmpty()) {
            return false;
        }
        
        return driveLink.contains("drive.google.com") && 
               (driveLink.contains("/d/") || driveLink.contains("id="));
    }

    /**
     * Clean up temporary file
     */
    public void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
                log.info("Cleaned up temp file: {}", file.getAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}
