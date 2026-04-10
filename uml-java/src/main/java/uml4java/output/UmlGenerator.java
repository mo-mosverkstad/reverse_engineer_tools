package uml4java.output;

import uml4java.model.ClassInfo;
import uml4java.model.Relationship;

import java.io.*;
import java.util.*;

/**
 * Generates a Mermaid class-diagram Markdown file from parsed class and relationship data.
 *
 * <p>Classes are grouped by their Java package into Mermaid {@code namespace} blocks.
 * Classes without a package declaration are rendered at the top level.</p>
 */
public class UmlGenerator {
    /**
     * Writes a Mermaid class-diagram to the specified output file.
     *
     * @param classes    the parsed classes to render
     * @param rels       the relationships to render
     * @param outputFile the path of the Markdown file to create
     */
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
