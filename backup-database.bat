@echo off
REM Script para backup do banco de dados (Windows)
REM Utiliza as configurações de conexão fornecidas

REM Variáveis de conexão
set DB_HOST=aws-0-us-east-1.pooler.supabase.com
set DB_PORT=6543
set DB_NAME=postgres
set DB_USER=postgres.icctubswerltuucbpzhp
set DB_PASSWORD=Gratidao@2025

REM Diretório para armazenar backups
set BACKUP_DIR=.\db-backups
set TIMESTAMP=%date:~6,4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_FILE=%BACKUP_DIR%\wad_calendar_backup_%TIMESTAMP%.sql

echo Iniciando backup do banco de dados WAD Calendar Service...

REM Verificar se o diretório de backup existe, se não existir, criar
if not exist %BACKUP_DIR% (
    mkdir %BACKUP_DIR%
    echo Diretório de backup criado: %BACKUP_DIR%
)

REM Verificar se o pg_dump está instalado
where pg_dump >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Erro: PostgreSQL client (pg_dump) não encontrado. Por favor, instale-o primeiro.
    exit /b 1
)

REM Realizar o backup
echo Gerando backup...
set PGPASSWORD=%DB_PASSWORD%
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -t appointments > "%BACKUP_FILE%"

REM Verificar o status do último comando
if %ERRORLEVEL% neq 0 (
    echo Erro ao gerar o backup do banco de dados.
    exit /b 1
)

echo Backup do banco de dados concluído com sucesso!
echo Arquivo de backup: %BACKUP_FILE%

echo Processo de backup finalizado.
