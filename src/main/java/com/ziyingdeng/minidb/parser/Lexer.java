package com.ziyingdeng.minidb.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Lexer {

    public enum Keyword {
        Create, Table, 
        Insert, Into, 
        Select, From, 
        Where, Null, Default,
        And, Or, Not, 
        True, False, Boolean, Bool, 
        Float, Double, Integer, Int, String, Text, Varchar,
        Values, Primary, Key;

        private static final Map<String, Keyword> LOOKUP;
        static {
            Map<String, Keyword> m = new HashMap<>();
            m.put("CREATE", Create);    m.put("TABLE", Table);
            m.put("INSERT", Insert);    m.put("INTO", Into);
            m.put("SELECT", Select);    m.put("FROM", From);
            m.put("WHERE", Where);      m.put("AND", And);
            m.put("OR", Or);            m.put("NOT", Not);
            m.put("NULL", Null);        m.put("DEFAULT", Default);
            m.put("TRUE", True);        m.put("FALSE", False);
            m.put("BOOLEAN", Boolean);  m.put("BOOL", Bool);
            m.put("FLOAT", Float);      m.put("DOUBLE", Double);
            m.put("INTEGER", Integer);  m.put("INT", Int);
            m.put("STRING", String);    m.put("TEXT", Text);
            m.put("VARCHAR", Varchar);  m.put("VALUES", Values);
            m.put("PRIMARY", Primary);  m.put("KEY", Key);
            LOOKUP = Collections.unmodifiableMap(m);
        }

        // Convert String to Keyword
        public static Keyword fromString(String str) {
            return LOOKUP.get(str.toUpperCase());
        }
    }

    // Five kinds of tokens:
    // 1. Keyword: Select, table, create...
    // 2. Identity: column name, row name...
    // 3. String: string literal
    // 4. Number: integer, double, boolean...
    // 5. Symbol: ";", "*" ...
    public enum TokenKind {
        KEYWORD, 
        IDENTITY, 
        STRING, 
        NUMBER, 
        OPEN_PAREN, CLOSE_PAREN, COMMA, SEMICOLON,
        ASTERISK, PLUS, MINUS, SLASH
    }

    public static class Token {
        private TokenKind kind;
        private String text;        // IDENT/STRING/NUMBER/symbol
        private Keyword keyword;    // KEYWORD

        private Token(TokenKind tk, String t, Keyword kw) { this.kind = tk; this.text = t; this.keyword = kw; }

        public static Token keyword(Keyword kw) { return new Token(TokenKind.KEYWORD, kw.name(), kw); }
        public static Token identity(String s) { return new Token(TokenKind.IDENTITY, s, null); }
        public static Token string(String s) { return new Token(TokenKind.STRING, s, null); }
        public static Token number(String s) { return new Token(TokenKind.NUMBER, s, null); }
        public static Token symbol(TokenKind k, String literal) { return new Token(k, literal, null); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (! (o instanceof Token t)) return false;
            return kind == t.kind 
                    && Objects.equals(text, t.text) 
                    && keyword == t.keyword;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, text, keyword);
        }
        
        @Override 
        public String toString() {
            return kind == TokenKind.KEYWORD ? "KEYWORD(" + keyword + ")" :
            kind + "(" + text + ")";
        }
    }

    // Lexer code
    private char[] s; // consume sql text char by char
    private int i;  // record position
    private int n; // length of string
    private boolean finished; // if we reach the end of input
    private Token cachedToken; // saved next token for peek()

    public Lexer(String sqlText) {
        this.s = sqlText.toCharArray();
        this.i = 0;
        this.n = sqlText.length();
    }

    // Scan and check if there is the next token
    // -- if true, scan one token and store in cache, advance pointer
    // -- if false, set finisehd = true
    public boolean hasNext() {
        if (finished) return false;
        if (cachedToken != null) return true;

        cachedToken = scanToken();
        if (cachedToken == null) {
            finished = true;
            return false;
        }
        return true;
    }

    // Consume and return the token in cached
    public Token nextToken() {
        if (!hasNext()) throw new NoSuchElementException("No more tokens");
        Token t = cachedToken;
        cachedToken = null; // clean
        return t;
    }

    // Peek the next token but doesn't consume
    public Token peek() {
        return hasNext()? cachedToken : null;
    }
    
    // For test: collect all the tokens
    List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (hasNext()) {
            out.add(nextToken());
        }
        return out;
    }

    // Scan and return one token from the char array
    private Token scanToken() {
        // skip all white space before each token
        skipWhiteSpace();
        if (eof()) return null;

        char c = s[i];
        if (c == '\'') return scanString();
        if (isDigit(c)) return scanNumber();
        if (isLetter(c)) return scanIdentityKeyword();
        if (isSymbol(c)) return scanSymbol(c);
        throw new ParseException("[Lexer] Unexpected character: '" + c + "'");
    }

    // String format: '...'
    private Token scanString() {
        i++; // skip the beginning '\''
        int start = i;
        while (!eof()) { 
            char c = s[i++];
            if (c == '\'') {
                return Token.string(new String(s, start, (i - 1) - start));  // skip the ending '\''
            }
        }
        throw new ParseException("[Lexer] Unexpected end of string");
    }

    // Number format: 1234 and 12.34
    private Token scanNumber() {
        int start = i;
        while (!eof() && isDigit(s[i])) { i++; }
        if (!eof() && s[i] == '.') { // handle float
            i++;
            while (!eof() && isDigit(s[i])) { i++; }
        }
        return Token.number(new String(s, start, i - start));
    }

    // Identity format: must start with letter, followed by letter/number/'_'
    // Keyword format: LOOK UP table
    private Token scanIdentityKeyword() {
        int start = i;
        i++; // first letter checked
        while (!eof()) {
            char c = s[i];
            if (isLetter(c) || isDigit(c) || c == '_') { i++; }
            else break;
        }
        String t = new String(s, start, i - start);
        Keyword kw = Keyword.fromString(t.toUpperCase());
        return (kw == null) ?  Token.identity(t) : Token.keyword(kw);
        
    }
    
    // Suppported symbols: ( ) , ; * + - / 
    private Token scanSymbol(char c) {
        i++; // consume
        return switch (c) {
            case '(' -> Token.symbol(TokenKind.OPEN_PAREN, "(");
            case ')' -> Token.symbol(TokenKind.CLOSE_PAREN, ")");
            case ',' -> Token.symbol(TokenKind.COMMA, ",");
            case ';' -> Token.symbol(TokenKind.SEMICOLON, ";");
            case '*' -> Token.symbol(TokenKind.ASTERISK, "*");
            case '+' -> Token.symbol(TokenKind.PLUS, "+");
            case '-' -> Token.symbol(TokenKind.MINUS, "-");
            case '/' -> Token.symbol(TokenKind.SLASH, "/");
            default  -> throw new ParseException("[Lexer] Unknown symbol: '" + c + "'");
        };
    }

    private void skipWhiteSpace() { while (!eof() && isWhiteSpace(s[i])) { i++; }}
    private boolean eof() { return i >= n; }

    private boolean isWhiteSpace(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    private boolean isSymbol(char c) {
        return c == '(' || c == ')' || c == ',' || c == ';' || c == '*' || c == '+' || c == '-' || c == '/';
    }

}
