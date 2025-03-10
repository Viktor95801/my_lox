package lox;

class FailedRuntime extends RuntimeException 
{
    final Token token;

    FailedRuntime(Token token, String message) {
        super(message);
        this.token = token;
    }
}