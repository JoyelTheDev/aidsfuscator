package dev.lvstrng.aidsfuscator.polymorph;

import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

import java.util.Stack;

public class IntPolymorphStack extends Stack<IntMask<?>> {
    public static IntPolymorphStack of(IntMask<?>... masks) {
        var stack = new IntPolymorphStack();
        for(var mask : masks) {
            stack.push(mask);
        }

        return stack;
    }

    public int apply(int n) {
        for(var mask : this) {
            n = mask.apply(n);
        }

        return n;
    }

    public InsnList dump() {
        var ls = new InsnList();
        for(var mask : this) {
            ls.add(mask.insns());
        }

        return ls;
    }
}
