package dev.lvstrng.aidsfuscator;

import com.google.gson.JsonParser;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.*;
import java.net.*;

public class UpdateChecker {
    private static String getLatestReleaseTag() throws IOException {
        var connection = (HttpURLConnection) new URL(
                "https://api.github.com/repos/LvStrnggg/aidsfuscator/releases/latest"
        ).openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalArgumentException("Invalid status code");
        }

        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        var response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null)
            response.append(line);

        reader.close();
        return JsonParser.parseString(response.toString()).getAsJsonObject().get("tag_name").getAsString();
    }

    public static void checkAndPrintUpdates() {
        try {
            var latest = getLatestReleaseTag();
            var current = AidsfuscatorInfo.build();
            if (!latest.equals(current)) {
                Logger.warn("You're using an outdated version of Aidsfuscator, current version: %s, latest version: %s", current, latest);
                return;
            }

            Logger.info("Everything is up to date. Current version: %s", AidsfuscatorInfo.versionText());
        } catch (IOException e) {
            Logger.error("There was an error checking for the latest version of Aidsfuscator", e);
        }
    }
}
