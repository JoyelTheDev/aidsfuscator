package dev.lvstrng.aidsfuscator.naming.dictionary;


import dev.lvstrng.aidsfuscator.tree.JClass;

public interface IDictionary {
    String newClassName();
    String newMethodName(JClass owner, String desc);
    String newFieldName(JClass owner, String desc);

    String newName(int count);

    void revertClass();
    void revertMethod();
    void revertField();
}
