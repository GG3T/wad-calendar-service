#!/bin/bash

# Script para parar o ambiente de desenvolvimento

echo "Parando ambiente de desenvolvimento do WAD Calendar Service..."

# Parar todos os containers
docker-compose down

echo "Ambiente de desenvolvimento parado com sucesso!"
