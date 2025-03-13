
package lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import static lox.TokenType.*;


class Parser
{

    static class ParseError extends Exception {
    }

    private final List<Token> tokens;
    private int current = 0;

    List<Stmt> parse() throws ParseError {
        return program();
    }

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // helpers
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private Token consume(TokenType type, String message) throws ParseError {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> { return; }
                default -> { break; }
            }

            advance();
        }
    }

    // implementation
    
    // statement handlers
    private Stmt printStatement() throws ParseError {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value. (Not " + peek().type + ")");
        return new Stmt.Print(value);
    }
    private Stmt quitStatement() throws ParseError {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value. (Not " + peek().type + ")");
        if (expr instanceof Expr.Literal) {
            if (((Expr.Literal)expr).value == null) error(peek(), "Exit code must be some sort of number. (Not \"nil\").");
        }
        return new Stmt.Quit(expr, peek());
    }

    private Stmt varDeclaration() throws ParseError {
        Token name = consume(IDENTIFIER, "Expect variable name. (Not " + peek().type + ")");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration. (Not " + peek().type + ")");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() throws ParseError {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression. (Not " + peek().type + ")");
        return new Stmt.Expression(expr);
    }

    private Stmt delStatement() throws ParseError {
        Token name = consume(IDENTIFIER, "Cannot delete non-identifier value. ("+peek().type+")");
        consume(SEMICOLON, "Expect ';' after expression. (Not " + peek().type + ")");
        return new Stmt.Delete(name);
    }

    private List<Stmt> block() throws ParseError {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block. (Not " + peek().type + ")");
        return statements;
    }

    private Stmt ifStatement() throws ParseError {
        consume(LEFT_PAREN, "Expect '(' after 'if'. (Not " + peek().type + ")");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition expression. (Not " + peek().type + ")");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) elseBranch = statement();
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() throws ParseError {
        consume(LEFT_PAREN, "Expect '(' after 'while' loop. (Not " + peek().type + ")");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition expression. (Not " + peek().type + ")");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() throws ParseError {
        consume(LEFT_PAREN, "Expect '(' after 'for' loop. (Not " + peek().type + ")");
        
        Stmt i;
        if (match(SEMICOLON)) {
            i = null;
        } else if (match(VAR)) {
            i = varDeclaration();
        } else {
            i = expressionStatement();
        }

        Expr cond = null;
        if (!check(SEMICOLON)) {
            cond = expression();
        }
        consume(SEMICOLON, "Expect ';' after for loop condition. (Not " + peek().type + ")");

        Expr inc = null;
        if (!check(RIGHT_PAREN)) {
            inc = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after 'for' loop. (Not " + peek().type + ")");

        Stmt body = statement();

        if (inc != null) {
            body = new Stmt.Block(Arrays.asList(
                body,
                new Stmt.Expression(inc) // makes a ExprStmt from Expression
            )); // creates a block and appends the increment stmt to it
        }

        if (cond == null) cond = new Expr.Literal(true);
        body = new Stmt.While(cond, body);

        if (i != null) {
            body = new Stmt.Block(Arrays.asList(
                i, // appends the declaration to the start of the body
                body
            ));
        }

        return body;
    }

    // expr helper

    private Expr finishCall(Expr callee) throws ParseError {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) error(peek(), "Cannot have more than 255 arguments.");
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments. (Not " + peek().type + ")");
        return new Expr.Call(callee, paren, arguments);
    }

    // parser

    private List<Stmt> program() throws ParseError {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }    

    // stmt
    private Stmt declaration() throws ParseError {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }    
    }    



    private Stmt statement() throws ParseError {
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(PRINT)) return printStatement();
        if (match(QUIT)) return quitStatement();
        if (match(DEL)) return delStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    // expr handling
    
    private Expr expression() throws ParseError {
        return assignment();
    }

    private Expr assignment() throws ParseError {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }  
        
        return expr;
    }
    
    private Expr or() throws ParseError {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() throws ParseError {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() throws ParseError {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() throws ParseError {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() throws ParseError {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() throws ParseError {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() throws ParseError {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() throws ParseError {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr primary() throws ParseError {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression. (Not " + peek().type + ")");
            return new Expr.Grouping(expr);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        throw error(peek(), "Expected an expression.");
    }
}
