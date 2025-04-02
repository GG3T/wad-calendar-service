package wad_calendar_service.com.br.wad_calendar_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wad_calendar_service.com.br.wad_calendar_service.dto.AppointmentDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.AvailabilityResponseDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.RescheduleDTO;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;
import wad_calendar_service.com.br.wad_calendar_service.model.Appointment;
import wad_calendar_service.com.br.wad_calendar_service.model.AppointmentStatus;
import wad_calendar_service.com.br.wad_calendar_service.repository.AppointmentRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciamento de agendamentos
 */
@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Verifica disponibilidade para uma data e hora
     * Se não houver disponibilidade, encontra os próximos 4 dias disponíveis
     */
    public AvailabilityResponseDTO checkAvailability(LocalDate date, LocalTime time) {
        logger.info("Verificando disponibilidade para data: {} e hora: {}", date, time);
        
        // Validar se a data é passada
        if (date.isBefore(LocalDate.now())) {
            logger.warn("Tentativa de verificar disponibilidade para data passada: {}", date);
            throw new AppointmentException("Não é possível agendar para datas passadas");
        }

        // Validar se não é fim de semana
        if (isWeekend(date)) {
            logger.warn("Tentativa de verificar disponibilidade para fim de semana: {}, dia da semana: {}", 
                    date, date.getDayOfWeek());
            throw new AppointmentException("Não é possível agendar para fins de semana");
        }

        // Verificar disponibilidade no Google Calendar
        boolean available = googleCalendarService.isTimeSlotAvailable(date, time);
        logger.info("Disponibilidade para data: {} e hora: {} - Disponível: {}", date, time, available);
        
        AvailabilityResponseDTO response = new AvailabilityResponseDTO(available, date, time);

        // Se não estiver disponível, buscar próximas datas disponíveis no Google Calendar
        if (!available) {
            logger.info("Buscando próximas 4 datas disponíveis a partir de: {}", date.plusDays(1));
            List<AvailabilityResponseDTO.AvailableDateTimeDTO> alternativeDates = 
                    googleCalendarService.findNextAvailableDates(date, time, 4);
            response.setAlternativeDates(alternativeDates);
            
            logger.info("Encontradas {} datas alternativas", alternativeDates.size());
            for (int i = 0; i < alternativeDates.size(); i++) {
                AvailabilityResponseDTO.AvailableDateTimeDTO alt = alternativeDates.get(i);
                logger.debug("Alternativa {}: Data: {}, Hora: {}", i+1, alt.getDate(), alt.getTime());
            }
        }
        
        return response;
    }

    /**
     * Cria um novo agendamento
     */
    @Transactional
    public void createAppointment(AppointmentDTO appointmentDTO) {
        try {
            logger.info("Iniciando criação de agendamento - Telefone: {}, Data: {}, Hora: {}", 
                    appointmentDTO.getPhoneNumber(), appointmentDTO.getDate(), appointmentDTO.getTime());
            
            // Verificar se já existe agendamento para o telefone
            Optional<Appointment> existingAppointment = appointmentRepository.findActiveByPhoneNumber(appointmentDTO.getPhoneNumber());
            if (existingAppointment.isPresent()) {
                Appointment existing = existingAppointment.get();
                logger.warn("Tentativa de criar agendamento para telefone que já possui agendamento - Telefone: {}, Data existente: {}, Hora existente: {}, Status: {}", 
                        appointmentDTO.getPhoneNumber(), existing.getDate(), existing.getTime(), existing.getStatus());
                throw new AppointmentException("Já existe um agendamento para este número de telefone");
            }

            // Verificar disponibilidade no Google Calendar
            boolean available = googleCalendarService.isTimeSlotAvailable(appointmentDTO.getDate(), appointmentDTO.getTime());
            if (!available) {
                logger.warn("Tentativa de criar agendamento para data/hora indisponível no Google Calendar - Telefone: {}, Data: {}, Hora: {}", 
                        appointmentDTO.getPhoneNumber(), appointmentDTO.getDate(), appointmentDTO.getTime());
                throw new AppointmentException("Horário não disponível para agendamento no Google Calendar");
            }

            // Primeiro criar o evento no Google Calendar
            logger.info("Criando evento no Google Calendar - Telefone: {}, Data: {}, Hora: {}", 
                    appointmentDTO.getPhoneNumber(), appointmentDTO.getDate(), appointmentDTO.getTime());
            
            String eventId = googleCalendarService.createEvent(
                    appointmentDTO.getPhoneNumber(),
                    appointmentDTO.getDate(),
                    appointmentDTO.getTime(),
                    appointmentDTO.getSummary()
            );
            logger.info("Evento criado no Google Calendar - EventID: {}", eventId);

            // Depois, criar o agendamento no banco de dados
            Appointment appointment = new Appointment();
            appointment.setPhoneNumber(appointmentDTO.getPhoneNumber());
            appointment.setDate(appointmentDTO.getDate());
            appointment.setTime(appointmentDTO.getTime());
            appointment.setSummary(appointmentDTO.getSummary());
            appointment.setStatus(AppointmentStatus.AGENDADA);
            appointment.setGoogleEventId(eventId);
            appointment.setConfirmationSent(false);

            // Salvar no banco de dados
            Appointment savedAppointment = appointmentRepository.save(appointment);
            logger.info("Agendamento salvo com sucesso no banco de dados - ID: {}, Telefone: {}, GoogleEventID: {}", 
                    savedAppointment.getId(), savedAppointment.getPhoneNumber(), savedAppointment.getGoogleEventId());
        } catch (Exception e) {
            logger.error("Erro ao criar agendamento: {}", e.getMessage(), e);
            throw e; // Re-lançar a exceção para garantir rollback da transação
        }
    }

    /**
     * Cancela um agendamento
     */
    @Transactional
    public void cancelAppointment(String phoneNumber) {
        try {
            logger.info("Iniciando cancelamento de agendamento - Telefone: {}", phoneNumber);
            
            Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> {
                        logger.warn("Tentativa de cancelar agendamento inexistente - Telefone: {}", phoneNumber);
                        return new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber);
                    });

            logger.info("Agendamento encontrado para cancelamento - ID: {}, Data: {}, Hora: {}, Status: {}", 
                    appointment.getId(), appointment.getDate(), appointment.getTime(), appointment.getStatus());
            
            // Primeiro cancelar no Google Calendar
            logger.info("Cancelando evento no Google Calendar - EventID: {}", appointment.getGoogleEventId());
            googleCalendarService.cancelEvent(appointment.getGoogleEventId());
            logger.info("Evento cancelado com sucesso no Google Calendar");
            
            // Depois atualizar status no banco de dados
            appointment.setStatus(AppointmentStatus.CANCELADA);
            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Agendamento cancelado com sucesso no banco de dados - ID: {}, Telefone: {}, Novo status: {}", 
                    updatedAppointment.getId(), updatedAppointment.getPhoneNumber(), updatedAppointment.getStatus());
        } catch (Exception e) {
            logger.error("Erro ao cancelar agendamento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Reagenda um agendamento
     */
    @Transactional
    public void rescheduleAppointment(RescheduleDTO rescheduleDTO) {
        try {
            logger.info("Iniciando reagendamento - Telefone: {}, Nova data: {}, Nova hora: {}", 
                    rescheduleDTO.getPhoneNumber(), rescheduleDTO.getDate(), rescheduleDTO.getTime());
            
            // Buscar agendamento existente
            Appointment appointment = appointmentRepository.findActiveByPhoneNumber(rescheduleDTO.getPhoneNumber())
                    .orElseThrow(() -> {
                        logger.warn("Tentativa de reagendar agendamento inexistente - Telefone: {}", rescheduleDTO.getPhoneNumber());
                        return new AppointmentException("Agendamento não encontrado para o número de telefone: " + rescheduleDTO.getPhoneNumber());
                    });

            logger.info("Agendamento encontrado para reagendamento - ID: {}, Data atual: {}, Hora atual: {}, Status: {}", 
                    appointment.getId(), appointment.getDate(), appointment.getTime(), appointment.getStatus());
            
            // Verificar disponibilidade no Google Calendar
            boolean available = googleCalendarService.isTimeSlotAvailable(rescheduleDTO.getDate(), rescheduleDTO.getTime());
            if (!available) {
                logger.warn("Tentativa de reagendar para data/hora indisponível no Google Calendar - Telefone: {}, Nova data: {}, Nova hora: {}", 
                        rescheduleDTO.getPhoneNumber(), rescheduleDTO.getDate(), rescheduleDTO.getTime());
                throw new AppointmentException("Novo horário não disponível para reagendamento no Google Calendar");
            }

            // Primeiro atualizar no Google Calendar
            logger.info("Atualizando evento no Google Calendar - EventID: {}", appointment.getGoogleEventId());
            googleCalendarService.updateEvent(
                    appointment.getGoogleEventId(),
                    appointment.getPhoneNumber(),
                    rescheduleDTO.getDate(),
                    rescheduleDTO.getTime(),
                    rescheduleDTO.getSummary() != null ? rescheduleDTO.getSummary() : appointment.getSummary()
            );
            logger.info("Evento atualizado com sucesso no Google Calendar");

            // Depois atualizar no banco de dados
            logger.info("Atualizando dados do agendamento no banco de dados - De: {}/{} Para: {}/{}", 
                    appointment.getDate(), appointment.getTime(), rescheduleDTO.getDate(), rescheduleDTO.getTime());
            
            appointment.setDate(rescheduleDTO.getDate());
            appointment.setTime(rescheduleDTO.getTime());
            if (rescheduleDTO.getSummary() != null && !rescheduleDTO.getSummary().isEmpty()) {
                appointment.setSummary(rescheduleDTO.getSummary());
            }
            appointment.setStatus(AppointmentStatus.REAGENDADA);
            appointment.setConfirmationSent(false);

            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Agendamento reagendado com sucesso no banco de dados - ID: {}, Telefone: {}, Nova data: {}, Nova hora: {}, Novo status: {}", 
                    updatedAppointment.getId(), updatedAppointment.getPhoneNumber(), 
                    updatedAppointment.getDate(), updatedAppointment.getTime(), updatedAppointment.getStatus());
        } catch (Exception e) {
            logger.error("Erro ao reagendar agendamento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Busca um agendamento pelo número de telefone
     */
    public AppointmentDTO getAppointmentByPhone(String phoneNumber) {
        try {
            logger.info("Consultando agendamento por telefone - Telefone: {}", phoneNumber);
            
            Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> {
                        logger.warn("Agendamento não encontrado na consulta - Telefone: {}", phoneNumber);
                        return new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber);
                    });

            logger.info("Agendamento encontrado - ID: {}, Data: {}, Hora: {}, Status: {}", 
                    appointment.getId(), appointment.getDate(), appointment.getTime(), appointment.getStatus());
            
            // Validar se o evento ainda existe no Google Calendar
            boolean eventExists = googleCalendarService.checkEventExists(appointment.getGoogleEventId());
            logger.info("Evento no Google Calendar - EventID: {}, Existe: {}", appointment.getGoogleEventId(), eventExists);
            
            // Converter para DTO
            AppointmentDTO dto = new AppointmentDTO();
            dto.setPhoneNumber(appointment.getPhoneNumber());
            dto.setDate(appointment.getDate());
            dto.setTime(appointment.getTime());
            dto.setSummary(appointment.getSummary());
            dto.setStatus(appointment.getStatus().toString());
            dto.setGoogleEventId(appointment.getGoogleEventId());
            
            return dto;
        } catch (Exception e) {
            if (!(e instanceof AppointmentException)) {
                logger.error("Erro ao consultar agendamento: {}", e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Confirma um agendamento
     */
    @Transactional
    public void confirmAppointment(String phoneNumber) {
        try {
            logger.info("Iniciando confirmação de agendamento - Telefone: {}", phoneNumber);
            
            Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> {
                        logger.warn("Tentativa de confirmar agendamento inexistente - Telefone: {}", phoneNumber);
                        return new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber);
                    });

            logger.info("Agendamento encontrado para confirmação - ID: {}, Data: {}, Hora: {}, Status atual: {}", 
                    appointment.getId(), appointment.getDate(), appointment.getTime(), appointment.getStatus());
            
            appointment.setStatus(AppointmentStatus.CONFIRMADA);
            Appointment updatedAppointment = appointmentRepository.save(appointment);
            
            logger.info("Agendamento confirmado com sucesso - ID: {}, Telefone: {}, Novo status: {}", 
                    updatedAppointment.getId(), updatedAppointment.getPhoneNumber(), updatedAppointment.getStatus());
        } catch (Exception e) {
            logger.error("Erro ao confirmar agendamento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Verifica se uma data é fim de semana (sábado ou domingo)
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Processa confirmações para agendamentos do dia seguinte
     */
    public void processConfirmations() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            logger.info("Processando confirmações automáticas para agendamentos de amanhã: {}", tomorrow);
            
            List<Appointment> appointments = appointmentRepository.findUpcomingAppointmentsForConfirmation(tomorrow);
            logger.info("Encontrados {} agendamentos pendentes de confirmação para amanhã", appointments.size());
            
            for (Appointment appointment : appointments) {
                logger.info("Enviando solicitação de confirmação - ID: {}, Telefone: {}, Data: {}, Hora: {}",
                        appointment.getId(), appointment.getPhoneNumber(), appointment.getDate(), appointment.getTime());
                
                notificationService.sendConfirmationRequest(appointment.getPhoneNumber(), appointment.getDate(), appointment.getTime());
                appointment.setConfirmationSent(true);
                appointmentRepository.save(appointment);
                
                logger.info("Confirmação enviada e status atualizado - ID: {}, ConfirmationSent: {}",
                        appointment.getId(), appointment.getConfirmationSent());
            }
            
            logger.info("Processamento de confirmações automáticas concluído");
        } catch (Exception e) {
            logger.error("Erro ao processar confirmações: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Processa notificações de eventos do Google Calendar
     */
    @Transactional
    public void processGoogleCalendarNotification(String eventId, String eventStatus) {
        try {
            logger.info("Processando notificação do Google Calendar - EventID: {}, Status: {}", eventId, eventStatus);
            
            Appointment appointment = appointmentRepository.findByGoogleEventId(eventId)
                    .orElseThrow(() -> {
                        logger.warn("Agendamento não encontrado para o evento do Google Calendar - EventID: {}", eventId);
                        return new AppointmentException("Agendamento não encontrado para o evento: " + eventId);
                    });

            logger.info("Agendamento encontrado para atualização - ID: {}, Telefone: {}, Status atual: {}", 
                    appointment.getId(), appointment.getPhoneNumber(), appointment.getStatus());
            
            // Atualizar status de acordo com a notificação do Google
            AppointmentStatus oldStatus = appointment.getStatus();
            switch (eventStatus) {
                case "cancelled":
                    appointment.setStatus(AppointmentStatus.CANCELADA);
                    break;
                case "confirmed":
                    appointment.setStatus(AppointmentStatus.CONFIRMADA);
                    break;
                // Pode adicionar outros status conforme necessário
                default:
                    logger.info("Status do evento não requer atualização: {}", eventStatus);
                    break;
            }

            if (oldStatus != appointment.getStatus()) {
                logger.info("Status do agendamento atualizado - De: {} Para: {}", oldStatus, appointment.getStatus());
                appointmentRepository.save(appointment);
            } else {
                logger.info("Nenhuma alteração de status necessária");
            }
            
            logger.info("Processamento de notificação do Google Calendar concluído");
        } catch (Exception e) {
            logger.error("Erro ao processar notificação do Google Calendar: {}", e.getMessage(), e);
            throw e;
        }
    }
}
