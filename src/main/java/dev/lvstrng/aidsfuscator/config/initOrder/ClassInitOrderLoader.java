package dev.lvstrng.aidsfuscator.config.initOrder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;

public class ClassInitOrderLoader {
    private final Context context;
    private final String orderPath;

    public ClassInitOrderLoader(Context context, String orderPath) {
        this.context = context;
        this.orderPath = orderPath;
    }

    public void load() {
        var file = Context.getFromWorkspace(orderPath);
        if(!file.exists()) {
            Logger.warn("Couldn't find class init order config. Running with no set class init order.");
            return;
        }

        var pairs = new Gson().fromJson(Context.readWorkspaceString(orderPath), JsonArray.class);
        for(var elem : pairs) {
            var arr = elem.getAsJsonArray();
            if(arr.size() != 2) // user clearly doesn't know what he's doing
                continue;

            context.initOrder().add(
                    arr.get(0).getAsString(),
                    arr.get(1).getAsString()
            );
        }
    }
}
