#!/bin/bash

# Script para backup do banco de dados
# Utiliza as configurações de conexão fornecidas

# Variáveis de conexão
DB_HOST="aws-0-us-east-1.pooler.supabase.com"
DB_PORT="6543"
DB_NAME="postgres"
DB_USER="postgres.icctubswerltuucbpzhp"
DB_PASSWORD="Gratidao@2025"

# Diretório para armazenar backups
BACKUP_DIR="./db-backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/wad_calendar_backup_${TIMESTAMP}.sql"

echo "Iniciando backup do banco de dados WAD Calendar Service..."

# Verificar se o diretório de backup existe, se não existir, criar
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    echo "Diretório de backup criado: $BACKUP_DIR"
fi

# Verificar se o pg_dump está instalado
if ! command -v pg_dump &> /dev/null; then
    echo "Erro: PostgreSQL client (pg_dump) não encontrado. Por favor, instale-o primeiro."
    exit 1
fi

# Realizar o backup
echo "Gerando backup..."
PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t appointments > "$BACKUP_FILE"

# Verificar o status do último comando
if [ $? -ne 0 ]; then
    echo "Erro ao gerar o backup do banco de dados."
    exit 1
fi

echo "Backup do banco de dados concluído com sucesso!"
echo "Arquivo de backup: $BACKUP_FILE"

# Compactar o arquivo de backup
echo "Compactando arquivo de backup..."
gzip -f "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "Backup compactado com sucesso: ${BACKUP_FILE}.gz"
else
    echo "Aviso: Não foi possível compactar o arquivo de backup."
fi

echo "Processo de backup finalizado."
