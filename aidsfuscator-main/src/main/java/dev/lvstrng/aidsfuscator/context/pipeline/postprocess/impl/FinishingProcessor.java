package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.file.impl.ConfigWriter;
import dev.lvstrng.aidsfuscator.file.mapping.MappingExport;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;

public class FinishingProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        try {
            new MappingExport(context).write();
            Logger.success("Finished exporting files...");
        } catch (IOException _) {
            Logger.error("There was an error exporting files");
        }
    }
}
