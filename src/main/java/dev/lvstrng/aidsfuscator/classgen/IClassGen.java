package dev.lvstrng.aidsfuscator.classgen;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;

public interface IClassGen {
    JClass create(Context context);
}
