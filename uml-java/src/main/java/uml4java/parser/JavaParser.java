package uml4java.parser;

import uml4java.model.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class JavaParser {
    private final List<ClassInfo> classes = new ArrayList<>();
    private final Set<String> unresolved = new LinkedHashSet<>();
    private final TypeResolver resolver;

    public JavaParser() {
        this.resolver = new TypeResolver(classes);
    }

    public List<ClassInfo> getClasses() { return classes; }
    public Set<String> getUnresolved() { return unresolved; }
    public TypeResolver getResolver() { return resolver; }

    private void addUnresolved(String name) {
        name = resolver.strip(name);
        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) return;
        if (resolver.isPrimitive(name) || resolver.isKnownClass(name)) return;
        unresolved.add(name);
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

    private boolean isFieldWithInitializer(String line) {
        int eq = line.indexOf('=');
        int paren = line.indexOf('(');
        return eq >= 0 && paren >= 0 && eq < paren;
    }

    private Member parseField(String line) {
        char vis = getVisibility(line);
        boolean isStatic = line.contains("static");

        String rest = skipModifiers(line.trim());
        int eqIdx = rest.indexOf('=');
        if (eqIdx >= 0) rest = rest.substring(0, eqIdx).trim();
        if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1).trim();
        int sp = rest.indexOf(' ');
        if (sp < 0) return null;

        String type = resolver.strip(rest.substring(0, sp));
        String name = rest.substring(sp + 1).trim();
        if (name.isEmpty()) return null;
        addUnresolved(type);
        return new FieldMember(vis, type, name, isStatic);
    }

    private Member parseMethod(String line) {
        char vis = getVisibility(line);
        boolean isStatic = line.contains("static");

        String cleaned = line.trim();
        int braceIdx = cleaned.indexOf('{');
        if (braceIdx >= 0) cleaned = cleaned.substring(0, braceIdx).trim();

        String rest = skipModifiers(cleaned);
        int paren = rest.indexOf('(');
        if (paren < 0) return null;

        String before = rest.substring(0, paren).trim();
        int endParen = rest.indexOf(')');
        String params = (endParen > paren) ? rest.substring(paren + 1, endParen).trim() : "";

        String type, name;
        int sp = before.lastIndexOf(' ');
        if (sp >= 0) {
            type = before.substring(0, sp).trim();
            name = before.substring(sp + 1).trim();
        } else {
            type = "";
            name = before;
        }
        if (name.isEmpty()) return null;

        if (!params.isEmpty()) {
            for (String param : params.split(",")) {
                param = param.trim();
                int s = param.indexOf(' ');
                if (s > 0) addUnresolved(param.substring(0, s));
            }
        }
        if (!type.isEmpty()) addUnresolved(type);

        return new MethodMember(vis, type, name, params, isStatic);
    }

    private ClassInfo currentParsingClass;

    private void collectBodyRefs(String line) {
        Matcher mat = Pattern.compile("new\\s+(\\w+)").matcher(line);
        while (mat.find()) {
            String ref = mat.group(1);
            addUnresolved(ref);
            if (currentParsingClass != null) {
                currentParsingClass.addBodyRef(ref);
            }
        }
    }

    public ClassInfo parseJavaFile(String filename) {
        ClassInfo cls = new ClassInfo();
        cls.setFilepath(filename);
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
                        cls.setExtendsName(extName);
                        addUnresolved(extName);
                    }

                    int implIdx = line.indexOf("implements ");
                    if (implIdx >= 0) {
                        String after = line.substring(implIdx + 11);
                        int bi = after.indexOf('{');
                        if (bi >= 0) after = after.substring(0, bi);
                        for (String iface : after.split(",")) {
                            iface = iface.trim();
                            if (!iface.isEmpty()) {
                                cls.addImplements(iface);
                                addUnresolved(iface);
                            }
                        }
                    }

                    int classIdx = line.indexOf("class ") + 6;
                    String rest = line.substring(classIdx).trim();
                    cls.setName(rest.split("[\\s{<]")[0]);
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
                    if (m != null) cls.addMember(m);
                } else if ((line.contains(";") || isFieldWithInitializer(line)) && (line.contains("public") || line.contains("private") || line.contains("protected"))) {
                    Member m = parseField(line);
                    if (m != null) cls.addMember(m);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Cannot open file " + filename);
        }
        return cls;
    }

    public void addClass(ClassInfo cls) {
        if (!cls.getName().isEmpty()) classes.add(cls);
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
}
