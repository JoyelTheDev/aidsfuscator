package dev.lvstrng.aidsfuscator.transform.impl.data.strings;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public interface IStringDecryptor extends Opcodes {
    SecureRandom random = new SecureRandom();

    /**
     * Creates the method in specific class
     * @param context obfuscator context
     * @param clazz owner class
     * @param fieldName string array field name
     * @param cacheName stacktrace cache field
     */
    void generate(Context context, JClass clazz, String fieldName, String cacheName);

    /**
     * Adds a string to the {@param strings} list and returns a set of instructions to replace the previous instruction with.
     * @param context obfuscator context
     * @param method method containing the string (class can be retrieved via method.owner())
     * @param callSite the instruction being replaced
     * @param frames frames of the method
     * @param strings list of already added strings
     * @param str string being obfuscated
     * @return a set of instructions to replace the previous instruction with
     */
    InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str);

    /**
     * Sets the name of the decryptor method
     * @param name the name
     */
    void setName(String name);

    /**
     * @return descriptor of this decryptor method
     */
    String getDescriptor();
}
