package uml4java.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClassInfo {
    private String name = "";
    private String filepath = "";
    private String packageName = "";
    private String extendsName = "";
    private final List<String> implementsNames = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();
    private final Set<String> bodyRefs = new LinkedHashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFilepath() { return filepath; }
    public void setFilepath(String filepath) { this.filepath = filepath; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getExtendsName() { return extendsName; }
    public void setExtendsName(String extendsName) { this.extendsName = extendsName; }
    public List<String> getImplementsNames() { return implementsNames; }
    public List<Member> getMembers() { return members; }
    public Set<String> getBodyRefs() { return bodyRefs; }

    public void addMember(Member m) { members.add(m); }
    public void addImplements(String iface) { implementsNames.add(iface); }
    public void addBodyRef(String ref) { bodyRefs.add(ref); }

    public String toMermaid(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%sclass %s {\r\n", indent, name));
        for (Member m : members) {
            if (!m.isMethod())
                sb.append(String.format("%s    %s\r\n", indent, m.toMermaid()));
        }
        for (Member m : members) {
            if (m.isMethod())
                sb.append(String.format("%s    %s\r\n", indent, m.toMermaid()));
        }
        sb.append(String.format("%s}\r\n", indent));
        return sb.toString();
    }
}
