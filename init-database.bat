@echo off
REM Script para inicializar o banco de dados (Windows)
REM Utiliza as configurações de conexão fornecidas

REM Variáveis de conexão
set DB_HOST=aws-0-us-east-1.pooler.supabase.com
set DB_PORT=6543
set DB_NAME=postgres
set DB_USER=postgres.icctubswerltuucbpzhp
set DB_PASSWORD=Gratidao@2025

echo Inicializando banco de dados para WAD Calendar Service...

REM Verificar se o psql está instalado
where psql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro.
    exit /b 1
)

REM Criar a estrutura do banco de dados
echo Criando estrutura do banco de dados...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f src/main/resources/db/schema.sql

REM Verificar o status do último comando
if %ERRORLEVEL% neq 0 (
    echo Erro ao criar a estrutura do banco de dados.
    exit /b 1
)

echo Estrutura do banco de dados criada com sucesso!

REM Opção para carregar dados de exemplo
set /p choice=Deseja carregar dados de exemplo? (s/n): 
if /i "%choice%"=="s" (
    echo Carregando dados de exemplo...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f src/main/resources/db/data.sql
    
    if %ERRORLEVEL% neq 0 (
        echo Erro ao carregar os dados de exemplo.
        exit /b 1
    )
    
    echo Dados de exemplo carregados com sucesso!
)

echo Inicialização do banco de dados concluída com sucesso!
