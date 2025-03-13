package lox;

import java.util.HashMap;
import java.util.Map;

public class Environment
{
    class FailedNative extends RuntimeException {
        public FailedNative(String name, String message) {
            super(name + ": " + message);
        }
    }
    final Environment enclosing;
    private Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void del(Token name) {
       if (!values.containsKey(name.lexeme)) {
           if (enclosing != null) enclosing.del(name);
           throw new FailedRuntime(name, "Cannot delete undefined identifier '" + name.lexeme + "'.");
        }
        values.remove(name.lexeme);
    }
    void define(Token identifier, Object value) throws FailedRuntime {
        if (values.containsKey(identifier.lexeme)) {
            throw new FailedRuntime(identifier, "Variable '" + identifier.lexeme + "' is already defined.");
        }
        values.put(identifier.lexeme, value);
    }
    void defineNative(String name, Object value) throws FailedNative {
        if (values.containsKey(name)) {
            throw new FailedNative(name, "Native '" + name + "' is already defined.");
        }
        values.put(name, value);
    }
    void assign(Token name, Object value) throws FailedRuntime {
        if (values.containsKey((name.lexeme))) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) { enclosing.assign(name, value); return; };
        throw new FailedRuntime(name, "Undefined variable: '" + name.lexeme + "'.");
    }

    Object get(Token name) throws FailedRuntime {
        if (!values.containsKey(name.lexeme)) {
            if (enclosing != null) return enclosing.get(name);
            throw new FailedRuntime(name, "Undefined identifier '" + name.lexeme + "'.");
        }
        return values.get(name.lexeme);
    }
}
