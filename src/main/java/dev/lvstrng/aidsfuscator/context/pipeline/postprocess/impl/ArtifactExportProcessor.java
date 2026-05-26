package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.asm.HierarchyClassWriter;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ArtifactExportProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        Logger.info("Exporting JAR...");
        var outputFile = new File(context.out());

        try (var jos = new JarOutputStream(new FileOutputStream(outputFile))) {
            var classes = new ArrayList<>(context.jarClasses());
            classes.addAll(context.artificials().values());

            for(var clazz : classes) {
                var writer = new HierarchyClassWriter(context);

                try {
                    clazz.core().accept(writer);
                } catch (Exception e) {
                    Logger.error("Error writing class %s", clazz.name());
                    e.printStackTrace();
                }

                jos.putNextEntry(new ZipEntry(clazz.name() + ".class"));
                jos.write(writer.toByteArray());
                jos.closeEntry();
            }

            context.resourceHandler().handle(jos);
        } catch (IOException e) {
            Logger.error("Error writing output JAR: %s", e);
        }

        Logger.success("Exported JAR successfully!");
        Logger.success("%s (%skb) -> %s (%skb)",
                context.in(), Utils.bytesToKB(new File(context.in()).length()),
                context.out(), Utils.bytesToKB(outputFile.length())
        );
    }
}
