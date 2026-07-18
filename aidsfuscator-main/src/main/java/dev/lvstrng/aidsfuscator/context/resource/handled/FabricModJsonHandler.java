package dev.lvstrng.aidsfuscator.context.resource.handled;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.HandledResource;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.IOException;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class FabricModJsonHandler implements HandledResource {
    @Override
    public void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException {
        var gson = new Gson();
        var modJson = gson.fromJson(new String(bytes), JsonObject.class);

        var entrypoints = modJson.getAsJsonObject("entrypoints");
        if (entrypoints == null) {
            jos.putNextEntry(new ZipEntry(name));
            jos.write(bytes);
            jos.closeEntry();
            Logger.warn("Ignoring `" + name + "`, no entry points found");
            return;
        }

        for (Map.Entry<String, com.google.gson.JsonElement> typeEntry : entrypoints.entrySet()) {
            var value = typeEntry.getValue();
            if (value == null || !value.isJsonArray())
                continue;

            var entrypointArray = value.getAsJsonArray();
            for (int i = 0; i < entrypointArray.size(); i++) {
                var element = entrypointArray.get(i);
                var className = element.getAsString().replace('.', '/');
                var newName = Mappings.CLASS.retrieve(className).value().replace('/', '.');
                entrypointArray.set(i, new JsonPrimitive(newName));
            }
        }

        jos.putNextEntry(new ZipEntry(name));
        jos.write(gson.toJson(modJson).getBytes());
        jos.closeEntry();
    }
}
