package dev.lvstrng.aidsfuscator.context.resource.handled;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.HandledResource;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class FabricMixinJsonHandler implements HandledResource {
    @Override
    public void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException {
        var gson = new Gson();
        var modJson = gson.fromJson(new String(bytes), JsonObject.class);

        if(!modJson.has("package")) {
            Logger.warn("Couldn't handle `%s`. Couldn't find `package` field in mixin json", name);
            jos.putNextEntry(new ZipEntry(name));
            jos.write(bytes);
            jos.closeEntry();
            return;
        }

        var pkg = modJson.get("package").getAsString();
        if(modJson.has("mixins"))
            handle(modJson, pkg, "mixins");

        if(modJson.has("client"))
            handle(modJson, pkg, "client");

        jos.putNextEntry(new ZipEntry(name));
        jos.write(gson.toJson(modJson).getBytes());
        jos.closeEntry();
    }

    private void handle(JsonObject modJson, String pkg, String type) {
        var mixinArr = modJson.get(type).getAsJsonArray();

        for(int i = 0; i < mixinArr.size(); i++) {
            var mixin = pkg + "/" + mixinArr.get(i).getAsString();
            mixin = mixin.replace('.', '/');

            if(!Mappings.CLASS.containsOld(mixin))
                continue;

            mixinArr.set(i, new JsonPrimitive(Mappings.CLASS.retrieve(mixin).value().replace('/', '.')));
        }
    }
}
