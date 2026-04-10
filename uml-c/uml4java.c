#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <dirent.h>
#include <sys/stat.h>

#define MAX_LINE 1024
#define MAX_MEMBERS 100
#define MAX_CLASSES 50
#define MAX_RELS 200
#define MAX_REFS 200

typedef struct {
    char visibility;
    char type[128];
    char name[128];
    char params[512];
    int is_method;
    int is_static;
} Member;

typedef struct {
    char name[128];
    char filepath[MAX_LINE];
    char extends_name[128];
    char implements_names[10][128];
    int implements_count;
    Member members[MAX_MEMBERS];
    int member_count;
    char body_refs[MAX_REFS][128];
    int body_ref_count;
} Class;

typedef enum {
    REL_ASSOCIATION,
    REL_DEPENDENCY,
    REL_INHERITANCE,
    REL_IMPLEMENTATION
} RelType;

typedef struct {
    char from[128];
    char to[128];
    RelType type;
    char label[128];
} Relationship;

static Class classes[MAX_CLASSES];
static int class_count = 0;
static Relationship rels[MAX_RELS];
static int rel_count = 0;
static char unresolved[MAX_REFS][128];
static int unresolved_count = 0;
static const char *PRIMITIVES[] = {
    "int","long","short","byte","float","double","boolean","char",
    "String","void","Object","System","Integer","Long","Short","Byte",
    "Float","Double","Boolean","Character","Math","Arrays","Collections",
    "List","ArrayList","Map","HashMap","Set","HashSet","Optional",
    "StringBuilder","StringBuffer","Exception","RuntimeException",NULL
};

int is_primitive(const char *type) {
    for(int i = 0; PRIMITIVES[i]; i++)
        if(strcmp(type, PRIMITIVES[i]) == 0) return 1;
    return 0;
}

int is_known_class(const char *name) {
    for(int i = 0; i < class_count; i++)
        if(strcmp(classes[i].name, name) == 0) return 1;
    return 0;
}

int is_unresolved(const char *name) {
    for(int i = 0; i < unresolved_count; i++)
        if(strcmp(unresolved[i], name) == 0) return 1;
    return 0;
}

void add_unresolved(const char *name) {
    if(is_primitive(name) || is_known_class(name) || is_unresolved(name)) return;
    if(strlen(name) == 0 || !isupper((unsigned char)name[0])) return;
    if(unresolved_count >= MAX_REFS) return;
    strcpy(unresolved[unresolved_count++], name);
}

int rel_exists(const char *from, const char *to, RelType type) {
    for(int i = 0; i < rel_count; i++)
        if(strcmp(rels[i].from, from) == 0 && strcmp(rels[i].to, to) == 0 && rels[i].type == type) return 1;
    return 0;
}

void add_rel(const char *from, const char *to, RelType type, const char *label) {
    if(strcmp(from, to) == 0) return;
    if(rel_exists(from, to, type)) return;
    if(rel_count >= MAX_RELS) return;
    strcpy(rels[rel_count].from, from);
    strcpy(rels[rel_count].to, to);
    rels[rel_count].type = type;
    if(label) strcpy(rels[rel_count].label, label);
    else rels[rel_count].label[0] = '\0';
    rel_count++;
}

void trim(char *str) {
    char *start = str;
    char *end;
    while(isspace((unsigned char)*start)) start++;
    if(*start == 0) { *str = '\0'; return; }
    end = start + strlen(start) - 1;
    while(end > start && isspace((unsigned char)*end)) end--;
    end[1] = '\0';
    if(start != str) memmove(str, start, strlen(start) + 1);
}

/* strip generic parameters: Map<String, List<Integer>> -> Map */
void strip_generics(char *type) {
    char *lt = strchr(type, '<');
    if(lt) *lt = '\0';
}

/* strip array brackets: String[] -> String */
void strip_array(char *type) {
    char *br = strchr(type, '[');
    if(br) *br = '\0';
}

char get_visibility(const char *line) {
    if(strstr(line, "public")) return '+';
    if(strstr(line, "private")) return '-';
    if(strstr(line, "protected")) return '#';
    return '~';
}

void skip_modifiers(char **ptr) {
    while(**ptr && (strstr(*ptr, "public") == *ptr || strstr(*ptr, "private") == *ptr ||
                    strstr(*ptr, "protected") == *ptr || strstr(*ptr, "static") == *ptr ||
                    strstr(*ptr, "final") == *ptr || strstr(*ptr, "abstract") == *ptr)) {
        while(**ptr && !isspace(**ptr)) (*ptr)++;
        while(**ptr && isspace(**ptr)) (*ptr)++;
    }
}

void parse_field(const char *line, Member *member) {
    char temp[MAX_LINE];
    strcpy(temp, line);
    char *ptr = temp;
    skip_modifiers(&ptr);

    char *space = strchr(ptr, ' ');
    if(space) {
        *space = '\0';
        strcpy(member->type, ptr);
        strip_generics(member->type);
        strip_array(member->type);
        ptr = space + 1;
        char *semicolon = strchr(ptr, ';');
        char *equals = strchr(ptr, '=');
        if(equals && (!semicolon || equals < semicolon)) *equals = '\0';
        if(semicolon) *semicolon = '\0';
        trim(ptr);
        strcpy(member->name, ptr);
    }
    member->is_method = 0;
}

void parse_method(const char *line, Member *member) {
    char temp[MAX_LINE];
    strcpy(temp, line);
    member->is_static = (strstr(temp, "static") != NULL);

    char *ptr = temp;
    skip_modifiers(&ptr);

    if(strstr(ptr, "void") == ptr) {
        strcpy(member->type, "void");
        ptr += 4;
        while(*ptr && isspace(*ptr)) ptr++;
    }

    char *paren = strchr(ptr, '(');
    if(paren) {
        *paren = '\0';
        char *space = strrchr(ptr, ' ');
        if(space) {
            *space = '\0';
            if(strlen(member->type) == 0) strcpy(member->type, ptr);
            strcpy(member->name, space + 1);
        } else {
            strcpy(member->name, ptr);
        }
        trim(member->name);
        char *end_paren = strchr(paren + 1, ')');
        if(end_paren) {
            *end_paren = '\0';
            strcpy(member->params, paren + 1);
            trim(member->params);
        }
    }
    member->is_method = 1;
}

static Class *current_parsing_class = NULL;

/* extract type names referenced in method body lines (e.g. "new Bowl()") */
void collect_body_refs(const char *line) {
    const char *p = line;
    while((p = strstr(p, "new ")) != NULL) {
        p += 4;
        while(*p && isspace(*p)) p++;
        char type[128];
        int k = 0;
        while(*p && (isalnum(*p) || *p == '_') && k < 127)
            type[k++] = *p++;
        type[k] = '\0';
        if(k > 0) {
            add_unresolved(type);
            if(current_parsing_class && current_parsing_class->body_ref_count < MAX_REFS) {
                strcpy(current_parsing_class->body_refs[current_parsing_class->body_ref_count++], type);
            }
        }
    }
}

void parse_java_file(const char *filename, Class *cls) {
    FILE *fp = fopen(filename, "r");
    if(!fp) { fprintf(stderr, "Error: Cannot open file %s\n", filename); return; }

    strcpy(cls->filepath, filename);
    current_parsing_class = cls;
    char line[MAX_LINE];
    int in_class = 0, brace_count = 0, in_comment = 0;

    while(fgets(line, sizeof(line), fp)) {
        char *nl = strchr(line, '\n'); if(nl) *nl = '\0';
        char *cr = strchr(line, '\r'); if(cr) *cr = '\0';
        trim(line);

        if(!in_class && strstr(line, "class ") && !strstr(line, "//")) {
            int has_brace = (strchr(line, '{') != NULL);

            char *ext = strstr(line, "extends ");
            if(ext) {
                ext += 8;
                while(*ext && isspace(*ext)) ext++;
                char *e = ext;
                while(*e && !isspace(*e) && *e != '{' && *e != ',') e++;
                char save = *e; *e = '\0';
                strcpy(cls->extends_name, ext);
                add_unresolved(cls->extends_name);
                *e = save;
            }

            char *impl = strstr(line, "implements ");
            if(impl) {
                impl += 11;
                while(*impl) {
                    while(*impl && isspace(*impl)) impl++;
                    if(*impl == '{' || *impl == '\0') break;
                    char *e = impl;
                    while(*e && !isspace(*e) && *e != ',' && *e != '{') e++;
                    char save = *e; *e = '\0';
                    strcpy(cls->implements_names[cls->implements_count++], impl);
                    add_unresolved(impl);
                    *e = save;
                    if(save == ',') e++;
                    impl = e;
                }
            }

            char *class_ptr = strstr(line, "class ");
            class_ptr += 6;
            while(*class_ptr && isspace(*class_ptr)) class_ptr++;
            char *end = class_ptr;
            while(*end && !isspace(*end) && *end != '{' && *end != '<') end++;
            *end = '\0';
            strcpy(cls->name, class_ptr);
            in_class = 1;
            if(has_brace) brace_count++;
            continue;
        }

        if(!in_class) continue;

        if(strlen(line) == 0 || strstr(line, "//") == line) continue;
        if(line[0] == '/' && line[1] == '*') { in_comment = 1; continue; }
        if(in_comment) { if(strstr(line, "*/")) in_comment = 0; continue; }
        if(line[0] == '*') continue;

        int prev_brace = brace_count;
        for(int i = 0; line[i]; i++) {
            if(line[i] == '{') brace_count++;
            if(line[i] == '}') brace_count--;
        }

        if(brace_count == 0) break;

        /* collect references from method bodies */
        if(prev_brace > 1) {
            collect_body_refs(line);
            continue;
        }
        if(prev_brace != 1) continue;

        if(strchr(line, '(') && !strstr(line, "class")) {
            Member member = {0};
            member.visibility = get_visibility(line);
            parse_method(line, &member);
            if(strlen(member.name) > 0) {
                cls->members[cls->member_count++] = member;
                /* collect param type refs */
                char params_copy[512];
                strcpy(params_copy, member.params);
                char *tok = strtok(params_copy, ",");
                while(tok) {
                    trim(tok);
                    char *sp = strchr(tok, ' ');
                    if(sp) {
                        *sp = '\0';
                        char ptype[128];
                        strcpy(ptype, tok);
                        strip_generics(ptype);
                        strip_array(ptype);
                        add_unresolved(ptype);
                    }
                    tok = strtok(NULL, ",");
                }
                /* return type ref */
                if(strlen(member.type) > 0) {
                    char rtype[128];
                    strcpy(rtype, member.type);
                    strip_generics(rtype);
                    strip_array(rtype);
                    add_unresolved(rtype);
                }
            }
        } else if(strchr(line, ';') && (strstr(line, "public") || strstr(line, "private") || strstr(line, "protected"))) {
            Member member = {0};
            member.visibility = get_visibility(line);
            parse_field(line, &member);
            if(strlen(member.name) > 0) {
                cls->members[cls->member_count++] = member;
                add_unresolved(member.type);
            }
        }
    }
    fclose(fp);
}

/* get directory part of a file path */
void get_directory(const char *filepath, char *dir, size_t size) {
    strncpy(dir, filepath, size);
    dir[size - 1] = '\0';
    /* find last / or \ */
    char *last_sep = NULL;
    for(char *p = dir; *p; p++) {
        if(*p == '/' || *p == '\\') last_sep = p;
    }
    if(last_sep) *(last_sep + 1) = '\0';
    else strcpy(dir, "./");
}

/* try to find ClassName.java in the given directory */
int find_and_parse_class(const char *class_name, const char *search_dir) {
    DIR *dir = opendir(search_dir);
    if(!dir) return 0;

    char expected[256];
    snprintf(expected, sizeof(expected), "%s.java", class_name);

    struct dirent *entry;
    while((entry = readdir(dir)) != NULL) {
        if(strcmp(entry->d_name, expected) == 0) {
            char path[MAX_LINE];
            snprintf(path, sizeof(path), "%s%s", search_dir, entry->d_name);
            if(class_count < MAX_CLASSES) {
                parse_java_file(path, &classes[class_count]);
                if(strlen(classes[class_count].name) > 0) {
                    class_count++;
                    closedir(dir);
                    return 1;
                }
            }
        }
    }
    closedir(dir);
    return 0;
}

/* iteratively discover all referenced classes */
void discover_classes(const char *initial_dir) {
    int changed = 1;
    while(changed) {
        changed = 0;
        for(int i = 0; i < unresolved_count; i++) {
            if(is_known_class(unresolved[i]) || is_primitive(unresolved[i])) continue;
            if(find_and_parse_class(unresolved[i], initial_dir)) {
                changed = 1;
            }
        }
    }
}

void detect_relationships(void) {
    for(int i = 0; i < class_count; i++) {
        Class *c = &classes[i];

        if(strlen(c->extends_name) > 0 && is_known_class(c->extends_name))
            add_rel(c->name, c->extends_name, REL_INHERITANCE, NULL);

        for(int j = 0; j < c->implements_count; j++)
            if(is_known_class(c->implements_names[j]))
                add_rel(c->name, c->implements_names[j], REL_IMPLEMENTATION, NULL);

        for(int j = 0; j < c->member_count; j++) {
            Member *m = &c->members[j];
            if(!m->is_method) {
                if(!is_primitive(m->type) && is_known_class(m->type))
                    add_rel(c->name, m->type, REL_ASSOCIATION, m->name);
            } else {
                char params_copy[512];
                strcpy(params_copy, m->params);
                char *tok = strtok(params_copy, ",");
                while(tok) {
                    trim(tok);
                    char *sp = strchr(tok, ' ');
                    if(sp) {
                        *sp = '\0';
                        char ptype[128];
                        strcpy(ptype, tok);
                        strip_generics(ptype);
                        strip_array(ptype);
                        if(!is_primitive(ptype) && is_known_class(ptype))
                            add_rel(c->name, ptype, REL_DEPENDENCY, NULL);
                    }
                    tok = strtok(NULL, ",");
                }
                if(strlen(m->type) > 0) {
                    char rtype[128];
                    strcpy(rtype, m->type);
                    strip_generics(rtype);
                    strip_array(rtype);
                    if(!is_primitive(rtype) && is_known_class(rtype))
                        add_rel(c->name, rtype, REL_DEPENDENCY, NULL);
                }
            }
        }

        for(int j = 0; j < c->body_ref_count; j++) {
            if(!is_primitive(c->body_refs[j]) && is_known_class(c->body_refs[j]))
                add_rel(c->name, c->body_refs[j], REL_DEPENDENCY, NULL);
        }
    }
}

void generate_uml_md(const char *output_file) {
    FILE *fp = fopen(output_file, "w");
    if(!fp) { fprintf(stderr, "Error: Cannot create output file %s\n", output_file); exit(1); }

    fprintf(fp, "# UML Class Diagram\r\n\r\n");
    fprintf(fp, "```mermaid\r\n");
    fprintf(fp, "classDiagram\r\n");

    for(int c = 0; c < class_count; c++) {
        Class *cls = &classes[c];
        fprintf(fp, "    class %s {\r\n", cls->name);

        for(int i = 0; i < cls->member_count; i++) {
            Member m = cls->members[i];
            if(!m.is_method)
                fprintf(fp, "        %c%s %s\r\n", m.visibility, m.type, m.name);
        }
        for(int i = 0; i < cls->member_count; i++) {
            Member m = cls->members[i];
            if(m.is_method) {
                if(m.is_static)
                    fprintf(fp, "        %c%s(%s)$ %s\r\n", m.visibility, m.name, m.params, m.type);
                else if(strlen(m.type) > 0)
                    fprintf(fp, "        %c%s(%s) %s\r\n", m.visibility, m.name, m.params, m.type);
                else
                    fprintf(fp, "        %c%s(%s)\r\n", m.visibility, m.name, m.params);
            }
        }
        fprintf(fp, "    }\r\n");
    }

    for(int i = 0; i < rel_count; i++) {
        switch(rels[i].type) {
            case REL_INHERITANCE:
                fprintf(fp, "    %s --|> %s\r\n", rels[i].from, rels[i].to);
                break;
            case REL_IMPLEMENTATION:
                fprintf(fp, "    %s ..|> %s\r\n", rels[i].from, rels[i].to);
                break;
            case REL_ASSOCIATION:
                if(strlen(rels[i].label) > 0)
                    fprintf(fp, "    %s --> %s : %s\r\n", rels[i].from, rels[i].to, rels[i].label);
                else
                    fprintf(fp, "    %s --> %s\r\n", rels[i].from, rels[i].to);
                break;
            case REL_DEPENDENCY:
                fprintf(fp, "    %s ..> %s\r\n", rels[i].from, rels[i].to);
                break;
        }
    }

    fprintf(fp, "```\r\n");
    fclose(fp);
}

int ends_with_java(const char *name) {
    size_t len = strlen(name);
    return len > 5 && strcmp(name + len - 5, ".java") == 0;
}

void scan_directory(const char *dir_path) {
    DIR *dir = opendir(dir_path);
    if(!dir) return;
    struct dirent *entry;
    while((entry = readdir(dir)) != NULL) {
        if(entry->d_name[0] == '.') continue;
        char path[MAX_LINE];
        snprintf(path, sizeof(path), "%s/%s", dir_path, entry->d_name);
        struct stat st;
        if(stat(path, &st) == 0 && S_ISDIR(st.st_mode)) {
            scan_directory(path);
        } else if(ends_with_java(entry->d_name) && class_count < MAX_CLASSES) {
            parse_java_file(path, &classes[class_count]);
            if(strlen(classes[class_count].name) > 0) class_count++;
        }
    }
    closedir(dir);
}

int main(int argc, char *argv[]) {
    if(argc < 7) {
        fprintf(stderr, "Usage: %s --type class --input <java_file_or_dir> [<file2> ...] --output <md_file>\n", argv[0]);
        return 1;
    }

    char *output_file = NULL;
    int input_start = -1, input_end = -1;

    for(int i = 1; i < argc; i++) {
        if(strcmp(argv[i], "--output") == 0 && i + 1 < argc) {
            output_file = argv[++i];
        } else if(strcmp(argv[i], "--input") == 0) {
            input_start = i + 1;
        }
    }

    if(input_start < 0 || !output_file) {
        fprintf(stderr, "Error: Missing required arguments\n");
        return 1;
    }

    input_end = argc;
    for(int i = input_start; i < argc; i++) {
        if(strncmp(argv[i], "--", 2) == 0) { input_end = i; break; }
    }

    /* track directories of input files for iterative discovery */
    char search_dirs[MAX_CLASSES][MAX_LINE];
    int search_dir_count = 0;

    for(int i = input_start; i < input_end; i++) {
        struct stat st;
        if(stat(argv[i], &st) == 0 && S_ISDIR(st.st_mode)) {
            scan_directory(argv[i]);
        } else if(ends_with_java(argv[i]) && class_count < MAX_CLASSES) {
            parse_java_file(argv[i], &classes[class_count]);
            if(strlen(classes[class_count].name) > 0) {
                /* remember directory for discovery */
                if(search_dir_count < MAX_CLASSES) {
                    get_directory(argv[i], search_dirs[search_dir_count], MAX_LINE);
                    search_dir_count++;
                }
                class_count++;
            }
        }
    }

    /* iteratively discover referenced classes */
    for(int d = 0; d < search_dir_count; d++) {
        discover_classes(search_dirs[d]);
    }

    detect_relationships();
    generate_uml_md(output_file);

    printf("UML diagram generated: %s (%d classes, %d relationships)\n", output_file, class_count, rel_count);
    return 0;
}
