package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import lox.Parser.ParseError;

public class Lox
{
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        interpreter.setRepl(false);
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        StringBuilder to_run = new StringBuilder();
        interpreter.setRepl(true);

        System.out.println("jLox v0.0.1 - alpha https://github.com/Viktor95801/my_lox (original https://www.craftinginterpreters.com/ by Robert Nystrom)");
        System.out.println("Type 'help' for help.");

        
        String prompt_append = "";
        for (;;) {
            System.out.print(prompt_append+"> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.equals("help")) {
                System.out.println("Commands:");
                System.out.println("  help - print this help message");
                System.out.println("  quit - you can use the interpreter specific quit keyword to also quit the REPL");
                System.out.println("REPL specific usage:");
                System.out.println("  ... - continue to the next line without running it");
                continue;
            }
            
            if (!(line.endsWith("..."))) {
                if (to_run.length() > 0) {
                    to_run.append(line);
                    run(to_run.toString());
                    to_run.setLength(0);
                } else run(line);
                prompt_append = "";
            } else {
                prompt_append = "--- ";
                to_run.append(line.substring(0, line.length() - "...".length()));
            }


            // Reset error state on each new prompt.
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        
        List<Stmt> programAST = null;
        try {
            programAST = parser.parse();
        } catch (ParseError e) {
            hadError = true;
        }
        // Stop if there was a syntax error.
        if (hadError) return;

        System.out.print(new AstPrinter().print(programAST));
        interpreter.interpret(programAST);
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error"
                + where + ": "
                + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(FailedRuntime error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
