package wad_calendar_service.com.br.wad_calendar_service.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciamento de agendamentos
 */
@Service
public class AppointmentService {

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
        // Validar se a data é passada
        if (date.isBefore(LocalDate.now())) {
            throw new AppointmentException("Não é possível agendar para datas passadas");
        }

        // Validar se não é fim de semana
        if (isWeekend(date)) {
            throw new AppointmentException("Não é possível agendar para fins de semana");
        }

        // Verificar disponibilidade no horário especificado
        boolean available = !appointmentRepository.existsByDateAndTimeAndStatusNot(date, time, AppointmentStatus.CANCELADA);
        
        AvailabilityResponseDTO response = new AvailabilityResponseDTO(available, date, time);

        // Se não estiver disponível, buscar próximas datas disponíveis
        if (!available) {
            List<AvailabilityResponseDTO.AvailableDateTimeDTO> alternativeDates = findNextAvailableDates(date, time, 4);
            response.setAlternativeDates(alternativeDates);
        }
        
        return response;
    }

    /**
     * Cria um novo agendamento
     */
    @Transactional
    public void createAppointment(AppointmentDTO appointmentDTO) {
        // Verificar se já existe agendamento para o telefone
        Optional<Appointment> existingAppointment = appointmentRepository.findActiveByPhoneNumber(appointmentDTO.getPhoneNumber());
        if (existingAppointment.isPresent()) {
            throw new AppointmentException("Já existe um agendamento para este número de telefone");
        }

        // Verificar disponibilidade
        AvailabilityResponseDTO availability = checkAvailability(appointmentDTO.getDate(), appointmentDTO.getTime());
        if (!availability.isAvailable()) {
            throw new AppointmentException("Horário não disponível para agendamento");
        }

        // Criar novo agendamento
        Appointment appointment = new Appointment();
        appointment.setPhoneNumber(appointmentDTO.getPhoneNumber());
        appointment.setDate(appointmentDTO.getDate());
        appointment.setTime(appointmentDTO.getTime());
        appointment.setSummary(appointmentDTO.getSummary());
        appointment.setStatus(AppointmentStatus.AGENDADA);

        // Criar evento no Google Calendar
        String eventId = googleCalendarService.createEvent(
                appointment.getPhoneNumber(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getSummary()
        );
        appointment.setGoogleEventId(eventId);

        // Salvar no banco de dados
        appointmentRepository.save(appointment);
    }

    /**
     * Cancela um agendamento
     */
    @Transactional
    public void cancelAppointment(String phoneNumber) {
        Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber));

        // Atualizar status
        appointment.setStatus(AppointmentStatus.CANCELADA);
        
        // Cancelar no Google Calendar
        googleCalendarService.cancelEvent(appointment.getGoogleEventId());
        
        // Salvar atualização
        appointmentRepository.save(appointment);
    }

    /**
     * Reagenda um agendamento
     */
    @Transactional
    public void rescheduleAppointment(RescheduleDTO rescheduleDTO) {
        // Buscar agendamento existente
        Appointment appointment = appointmentRepository.findActiveByPhoneNumber(rescheduleDTO.getPhoneNumber())
                .orElseThrow(() -> new AppointmentException("Agendamento não encontrado para o número de telefone: " + rescheduleDTO.getPhoneNumber()));

        // Verificar disponibilidade da nova data
        AvailabilityResponseDTO availability = checkAvailability(rescheduleDTO.getDate(), rescheduleDTO.getTime());
        if (!availability.isAvailable()) {
            throw new AppointmentException("Novo horário não disponível para reagendamento");
        }

        // Atualizar dados
        appointment.setDate(rescheduleDTO.getDate());
        appointment.setTime(rescheduleDTO.getTime());
        if (rescheduleDTO.getSummary() != null && !rescheduleDTO.getSummary().isEmpty()) {
            appointment.setSummary(rescheduleDTO.getSummary());
        }
        appointment.setStatus(AppointmentStatus.REAGENDADA);

        // Atualizar no Google Calendar
        googleCalendarService.updateEvent(
                appointment.getGoogleEventId(),
                appointment.getPhoneNumber(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getSummary()
        );

        // Salvar atualização
        appointmentRepository.save(appointment);
    }

    /**
     * Busca um agendamento pelo número de telefone
     */
    public AppointmentDTO getAppointmentByPhone(String phoneNumber) {
        Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber));

        // Converter para DTO
        AppointmentDTO dto = new AppointmentDTO();
        dto.setPhoneNumber(appointment.getPhoneNumber());
        dto.setDate(appointment.getDate());
        dto.setTime(appointment.getTime());
        dto.setSummary(appointment.getSummary());
        dto.setStatus(appointment.getStatus().toString());
        
        return dto;
    }

    /**
     * Confirma um agendamento
     */
    @Transactional
    public void confirmAppointment(String phoneNumber) {
        Appointment appointment = appointmentRepository.findActiveByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AppointmentException("Agendamento não encontrado para o número de telefone: " + phoneNumber));

        appointment.setStatus(AppointmentStatus.CONFIRMADA);
        appointmentRepository.save(appointment);
    }

    /**
     * Verifica se uma data é fim de semana (sábado ou domingo)
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Encontra as próximas datas disponíveis
     */
    private List<AvailabilityResponseDTO.AvailableDateTimeDTO> findNextAvailableDates(LocalDate startDate, LocalTime time, int count) {
        List<AvailabilityResponseDTO.AvailableDateTimeDTO> availableDates = new ArrayList<>();
        LocalDate currentDate = startDate.plusDays(1);
        
        while (availableDates.size() < count) {
            if (!isWeekend(currentDate)) {
                boolean available = !appointmentRepository.existsByDateAndTimeAndStatusNot(currentDate, time, AppointmentStatus.CANCELADA);
                if (available) {
                    availableDates.add(new AvailabilityResponseDTO.AvailableDateTimeDTO(currentDate, time));
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return availableDates;
    }

    /**
     * Processa confirmações para agendamentos do dia seguinte
     */
    public void processConfirmations() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findUpcomingAppointmentsForConfirmation(tomorrow);
        
        for (Appointment appointment : appointments) {
            notificationService.sendConfirmationRequest(appointment.getPhoneNumber(), appointment.getDate(), appointment.getTime());
            appointment.setConfirmationSent(true);
            appointmentRepository.save(appointment);
        }
    }

    /**
     * Processa notificações de eventos do Google Calendar
     */
    @Transactional
    public void processGoogleCalendarNotification(String eventId, String eventStatus) {
        Appointment appointment = appointmentRepository.findByGoogleEventId(eventId)
                .orElseThrow(() -> new AppointmentException("Agendamento não encontrado para o evento: " + eventId));

        // Atualizar status de acordo com a notificação do Google
        switch (eventStatus) {
            case "cancelled":
                appointment.setStatus(AppointmentStatus.CANCELADA);
                break;
            case "confirmed":
                appointment.setStatus(AppointmentStatus.CONFIRMADA);
                break;
            // Pode adicionar outros status conforme necessário
        }

        appointmentRepository.save(appointment);
    }
}
