// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/Vortexia/vortexia-core/releases";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?");

    private final String currentVersion;
    private final String currentLoader;
    private final Consumer<String> infoLogger;
    private final Consumer<String> warnLogger;
    private final Consumer<String> debugLogger;
    private final Consumer<Runnable> asyncExecutor;

    private String latestVersion;
    private volatile boolean updateAvailable;
    private volatile boolean checked;

    public UpdateChecker(String currentVersion, String currentLoader,
                         Consumer<String> infoLogger, Consumer<String> warnLogger, Consumer<String> debugLogger,
                         Consumer<Runnable> asyncExecutor) {
        this.currentVersion = currentVersion;
        this.currentLoader = currentLoader.toLowerCase();
        this.infoLogger = infoLogger;
        this.warnLogger = warnLogger;
        this.debugLogger = debugLogger;
        this.asyncExecutor = asyncExecutor;
        this.updateAvailable = false;
        this.checked = false;
    }

    public CompletableFuture<Void> checkAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        asyncExecutor.accept(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(GITHUB_API_URL).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "VortexiaCore-UpdateChecker");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    debugLogger.accept("Update check failed with HTTP " + responseCode);
                    checked = true;
                    future.complete(null);
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                List<ReleaseInfo> releases = parseReleases(response.toString());
                String foundVersion = null;
                for (ReleaseInfo release : releases) {
                    if (isCompatible(release.body)) {
                        foundVersion = release.tag.replaceFirst("^v", "");
                        break;
                    }
                }

                if (foundVersion != null) {
                    latestVersion = foundVersion;
                    updateAvailable = isOlderVersion(currentVersion, latestVersion);
                    checked = true;

                    if (updateAvailable) {
                        warnLogger.accept("A new version of VortexiaCore for " + currentLoader + " is available: v" + latestVersion + " (current: v" + currentVersion + ")");
                        warnLogger.accept("Download: https://github.com/Vortexia/vortexia-core/releases");
                    } else {
                        infoLogger.accept("VortexiaCore (" + currentLoader + ") is up to date (v" + currentVersion + ")");
                    }
                } else {
                    checked = true;
                    debugLogger.accept("Update check: no compatible release found for loader: " + currentLoader);
                }
            } catch (Exception e) {
                debugLogger.accept("Update check failed: " + e.getMessage());
                checked = true;
            }
            future.complete(null);
        });
        return future;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean hasChecked() {
        return checked;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUpdateMessage() {
        if (!checked || !updateAvailable) return null;
        return "§eVortexiaCore update available for " + currentLoader + ": §fv" + latestVersion + " §7(current: v" + currentVersion + ")\n§7Download: §fhttps://github.com/Vortexia/vortexia-core/releases";
    }

    private boolean isCompatible(String body) {
        if (body == null || body.isEmpty()) return true;
        String lowerBody = body.toLowerCase();
        
        Pattern pattern = Pattern.compile("loaders?\\s*:\\s*([^\\r\\n]+)");
        Matcher matcher = pattern.matcher(lowerBody);
        if (matcher.find()) {
            String loadersList = matcher.group(1);
            if (loadersList.contains("all")) return true;
            return loadersList.contains(currentLoader);
        }
        
        Pattern bracketPattern = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher bracketMatcher = bracketPattern.matcher(lowerBody);
        while (bracketMatcher.find()) {
            String content = bracketMatcher.group(1);
            if (content.contains(currentLoader)) return true;
        }
        
        return !lowerBody.contains("loader:");
    }

    private static class ReleaseInfo {
        String tag;
        String body;
    }

    private static List<ReleaseInfo> parseReleases(String jsonArray) {
        List<ReleaseInfo> list = new ArrayList<>();
        int index = 0;
        while (true) {
            int start = jsonArray.indexOf("{", index);
            if (start == -1) break;
            int end = findClosingBrace(jsonArray, start);
            if (end == -1) break;
            String obj = jsonArray.substring(start, end + 1);
            ReleaseInfo info = new ReleaseInfo();
            info.tag = extractJsonField(obj, "tag_name");
            info.body = extractJsonField(obj, "body");
            if (info.tag != null) {
                list.add(info);
            }
            index = end + 1;
        }
        return list;
    }

    private static int findClosingBrace(String json, int start) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (!inQuote) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":";
        int index = json.indexOf(search);
        if (index == -1) return null;
        int start = json.indexOf("\"", index + search.length());
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private static boolean isOlderVersion(String current, String latest) {
        int[] currentParts = parseVersion(current);
        int[] latestParts = parseVersion(latest);

        if (currentParts == null || latestParts == null) {
            return !current.equals(latest);
        }

        for (int i = 0; i < 3; i++) {
            if (currentParts[i] < latestParts[i]) return true;
            if (currentParts[i] > latestParts[i]) return false;
        }

        boolean currentIsPre = current.contains("-");
        boolean latestIsPre = latest.contains("-");

        if (currentIsPre && !latestIsPre) return true;
        if (!currentIsPre && !latestIsPre) return false;
        if (currentIsPre && latestIsPre) return current.compareTo(latest) < 0;

        return false;
    }

    private static int[] parseVersion(String version) {
        String clean = version.replaceFirst("^v", "");
        Matcher matcher = VERSION_PATTERN.matcher(clean);
        if (!matcher.matches()) return null;
        return new int[]{
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))
        };
    }
}
