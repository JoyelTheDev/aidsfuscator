package dev.lvstrng.aidsfuscator.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.TransformerOrder;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;

public class ConfigLoader {
    private final String configPath;
    private final Context context;

    public ConfigLoader(String configPath) {
        this.configPath = configPath;
        this.context = Context.newInstance();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        var file = Context.getFromWorkspace(configPath);
        if(!file.exists()) {
            Logger.warn("No config file found. Running with no obfuscation.");
            throw new RuntimeException();
        }
        Logger.info("Loading config (%s)...", configPath);

        // ---- CONFIG ----
        var configObject = new Gson().fromJson(Context.readWorkspaceString(configPath), JsonObject.class);
        context.in(configObject.get("in").getAsString())
                .out(configObject.get("out").getAsString())
                .libs(configObject.get("libs").getAsString())
                .setDictionary(configObject.get("dictionary").getAsString())
                .setWatermark(configObject.get("watermark").getAsString());

        if(configObject.get("computeFrames").getAsBoolean())
            context.computeFrames();
      // ---- TRANSFORMERS ----
        var transformers = configObject.get("transformers").getAsJsonObject();
        for(var transformer : TransformerOrder.transformers()) {
            var e = transformers.get(transformer.key());
            if(e == null)
                continue;

            var obj = e.getAsJsonObject();
            if(obj == null)
                continue;

            var enabled = obj.get("enabled").getAsBoolean();
            if(enabled)
                context.transformers().add(transformer);

            // ---- SETTINGS ----
            for(var setting : transformer.settings()) {
                var settingObj = obj.get(setting.key());
                if(settingObj == null)
                    continue;

                switch (setting.value()) {
                    case Boolean _ -> ((Setting<Boolean>) setting).setValue(settingObj.getAsBoolean());
                    case String _ -> ((Setting<String>) setting).setValue(settingObj.getAsString());
                    case Integer _ -> ((Setting<Integer>) setting).setValue(settingObj.getAsInt());
                    default -> {}
                }
            }
        }
    }

    public Context result() {
        return context;
    }
}
