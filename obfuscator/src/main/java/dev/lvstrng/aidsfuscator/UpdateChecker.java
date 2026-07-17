package dev.lvstrng.aidsfuscator;

import com.google.gson.JsonParser;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private static String getLatestReleaseTag() throws IOException {
        var connection = (HttpURLConnection) new URL(
                "https://api.github.com/repos/LvStrnggg/aidsfuscator/releases/latest"
        ).openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Logger.error("Invalid status code");
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
                Logger.warn("Running %s (Outdated). Please update to the latest version (%s)", AidsfuscatorInfo.versionText(), latest);
                return;
            }

            Logger.info("Running %s (Up to date)", AidsfuscatorInfo.versionText());
        } catch (IOException e) {
            Logger.error("There was an error checking for the latest version of Aidsfuscator, running anyway", e);
        }
    }
}
