package uml4java.analysis;

import uml4java.model.ClassInfo;
import uml4java.parser.JavaParser;
import uml4java.parser.TypeResolver;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Discovers additional Java classes referenced by already-parsed classes.
 *
 * <p>Iterates over unresolved type names and attempts to locate matching
 * {@code .java} files in a given search directory. Newly found classes are
 * parsed and added to the shared class list, and the process repeats until
 * no new classes are discovered.</p>
 */
public class ClassDiscoverer {
    private final List<ClassInfo> classes;
    private final Set<String> unresolved;
    private final JavaParser parser;
    private final TypeResolver resolver;

    /**
     * @param classes    the shared list of parsed classes (new discoveries are appended here)
     * @param unresolved the set of type names not yet resolved to a parsed class
     * @param parser     the parser used to parse newly discovered source files
     * @param resolver   the type resolver for primitive/known-class checks
     */
    public ClassDiscoverer(List<ClassInfo> classes, Set<String> unresolved, JavaParser parser, TypeResolver resolver) {
        this.classes = classes;
        this.unresolved = unresolved;
        this.parser = parser;
        this.resolver = resolver;
    }

    /**
     * Searches the given directory for source files matching unresolved type names.
     * Repeats until no new classes are found.
     *
     * @param searchDir the directory to search for {@code <ClassName>.java} files
     */
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
