package dev.lvstrng.aidsfuscator.context.resource.handled;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.HandledResource;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.IOException;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class FabricModJsonHandler implements HandledResource {
    private final List<String> entrypointTypes = List.of(
            "main", "client"
    );

    @Override
    public void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException {
        var gson = new Gson();
        var modJson = gson.fromJson(new String(bytes), JsonObject.class);

        var entrypoints = modJson.getAsJsonObject("entrypoints");
        if(entrypoints == null) {
            Logger.warn("Ignoring `" + name + "`, no entry points found");
            return;
        }

        for(var type : entrypointTypes) {
            var p = entrypoints.get(type);
            if(p == null)
                continue;

            var entrypoint = p.getAsJsonArray();
            if(entrypoint == null)
                continue;

            for(int i = 0; i < entrypoint.size(); i++) {
                var className = entrypoint.get(i).getAsString().replace('.', '/');
                var newName = Mappings.CLASS.retrieve(className).value().replace('/', '.');
                entrypoint.set(i, new JsonPrimitive(newName));
            }
        }

        jos.putNextEntry(new ZipEntry(name));
        jos.write(gson.toJson(modJson).getBytes());
        jos.closeEntry();
    }
}
