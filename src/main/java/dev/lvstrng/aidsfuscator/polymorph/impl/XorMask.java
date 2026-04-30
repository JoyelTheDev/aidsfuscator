package dev.lvstrng.aidsfuscator.polymorph.impl;

import dev.lvstrng.aidsfuscator.polymorph.IntMask;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class XorMask extends IntMask<XorMask> {
    @Override
    public int apply(int num) {
        return num ^ value;
    }

    @Override
    public InsnList insns() {
        return new InsnBuilder()._int(value).ixor().result();
    }
}
