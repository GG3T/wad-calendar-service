-- Script para criação do banco de dados e tabelas
-- WAD Calendar Service

-- Tabela de agendamentos
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    summary VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    google_event_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmation_sent BOOLEAN NOT NULL DEFAULT FALSE
);

-- Índices para otimização de consultas
CREATE INDEX IF NOT EXISTS idx_appointments_phone ON appointments(phone_number);
CREATE INDEX IF NOT EXISTS idx_appointments_date_time ON appointments(appointment_date, appointment_time);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_google_event_id ON appointments(google_event_id);

-- Comentários nas tabelas e colunas para documentação
COMMENT ON TABLE appointments IS 'Tabela para armazenar agendamentos de clientes';
COMMENT ON COLUMN appointments.id IS 'Identificador único do agendamento';
COMMENT ON COLUMN appointments.phone_number IS 'Número de telefone do cliente (identificador de negócio)';
COMMENT ON COLUMN appointments.appointment_date IS 'Data do agendamento';
COMMENT ON COLUMN appointments.appointment_time IS 'Hora do agendamento';
COMMENT ON COLUMN appointments.summary IS 'Resumo da conversa/agendamento';
COMMENT ON COLUMN appointments.status IS 'Status do agendamento: AGENDADA, CONFIRMADA, CANCELADA, REAGENDADA';
COMMENT ON COLUMN appointments.google_event_id IS 'ID do evento no Google Calendar';
COMMENT ON COLUMN appointments.created_at IS 'Data e hora de criação do registro';
COMMENT ON COLUMN appointments.updated_at IS 'Data e hora da última atualização do registro';
COMMENT ON COLUMN appointments.confirmation_sent IS 'Flag que indica se a confirmação foi enviada';
