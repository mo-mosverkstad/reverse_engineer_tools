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
                pw.print(cls.toMermaid());
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
