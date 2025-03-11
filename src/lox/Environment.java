package lox;

import java.util.HashMap;
import java.util.Map;

public class Environment
{
    private Map<String, Object> values = new HashMap<>();

    void define(Token identifier, Object value) throws FailedRuntime {
        if (values.containsKey(identifier.lexeme)) {
            throw new FailedRuntime(identifier, "Variable '" + identifier.lexeme + "' is already defined.");
        }
        values.put(identifier.lexeme, value);
    }

    boolean contains(String identifier) {
        return values.containsKey(identifier);
    }

    Object get(Token identifier) throws FailedRuntime {
        if (!values.containsKey(identifier.lexeme)) {
            throw new FailedRuntime(identifier, "Undefined identifier '" + identifier.lexeme + "'.");
        }
        return values.get(identifier.lexeme);
    }
}
