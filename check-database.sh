#!/bin/bash

# Script para verificar a estrutura do banco de dados
# Utiliza as configurações de conexão fornecidas

# Variáveis de conexão
DB_HOST="aws-0-us-east-1.pooler.supabase.com"
DB_PORT="6543"
DB_NAME="postgres"
DB_USER="postgres.icctubswerltuucbpzhp"
DB_PASSWORD="Gratidao@2025"

echo "Verificando estrutura do banco de dados WAD Calendar Service..."

# Verificar se o psql está instalado
if ! command -v psql &> /dev/null; then
    echo "Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro."
    exit 1
fi

# Executar o script de validação
echo "Executando verificação..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/validate.sql

# Verificar o status do último comando
if [ $? -ne 0 ]; then
    echo "Erro ao verificar a estrutura do banco de dados."
    exit 1
fi

echo "Verificação da estrutura do banco de dados concluída!"
