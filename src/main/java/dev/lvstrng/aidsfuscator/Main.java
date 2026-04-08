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
    private static final String configPrefix = "--config=";
    private static final String exclusionPrefix = "--exclusions=";
    private static final String initOrderPrefix = "--initOrder=";

    private static final String BAD_ARGS = """
            Usage tutorial.
            You ran aidsfuscator with no arguments (or bad arguments). Aidsfuscator is a CLI tool, run the obfuscator using any of these args:
            These files have to be in the `workspace/` folder provided in the ZIP file. If the path contains spaces, add double quotes.
            \t`--config=` (Required)
            \t`--exclusions=` (Optional)
            \t`--initOrder=` (Optional)
            """;

    public static void main(String[] args) {
        UpdateChecker.checkAndPrintUpdates();
        if(args.length == 0) {
            Logger.error(BAD_ARGS);
            return;
        }

        // ---- PARSE ARGS ----
        var configPath = "";
        var exclusionPath = "";
        var initOrderPath = "";
        for(var arg : args) {
            if(arg.startsWith(configPrefix))
                configPath = arg.substring(configPrefix.length());

            if(arg.startsWith(exclusionPrefix))
                exclusionPath = arg.substring(exclusionPrefix.length());

            if(arg.startsWith(initOrderPrefix))
                initOrderPath = arg.substring(initOrderPrefix.length());
        }

        if(configPath.isEmpty()) {
            Logger.error(BAD_ARGS);
            return;
        }

        // ---- LOAD CONFIGS ----
        var loader = new ConfigLoader(configPath);
        var context = loader.result();
        loader.load();

        if(!exclusionPath.isEmpty())
            new ExclusionLoader(exclusionPath).load();

        if(!initOrderPath.isEmpty())
            new ClassInitOrderLoader(context, initOrderPath).load();

        // ---- RUN OBFUSCATOR ----
        context.initialize()
                .transform()
                .exportJar();

        // ---- SAVE CONFIGS ----
        try {
            new ConfigWriter(context, configPath).write();
            if(!exclusionPath.isEmpty())
                new ExclusionWriter(exclusionPath).write();

            if(!initOrderPath.isEmpty())
                new ClassInitOrderWriter(context, initOrderPath).write();
        } catch (IOException e) {
            Logger.error("An exception was thrown when saving configs:");
            e.printStackTrace();
        }
    }
}
