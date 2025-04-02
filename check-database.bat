@echo off
REM Script para verificar a estrutura do banco de dados (Windows)
REM Utiliza as configurações de conexão fornecidas

REM Variáveis de conexão
set DB_HOST=aws-0-us-east-1.pooler.supabase.com
set DB_PORT=6543
set DB_NAME=postgres
set DB_USER=postgres.icctubswerltuucbpzhp
set DB_PASSWORD=Gratidao@2025

echo Verificando estrutura do banco de dados WAD Calendar Service...

REM Verificar se o psql está instalado
where psql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro.
    exit /b 1
)

REM Executar o script de validação
echo Executando verificação...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f src/main/resources/db/validate.sql

REM Verificar o status do último comando
if %ERRORLEVEL% neq 0 (
    echo Erro ao verificar a estrutura do banco de dados.
    exit /b 1
)

echo Verificação da estrutura do banco de dados concluída!
