package dev.lvstrng.aidsfuscator.polymorph.impl;

import dev.lvstrng.aidsfuscator.polymorph.IntMask;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class NegMask extends IntMask<NegMask> {
    @Override
    public int apply(int num) {
        return -num;
    }

    @Override
    public InsnList insns() {
        return new InsnBuilder().ineg().result();
    }
}
