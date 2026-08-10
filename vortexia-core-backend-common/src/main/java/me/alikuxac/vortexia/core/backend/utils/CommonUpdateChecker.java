// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.utils;

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

public class CommonUpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/Vortexia/vortexia-core/releases";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z]+))?(?:\\.(?:b\\.)?(\\d+))?");

    private final String currentVersion;
    private final String currentLoader;
    private final Consumer<String> infoLogger;
    private final Consumer<String> warnLogger;
    private final Consumer<String> debugLogger;
    private final Consumer<Runnable> asyncExecutor;

    private String latestVersion;
    private volatile boolean updateAvailable;
    private volatile boolean checked;

    public CommonUpdateChecker(String currentVersion, String currentLoader,
                               Consumer<String> infoLogger, Consumer<String> warnLogger, Consumer<String> debugLogger,
                               Consumer<Runnable> asyncExecutor) {
        this.currentVersion = currentVersion.replaceFirst("^v", "");
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
                        String versionStr = release.tag.replaceFirst("^v", "");
                        if (isTargetReleaseMatchesCurrent(currentVersion, versionStr) && isOlderVersion(currentVersion, versionStr)) {
                            foundVersion = versionStr;
                            break;
                        }
                    }
                }

                if (foundVersion != null) {
                    latestVersion = foundVersion;
                    updateAvailable = true;
                    checked = true;
                    warnLogger.accept("A new version of VortexiaCore for " + currentLoader + " is available: v" + latestVersion + " (current: v" + currentVersion + ")");
                    warnLogger.accept("Download: https://github.com/Vortexia/vortexia-core/releases");
                } else {
                    checked = true;
                    infoLogger.accept("VortexiaCore (" + currentLoader + ") is up to date (v" + currentVersion + ")");
                }
            } catch (Exception e) {
                debugLogger.accept("Update check failed: " + e.getMessage());
                checked = true;
            }
            future.complete(null);
        });
        return future;
    }

    private boolean isTargetReleaseMatchesCurrent(String current, String target) {
        String currLower = current.toLowerCase();
        String targLower = target.toLowerCase();

        boolean currentIsAlpha = currLower.contains("alpha");
        boolean currentIsBeta = currLower.contains("beta");
        boolean currentIsRelease = !currentIsAlpha && !currentIsBeta;

        boolean targetIsAlpha = targLower.contains("alpha");
        boolean targetIsBeta = targLower.contains("beta");
        boolean targetIsRelease = !targetIsAlpha && !targetIsBeta;

        if (currentIsRelease) {
            // Chỉ nhận bản target là release chính thức
            return targetIsRelease;
        }
        if (currentIsBeta) {
            // Nhận bản target là release chính thức hoặc beta (bỏ qua alpha)
            return targetIsRelease || targetIsBeta;
        }
        // Nếu hiện tại là alpha: nhận tất cả (cả alpha, beta và release chính thức)
        return true;
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
        Version currVer = Version.parse(current);
        Version lateVer = Version.parse(latest);

        if (currVer == null || lateVer == null) {
            return !current.equals(latest);
        }

        // So sánh 3 số đầu chính (Major.Minor.Patch)
        for (int i = 0; i < 3; i++) {
            if (currVer.parts[i] < lateVer.parts[i]) return true;
            if (currVer.parts[i] > lateVer.parts[i]) return false;
        }

        // Nếu 3 số đầu giống nhau, so sánh loại release: Release chính thức > Beta > Alpha
        int currTypeVal = getReleaseTypePriority(currVer.type);
        int lateTypeVal = getReleaseTypePriority(lateVer.type);

        if (currTypeVal < lateTypeVal) return true;
        if (currTypeVal > lateTypeVal) return false;

        // Nếu cùng loại (ví dụ cùng alpha hoặc cùng beta), so sánh build number
        return currVer.build < lateVer.build;
    }

    private static int getReleaseTypePriority(String type) {
        if (type == null || type.isEmpty()) return 3; // Release chính thức (cao nhất)
        String t = type.toLowerCase();
        if (t.contains("beta")) return 2;
        if (t.contains("alpha")) return 1; // Alpha (thấp nhất)
        return 0;
    }

    private static class Version {
        int[] parts;
        String type;
        int build;

        static Version parse(String versionStr) {
            Matcher m = VERSION_PATTERN.matcher(versionStr);
            if (!m.matches()) return null;
            Version v = new Version();
            v.parts = new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
            };
            v.type = m.group(4);
            String buildStr = m.group(5);
            v.build = buildStr != null ? Integer.parseInt(buildStr) : 0;
            return v;
        }
    }
}
