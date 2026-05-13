package dev.lvstrng.aidsfuscator.context.library;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class LibraryLoader {
    private static final Predicate<File> filter = f -> f.getName().endsWith(".jar") || f.getName().endsWith(".jmod");
    private final Context context;
    private final String javaPath;

    public LibraryLoader(Context context, String javaPath) {
        this.context = context;
        this.javaPath = javaPath;
    }

    public void loadLibraries(String path) {
        loadJavaClasspath();

        var depDir = new File(path);
        if (!depDir.isDirectory())
            return;

        var jarFiles = listFiles(depDir);
        for (var jar : jarFiles) {
            parseJar(jar);
        }
    }

    private void loadJavaClasspath() {
        try {
            var fs = getJRTFS();
            var stream = Files.walk(fs.getPath("/modules"));

            stream.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                try (var in = Files.newInputStream(path)) {
                    var node = new JClass(ClassUtils.readAsDependency(in));
                    node.setLibrary();
                    context.addLibrary(node);
                } catch (IOException _) {}
            });
        } catch (IOException _) {}
    }

    public void parseJar(File path) {
        try (var zip = new ZipFile(path)) {
            for (var entry : Collections.list(zip.entries()))
                handleEntry(zip, entry);
        } catch (IOException _) {}
    }

    private void handleEntry(ZipFile zip, ZipEntry entry) {
        var name = entry.getName();
        try (var in = zip.getInputStream(entry)) {
            switch (name) {
                case String s when s.endsWith(".class") -> {
                    var clazz = new JClass(ClassUtils.readAsDependency(in));
                    clazz.setLibrary();
                    context.addLibrary(clazz);
                }
                case String s when s.endsWith(".jar") -> parseJar(in.readAllBytes());
                default -> {}
            }
        } catch (IOException _) {}
    }

    public void parseJar(byte[] jarBytes) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                handleEntry(zis, entry);
                zis.closeEntry();
            }
        } catch (IOException _) {}
    }

    private void handleEntry(ZipInputStream zis, ZipEntry entry) {
        var name = entry.getName();
        if (!name.endsWith(".class"))
            return;

        try {
            var clazz = new JClass(ClassUtils.readAsDependency(zis));
            clazz.setLibrary();
            context.addLibrary(clazz);
        } catch (IOException _) {}
    }

    private FileSystem getJRTFS() throws IOException {
        var uri = URI.create("jrt:/");
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Collections.emptyMap());
        }
    }

    private static List<File> listFiles(File dir) {
        var ls = new ArrayList<File>();
        listFiles(dir, ls);
        return ls;
    }

    private static void listFiles(File dir, List<File> checked) {
        for(var f : Objects.requireNonNull(dir.listFiles())) {
            if(f.isDirectory()) {
                listFiles(f, checked);
                continue;
            }

            if(!filter.test(f))
                continue;

            checked.add(f);
        }
    }
}
