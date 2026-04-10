package uml4java.parser;

import uml4java.model.ClassInfo;

import java.util.List;
import java.util.Set;

/**
 * Classifies type names as primitives/well-known JDK types or project-defined classes,
 * and strips generic and array suffixes from type strings.
 *
 * <p>Shared by both the parser (to track unresolved references) and the analysis
 * layer (to decide which types warrant UML relationships).</p>
 */
public class TypeResolver {
    private static final Set<String> PRIMITIVES = Set.of(
        "int","long","short","byte","float","double","boolean","char",
        "String","void","Object","System","Integer","Long","Short","Byte",
        "Float","Double","Boolean","Character","Math","Arrays","Collections",
        "List","ArrayList","Map","HashMap","Set","HashSet","Optional",
        "StringBuilder","StringBuffer","Exception","RuntimeException"
    );

    private final List<ClassInfo> classes;

    /**
     * @param classes the live list of parsed classes; looked up by {@link #isKnownClass(String)}
     */
    public TypeResolver(List<ClassInfo> classes) {
        this.classes = classes;
    }

    /**
     * @param type a simple type name
     * @return {@code true} if the type is a primitive, boxed type, or common JDK class
     */
    public boolean isPrimitive(String type) { return PRIMITIVES.contains(type); }

    /**
     * @param name a simple class name
     * @return {@code true} if a class with this name has already been parsed
     */
    public boolean isKnownClass(String name) {
        return classes.stream().anyMatch(c -> c.getName().equals(name));
    }

    /**
     * Strips generic type parameters (e.g. {@code List<String>} → {@code List})
     * and array brackets (e.g. {@code int[]} → {@code int}).
     *
     * @param type the raw type string
     * @return the base type name
     */
    public String strip(String type) {
        int lt = type.indexOf('<');
        if (lt >= 0) type = type.substring(0, lt);
        int br = type.indexOf('[');
        if (br >= 0) type = type.substring(0, br);
        return type.trim();
    }
}
