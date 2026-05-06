package dev.lvstrng.aidsfuscator.transform.impl.data.strings;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;
import java.util.List;

public interface IStringInitializer extends Opcodes {
    SecureRandom random = new SecureRandom();

    /**
     * Generates this initializer
     * @param context obfuscator context
     * @param clazz owner class
     * @param fieldName string array field name
     * @param cacheName stacktrace array field name
     * @param strings list of strings being added
     */
    void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings);
}
