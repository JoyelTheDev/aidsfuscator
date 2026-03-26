package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;

public class NamingTest extends Transformer {
    public NamingTest() {
        super("Naming", "namingTest");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            var newClassName = context.dictionary().newClassName();
            Mappings.CLASS.register(clazz.name(), new Mapping(newClassName, newClassName));

            mapFields(context, clazz);
            mapMethods(context, clazz);
        }

        for(var val : Mappings.values()) {
            System.out.println(val);

            for(var m : val.getMappings().entrySet()) {
                var mapping = m.getValue();
                System.out.println("\t" + m.getKey() + " -> " + mapping.key() + " [" + mapping.value() + "]");
            }
        }
    }

    private void mapFields(Context context, JClass clazz) {
        for(var field : clazz.fields()) {
            var selfOld = field.fullName();
            if(Mappings.FIELD.containsOld(selfOld))
                continue;

            var newName = "";
            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var id = String.format("%s.%s", member.name(), field.simpleName());
                if(Mappings.FIELD.containsOld(id)) {
                    newName = Mappings.FIELD.retrieve(id).value();
                    break;
                }
            }

            if(newName.isEmpty())
                newName = context.dictionary().newFieldName(clazz, field.desc());

            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var oldId = String.format("%s.%s", member.name(), field.simpleName());
                var newId = String.format("%s.%s %s", member.name(), newName, field.desc());
                Mappings.FIELD.register(oldId, new Mapping(newId, newName));
            }

            var selfNew = String.format("%s.%s %s", clazz.name(), newName, field.desc());
            Mappings.FIELD.register(selfOld, new Mapping(selfNew, newName));
        }
    }

    private void mapMethods(Context context, JClass clazz) {
        for(var method : clazz.methods()) {
            var selfOld = method.fullName();
            if(Mappings.METHOD.containsOld(selfOld))
                continue;

            if(cantEditMethod(clazz, method))
                continue;

            var newName = "";
            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var id = String.format("%s.%s", member.name(), method.simpleName());
                if(Mappings.METHOD.containsOld(id)) {
                    newName = Mappings.METHOD.retrieve(id).value();
                    break;
                }
            }

            if(newName.isEmpty())
                newName = context.dictionary().newMethodName(clazz, method.desc());

            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var oldId = String.format("%s.%s", member.name(), method.simpleName());
                var newId = String.format("%s.%s%s", member.name(), newName, method.desc());
                Mappings.METHOD.register(oldId, new Mapping(newId, newName));
            }

            var selfNew = String.format("%s.%s%s", clazz.name(), newName, method.desc());
            Mappings.METHOD.register(selfOld, new Mapping(selfNew, newName));
        }
    }
}
