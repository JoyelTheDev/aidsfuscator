package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.config.ConfigLoader;
import dev.lvstrng.aidsfuscator.log.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.info("Current version: %s", AidsfuscatorInfo.versionText());
        if(args.length < 1) {
            Logger.error("Specify a config path in the 'workspace' directory. Usage: java -jar aidsfuscator.jar config.json");
            return;
        }

        var loader = new ConfigLoader(args[0]);
        loader.load();

        loader.result().initialize()
                .transform()
                .exportJar();
    }
}
