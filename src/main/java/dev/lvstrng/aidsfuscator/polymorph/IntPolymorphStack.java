package dev.lvstrng.aidsfuscator.polymorph;

import dev.lvstrng.aidsfuscator.polymorph.impl.*;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

public class IntPolymorphStack extends Stack<IntMask<?>> {
    private static final List<Supplier<IntMask<?>>> allMasks = List.of(
            AddMask::new,
            AndMask::new,

            MulMask::new,
            DivMask::new,

            UShiftRightMask::new,
            RightShiftMask::new,
            LeftShiftMask::new,

            XorMask::new,
            OrMask::new,
            AndMask::new,

            NegMask::new
    );

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
