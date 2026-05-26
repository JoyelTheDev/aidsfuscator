package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.cli.ArgumentParser;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.impl.ConfigLoader;
import dev.lvstrng.aidsfuscator.file.impl.ConfigWriter;
import dev.lvstrng.aidsfuscator.file.impl.exclusions.ExclusionLoader;
import dev.lvstrng.aidsfuscator.file.impl.initOrder.ClassInitOrderLoader;
import dev.lvstrng.aidsfuscator.file.impl.references.ReferenceLoader;
import dev.lvstrng.aidsfuscator.file.mapping.MappingExport;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;

public class Main {

    private static final String BAD_ARGS = """
            Usage tutorial.
            You ran aidsfuscator with no arguments (or bad arguments). Aidsfuscator is a CLI tool, run the obfuscator using any of these args:
            These files have to be in the `workspace/` folder provided in the ZIP file. If the path contains spaces, add double quotes.
            \t`--config=`     (Required)
            \t`--exclusions=` (Optional)
            \t`--initOrder=`  (Optional)
            \t`--references=` (Optional)
            \t'--javaPath='   (Optional) (Specifies the Java Runtime class path to use to obfuscate the JAR file)
            """;

    public static void main(String[] args) {
        UpdateChecker.checkAndPrintUpdates();
        if(args.length == 0) {
            Logger.error(BAD_ARGS);
            return;
        }

        var context = Context.newInstance();
        new ArgumentParser(context).parse(args);
        context.run();
    }
}
