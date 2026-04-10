package uml4java;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClassDiscoverer {
    private final List<ClassInfo> classes;
    private final Set<String> unresolved;
    private final JavaParser parser;
    private final TypeResolver resolver;

    public ClassDiscoverer(List<ClassInfo> classes, Set<String> unresolved, JavaParser parser, TypeResolver resolver) {
        this.classes = classes;
        this.unresolved = unresolved;
        this.parser = parser;
        this.resolver = resolver;
    }

    public void discover(String searchDir) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String name : new ArrayList<>(unresolved)) {
                if (resolver.isKnownClass(name) || resolver.isPrimitive(name)) continue;
                Path path = Paths.get(searchDir, name + ".java");
                if (Files.exists(path)) {
                    ClassInfo cls = parser.parseJavaFile(path.toString());
                    if (!cls.getName().isEmpty()) {
                        classes.add(cls);
                        changed = true;
                    }
                }
            }
        }
    }
}
