package lox;

abstract class Stmt
{
    interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitQuitStmt(Quit stmt);
        R visitVarStmt(Var stmt);
    }
    static class Expression extends Stmt {
        Expression(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
        final Expr expression;
    }
    static class Print extends Stmt {
        Print(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
        final Expr expression;
    }
    static class Quit extends Stmt {
        Quit(Expr value, Token quit) {
            this.value = value;
            this.quit = quit;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitQuitStmt(this);
        }
        final Expr value;
        final Token quit;
    }
    static class Var extends Stmt {
        Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
        final Token name;
        final Expr initializer;
    }
    abstract <R> R accept(Visitor<R> visitor);
}
