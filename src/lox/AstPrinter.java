package lox;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String>
{
    String print(List<Stmt> stmts) {
        StringBuilder sb = new StringBuilder();
        for (Stmt stmt : stmts) {
            sb.append(stmt.accept(this) + " | ");
        }
        if (sb.length() > 3) sb.delete(sb.length() - 3, sb.length());
        return sb.toString() + ((sb.length() > 0) ? "\n" : "");
    }

    // statements
    @Override public String visitIfStmt(Stmt.If stmt) {
        return parenthesize("if " + stmt.condition.accept(this) + curlyieBlock("then", List.of(stmt.thenBranch)) + ((stmt.elseBranch != null) ? curlyieBlock("else", List.of(stmt.elseBranch)) : ""));
    }

    @Override public String visitWhileStmt(Stmt.While stmt) {
        return parenthesize("while " + stmt.condition.accept(this) + curlyieBlock("do", List.of(stmt.body)));
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return curlyieBlock("block", stmt.statements);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize(stmt.expression.accept(this) + ";");
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print " + stmt.expression.accept(this) + ";");
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer == null) return parenthesize("var " + stmt.name.lexeme + ";");
        return parenthesize("var " + stmt.name.lexeme + " = " + stmt.initializer.accept(this) + ";");
    }

    @Override
    public String visitQuitStmt(Stmt.Quit stmt) {
        return parenthesize("quit " + stmt.value.accept(this) + ";");
    }

    @Override
    public String visitDeleteStmt(Stmt.Delete stmt) {
        return parenthesize("del " + stmt.name.lexeme + ";");
    }
    // expressions
    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme,
                            expr.left, expr.right);
    }
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize(expr.name.lexeme + "<-" + expr.value.accept(this));
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize(expr.name.lexeme);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme,
                expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return fmtSTR(expr.value.toString());
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return callieCallee(expr);
    }


    private String callieCallee(Expr.Call expr) {
        StringBuilder sb = new StringBuilder();
        for (Expr each : expr.arguments) {
            sb.append(each.accept(this) + " | ");
        }
        if (sb.length() > 3) sb.delete(sb.length() - 3, sb.length());
        return "(fn->" + expr.callee.accept(this) + ": (" + sb.toString() + ") )";
    }
    private String curlyieBlock(String name, List<Stmt> block) {
        StringBuilder sb = new StringBuilder();
        for (Stmt stmt : block) {
            sb.append(stmt.accept(this) + " | ");
        }
        if (sb.length() > 3) sb.delete(sb.length() - 3, sb.length());
        return "(" +name + " " + "{" + sb.toString() + "}" + ")";
    }
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(name);
        for (Expr expr : exprs) {
            sb.append(" ");
            sb.append(fmtSTR(expr.accept(this)));
        }
        sb.append(")");

        return sb.toString();
    }

    private String fmtSTR(String str) {
        if (str.endsWith(".0")) str = str.substring(0, str.length() - 2);
        if (str.length() == 0) return "'emptSTR'";
        return str;
    }
}
