package com.ziyingdeng.minidb.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.ziyingdeng.minidb.parser.Lexer.Keyword;
import com.ziyingdeng.minidb.parser.Lexer.Token;
import com.ziyingdeng.minidb.parser.Lexer.TokenKind;

public class Parser {
    private Lexer lexer;
    private Token look; 

    public Parser(String input) {
        this.lexer = new Lexer(input);
        this.look = lexer.hasNext()? lexer.nextToken() : null; // take the first token
    }

    /* ================================ entry ================================ */

    // program := statement ";" EOF
    public AST.Statement parse() {
        AST.Statement stmt = parseStatement();
        nextExpect(Token.symbol(TokenKind.SEMICOLON, ";"));
        if (look != null) {
            throw new ParseException("[Parser] Unexpected token after ';': " + look);
        }
        return stmt;
    }
    
    private AST.Statement parseStatement() {
        Token t = peek();
        if (t.kind == TokenKind.KEYWORD) {
            return switch (t.keyword) {
                case Create -> parseDDL();
                case Select -> parseSelect();
                case Insert -> parseInsert();
                default -> throw new ParseException("[Parser] Unexpected keyword " + t);
            };
        }
        throw new ParseException("[Parser] Unexpected token " + t);
    }

    /* ================================ CREATE ================================= */
    private AST.Statement parseDDL() {
        nextExpect(Token.keyword(Keyword.Create));
        nextExpect(Token.keyword(Keyword.Table));
        return parseDDLCreateTable();
    }

    // CREATE TABLE name "(" column_def ("," column_def)* ")"
    private AST.Statement parseDDLCreateTable() {
        String tableName = nextIdentity();
        nextExpect(Token.symbol(TokenKind.OPEN_PAREN, "("));

        List<AST.Column> columns = new ArrayList<>();
        while (true) {
            columns.add(parseDDLColumns());
            if (nextIfToken(Token.symbol(TokenKind.COMMA, ",")) == null) break;
        }
        nextExpect(Token.symbol(TokenKind.CLOSE_PAREN, ")"));
        return new AST.CreateTable(tableName, columns);
    }

    // column_def := ident data_type ( "NOT" "NULL" | "NULL" | "DEFAULT" expr )*
    private AST.Column parseDDLColumns() {
        String name = nextIdentity();
        
        // Convert Token.KEYWORD -> AST.DataType
        Token t = next();
        if (t.kind != TokenKind.KEYWORD) {
            throw new ParseException("[Parser] Unexpected token " + t);
        }
        AST.DataType dataType = switch (t.keyword) {
            case Boolean, Bool -> AST.DataType.BOOLEAN;
            case Integer, Int  -> AST.DataType.INTEGER;
            case Float, Double -> AST.DataType.FLOAT;
            case String, Text, Varchar -> AST.DataType.STRING;
            default -> throw new ParseException("[Parser] Unexpected keyword " + t.keyword);
        };

        // If unspecified in sql text, set to null initially
        Boolean nullable = null;  
        AST.Expression defaultExpr = null;

        while (true) {
            // continue to retrieve keyword until no keyword
            Token k = nextIfKeyword();
            if (k == null) break;

            switch (k.keyword) {
                // Nullable is set when "NOT NULL"/"NULL" provided, otherwise remains null
                case Null -> nullable = Boolean.TRUE;
                case Not -> { 
                    nextExpect(Token.keyword(Keyword.Null)); 
                    nullable = Boolean.FALSE; 
                }
                case Default -> defaultExpr = parseExpression();
                default -> throw new ParseException("[Parser] Unexpected keyword " + k.keyword);
            }
        }
        return new AST.Column(name, dataType, nullable, defaultExpr);
    }

    // Now only support：NUMBER / STRING / TRUE / FALSE / NULL
    private AST.Expression parseExpression() {
        Token t = next();
        // convert Token.KEYWORD/Token.STRING/Token.NUMBER -> AST.Expression
        return switch (t.kind) {
            case STRING -> AST.Const.ofString(t.text);
            case NUMBER -> {
                String raw = t.text;
                boolean isFloat = raw.indexOf('.') >= 0;
                try {
                    if (isFloat) {
                        double v = Double.parseDouble(raw);
                        yield AST.Const.ofFloat(v);
                    } else {
                        int v = Integer.parseInt(raw); 
                        yield AST.Const.ofInteger(v);
                    }

                } catch (NumberFormatException e) {
                    throw new ParseException("[Parser] Invalid numeric literal: " + raw, e);
                } 
            }
            case KEYWORD -> switch (t.keyword) {
                case True -> AST.Const.ofBoolean(true);
                case False -> AST.Const.ofBoolean(false);
                case Null -> AST.Const.ofNull();
                default -> throw new ParseException("[Parser] Unexpected keyword" + t.keyword);
            };
            default -> throw new ParseException("[Parser] Unexpected token" + t);
        };
    }

    /* ================================= SELECT ============================= */

    // Now only support: SELECT "*" FROM ident
    private AST.Statement parseSelect() {
        nextExpect(Token.keyword(Keyword.Select));
        nextExpect(Token.symbol(TokenKind.ASTERISK, "*"));
        nextExpect(Token.keyword(Keyword.From));
        String table = nextIdentity();
        return new AST.Select(table);
    }

    /* ================================= INSERT ============================= */

    // INSERT INTO table [(col,...)] VALUES (expr,...) [, (expr,...)]*
    private AST.Statement parseInsert() {
        nextExpect(Token.keyword(Keyword.Insert));
        nextExpect(Token.keyword(Keyword.Into));
        
        String table = nextIdentity();

        // Parse list of column names
        List<String> colNames = null;
        if (nextIfToken(Token.symbol(TokenKind.OPEN_PAREN, "(")) != null) {
            colNames = new ArrayList<>();
            while (true) {
                // continue to retrieve pair of [ col_name, "," ], until ")" is met
                colNames.add(nextIdentity());
                Token sep = next();
                if (sep.equals(Token.symbol(TokenKind.CLOSE_PAREN, ")"))) break; 
                if (sep.equals(Token.symbol(TokenKind.COMMA, ","))) continue;
                throw new ParseException("[Parser] Unexpected token " + sep);
            }
        }

        // Parse list of values 
        nextExpect(Token.keyword(Keyword.Values));

        // Values could be a single expression or contain nested tuples
        List<List<AST.Expression>> rows = new ArrayList<>();
        while (true) {
            nextExpect(Token.symbol(TokenKind.OPEN_PAREN, "("));
            List<AST.Expression> exprs = new ArrayList<>();
            while (true) {
                if (peek().equals(Token.symbol(TokenKind.OPEN_PAREN, "("))) {
                    exprs.add(parseParenthesizedConst());
                } else {
                    exprs.add(parseExpression());
                }
                Token sep = next();
                if (sep.equals(Token.symbol(TokenKind.CLOSE_PAREN, ")"))) break;
                if (sep.equals(Token.symbol(TokenKind.COMMA, ","))) continue;
                throw new ParseException("[Parser] Unexpected token " + sep);
            }
            rows.add(exprs);
            if (nextIfToken(Token.symbol(TokenKind.COMMA, ",")) == null) break;
        }
        return new AST.Insert(table, colNames, rows);
    }

    private AST.Const parseParenthesizedConst() {
        nextExpect(Token.symbol(TokenKind.OPEN_PAREN, "("));
        List<String> parts = new ArrayList<>();
        while (true) {
            AST.Expression expr = parseExpression();
            parts.add(expressionToString(expr));
            Token sep = next();
            if (sep.equals(Token.symbol(TokenKind.CLOSE_PAREN, ")"))) break;
            if (sep.equals(Token.symbol(TokenKind.COMMA, ","))) continue;
            throw new ParseException("[Parser] Unexpected token " + sep);
        }
        return AST.Const.ofString("(" + String.join(", ", parts) + ")");
    }

    private String expressionToString(AST.Expression expr) {
        if (expr instanceof AST.Const c) {
            return switch (c.kind) {
                case STRING -> (String) c.value;
                case INTEGER -> Integer.toString((Integer) c.value);
                case FLOAT -> Double.toString((Double) c.value);
                case BOOLEAN -> Boolean.toString((Boolean) c.value);
                case NULL -> "NULL";
            };
        }
        throw new ParseException("[Parser] Unsupported nested expression: " + expr);
    }


    /* ================================ helpers ============================== */

    // Returns the current lookahead token without consuming it
    private Token peek() {
        if (look == null) throw new ParseException("[Parser] Unexpected End of input");
        return look;
    }

    // Returns the current token and advances look to the next token 
    private Token next() {
        if (look == null) throw new ParseException("[Parser] Unexpected End of input");
        Token cur = look;
        look = lexer.hasNext() ? lexer.nextToken() : null;
        return cur;
    }

    // Check if the next token is the expected token
    private void nextExpect(Token expected) {
        Token t = next();
        if (!t.equals(expected)) {
            throw new ParseException("[Parser] Expected token " + expected + ", got " + t);
        }
    }

    // Parse and return identity
    private String nextIdentity() {
        Token t = next();
        if (t.kind == TokenKind.IDENTITY) return t.text;
        throw new ParseException("[Parser] Expected ident, got token " + t); 
    }

    // Return the next token if it satisfies the predicate
    private Token nextIf(Predicate<Token> predicate) {
        if (look != null && predicate.test(look)) {
            return next();
        }
        return null;
    }

    private Token nextIfKeyword() {
        return nextIf(t -> t.kind == TokenKind.KEYWORD);
    }

    private Token nextIfToken(Token token) {
        return nextIf(token::equals);
    }  
}
