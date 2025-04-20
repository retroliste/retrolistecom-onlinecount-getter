package com.retroliste.plugin;

import com.eu.habbo.Emulator;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String REPO_OWNER = "retroliste";
    private static final String REPO_NAME = "retrolistecom-onlinecount-getter";
    private static final String API_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME;
    // JAR name will be constructed dynamically based on the version
    private static final String JAR_NAME_PATTERN = "retrolist-%s-jar-with-dependencies.jar";
    private String downloadUrl;

    private String currentVersion;
    private String latestVersion;
    private String changelog;
    private boolean updateAvailable = false;

    public UpdateChecker() {
        // Get current version from pom.xml (already loaded by Maven)
        this.currentVersion = getClass().getPackage().getImplementationVersion();
        if (this.currentVersion == null) {
            // Fallback to pom.xml version if not available
            this.currentVersion = "1.0.0"; // This should match your pom.xml
        }

        // Build the download URL with the current version
        String jarName = String.format(JAR_NAME_PATTERN, this.currentVersion);
        this.downloadUrl = "https://github.com/" + REPO_OWNER + "/" + REPO_NAME + "/raw/master/target/" + jarName;
    }

    public void checkForUpdates() {
        LOGGER.info("[RetroListe] Checking for updates...");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Check latest commit/release
            HttpGet request = new HttpGet(API_URL + "/commits/master");
            request.addHeader("Accept", "application/vnd.github.v3+json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    JSONObject json = new JSONObject(result);

                    String latestCommitSha = json.getString("sha");
                    this.changelog = json.getJSONObject("commit").getString("message");

                    // Use commit SHA as fallback version
                    latestVersion = latestCommitSha.substring(0, 7); // Short SHA

                    // First priority: Try to get version from pom.xml
                    try {
                        HttpGet pomRequest = new HttpGet(API_URL + "/contents/pom.xml");
                        pomRequest.addHeader("Accept", "application/vnd.github.v3+json");

                        try (CloseableHttpResponse pomResponse = httpClient.execute(pomRequest)) {
                            if (pomResponse.getStatusLine().getStatusCode() == 200) {
                                HttpEntity pomEntity = pomResponse.getEntity();
                                if (pomEntity != null) {
                                    String pomResult = EntityUtils.toString(pomEntity);
                                    JSONObject pomJson = new JSONObject(pomResult);

                                    // Decode content from base64
                                    String content = pomJson.getString("content");
                                    String pomContent = new String(java.util.Base64.getDecoder().decode(content.replaceAll("\\n", "")));

                                    // Extract version from pom.xml (simple regex approach)
                                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<version>(.*?)</version>");
                                    java.util.regex.Matcher matcher = pattern.matcher(pomContent);
                                    if (matcher.find()) {
                                        // The first occurrence should be the project version
                                        this.latestVersion = matcher.group(1).trim();
                                        LOGGER.info("[RetroListe] Found version in pom.xml: " + this.latestVersion);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[RetroListe] Could not get version from pom.xml, trying VERSION file");
                    }

                    // Second priority: Check if there's a VERSION file in the repo
                    if (latestVersion.equals(latestCommitSha.substring(0, 7))) {
                        try {
                            HttpGet versionRequest = new HttpGet(API_URL + "/contents/VERSION");
                            versionRequest.addHeader("Accept", "application/vnd.github.v3+json");

                            try (CloseableHttpResponse versionResponse = httpClient.execute(versionRequest)) {
                                if (versionResponse.getStatusLine().getStatusCode() == 200) {
                                    HttpEntity versionEntity = versionResponse.getEntity();
                                    if (versionEntity != null) {
                                        String versionResult = EntityUtils.toString(versionEntity);
                                        JSONObject versionJson = new JSONObject(versionResult);

                                        // Decode content from base64
                                        String content = versionJson.getString("content");
                                        String version = new String(java.util.Base64.getDecoder().decode(content.replaceAll("\\n", "")));
                                        this.latestVersion = version.trim();
                                        LOGGER.info("[RetroListe] Found version in VERSION file: " + this.latestVersion);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("[RetroListe] Could not get VERSION file, using commit SHA as version: " + latestVersion);
                        }
                    }

                    // Compare versions
                    boolean isNewer = false;

                    // Try semantic versioning comparison if both versions have a semantic format
                    if (isSemanticVersion(this.currentVersion) && isSemanticVersion(this.latestVersion)) {
                        isNewer = compareSemanticVersions(this.latestVersion, this.currentVersion) > 0;
                    } else {
                        // Fallback to string comparison
                        isNewer = !this.currentVersion.equals(this.latestVersion);
                    }

                    if (isNewer) {
                        this.updateAvailable = true;
                        LOGGER.info("[RetroListe] Update available! Current version: " + this.currentVersion + ", Latest version: " + this.latestVersion);
                        LOGGER.info("[RetroListe] Changelog: " + this.changelog);
                    } else {
                        LOGGER.info("[RetroListe] Plugin is up to date. Version: " + this.currentVersion);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[RetroListe] Failed to check for updates", e);
        }
    }

    /**
     * Check if a string is a semantic version (e.g., "1.0.0")
     */
    private boolean isSemanticVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*");
    }

    /**
     * Compare two semantic versions
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private int compareSemanticVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (p1 != p2) {
                return p1 - p2;
            }
        }

        return 0;
    }

    public boolean downloadUpdate() {
        if (!this.updateAvailable) {
            LOGGER.info("[RetroListe] No update available to download.");
            return false;
        }

        LOGGER.info("[RetroListe] Downloading update...");

        try {
            // Create plugins/updates directory if it doesn't exist
            Path updatesDir = Paths.get("plugins", "updates");
            if (!Files.exists(updatesDir)) {
                Files.createDirectories(updatesDir);
            }

            // Build the download URL with the latest version
            String latestJarName = String.format(JAR_NAME_PATTERN, this.latestVersion);
            String downloadUrl = "https://github.com/" + REPO_OWNER + "/" + REPO_NAME + "/raw/master/target/" + latestJarName;

            LOGGER.info("[RetroListe] Downloading from: " + downloadUrl);

            // Download the file
            URL url = new URL(downloadUrl);
            Path targetPath = updatesDir.resolve("retrolist-latest.jar");

            try (InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("[RetroListe] Update downloaded successfully to " + targetPath);

            // Create an update marker file
            Path updateMarker = updatesDir.resolve("update-pending.txt");
            Files.write(updateMarker, this.latestVersion.getBytes());

            return true;
        } catch (Exception e) {
            LOGGER.error("[RetroListe] Failed to download update", e);
            return false;
        }
    }

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    public String getLatestVersion() {
        return this.latestVersion;
    }

    public String getChangeLog() {
        return this.changelog;
    }

    public String getCurrentVersion() {
        return this.currentVersion;
    }

    public static boolean applyPendingUpdate() {
        Path updateMarker = Paths.get("plugins", "updates", "update-pending.txt");
        if (!Files.exists(updateMarker)) {
            return false;
        }

        try {
            String newVersion = new String(Files.readAllBytes(updateMarker));
            Path updatedJar = Paths.get("plugins", "updates", "retrolist-latest.jar");
            Path currentJar = Paths.get("plugins", "RetroListe.jar");

            // Backup current jar
            Path backupJar = Paths.get("plugins", "RetroListe.jar.bak");
            Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);

            // Replace current jar with the updated one
            Files.copy(updatedJar, currentJar, StandardCopyOption.REPLACE_EXISTING);

            // Delete marker file
            Files.delete(updateMarker);

            LOGGER.info("[RetroListe] Update applied successfully! New version: " + newVersion);
            return true;
        } catch (IOException e) {
            LOGGER.error("[RetroListe] Failed to apply update", e);
            return false;
        }
    }

    /**
     * Schedule update checks periodically
     */
    public void scheduleUpdateChecks(int initialDelayHours, int periodHours) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            checkForUpdates();
            if (isUpdateAvailable()) {
                downloadUpdate();
            }
        }, initialDelayHours, periodHours, TimeUnit.HOURS);
    }
}