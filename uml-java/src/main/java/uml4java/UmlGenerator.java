package uml4java;

import java.io.*;
import java.util.List;

public class UmlGenerator {
    public static void generate(List<ClassInfo> classes, List<Relationship> rels, String outputFile) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            pw.print("# UML Class Diagram\r\n\r\n");
            pw.print("```mermaid\r\n");
            pw.print("classDiagram\r\n");

            for (ClassInfo cls : classes) {
                pw.printf("    class %s {\r\n", cls.name);
                for (Member m : cls.members) {
                    if (!m.isMethod)
                        pw.printf("        %c%s %s\r\n", m.visibility, m.type, m.name);
                }
                for (Member m : cls.members) {
                    if (m.isMethod) {
                        if (m.isStatic)
                            pw.printf("        %c%s(%s)$ %s\r\n", m.visibility, m.name, m.params, m.type);
                        else if (!m.type.isEmpty())
                            pw.printf("        %c%s(%s) %s\r\n", m.visibility, m.name, m.params, m.type);
                        else
                            pw.printf("        %c%s(%s)\r\n", m.visibility, m.name, m.params);
                    }
                }
                pw.print("    }\r\n");
            }

            for (Relationship r : rels) {
                switch (r.type) {
                    case INHERITANCE:
                        pw.printf("    %s --|> %s\r\n", r.from, r.to); break;
                    case IMPLEMENTATION:
                        pw.printf("    %s ..|> %s\r\n", r.from, r.to); break;
                    case ASSOCIATION:
                        if (r.label != null && !r.label.isEmpty())
                            pw.printf("    %s --> %s : %s\r\n", r.from, r.to, r.label);
                        else
                            pw.printf("    %s --> %s\r\n", r.from, r.to);
                        break;
                    case DEPENDENCY:
                        pw.printf("    %s ..> %s\r\n", r.from, r.to); break;
                }
            }

            pw.print("```\r\n");
        } catch (IOException e) {
            System.err.println("Error: Cannot create output file " + outputFile);
            System.exit(1);
        }
    }
}
