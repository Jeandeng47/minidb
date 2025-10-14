package com.ziyingdeng.minidb.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
        public static Keyword from(String str) {
            return LOOKUP.get(str.toUpperCase());
        }
    }

    // Five kinds of tokens:
    // 1. Keyword: Select, table, create...
    // 2. Identity: column name, row name...
    // 3. String: tsring literal
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

        private Token(TokenKind tk, String t, Keyword kw) {
            this.kind = tk; this.text = t; this.keyword = kw;
        }

        public static Token keyword(Keyword kw) {
            return new Token(TokenKind.KEYWORD, kw.name(), kw);
        }
        public static Token identity(String s) {
            return new Token(TokenKind.IDENTITY, s, null);
        }
        public static Token string(String s) {
            return new Token(TokenKind.STRING, s, null);
        }
        public static Token number(String s) {
            return new Token(TokenKind.NUMBER, s, null);
        }
        public static Token symbol(TokenKind k, String literal) {
            return new Token(k, literal, null);
        }

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

}
