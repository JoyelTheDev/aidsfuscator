package dev.lvstrng.aidsfuscator.file.impl.initOrder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import dev.lvstrng.aidsfuscator.file.Loader;
import dev.lvstrng.aidsfuscator.context.Context;

public class ClassInitOrderLoader implements Loader {
    private final Context context;
    private final String orderPath;

    public ClassInitOrderLoader(Context context, String orderPath) {
        this.context = context;
        this.orderPath = orderPath;
    }

    @Override
    public void load() {
        var file = Context.getFromWorkspace(orderPath);
        if(!file.exists())
            return;

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
