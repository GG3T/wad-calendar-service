-- Script para inserção de dados iniciais
-- Este script pode ser usado para testes ou para pré-popular o banco de dados

-- Inserindo alguns agendamentos de exemplo (para ambiente de teste)
INSERT INTO appointments (
    phone_number, 
    appointment_date, 
    appointment_time, 
    summary, 
    status, 
    google_event_id, 
    created_at, 
    updated_at, 
    confirmation_sent
) VALUES 
(
    '11999887766', 
    CURRENT_DATE + INTERVAL '1 day', 
    '14:00:00', 
    'Agendamento de teste 1', 
    'AGENDADA', 
    'google-event-id-1', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP, 
    FALSE
),
(
    '11988776655', 
    CURRENT_DATE + INTERVAL '2 day', 
    '15:30:00', 
    'Agendamento de teste 2', 
    'CONFIRMADA', 
    'google-event-id-2', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP, 
    TRUE
),
(
    '11977665544', 
    CURRENT_DATE + INTERVAL '3 day', 
    '10:00:00', 
    'Agendamento de teste 3', 
    'REAGENDADA', 
    'google-event-id-3', 
    CURRENT_TIMESTAMP - INTERVAL '2 day', 
    CURRENT_TIMESTAMP, 
    FALSE
),
(
    '11966554433', 
    CURRENT_DATE - INTERVAL '1 day', 
    '16:00:00', 
    'Agendamento de teste 4', 
    'CANCELADA', 
    'google-event-id-4', 
    CURRENT_TIMESTAMP - INTERVAL '5 day', 
    CURRENT_TIMESTAMP - INTERVAL '1 day', 
    TRUE
);
