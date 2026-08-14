package dev.lvstrng.aidsfuscator.classgen.impl.hash;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;

public interface IHashIntegrityClass {
    void init();

    JClass get();

    void add(JClass clazz);

    void obfuscate(JClass clazz);
}
