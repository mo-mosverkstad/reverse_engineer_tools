package uml4java;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClassInfo {
    public String name = "";
    public String filepath = "";
    public String extendsName = "";
    public List<String> implementsNames = new ArrayList<>();
    public List<Member> members = new ArrayList<>();
    public Set<String> bodyRefs = new LinkedHashSet<>();
}
