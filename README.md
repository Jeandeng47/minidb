
## Scope of supported SQL statements
### 1. Overview
```css
program        := ( statement ";" )* EOF ;

statement      := create_table
                | insert_stmt
                | select_stmt ;
```
### 2. Create
```css
create_table := "CREATE" "TABLE" table_name 
                "(" column_def ( "," column_def )* ")" ;

column_def     := ident data_type ( column_constraint )* ;

data_type      := "BOOLEAN" | "BOOL"
                | "FLOAT"   | "DOUBLE"
                | "INTEGER" | "INT"
                | "STRING"  | "TEXT" | "VARCHAR" [ "(" INT_LIT ")" ] ;

column_constraint
              := "NOT" "NULL"
               | "NULL"
               | "DEFAULT" expr ;

table_name     := identity ;
identity       := IDENTITY ;

```
### 3. Insert
```css
insert_stmt    := "INSERT" "INTO" table_name
                  [ "(" identity ( "," identity )* ")" ]
                  "VALUES" "(" expr ( "," expr )* ")" ;
```

### 4. Select
```css
select_stmt    := "SELECT" "*" "FROM" table_name ;
```

### 5. Expression
```css
expr           := term ( ("+" | "-") term )* ;
term           := factor ( ("*" | "/") factor )* ;
factor         := literal
                | ident
                | "(" expr ")"
                | ( "NOT" | "+" | "-" ) factor ;

literal        := INT_LIT | FLOAT_LIT | STR_LIT | "TRUE" | "FALSE" | "NULL" ;
```