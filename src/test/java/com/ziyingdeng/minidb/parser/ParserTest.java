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
import com.ziyingdeng.minidb.parser.AST.Expression;
import com.ziyingdeng.minidb.parser.AST.Insert;
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

    /* =============================== INSERT =============================== */

    @Test
    void parseInsertReturnsStatement() {
        Insert insert = parseInsert("INSERT INTO tbl VALUES (1, 'foo', TRUE);");

        assertEquals("tbl", insert.tableName());
        assertNull(insert.columns());
        List<Expression> row = insert.values().get(0);
        Const first = (Const) row.get(0);
        Const second = (Const) row.get(1);
        Const third = (Const) row.get(2);
        assertEquals(Const.Kind.INTEGER, first.kind);
        assertEquals(1, first.value);
        assertEquals(Const.Kind.STRING, second.kind);
        assertEquals("foo", second.value);
        assertEquals(Const.Kind.BOOLEAN, third.kind);
        assertEquals(Boolean.TRUE, third.value);
    }

    @Test
    void parseInsertParsesColumnListAndMultipleRows() {
        Insert insert = parseInsert("INSERT INTO tbl (id, name) VALUES (1, 'a'), (2, 'b');");

        assertEquals(List.of("id", "name"), insert.columns());
        assertEquals(2, insert.values().size());
        Const secondRowFirst = (Const) insert.values().get(1).get(0);
        Const secondRowSecond = (Const) insert.values().get(1).get(1);
        assertEquals(Const.Kind.INTEGER, secondRowFirst.kind);
        assertEquals(2, secondRowFirst.value);
        assertEquals(Const.Kind.STRING, secondRowSecond.kind);
        assertEquals("b", secondRowSecond.value);
    }

    @Test
    void parseInsertParsesNestedParenthesesIntoSingleValue() {
        Insert insert = parseInsert("INSERT INTO tbl (phones, name, age) VALUES (('phone1', 'phone2'), 'Alice', 38);");

        assertEquals(List.of("phones", "name", "age"), insert.columns());
        List<Expression> row = insert.values().get(0);
        assertEquals(3, row.size());
        Const phones = (Const) row.get(0);
        assertEquals(Const.Kind.STRING, phones.kind);
        assertEquals("(phone1, phone2)", phones.value);
        Const name = (Const) row.get(1);
        assertEquals("Alice", name.value);
        Const age = (Const) row.get(2);
        assertEquals(38, age.value);
    }

    @Test
    void parseInsertRejectsMissingValuesKeyword() {
        assertInsertFails("INSERT INTO tbl (id) (1);");
    }

    @Test
    void parseInsertRejectsMalformedRow() {
        assertInsertFails("INSERT INTO tbl VALUES (1, 2;");
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

    private static Insert parseInsert(String sql) {
        Parser parser = new Parser(sql);
        return assertInstanceOf(Insert.class, parser.parse());
    }

    private static void assertInsertFails(String sql) {
        Parser parser = new Parser(sql);
        assertThrows(ParseException.class, parser::parse);
    }
}
