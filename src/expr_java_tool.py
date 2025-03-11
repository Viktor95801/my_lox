import os
import sys
import io

def define_type(base_name:str, class_name: str, fields: str, f: io.TextIOWrapper, tab_spc: int = 4) -> None:
    tab: str = " " * tab_spc
    f.write(tab + "static class " + class_name + " extends " + base_name + " {\n")
    
    # constructor
    f.write(tab*2 + f"{class_name}({fields}) ""{\n")
    
    fieldList: list[str] = [field.strip() for field in fields.split(",")]
    
    for field in fieldList:
        name: str = field.split(" ")[1]
        f.write(tab*3 + "this." + name + " = " + name + ";\n")
    f.write(tab*2 + "}\n")
    
    f.write(tab*2+"@Override\n")
    f.write(tab*2+"<R> R accept(Visitor<R> visitor) {\n")
    f.write(tab*3+"return visitor.visit" + class_name + base_name + "(this);\n")
    f.write(tab*2+"}\n")
    # field declarations
    for field in fieldList:
        f.write(tab*2 + "final " + field + ";\n")
    f.write(tab + "}\n")

def define_visitor(base_name: str, types: list[str], f: io.TextIOWrapper, tab_spc: int = 4) -> None:
    tab: str = " " * tab_spc
    
    f.write(tab + "interface Visitor<R> {\n")
    
    for t in types:
        type_name: str = t.split(":")[0].strip()
        f.write(tab*2 + "R visit" + type_name + base_name + "(" + type_name + " " + base_name.lower() + ");\n")
    
    f.write(tab + "}\n")

def define_tree(out_dir: str, base_name: str, types: list[str], tab_spc: int = 4) -> None:
    tab: str = " " * tab_spc
    path: str = os.path.join(out_dir, base_name + ".java")
    
    with open(path, "w") as f:
        f.write("package lox;\n\n")
        f.write("abstract class " + base_name + "\n{\n")
        
        define_visitor(base_name, types, f)
        
        # the AST classes
        for t in types:
            t_split: list[str] = t.split(":")
            class_name: str = t_split[0].strip()
            fields: str = t_split[1].strip()
            del t_split
            define_type(base_name, class_name, fields, f)
        
        f.write(tab+"abstract <R> R accept(Visitor<R> visitor);\n")
        
        f.write("}\n")

def types1() -> list[str]:
    return ["Literal  : Object value","Grouping : Expr expression","Unary    : Token operator, Expr right","Binary   : Expr left, Token operator, Expr right","Variable : Token name", "Assign : Token name, Expr value"]
def types2() -> list[str]:
    return ["Expression : Expr expression", "Print : Expr expression", "Quit : Expr value, Token quit", "Var : Token name, Expr initializer"]
def main(args: list[str]) -> int:
    if len(args) != 1:
        print("Usage: expr_java_tool.py <output_path>")
        return 1
    output_path: str = args[0]
    define_tree(output_path, "Expr", types1())
    define_tree(output_path, "Stmt", types2())

if __name__ == "__main__":
    args: list[str] = sys.argv[1:]
    exit_code: int = main(args)
    sys.exit(exit_code)
