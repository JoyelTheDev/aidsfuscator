package dev.lvstrng.aidsfuscator.context.resource;

import dev.lvstrng.aidsfuscator.context.Context;

import java.io.IOException;
import java.util.jar.JarOutputStream;

public interface HandledResource {
    void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException;
}
