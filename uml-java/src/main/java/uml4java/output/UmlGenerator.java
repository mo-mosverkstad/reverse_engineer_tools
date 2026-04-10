package uml4java.output;

import uml4java.model.ClassInfo;
import uml4java.model.Relationship;

import java.io.*;
import java.util.*;

public class UmlGenerator {
    public static void generate(List<ClassInfo> classes, List<Relationship> rels, String outputFile) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            pw.print("# UML Class Diagram\r\n\r\n");
            pw.print("```mermaid\r\n");
            pw.print("classDiagram\r\n");

            Map<String, List<ClassInfo>> byPackage = new LinkedHashMap<>();
            for (ClassInfo cls : classes) {
                byPackage.computeIfAbsent(cls.getPackageName(), k -> new ArrayList<>()).add(cls);
            }

            for (Map.Entry<String, List<ClassInfo>> entry : byPackage.entrySet()) {
                String pkg = entry.getKey();
                if (pkg.isEmpty()) {
                    for (ClassInfo cls : entry.getValue()) {
                        pw.print(cls.toMermaid("    "));
                    }
                } else {
                    pw.printf("    namespace %s {\r\n", pkg);
                    for (ClassInfo cls : entry.getValue()) {
                        pw.print(cls.toMermaid("        "));
                    }
                    pw.print("    }\r\n");
                }
            }

            for (Relationship r : rels) {
                pw.print(r.toMermaid() + "\r\n");
            }

            pw.print("```\r\n");
        } catch (IOException e) {
            System.err.println("Error: Cannot create output file " + outputFile);
            System.exit(1);
        }
    }
}
