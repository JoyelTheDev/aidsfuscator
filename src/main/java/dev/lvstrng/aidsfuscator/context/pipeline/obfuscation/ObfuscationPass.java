package dev.lvstrng.aidsfuscator.context.pipeline.obfuscation;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IPass;
import dev.lvstrng.aidsfuscator.log.Logger;

public class ObfuscationPass implements IPass {
    @Override
    public void run(Context context) {
        Logger.info("Running obfuscation pass...");

        for(var transformer : context.transformers()) {
            Logger.info("Running '%s'", transformer.name());
            transformer.transform(context);
            Logger.success("Completed running '%s' with %s changes", transformer.name(), transformer.changes());
            Logger.info("");
        }

        Logger.success("Completed obfuscation pass!");
    }
}
