package com.ziyingdeng.minidb.parser;

import java.util.List;

public class AST {

    /* ------------ Statements ------------ */
    public interface Statement {}

    // SELECT * FROM tableName
    public record Select(String tableName) implements Statement {}

    // INSERT INTO tableName [(columns)] VALUES (rows...)
    public record Insert(
        String tableName,
        List<String> columns,
        List<List<Expression>> values
    ) implements Statement {}
    
    // CREATE TABLE name (columns...)
    public record CreateTable(
        String name,
        List<Column> columns
    ) implements Statement {}

    /* -------------- Column -------------- */
    public enum DataType { BOOLEAN, INTEGER, FLOAT, STRING }
    public record Column (
        String name,
        DataType DataType,
        Boolean nullable, // null == unspecified; T/F = explictly specified
        Expression defaulExpr // null == no default
    ) {}

    /* ----------- Expressions ------------ */

    // Only support const for now
    public interface Expression {}

    public static class Const implements Expression {
        public enum Kind { NULL, BOOLEAN, INTEGER, FLOAT, STRING }
        public final Kind kind;
        public final Object value; // null for NULL; Boolean/Long/Double/String for others

        private Const(Kind kind, Object value) { this.kind = kind; this.value = value; }

        // Factory helpers
        public static Const ofNull()                 { return new Const(Kind.NULL, null); }
        public static Const ofBoolean(boolean v)     { return new Const(Kind.BOOLEAN, v); }
        public static Const ofInteger(long v)        { return new Const(Kind.INTEGER, v); }
        public static Const ofFloat(double v)        { return new Const(Kind.FLOAT, v); }
        public static Const ofString(String v)       { return new Const(Kind.STRING, v); }
    }

}
