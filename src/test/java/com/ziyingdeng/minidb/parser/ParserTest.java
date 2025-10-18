package com.ziyingdeng.minidb.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ziyingdeng.minidb.parser.AST.Column;
import com.ziyingdeng.minidb.parser.AST.Const;
import com.ziyingdeng.minidb.parser.AST.CreateTable;
import com.ziyingdeng.minidb.parser.AST.DataType;
import com.ziyingdeng.minidb.parser.AST.Statement;

public class ParserTest {

    @Test
    void parseReturnsCreateTableStatement() {
        Parser parser = new Parser("CREATE TABLE tbl (id INT);");

        Statement stmt = parser.parse();

        CreateTable create = assertInstanceOf(CreateTable.class, stmt);
        assertEquals("tbl", create.name());
        Column id = create.columns().get(0);
        assertEquals("id", id.name());
        assertEquals(DataType.INTEGER, id.DataType());
        assertNull(id.nullable());
    }

    @Test
    void parseHandlesColumnConstraints() {
        Parser parser = new Parser("""
                CREATE TABLE tbl (
                    id INT NOT NULL,
                    nickname STRING DEFAULT 'anon',
                    active BOOL NULL
                );
                """);

        CreateTable create = assertInstanceOf(CreateTable.class, parser.parse());
        List<Column> columns = create.columns();

        Column id = columns.get(0);
        assertEquals(Boolean.FALSE, id.nullable());

        Column nickname = columns.get(1);
        Const nicknameDefault = (Const) nickname.defaulExpr();
        assertEquals(Const.Kind.STRING, nicknameDefault.kind);
        assertEquals("anon", nicknameDefault.value);

        Column active = columns.get(2);
        assertEquals(Boolean.TRUE, active.nullable());
    }

    @Test
    void parseRejectsMissingSemicolon() {
        Parser parser = new Parser("CREATE TABLE tbl (id INT)");

        assertThrows(ParseException.class, parser::parse);
    }

    @Test
    void parseRejectsTrailingTokens() {
        Parser parser = new Parser("CREATE TABLE tbl (id INT); extra");

        assertThrows(ParseException.class, parser::parse);
    }

    @Test
    void parseRejectsNonCreateStatement() {
        Parser parser = new Parser("SELECT * FROM tbl;");

        assertThrows(ParseException.class, parser::parse);
    }

    @Test
    void parseRejectsMissingTableKeyword() {
        Parser parser = new Parser("CREATE tbl (id INT);");

        assertThrows(ParseException.class, parser::parse);
    }
}
