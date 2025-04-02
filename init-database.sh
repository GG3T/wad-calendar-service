#!/bin/bash

# Script para inicializar o banco de dados
# Utiliza as configurações de conexão fornecidas

# Variáveis de conexão
DB_HOST="aws-0-us-east-1.pooler.supabase.com"
DB_PORT="6543"
DB_NAME="postgres"
DB_USER="postgres.icctubswerltuucbpzhp"
DB_PASSWORD="Gratidao@2025"
DB_URI="postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

echo "Inicializando banco de dados para WAD Calendar Service..."

# Verificar se o psql está instalado
if ! command -v psql &> /dev/null; then
    echo "Erro: PostgreSQL client (psql) não encontrado. Por favor, instale-o primeiro."
    exit 1
fi

# Criar a estrutura do banco de dados
echo "Criando estrutura do banco de dados..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/schema.sql

# Verificar o status do último comando
if [ $? -ne 0 ]; then
    echo "Erro ao criar a estrutura do banco de dados."
    exit 1
fi

echo "Estrutura do banco de dados criada com sucesso!"

# Opção para carregar dados de exemplo
read -p "Deseja carregar dados de exemplo? (s/n): " choice
if [ "$choice" = "s" ] || [ "$choice" = "S" ]; then
    echo "Carregando dados de exemplo..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/data.sql
    
    if [ $? -ne 0 ]; then
        echo "Erro ao carregar os dados de exemplo."
        exit 1
    fi
    
    echo "Dados de exemplo carregados com sucesso!"
fi

echo "Inicialização do banco de dados concluída com sucesso!"
