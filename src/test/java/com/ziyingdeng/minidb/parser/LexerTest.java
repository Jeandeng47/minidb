package com.ziyingdeng.minidb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.ziyingdeng.minidb.parser.Lexer.Keyword;
import com.ziyingdeng.minidb.parser.Lexer.Token;
import com.ziyingdeng.minidb.parser.Lexer.TokenKind;

public class LexerTest {

    @Test
    void hasNextReturnsFalseWhenInputEmpty() {
        Lexer lexer = new Lexer("");

        assertFalse(lexer.hasNext());
    }

    @Test
    void hasNextReturnsTrueWhenTokenAvailable() {
        Lexer lexer = new Lexer("SELECT");

        assertTrue(lexer.hasNext());
        assertTrue(lexer.hasNext()); // cached token keeps reporting availability
    }

    @Test
    void hasNextIgnoresLeadingWhitespace() {
        Lexer lexer = new Lexer("   \n\tSELECT");

        assertTrue(lexer.hasNext());
        assertEquals(Token.keyword(Keyword.Select), lexer.nextToken());
    }

    @Test
    void nextTokenReturnsTokensInOrder() {
        Lexer lexer = new Lexer("SELECT name FROM users");

        assertEquals(Token.keyword(Keyword.Select), lexer.nextToken());
        assertEquals(Token.identity("name"), lexer.nextToken());
        assertEquals(Token.keyword(Keyword.From), lexer.nextToken());
        assertEquals(Token.identity("users"), lexer.nextToken());
        assertFalse(lexer.hasNext());
    }

    @Test
    void nextTokenThrowsWhenNoTokens() {
        Lexer lexer = new Lexer("");

        assertThrows(NoSuchElementException.class, lexer::nextToken);
    }

    @Test
    void nextTokenParsesSymbolsAndNumbers() {
        Lexer lexer = new Lexer("(42.0,-7);");

        assertEquals(Token.symbol(TokenKind.OPEN_PAREN, "("), lexer.nextToken());
        assertEquals(Token.number("42.0"), lexer.nextToken());
        assertEquals(Token.symbol(TokenKind.COMMA, ","), lexer.nextToken());
        assertEquals(Token.symbol(TokenKind.MINUS, "-"), lexer.nextToken());
        assertEquals(Token.number("7"), lexer.nextToken());
        assertEquals(Token.symbol(TokenKind.CLOSE_PAREN, ")"), lexer.nextToken());
        assertEquals(Token.symbol(TokenKind.SEMICOLON, ";"), lexer.nextToken());
        assertFalse(lexer.hasNext());
    }

    @Test
    void peekReturnsSameTokenWithoutConsuming() {
        Lexer lexer = new Lexer("SELECT * FROM users");

        Token peek = lexer.peek();

        assertEquals(Token.keyword(Keyword.Select), peek);
        assertEquals(peek, lexer.peek());
        assertEquals(peek, lexer.nextToken());
    }

    @Test
    void peekReturnsNullWhenStreamConsumed() {
        Lexer lexer = new Lexer("");

        assertNull(lexer.peek());
    }

    @Test
    void tokenizeReturnsFullTokenList() {
        Lexer lexer = new Lexer("INSERT INTO books VALUES ('Alice', 42, 3.14);");

        assertEquals(
                List.of(
                        Token.keyword(Keyword.Insert),
                        Token.keyword(Keyword.Into),
                        Token.identity("books"),
                        Token.keyword(Keyword.Values),
                        Token.symbol(TokenKind.OPEN_PAREN, "("),
                        Token.string("Alice"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.number("42"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.number("3.14"),
                        Token.symbol(TokenKind.CLOSE_PAREN, ")"),
                        Token.symbol(TokenKind.SEMICOLON, ";")),
                lexer.tokenize());
    }

    @Test
    void tokenizeRecognizesBooleanKeywords() {
        Lexer lexer = new Lexer("TRUE FALSE BOOL BOOLEAN");

        assertEquals(
                List.of(
                        Token.keyword(Keyword.True),
                        Token.keyword(Keyword.False),
                        Token.keyword(Keyword.Bool),
                        Token.keyword(Keyword.Boolean)),
                lexer.tokenize());
    }

    @Test
    void tokenizeRecognizesIdentifiersWithUnderscores() {
        Lexer lexer = new Lexer("column_1 another_column");

        assertEquals(
                List.of(
                        Token.identity("column_1"),
                        Token.identity("another_column")),
                lexer.tokenize());
    }

    @Test
    void tokenizeHandlesMixedCaseKeywords() {
        Lexer lexer = new Lexer("cReAtE tAbLe people");

        assertEquals(
                List.of(
                        Token.keyword(Keyword.Create),
                        Token.keyword(Keyword.Table),
                        Token.identity("people")),
                lexer.tokenize());
    }

    @Test
    void tokenizeThrowsOnUnterminatedString() {
        Lexer lexer = new Lexer("SELECT 'abc");

        assertThrows(ParseException.class, lexer::tokenize);
    }

    @Test
    void tokenizeThrowsOnUnknownSymbol() {
        Lexer lexer = new Lexer("SELECT @");

        assertThrows(ParseException.class, lexer::tokenize);
    }

    @Test
    void integratedLexingProcessesStatementWithLookahead() {
        Lexer lexer = new Lexer(" \nSELECT name, age FROM users WHERE TRUE OR FALSE;");
        List<Token> collected = new ArrayList<>();

        while (lexer.hasNext()) {
            Token lookahead = lexer.peek();
            Token next = lexer.nextToken();
            assertEquals(lookahead, next);
            collected.add(next);
        }

        assertEquals(
                List.of(
                        Token.keyword(Keyword.Select),
                        Token.identity("name"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.identity("age"),
                        Token.keyword(Keyword.From),
                        Token.identity("users"),
                        Token.keyword(Keyword.Where),
                        Token.keyword(Keyword.True),
                        Token.keyword(Keyword.Or),
                        Token.keyword(Keyword.False),
                        Token.symbol(TokenKind.SEMICOLON, ";")),
                collected);
    }

    @Test
    void integratedLexingHandlesCreateTableStatement() {
        Lexer lexer = new Lexer("""
                CREATE TABLE tbl (
                    id1 INT PRIMARY KEY,
                    id2 INTEGER DEFAULT 100,
                    created_at DOUBLE
                );
                """);

        List<Token> tokens = lexer.tokenize();

        assertEquals(
                List.of(
                        Token.keyword(Keyword.Create),
                        Token.keyword(Keyword.Table),
                        Token.identity("tbl"),
                        Token.symbol(TokenKind.OPEN_PAREN, "("),
                        Token.identity("id1"),
                        Token.keyword(Keyword.Int),
                        Token.keyword(Keyword.Primary),
                        Token.keyword(Keyword.Key),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.identity("id2"),
                        Token.keyword(Keyword.Integer),
                        Token.keyword(Keyword.Default),
                        Token.number("100"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.identity("created_at"),
                        Token.keyword(Keyword.Double),
                        Token.symbol(TokenKind.CLOSE_PAREN, ")"),
                        Token.symbol(TokenKind.SEMICOLON, ";")),
                tokens);
    }

    @Test
    void integratedLexingHandlesInsertStatement() {
        Lexer lexer = new Lexer("INSERT INTO tbl VALUES (1, 'name', TRUE, 4.55);");

        assertEquals(
                List.of(
                        Token.keyword(Keyword.Insert),
                        Token.keyword(Keyword.Into),
                        Token.identity("tbl"),
                        Token.keyword(Keyword.Values),
                        Token.symbol(TokenKind.OPEN_PAREN, "("),
                        Token.number("1"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.string("name"),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.keyword(Keyword.True),
                        Token.symbol(TokenKind.COMMA, ","),
                        Token.number("4.55"),
                        Token.symbol(TokenKind.CLOSE_PAREN, ")"),
                        Token.symbol(TokenKind.SEMICOLON, ";")),
                lexer.tokenize());
    }
}
