package lox;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    private class NotInitializedType{}
    boolean repl;
    private Environment globals = new Environment();
    // implements Stmt.Visitor
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) throws FailedRuntime {
        Object value = evaluate(stmt.expression);
        if (repl) System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) throws FailedRuntime {
        Object value = new NotInitializedType();
        if (stmt.initializer != null) value = evaluate(stmt.initializer);
        globals.define(stmt.name, value);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) throws FailedRuntime {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitQuitStmt(Stmt.Quit stmt) throws FailedRuntime {
        Object value = evaluate(stmt.value);
        if (value instanceof Boolean) value = (boolean)value ? 1.0 : 0.0;
        else if (value == null) throw new FailedRuntime(stmt.quit, "Exit code must be some sort of number. (Not \"nil\")");
        else if (value instanceof String) throw new FailedRuntime(stmt.quit, "Exit code must be some sort of number. (Not str '" + value+ "')");
        System.exit(((Double)value).intValue());
        return null;
    }

    @Override
    public Void visitDeleteStmt(Stmt.Delete stmt) throws FailedRuntime {
        globals.del(stmt.name);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) throws FailedRuntime {
        executeBlock(stmt.statements, new Environment(globals));
        return null;
    }
    // implements Expr.Visitor
    @Override
    public Object visitAssignExpr(Expr.Assign expr) throws FailedRuntime {
        Object value = evaluate(expr.value);
        globals.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) throws FailedRuntime {
        if (globals.get(expr.name) instanceof NotInitializedType) throw new FailedRuntime(expr.name, "Variable '" + expr.name.lexeme + "' has not been initialized yet, can't use it's value.");
        return globals.get(expr.name);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }
            case BANG -> { return !isTruthy(right); }
            default -> { return null; /* unreachable */}
        }
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) throws FailedRuntime {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case EQUAL_EQUAL ->   { return isEqual(left, right); }
            case BANG_EQUAL ->    { return !isEqual(left, right); }
            case SLASH ->         { checkNumberOperands(expr.operator, left, right); handle_division(left, right, expr.operator); }
            case LESS_EQUAL ->    { checkNumberOperands(expr.operator, left, right); return (double) left <= (double) right; }
            case GREATER_EQUAL -> { checkNumberOperands(expr.operator, left, right); return (double) left >= (double) right; }
            case GREATER ->       { checkNumberOperands(expr.operator, left, right); return (double) left > (double) right; }
            case LESS ->          { checkNumberOperands(expr.operator, left, right); return (double) left < (double) right; }
            case MINUS ->         { checkNumberOperands(expr.operator, left, right); return (double) left - (double) right; }
            case STAR ->          { checkNumberOperands(expr.operator, left, right); return (double) left * (double) right; }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String || right instanceof String) {
                    if (left instanceof Double) {
                        String str = left.toString();
                        if (str.endsWith(".0")) str = str.substring(0, str.length() - 2);
                        return str + right;
                    }
                    if (right instanceof Double) {
                        String str = right.toString();
                        if (str.endsWith(".0")) str = str.substring(0, str.length() - 2);
                        return left + str;
                    }
                }
                throw new FailedRuntime(expr.operator, "Operands must be two numbers or two strings.");
            }
        }
        return null;
    }

    // helpers
    void executeBlock(List<Stmt> statements, Environment environment) throws FailedRuntime {
        Environment previous = this.globals;
        try {
            this.globals = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.globals = previous;
        }
    }

    private Object handle_division(Object left, Object right, Token operator) throws FailedRuntime {
        if ((double)right == 0.0) {
            throw new FailedRuntime(operator, "Division by zero is not allowed.");
        }
        return (double)left / (double)right;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null)              return false;
        return a.equals(b);
    }
    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Double && (double)object == 0.0) return false;
        if (object instanceof Boolean) {
            return (boolean)object;
        }
        return true;
    }

    private void checkNumberOperand(Token operator, Object operand) throws FailedRuntime {
        if (operand instanceof Double) return;
        throw new FailedRuntime(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) throws FailedRuntime {
        if (left instanceof Double && right instanceof Double) return;
        throw new FailedRuntime(operator, "Operand must be a number.");
    }
    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    public void interpret(List<Stmt> statements) throws FailedRuntime {
        try {
            for (Stmt stmt : statements) execute(stmt);
        } catch (FailedRuntime error) {
            Lox.runtimeError(error);
        }
    }
    public void setRepl(boolean repl) {
        this.repl = repl;
    }
}
