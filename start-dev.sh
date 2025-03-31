#!/bin/bash

# Script para iniciar o ambiente de desenvolvimento completo

echo "Iniciando ambiente de desenvolvimento do WAD Calendar Service..."

# Verificar se o Docker está em execução
if ! docker info > /dev/null 2>&1; then
  echo "Erro: Docker não está em execução. Por favor, inicie o Docker primeiro."
  exit 1
fi

# Iniciar serviços com docker-compose
echo "Iniciando containers..."
docker-compose up -d

# Esperar o banco de dados iniciar completamente
echo "Aguardando inicialização do banco de dados..."
sleep 10

echo "Ambiente de desenvolvimento inicializado com sucesso!"
echo "Aplicação disponível em: http://localhost:8080"
