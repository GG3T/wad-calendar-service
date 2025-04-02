-- Script de limpeza para o banco de dados
-- Use este script com cuidado pois ele apaga TODOS os dados da tabela

-- Remover todos os registros
TRUNCATE TABLE appointments CASCADE;

-- Resetar a sequência de ID
ALTER SEQUENCE appointments_id_seq RESTART WITH 1;

-- Confirmar a limpeza
SELECT 'Banco de dados limpo com sucesso!' as status;
