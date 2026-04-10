package uml4java;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class JavaParser {
    private static final Set<String> PRIMITIVES = Set.of(
        "int","long","short","byte","float","double","boolean","char",
        "String","void","Object","System","Integer","Long","Short","Byte",
        "Float","Double","Boolean","Character","Math","Arrays","Collections",
        "List","ArrayList","Map","HashMap","Set","HashSet","Optional",
        "StringBuilder","StringBuffer","Exception","RuntimeException"
    );

    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<Relationship> rels = new ArrayList<>();
    private final Set<String> unresolved = new LinkedHashSet<>();

    public List<ClassInfo> getClasses() { return classes; }
    public List<Relationship> getRels() { return rels; }

    public boolean isPrimitive(String type) { return PRIMITIVES.contains(type); }

    public boolean isKnownClass(String name) {
        return classes.stream().anyMatch(c -> c.name.equals(name));
    }

    private String strip(String type) {
        int lt = type.indexOf('<');
        if (lt >= 0) type = type.substring(0, lt);
        int br = type.indexOf('[');
        if (br >= 0) type = type.substring(0, br);
        return type.trim();
    }

    private void addUnresolved(String name) {
        name = strip(name);
        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) return;
        if (isPrimitive(name) || isKnownClass(name)) return;
        unresolved.add(name);
    }

    private boolean isFieldWithInitializer(String line) {
        int eq = line.indexOf('=');
        int paren = line.indexOf('(');
        return eq >= 0 && paren >= 0 && eq < paren;
    }

    private char getVisibility(String line) {
        if (line.contains("public")) return '+';
        if (line.contains("private")) return '-';
        if (line.contains("protected")) return '#';
        return '~';
    }

    private String skipModifiers(String line) {
        String[] mods = {"public","private","protected","static","final","abstract"};
        String[] tokens = line.split("\\s+");
        int i = 0;
        outer:
        while (i < tokens.length) {
            for (String m : mods) {
                if (tokens[i].equals(m)) { i++; continue outer; }
            }
            break;
        }
        return String.join(" ", Arrays.copyOfRange(tokens, i, tokens.length));
    }

    private Member parseField(String line) {
        Member m = new Member();
        m.visibility = getVisibility(line);
        m.isMethod = false;
        m.isStatic = line.contains("static");

        String rest = skipModifiers(line.trim());
        // Truncate at '=' to remove initializers like "= new ArrayList<>()"
        int eqIdx = rest.indexOf('=');
        if (eqIdx >= 0) rest = rest.substring(0, eqIdx).trim();
        // Remove trailing semicolon
        if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1).trim();
        int sp = rest.indexOf(' ');
        if (sp < 0) return null;
        m.type = strip(rest.substring(0, sp));
        m.name = rest.substring(sp + 1).trim();
        if (m.name.isEmpty()) return null;
        addUnresolved(m.type);
        return m;
    }

    private Member parseMethod(String line) {
        Member m = new Member();
        m.visibility = getVisibility(line);
        m.isMethod = true;
        m.isStatic = line.contains("static");

        // Strip inline method body: everything from '{' onward
        String cleaned = line.trim();
        int braceIdx = cleaned.indexOf('{');
        if (braceIdx >= 0) cleaned = cleaned.substring(0, braceIdx).trim();

        String rest = skipModifiers(cleaned);
        int paren = rest.indexOf('(');
        if (paren < 0) return null;

        String before = rest.substring(0, paren).trim();
        int endParen = rest.indexOf(')');
        m.params = (endParen > paren) ? rest.substring(paren + 1, endParen).trim() : "";

        int sp = before.lastIndexOf(' ');
        if (sp >= 0) {
            m.type = before.substring(0, sp).trim();
            m.name = before.substring(sp + 1).trim();
        } else {
            m.type = "";
            m.name = before;
        }

        if (m.name.isEmpty()) return null;

        if (!m.params.isEmpty()) {
            for (String param : m.params.split(",")) {
                param = param.trim();
                int s = param.indexOf(' ');
                if (s > 0) addUnresolved(param.substring(0, s));
            }
        }
        if (!m.type.isEmpty()) addUnresolved(m.type);

        return m;
    }

    private ClassInfo currentParsingClass;

    private void collectBodyRefs(String line) {
        Matcher mat = Pattern.compile("new\\s+(\\w+)").matcher(line);
        while (mat.find()) {
            String ref = mat.group(1);
            addUnresolved(ref);
            if (currentParsingClass != null) {
                currentParsingClass.bodyRefs.add(ref);
            }
        }
    }

    public ClassInfo parseJavaFile(String filename) {
        ClassInfo cls = new ClassInfo();
        cls.filepath = filename;
        currentParsingClass = cls;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean inClass = false;
            int braceCount = 0;
            boolean inComment = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (!inClass && line.contains("class ") && !line.startsWith("//")) {
                    boolean hasBrace = line.contains("{");

                    int extIdx = line.indexOf("extends ");
                    if (extIdx >= 0) {
                        String after = line.substring(extIdx + 8).trim();
                        String extName = after.split("[\\s{,<]")[0];
                        cls.extendsName = extName;
                        addUnresolved(extName);
                    }

                    int implIdx = line.indexOf("implements ");
                    if (implIdx >= 0) {
                        String after = line.substring(implIdx + 11);
                        int braceIdx = after.indexOf('{');
                        if (braceIdx >= 0) after = after.substring(0, braceIdx);
                        for (String iface : after.split(",")) {
                            iface = iface.trim();
                            if (!iface.isEmpty()) {
                                cls.implementsNames.add(iface);
                                addUnresolved(iface);
                            }
                        }
                    }

                    int classIdx = line.indexOf("class ") + 6;
                    String rest = line.substring(classIdx).trim();
                    cls.name = rest.split("[\\s{<]")[0];
                    inClass = true;
                    if (hasBrace) braceCount++;
                    continue;
                }

                if (!inClass) continue;

                if (line.isEmpty() || line.startsWith("//")) continue;
                if (line.startsWith("/*")) { inComment = true; continue; }
                if (inComment) { if (line.contains("*/")) inComment = false; continue; }
                if (line.startsWith("*")) continue;

                int prevBrace = braceCount;
                for (char ch : line.toCharArray()) {
                    if (ch == '{') braceCount++;
                    if (ch == '}') braceCount--;
                }

                if (braceCount == 0) break;

                if (prevBrace > 1) {
                    collectBodyRefs(line);
                    continue;
                }
                if (prevBrace != 1) continue;

                if (line.contains("(") && !line.matches(".*\\bclass\\b.*") && !isFieldWithInitializer(line)) {
                    Member m = parseMethod(line);
                    if (m != null) cls.members.add(m);
                } else if ((line.contains(";") || isFieldWithInitializer(line)) && (line.contains("public") || line.contains("private") || line.contains("protected"))) {
                    Member m = parseField(line);
                    if (m != null) cls.members.add(m);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Cannot open file " + filename);
        }
        return cls;
    }

    public void addClass(ClassInfo cls) {
        if (!cls.name.isEmpty()) classes.add(cls);
    }

    public void scanDirectory(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectory(f.getPath());
            } else if (f.getName().endsWith(".java")) {
                ClassInfo cls = parseJavaFile(f.getPath());
                addClass(cls);
            }
        }
    }

    public void discoverClasses(String searchDir) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String name : new ArrayList<>(unresolved)) {
                if (isKnownClass(name) || isPrimitive(name)) continue;
                Path path = Paths.get(searchDir, name + ".java");
                if (Files.exists(path)) {
                    ClassInfo cls = parseJavaFile(path.toString());
                    if (!cls.name.isEmpty()) {
                        classes.add(cls);
                        changed = true;
                    }
                }
            }
        }
    }

    public void detectRelationships() {
        for (ClassInfo c : classes) {
            if (!c.extendsName.isEmpty() && isKnownClass(c.extendsName))
                addRel(c.name, c.extendsName, Relationship.Type.INHERITANCE, null);

            for (String iface : c.implementsNames)
                if (isKnownClass(iface))
                    addRel(c.name, iface, Relationship.Type.IMPLEMENTATION, null);

            for (Member m : c.members) {
                if (!m.isMethod) {
                    String t = strip(m.type);
                    if (!isPrimitive(t) && isKnownClass(t))
                        addRel(c.name, t, Relationship.Type.ASSOCIATION, m.name);
                } else {
                    if (!m.params.isEmpty()) {
                        for (String param : m.params.split(",")) {
                            param = param.trim();
                            int sp = param.indexOf(' ');
                            if (sp > 0) {
                                String pt = strip(param.substring(0, sp));
                                if (!isPrimitive(pt) && isKnownClass(pt))
                                    addRel(c.name, pt, Relationship.Type.DEPENDENCY, null);
                            }
                        }
                    }
                    if (!m.type.isEmpty()) {
                        String rt = strip(m.type);
                        if (!isPrimitive(rt) && isKnownClass(rt))
                            addRel(c.name, rt, Relationship.Type.DEPENDENCY, null);
                    }
                }
            }

            for (String ref : c.bodyRefs) {
                String t = strip(ref);
                if (!isPrimitive(t) && isKnownClass(t))
                    addRel(c.name, t, Relationship.Type.DEPENDENCY, null);
            }
        }
    }

    private void addRel(String from, String to, Relationship.Type type, String label) {
        if (from.equals(to)) return;
        for (Relationship r : rels)
            if (r.from.equals(from) && r.to.equals(to) && r.type == type) return;
        rels.add(new Relationship(from, to, type, label));
    }
}
