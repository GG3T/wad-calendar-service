#!/bin/bash

# Script para restaurar um backup do banco de dados
# Utiliza as configurações de conexão fornecidas

# Variáveis de conexão
DB_HOST="aws-0-us-east-1.pooler.supabase.com"
DB_PORT="6543"
DB_NAME="postgres"
DB_USER="postgres.icctubswerltuucbpzhp"
DB_PASSWORD="Gratidao@2025"

# Verificar se foi fornecido o arquivo de backup
if [ -z "$1" ]; then
    echo "Erro: Nenhum arquivo de backup especificado."
    echo "Uso: $0 <arquivo_de_backup>"
    exit 1
fi

BACKUP_FILE="$1"

# Verificar se o arquivo existe
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Erro: Arquivo de backup não encontrado: $BACKUP_FILE"
    exit 1
fi

echo "Iniciando restauração do banco de dados WAD Calendar Service..."

# Verificar se o psql está instalado
if ! command -v psql &> /dev/null; then
    echo "Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro."
    exit 1
fi

# Descompactar se for um arquivo .gz
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo "Descompactando arquivo de backup..."
    TEMP_FILE="${BACKUP_FILE%.gz}.temp"
    gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
    BACKUP_FILE="$TEMP_FILE"
    
    if [ $? -ne 0 ]; then
        echo "Erro ao descompactar o arquivo de backup."
        exit 1
    fi
    
    echo "Arquivo descompactado com sucesso."
fi

# Limpar tabela antes de restaurar (opcional)
echo "Limpando dados existentes..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "TRUNCATE TABLE appointments CASCADE;"

if [ $? -ne 0 ]; then
    echo "Aviso: Não foi possível limpar a tabela existente."
else
    echo "Tabela limpa com sucesso."
fi

# Restaurar o backup
echo "Restaurando dados do backup..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$BACKUP_FILE"

# Verificar o status do último comando
if [ $? -ne 0 ]; then
    echo "Erro ao restaurar o backup do banco de dados."
    # Limpar arquivo temporário se existir
    if [[ "$BACKUP_FILE" == *.temp ]]; then
        rm "$BACKUP_FILE"
    fi
    exit 1
fi

# Limpar arquivo temporário se existir
if [[ "$BACKUP_FILE" == *.temp ]]; then
    rm "$BACKUP_FILE"
fi

echo "Restauração do banco de dados concluída com sucesso!"
