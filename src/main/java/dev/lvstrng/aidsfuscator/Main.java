package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.config.ConfigLoader;
import dev.lvstrng.aidsfuscator.config.ConfigWriter;
import dev.lvstrng.aidsfuscator.config.ExclusionLoader;
import dev.lvstrng.aidsfuscator.config.ExclusionWriter;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Logger.info("Current version: %s", AidsfuscatorInfo.versionText());
        if(args.length < 2) {
            Logger.error("Specify a config and exclusion file in the 'workspace' directory. Usage: java -jar aidsfuscator.jar <config.json> <exclusions.json>");
            return;
        }

        // ---- LOAD CONFIGS ----
        var loader = new ConfigLoader(args[0]);
        loader.load();
        new ExclusionLoader(args[1]).load();

        // ---- RUN OBFUSCATOR ----
        loader.result().initialize()
                .transform()
                .exportJar();

        // ---- SAVE CONFIGS ----
        try {
            new ConfigWriter(loader.result(), args[0]).write();
            new ExclusionWriter(args[1]).write();
        } catch (IOException e) {
            Logger.error("Failed to write config %s", e);
        }
    }
}
