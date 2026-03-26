package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.ControlFlowGraph;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleInterpreter;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleValue;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import dev.lvstrng.aidsfuscator.seed.MethodSalt;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A MethodNode wrapper for easier use.
 */
public class JMethod {
    private JClass owner;
    private MethodNode core;
    private final PropertyContainer properties;
    private boolean library;
    private MethodSalt salt;

    private List<JMethod> parents, children;

    public JMethod(MethodNode core) {
        this.properties = new PropertyContainer();
        this.library = false;
        this.setCore(core);
    }

    public void makeSalt(int value, int local) {
        this.salt = new MethodSalt(value, local);
    }

    public boolean hasSalt() {
        return salt != null;
    }

    public MethodSalt salt() {
        return salt;
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

        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public void setOwner(JClass owner) {
        this.owner = owner;
    }

    public JClass owner() {
        return owner;
    }

    public List<JMethod> parents() {
        return parents;
    }

    public List<JMethod> children() {
        return children;
    }

    public List<JMethod> tree() {
        var list = new ArrayList<>(parents);
        list.addAll(children);
        return list;
    }

    public boolean isVirtual() {
        return !Modifier.isStatic(access());
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

    public Map<AbstractInsnNode, Frame<SimpleValue>> frames(Context context) {
        try {
            var frameArr = new Analyzer<>(new SimpleInterpreter(context)).analyzeAndComputeMaxs(owner.name(), core);
            var frames = new HashMap<AbstractInsnNode, Frame<SimpleValue>>();

            for(int i = 0; i < insns().size(); i++) {
                var insn = insns().get(i);
                var frame = frameArr[i];

                frames.put(insn, frame);
            }

            return frames;
        } catch (AnalyzerException e) {
            Logger.error("Error analyzing frames: %s", e.getLocalizedMessage());
            return null;
        }
    }

    public String simpleName() {
        return "%s%s".formatted(name(), desc());
    }

    public String fullName() {
        return "%s.%s".formatted(owner, simpleName());
    }

    @Override
    public String toString() {
        return fullName();
    }
}
