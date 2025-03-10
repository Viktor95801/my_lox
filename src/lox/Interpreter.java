package lox;

class Interpreter implements Expr.Visitor<Object>
{

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

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) throws FailedRuntime {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
                    case EQUAL_EQUAL ->   { return isEqual(left, right); }
                    case BANG_EQUAL ->    { return !isEqual(left, right); }
                    case LESS_EQUAL ->    { checkNumberOperands(expr.operator, left, right); return (double) left <= (double) right; }
                    case GREATER_EQUAL -> { checkNumberOperands(expr.operator, left, right); return (double) left >= (double) right; }
                    case GREATER ->       { checkNumberOperands(expr.operator, left, right); return (double) left > (double) right; }
                    case LESS ->          { checkNumberOperands(expr.operator, left, right); return (double) left < (double) right; }
                    case MINUS ->         { checkNumberOperands(expr.operator, left, right); return (double) left - (double) right; }
                    case SLASH ->         { checkNumberOperands(expr.operator, left, right); return (double) left / (double) right; }
                    case STAR ->          { checkNumberOperands(expr.operator, left, right); return (double) left * (double) right; }
                    case PLUS -> {
                        if (left instanceof Double && right instanceof Double) {
                            return (double) left + (double) right;
                        } else if (left instanceof String && right instanceof String) {
                            return (String) left + (String) right;
                        }
                        throw new FailedRuntime(expr.operator, "Operands must be two numbers or two strings.");
                    }
                    default -> { return null; /* unreachable */}
        }
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

    public void interpret(Expr expression) throws FailedRuntime {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (FailedRuntime error) {
            Lox.runtimeError(error);
        }
    }
}
