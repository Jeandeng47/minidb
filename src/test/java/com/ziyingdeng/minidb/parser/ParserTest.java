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
import com.ziyingdeng.minidb.parser.AST.Select;

public class ParserTest {

    /* ============================ CREATE TABLE ============================ */

    @Test
    void parseCreateTableReturnsStatement() {
        CreateTable create = parseCreateTable("CREATE TABLE tbl (id INT);");

        assertEquals("tbl", create.name());
        Column id = create.columns().get(0);
        assertEquals("id", id.name());
        assertEquals(DataType.INTEGER, id.DataType());
        assertNull(id.nullable());
    }

    @Test
    void parseCreateTableHandlesConstraints() {
        CreateTable create = parseCreateTable("""
                CREATE TABLE tbl (
                    id INT NOT NULL,
                    nickname STRING DEFAULT 'anon',
                    active BOOL NULL
                );
                """);

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
    void parseCreateTableRejectsMissingSemicolon() {
        assertCreateTableFails("CREATE TABLE tbl (id INT)");
    }

    @Test
    void parseCreateTableRejectsTrailingTokens() {
        assertCreateTableFails("CREATE TABLE tbl (id INT); extra");
    }

    @Test
    void parseCreateTableRejectsMissingTableKeyword() {
        assertCreateTableFails("CREATE tbl (id INT);");
    }

    /* =============================== SELECT =============================== */

    @Test
    void parseSelectReturnsStatement() {
        Select select = parseSelect("SELECT * FROM people;");

        assertEquals("people", select.tableName());
    }

    @Test
    void parseSelectRejectsMissingAsterisk() {
        assertSelectFails("SELECT name FROM people;");
    }

    @Test
    void parseSelectRejectsMissingFromKeyword() {
        assertSelectFails("SELECT * people;");
    }

    @Test
    void parseSelectRejectsMissingTableName() {
        assertSelectFails("SELECT * FROM ;");
    }

    /* =============================== Helpers ============================== */

    private static CreateTable parseCreateTable(String sql) {
        Parser parser = new Parser(sql);
        return assertInstanceOf(CreateTable.class, parser.parse());
    }

    private static Select parseSelect(String sql) {
        Parser parser = new Parser(sql);
        return assertInstanceOf(Select.class, parser.parse());
    }

    private static void assertCreateTableFails(String sql) {
        Parser parser = new Parser(sql);
        assertThrows(ParseException.class, parser::parse);
    }

    private static void assertSelectFails(String sql) {
        Parser parser = new Parser(sql);
        assertThrows(ParseException.class, parser::parse);
    }
}
