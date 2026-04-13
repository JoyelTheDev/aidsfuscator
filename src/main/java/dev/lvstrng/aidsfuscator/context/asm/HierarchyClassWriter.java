package dev.lvstrng.aidsfuscator.context.asm;

import dev.lvstrng.aidsfuscator.context.Context;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.Frame;

public class HierarchyClassWriter extends ClassWriter {
    private final Context context;

    public HierarchyClassWriter(Context context) {
        super(context.writerFlags());
        this.context = context;
        if (!context.watermark().isEmpty()) {
            this.newUTF8(context.watermark());
        }
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        return context.hierarchy().commonSuperClass(type1, type2);
    }
}
