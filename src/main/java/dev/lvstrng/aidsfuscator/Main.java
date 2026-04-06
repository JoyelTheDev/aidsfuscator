package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.config.ConfigLoader;
import dev.lvstrng.aidsfuscator.config.ConfigWriter;
import dev.lvstrng.aidsfuscator.config.exclusions.ExclusionLoader;
import dev.lvstrng.aidsfuscator.config.exclusions.ExclusionWriter;
import dev.lvstrng.aidsfuscator.config.initOrder.ClassInitOrderLoader;
import dev.lvstrng.aidsfuscator.config.initOrder.ClassInitOrderWriter;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        UpdateChecker.checkAndPrintUpdates();
        if(args.length < 2) {
            Logger.error("Specify a config and exclusion file in the 'workspace' directory. Usage: java -jar aidsfuscator.jar <config.json> <exclusions.json>");
            return;
        }

        // ---- LOAD CONFIGS ----
        var loader = new ConfigLoader(args[0]); // load main config
        loader.load();
        new ExclusionLoader(args[1]).load(); // load exclusions

        var context = loader.result().initialize();
        if(args.length >= 3)
            new ClassInitOrderLoader(context, args[2]).load();

        // ---- RUN OBFUSCATOR ----
        context.transform().exportJar();

        // ---- SAVE CONFIGS ----
        try {
            new ConfigWriter(context, args[0]).write();
            new ExclusionWriter(args[1]).write();

            if(args.length >= 3)
                new ClassInitOrderWriter(context, args[2]);
        } catch (IOException e) {
            Logger.error("Failed to write config %s", e);
        }
    }
}
