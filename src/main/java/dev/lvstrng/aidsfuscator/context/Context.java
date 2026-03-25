package dev.lvstrng.aidsfuscator.context;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.asm.HierarchyClassWriter;
import dev.lvstrng.aidsfuscator.context.exception.MissingMemberException;
import dev.lvstrng.aidsfuscator.context.hierarchy.IHierarchy;
import dev.lvstrng.aidsfuscator.context.hierarchy.SimpleHierarchy;
import dev.lvstrng.aidsfuscator.context.library.LibraryLoader;
import dev.lvstrng.aidsfuscator.context.resource.ResourceHandler;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.ClassUtils;
import dev.lvstrng.aidsfuscator.utils.Utils;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Context {
    private String input, output, libPath;
    private final Map<String, JClass> classes, artificials, libraries, excluded;
    private int writerFlags;

    private final ResourceHandler resourceHandler;
    private final LibraryLoader libraryLoader;
    private final IHierarchy hierarchy;
    private final ReferenceGraph referenceGraph;

    private final List<Transformer> transformers;

    private Context() {
        this.classes = new HashMap<>();
        this.artificials = new HashMap<>();
        this.libraries = new HashMap<>();
        this.excluded = new HashMap<>();
        this.transformers = new ArrayList<>();

        this.resourceHandler    = new ResourceHandler(this);
        this.hierarchy          = new SimpleHierarchy(this);
        this.libraryLoader      = new LibraryLoader(this);
        this.referenceGraph     = new ReferenceGraph(this);

        this.writerFlags = ClassWriter.COMPUTE_MAXS;
    }

    // ---- INITIALIZE OBFUSCATOR ----
    public Context initialize() {
        Logger.info("Loading libraries...");
        this.libraryLoader().loadLibraries(libPath);

        Logger.info("Reading input JAR...");
        this.readJar();

        Logger.info("Building hierarchy...");
        this.hierarchy.build(); // build hierarchy
        return this;
    }

    private void readJar() {
        var file = new File(input);
        if(!file.exists())
            throw new IllegalArgumentException("Input file `" + input + "` does not exist");

        // ---- LOAD JAR CLASSES ----
        try (var zip = new ZipFile(file)) {
            for(var entry : zip.stream().toList()) {
                var name = entry.getName();
                var is = zip.getInputStream(entry);
                var bytes = is.readAllBytes();

                // if class, add new class
                if(name.endsWith(".class")) {
                    var node = ClassUtils.readClass(bytes);
                    add(new JClass(node));
                    continue;
                }

                // jars in a jar are "fat jars"
                if(name.endsWith(".jar")) {
                    libraryLoader.parseJar(bytes);
                    continue;
                }

                // add resource
                resourceHandler.add(name, bytes);
            }
        } catch (IOException _) {}
    }

    public Context transform(Transformer... transformers) {
        this.transformers.addAll(Arrays.asList(transformers));

        for(var transformer : transformers) {
            Logger.info("Running '%s'", transformer.name());
            transformer.transform(this);
            Logger.info("Completed '%s' with %s changes", transformer.name(), transformer.changes());
            Logger.info("");
        }

        return this;
    }

    @SuppressWarnings("all")
    public Context exportJar() {
        Logger.info("Exporting JAR...");

        var outputFile = new File(output);
        try (var jos = new JarOutputStream(new FileOutputStream(outputFile))) {
            for(var clazz : jarClasses()) {
                var writer = new HierarchyClassWriter(this);
                clazz.core().accept(writer);

                jos.putNextEntry(new ZipEntry(clazz.name() + ".class"));
                jos.write(writer.toByteArray());
                jos.closeEntry();
            }

            resourceHandler().handle(jos);
        } catch (IOException e) {
            Logger.error("Error writing output JAR: %s", e);
        }

        Logger.success("Exported JAR successfully!");
        Logger.success("%s (%skb) -> %s (%skb)",
                input, Utils.bytesToKB(new File(input).length()),
                output, Utils.bytesToKB(outputFile.length())
        );
        return this;
    }

    // -----------------
    // ----   MISC  ----
    // -----------------

    public ResourceHandler resourceHandler() {
        return resourceHandler;
    }

    public IHierarchy hierarchy() {
        return hierarchy;
    }

    public LibraryLoader libraryLoader() {
        return libraryLoader;
    }

    public ReferenceGraph referenceGraph() {
        return referenceGraph;
    }

    public int writerFlags() {
        return writerFlags;
    }

    // -----------------
    // ---- CLASSES ----
    // -----------------

    public JClass forName(String name) {
        var clazz = classes.get(name);
        if(clazz == null) clazz = libraries.get(name);
        if(clazz == null) clazz = excluded.get(name);
        if(clazz == null) clazz = artificials.get(name);

        if(clazz == null)
            throw new MissingMemberException(name);

        return clazz;
    }

    public void add(JClass clazz) {
        classes.put(clazz.name(), clazz);
    }

    public void addArtificial(JClass clazz) {
        artificials.put(clazz.name(), clazz);
    }

    public void addLibrary(JClass clazz) {
        artificials.put(clazz.name(), clazz);
    }

    public List<JClass> jarClasses() {
        var list = new ArrayList<>(classes.values().stream().toList());
        list.addAll(excluded.values());
        return list;
    }

    public List<JClass> classes() {
        return classes.values().stream().toList();
    }

    public Map<String, JClass> classMap() {
        return classes;
    }

    public Map<String, JClass> libraries() {
        return libraries;
    }

    public Map<String, JClass> artificials() {
        return artificials;
    }

    public Map<String, JClass> excluded() {
        return excluded;
    }

    // -----------------
    // ---- BUILDER ----
    // -----------------

    public static Context newInstance() {
        return new Context();
    }

    public Context in(String input) {
        this.input = input;
        return this;
    }

    public Context out(String output) {
        this.output = output;
        return this;
    }

    public Context libs(String libPath) {
        this.libPath = libPath;
        return this;
    }

    public Context computeFrames() {
        this.writerFlags |= ClassWriter.COMPUTE_FRAMES;
        return this;
    }
}
