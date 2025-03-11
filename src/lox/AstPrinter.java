package lox;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String>
{

    String print(List<Stmt> stmts) {
        StringBuilder sb = new StringBuilder();
        for (Stmt stmt : stmts) {
            sb.append(stmt.accept(this) + " | ");
        }
        sb.delete(sb.length() - 3, sb.length());
        return sb.toString();
    }

    // statements
    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize(stmt.expression.accept(this) + ";");
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print " + stmt.expression.accept(this) + ";");
    }

    @Override
    public String visitQuitStmt(Stmt.Quit stmt) {
        return parenthesize("quit " + stmt.value.accept(this) + ";");
    }
    // expressions
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
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(name);
        for (Expr expr : exprs) {
            sb.append(" ");
            sb.append(expr.accept(this));
        }
        sb.append(")");

        return sb.toString();
    }
}
