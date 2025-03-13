// THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    private class NotInitializedType{}
    boolean repl;
    private final Environment globals = new Environment();
    private Environment environment = globals;
    
    Interpreter() {
        try {
            globals.defineNative("clock", new LoxCallable() {
                @Override
                public int arity() { return 0; }
                
                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    return (double)System.currentTimeMillis() / 1000.0;
                }
                
                @Override
                public String toString() { return "<native 'clock' fn>"; } 
            });
        } catch (Environment.FailedNative e) {
            Lox.nativeError(e);
        }
    }
    
    // implements Stmt.Visitor
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) throws FailedRuntime {
        Object value = evaluate(stmt.expression);
        if (repl && !(stmt.expression instanceof Expr.Assign)) System.out.println(stringify(value));
        return null;
    }
    
    @Override
    public Void visitVarStmt(Stmt.Var stmt) throws FailedRuntime {
        Object value = new NotInitializedType();
        if (stmt.initializer != null) value = evaluate(stmt.initializer);
        environment.define(stmt.name, value);
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
        environment.del(stmt.name);
        return null;
    }
    
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) throws FailedRuntime {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    
    @Override
    public Void visitIfStmt(Stmt.If stmt) throws FailedRuntime {
        if (isTruthy(evaluate(stmt.condition))) execute(stmt.thenBranch);
        else if (stmt.elseBranch != null) execute(stmt.elseBranch);
        return null;
    }
    
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    // implements Expr.Visitor
    @Override
    public Object visitCallExpr(Expr.Call expr) throws FailedRuntime {
        Object callee = evaluate(expr.callee);
        
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        
        if (!(callee instanceof LoxCallable)) throw new FailedRuntime(expr.paren, "Can only call functions and classes. (Not " + stringify(callee) + ")");
        LoxCallable func = (LoxCallable)callee;
        if (arguments.size() != func.arity()) throw new FailedRuntime(expr.paren, "Expected " + func.arity() + " many arguments but got " + arguments.size() + ".");
        return func.call(this, arguments);
    }
    
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) throws FailedRuntime {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR && isTruthy(left)) return left;
        if (expr.operator.type == TokenType.AND && !isTruthy(left)) return left;
        return evaluate(expr.right);
    }
    
    @Override
    public Object visitAssignExpr(Expr.Assign expr) throws FailedRuntime {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }
    
    @Override
    public Object visitVariableExpr(Expr.Variable expr) throws FailedRuntime {
        if (environment.get(expr.name) instanceof NotInitializedType) throw new FailedRuntime(expr.name, "Variable '" + expr.name.lexeme + "' has not been initialized yet, can't use it's value.");
        return environment.get(expr.name);
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
    Environment previous = this.environment;
    try {
        this.environment = environment;
        
        for (Stmt statement : statements) {
            execute(statement);
        }
    } finally {
        this.environment = previous;
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
    if (object == null) return false;
    if (object instanceof String) return ((String)object).length() > 0;
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
