-- Script para verificação da estrutura do banco de dados
-- Use este script para validar se as tabelas e índices foram criados corretamente

-- Listar tabelas
SELECT 
    table_schema,
    table_name
FROM 
    information_schema.tables
WHERE 
    table_schema = 'public'
ORDER BY 
    table_schema, table_name;

-- Listar colunas da tabela appointments
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    column_default,
    is_nullable
FROM 
    information_schema.columns
WHERE 
    table_schema = 'public'
    AND table_name = 'appointments'
ORDER BY 
    ordinal_position;

-- Listar índices
SELECT
    tablename,
    indexname,
    indexdef
FROM
    pg_indexes
WHERE
    schemaname = 'public'
ORDER BY
    tablename, indexname;

-- Verificar restrições
SELECT 
    con.conname as constraint_name,
    con.contype as constraint_type,
    rel.relname as table_name,
    pg_get_constraintdef(con.oid) as constraint_definition
FROM 
    pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
WHERE 
    nsp.nspname = 'public'
ORDER BY 
    rel.relname, con.contype;
