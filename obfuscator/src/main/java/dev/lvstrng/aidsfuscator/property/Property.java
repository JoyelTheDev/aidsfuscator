package dev.lvstrng.aidsfuscator.property;

public enum Property {
    IGNORE_INTEGER,
    IGNORE_STRING,
    IGNORE_FLOW_INTS,

    INTEGER_DECRYPTOR,
    STRING_DECRYPTOR,

    SENSITIVE_CONSTANT,
    UNPROTECTED_SALT,
    IGNORE_REF_OBFUSCATION,

    SALT_ARTIFACT
    ;
}
