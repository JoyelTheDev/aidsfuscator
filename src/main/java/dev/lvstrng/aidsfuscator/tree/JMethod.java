package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.analysis.flow.graph.ControlFlowGraph;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleAnalyzer;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleInterpreter;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleValue;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import dev.lvstrng.aidsfuscator.salt.ISaltable;
import dev.lvstrng.aidsfuscator.salt.impl.MethodSalt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A MethodNode wrapper for easier use.
 */
public class JMethod implements ISaltable<MethodSalt> {
    private JClass owner;
    private MethodNode core;
    private final PropertyContainer properties;
    private MethodSalt salt;

    private boolean library;
    private final String originalName, originalDesc;

    private Set<JMethod> parents, children;
    private AbstractInsnNode safeInsn;

    public JMethod(MethodNode core) {
        this.properties = new PropertyContainer();
        this.library = false;

        this.originalName = core.name;
        this.originalDesc = core.desc;

        this.setCore(core);
    }

    public boolean isPublic() {
        return Modifier.isPublic(access());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(access());
    }

    public boolean isProtected() {
        return Modifier.isProtected(access());
    }

    public boolean isBridge() {
        return (access() & Opcodes.ACC_BRIDGE) != 0;
    }

    public boolean isSynthetic() {
        return (access() & Opcodes.ACC_BRIDGE) != 0;
    }

    public void insertSafe(InsnList list) {
        if(safeInsn == null) {
            insns().insert(list);
        } else {
            insns().insert(safeInsn, list);
        }
    }

    public AbstractInsnNode safeInsn() {
        return safeInsn;
    }

    public AbstractInsnNode setSafeInsn(AbstractInsnNode insn) {
        this.safeInsn = insn;
        return safeInsn;
    }

    public String originalName() {
        return originalName;
    }

    public String originalDesc() {
        return originalDesc;
    }

    public int allocParameter(Type type) {
        int argSize = Arrays.stream(args()).mapToInt(Type::getSize).sum();
        int spot = Modifier.isStatic(access()) ? argSize : argSize + 1;

        if (localVariables() != null) {
            for (var lv : localVariables()) {
                if (lv.index >= spot)
                    lv.index += type.getSize();
            }
        }

        for (var insn : insns()) {
            if (insn instanceof VarInsnNode v) {
                if (v.var >= spot)
                    v.var += type.getSize();
            } else if (insn instanceof IincInsnNode v) {
                if (v.var >= spot)
                    v.var += type.getSize();
            }
        }

        if(signature() != null) {
            core.signature = signature().replace(")", type.getDescriptor() + ")");
        }
        core.desc = desc().replace(")", type.getDescriptor() + ")");
        core.maxLocals += type.getSize();
        return spot;
    }

    public Type returnType() {
        return Type.getReturnType(desc());
    }

    public Type[] args() {
        return Type.getArgumentTypes(desc());
    }

    public int allocVar() {
        return allocVar(Type.getObjectType("java/lang/Object"));
    }

    public int allocVar(Type type) {
        var slot = maxLocals();
        setMaxLocals(maxLocals() + type.getSize());
        return slot;
    }

    public int maxLocals() {
        return core.maxLocals;
    }

    public void setMaxLocals(int maxLocals) {
        core.maxLocals = maxLocals;
    }

    public int maxStack() {
        return core.maxStack;
    }

    public void makeSalt(int value, int local) {
        this.salt = new MethodSalt(value, local);
    }

    @Override
    public boolean hasSalt() {
        return salt != null;
    }

    @Override
    public MethodSalt salt() {
        return salt;
    }

    @Override
    public boolean canSalt(Block block) {
        if(!hasSalt()) return false;
        return block.isInitialized(salt.local());
    }

    @Override
    public boolean canSalt(Frame<SimpleValue> frame) {
        if(frame == null) return false;
        if(!hasSalt()) return false;

        return !frame.getLocal(salt.local()).isUninitialized();
    }

    public PropertyContainer properties() {
        return properties;
    }

    public void setLibrary() {
        this.library = true;
    }

    public boolean isLibrary() {
        return library;
    }

    public void setCore(MethodNode core) {
        this.core = core;

        this.parents = new HashSet<>();
        this.children = new HashSet<>();
    }

    public void setOwner(JClass owner) {
        this.owner = owner;
    }

    public JClass owner() {
        return owner;
    }

    public Set<JMethod> parents() {
        return parents;
    }

    public Set<JMethod> children() {
        return children;
    }

    public Set<JMethod> tree() {
        var list = new HashSet<>(parents);
        list.addAll(children);
        return list;
    }

    public boolean isVirtual() {
        return !Modifier.isStatic(access());
    }

    public boolean isNative() {
        return Modifier.isNative(access());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(access());
    }

    public MethodNode core() {
        return core;
    }

    public int access() {
        return core.access;
    }

    public String name() {
        return core.name;
    }

    public String desc() {
        return core.desc;
    }

    public String signature() {
        return core.signature;
    }

    public List<TryCatchBlockNode> traps() {
        if(core.tryCatchBlocks == null)
            core.tryCatchBlocks = new ArrayList<>();

        return core.tryCatchBlocks;
    }

    public List<LocalVariableNode> localVariables() {
        if(core.localVariables == null)
            core.localVariables = new ArrayList<>();

        return core.localVariables;
    }

    public InsnList insns() {
        return core.instructions;
    }

    public ControlFlowGraph createFlowGraph(Context context) {
        return new ControlFlowGraph(context, this).build();
    }

    public Map<AbstractInsnNode, SimpleFrame> frames(Context context) {
        try {
            var frameArr = new SimpleAnalyzer(new SimpleInterpreter(context, this)).analyzeAndComputeMaxs(owner.name(), core);
            var frames = new HashMap<AbstractInsnNode, SimpleFrame>();

            for(int i = 0; i < insns().size(); i++) {
                var insn = insns().get(i);
                var frame = frameArr[i];
                if(frame == null)
                    continue;

                frames.put(insn, SimpleFrame.of(frame));
            }

            return frames;
        } catch (AnalyzerException e) {
            Logger.error("Error analyzing frames in (%s): %s", fullOriginalName(), e.getLocalizedMessage());
            return null;
        }
    }

    public String simpleName() {
        return "%s%s".formatted(name(), desc());
    }

    public String simpleOriginalName() {
        return "%s%s".formatted(originalName, originalDesc);
    }

    public String fullOriginalName() {
        return "%s.%s".formatted(owner.originalName(), simpleOriginalName());
    }

    public String fullName() {
        return "%s.%s".formatted(owner, simpleName());
    }

    @Override
    public String toString() {
        return fullName();
    }
}
