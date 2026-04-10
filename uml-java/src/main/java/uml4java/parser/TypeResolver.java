package uml4java.parser;

import uml4java.model.ClassInfo;

import java.util.List;
import java.util.Set;

public class TypeResolver {
    private static final Set<String> PRIMITIVES = Set.of(
        "int","long","short","byte","float","double","boolean","char",
        "String","void","Object","System","Integer","Long","Short","Byte",
        "Float","Double","Boolean","Character","Math","Arrays","Collections",
        "List","ArrayList","Map","HashMap","Set","HashSet","Optional",
        "StringBuilder","StringBuffer","Exception","RuntimeException"
    );

    private final List<ClassInfo> classes;

    public TypeResolver(List<ClassInfo> classes) {
        this.classes = classes;
    }

    public boolean isPrimitive(String type) { return PRIMITIVES.contains(type); }

    public boolean isKnownClass(String name) {
        return classes.stream().anyMatch(c -> c.getName().equals(name));
    }

    public String strip(String type) {
        int lt = type.indexOf('<');
        if (lt >= 0) type = type.substring(0, lt);
        int br = type.indexOf('[');
        if (br >= 0) type = type.substring(0, br);
        return type.trim();
    }
}
