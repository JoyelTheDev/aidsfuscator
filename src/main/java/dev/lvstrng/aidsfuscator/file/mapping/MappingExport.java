package dev.lvstrng.aidsfuscator.file.mapping;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Exports old-to-new names of obfuscated members to make keeping track of crashes and other things easier after obfuscation.
 * @author lvstrng
 */
public class MappingExport implements Writer {
    private final Context context;
    public MappingExport(Context context) {
        this.context = context;
    }

    @Override
    public void write() throws IOException {
        var file = Context.getFromWorkspace("mappings/latest.txt");
        if(!file.getParentFile().exists() && !file.getParentFile().mkdir()) {
            Logger.error("Couldn't create mappings folder");
            return;
        }

        if(!file.exists() && !file.createNewFile()) {
            Logger.error("Couldn't create mapping file");
            return;
        }

        var sb = new StringBuilder();
        for(var clazz : context.classes()) {
            if(!clazz.name().equals(clazz.originalName()))
                sb.append("class: ").append(clazz.originalName()).append(" -> ").append(clazz.name()).append('\n');
            else sb.append("class: ").append(clazz.originalName()).append('\n');

            for(var field : clazz.fields()) {
                if(field.name().equals(field.originalName()))
                    continue;

                sb.append("\tfield: ").append(field.originalName()).append(" -> ").append(field.name()).append('\n');
            }

            for(var method : clazz.methods()) {
                if(method.name().equals(method.originalName()))
                    continue;

                sb.append("\tmethod: ").append(method.originalName()).append(" -> ").append(method.name()).append('\n');
            }
            sb.append('\n');
        }

        Files.writeString(file.toPath(), sb.toString());
    }
}
