@echo off
REM Script para restaurar um backup do banco de dados (Windows)
REM Utiliza as configurações de conexão fornecidas

REM Variáveis de conexão
set DB_HOST=aws-0-us-east-1.pooler.supabase.com
set DB_PORT=6543
set DB_NAME=postgres
set DB_USER=postgres.icctubswerltuucbpzhp
set DB_PASSWORD=Gratidao@2025

REM Verificar se foi fornecido o arquivo de backup
if "%~1"=="" (
    echo Erro: Nenhum arquivo de backup especificado.
    echo Uso: %0 ^<arquivo_de_backup^>
    exit /b 1
)

set BACKUP_FILE=%~1

REM Verificar se o arquivo existe
if not exist "%BACKUP_FILE%" (
    echo Erro: Arquivo de backup não encontrado: %BACKUP_FILE%
    exit /b 1
)

echo Iniciando restauração do banco de dados WAD Calendar Service...

REM Verificar se o psql está instalado
where psql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro.
    exit /b 1
)

REM Limpar tabela antes de restaurar (opcional)
echo Limpando dados existentes...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "TRUNCATE TABLE appointments CASCADE;"

if %ERRORLEVEL% neq 0 (
    echo Aviso: Não foi possível limpar a tabela existente.
) else (
    echo Tabela limpa com sucesso.
)

REM Restaurar o backup
echo Restaurando dados do backup...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%BACKUP_FILE%"

REM Verificar o status do último comando
if %ERRORLEVEL% neq 0 (
    echo Erro ao restaurar o backup do banco de dados.
    exit /b 1
)

echo Restauração do banco de dados concluída com sucesso!
