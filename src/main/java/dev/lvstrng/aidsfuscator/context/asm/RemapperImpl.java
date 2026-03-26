package dev.lvstrng.aidsfuscator.context.asm;

import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.commons.Remapper;

public class RemapperImpl extends Remapper {
    @Override
    public String map(String internalName) {
        if(Mappings.CLASS.containsOld(internalName)) {
            return super.map(Mappings.CLASS.retrieve(internalName).value());
        }
        return super.map(internalName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        var id = MemberUtils.fullMethod(owner, name, descriptor);
        if(Mappings.METHOD.containsOld(id)) {
            return super.mapMethodName(owner, Mappings.METHOD.retrieve(id).value(), descriptor);
        }
        return super.mapMethodName(owner, name, descriptor);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        var id = MemberUtils.fullField(owner, name, descriptor);
        if(Mappings.FIELD.containsOld(id)) {
            return super.mapFieldName(owner, Mappings.FIELD.retrieve(id).value(), descriptor);
        }
        return super.mapFieldName(owner, name, descriptor);
    }
}
