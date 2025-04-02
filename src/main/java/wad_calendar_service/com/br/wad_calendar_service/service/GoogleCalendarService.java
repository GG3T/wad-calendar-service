package wad_calendar_service.com.br.wad_calendar_service.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import wad_calendar_service.com.br.wad_calendar_service.dto.AvailabilityResponseDTO;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço para integração com o Google Calendar
 */
@Service
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "WAD Calendar Service";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${google.calendar.id}")
    private String calendarId;

    @Value("${google.credentials.file}")
    private Resource credentialsFile;
    
    @Value("${appointment.duration.minutes:60}")
    private int appointmentDurationMinutes;
    
    @Value("${calendar.time-zone:America/Sao_Paulo}")
    private String timeZone;
    
    private ZoneId zoneId;
    
    // Inicializa o zoneId após injeção da propriedade timeZone
    public ZoneId getZoneId() {
        if (zoneId == null) {
            zoneId = ZoneId.of(timeZone);
        }
        return zoneId;
    }

    /**
     * Verifica a disponibilidade de horário na agenda do Google
     */
    public boolean isTimeSlotAvailable(LocalDate date, LocalTime time) {
        try {
            logger.info("Verificando disponibilidade no Google Calendar - Agenda: {}, Data/Hora: {}/{}, Fuso Horário: {}", 
                    calendarId, date, time, timeZone);
            
            Calendar service = getCalendarService();
            
            // Definir o intervalo de tempo para verificar (horário específico + duração)
            LocalDateTime startLocalDateTime = LocalDateTime.of(date, time);
            LocalDateTime endLocalDateTime = startLocalDateTime.plusMinutes(appointmentDurationMinutes);
            
            // Converter para ZonedDateTime com o fuso horário correto
            ZonedDateTime startZonedDateTime = startLocalDateTime.atZone(getZoneId());
            ZonedDateTime endZonedDateTime = endLocalDateTime.atZone(getZoneId());
            
            // Converter para DateTime do Google API
            DateTime timeMin = new DateTime(Date.from(startZonedDateTime.toInstant()));
            DateTime timeMax = new DateTime(Date.from(endZonedDateTime.toInstant()));
            
            logger.debug("Consultando FreeBusy API - ID Calendário: {}, Intervalo: de {} a {} ({})", 
                    calendarId,
                    startLocalDateTime.format(DATE_TIME_FORMATTER),
                    endLocalDateTime.format(DATE_TIME_FORMATTER),
                    timeZone);
            
            // Criar requisição freebusy para verificar disponibilidade
            FreeBusyRequest request = new FreeBusyRequest()
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setTimeZone(timeZone)
                    .setItems(Collections.singletonList(new FreeBusyRequestItem().setId(calendarId)));
            
            logger.debug("Enviando requisição FreeBusy para API do Google: {}", request.toPrettyString());
            
            FreeBusyResponse response = service.freebusy()
                    .query(request)
                    .execute();
            
            logger.debug("Resposta da API FreeBusy: {}", response.toPrettyString());
            
            // Verificar se o calendário está disponível neste horário
            if (!response.getCalendars().containsKey(calendarId)) {
                logger.error("Erro ao verificar disponibilidade: Calendário {} não encontrado na resposta", calendarId);
                throw new AppointmentException("Calendário não encontrado: " + calendarId);
            }
            
            boolean hasConflict = !response.getCalendars()
                    .get(calendarId)
                    .getBusy()
                    .isEmpty();
            
            boolean isAvailable = !hasConflict;
            logger.info("Disponibilidade no Google Calendar para {}/{} ({}): {}", 
                    date, time, timeZone, isAvailable ? "Disponível" : "Indisponível");
            
            return isAvailable;
        } catch (Exception e) {
            logger.error("Erro ao verificar disponibilidade no Google Calendar - Agenda: {}, Data/Hora: {}/{}, Erro: {}", 
                    calendarId, date, time, e.getMessage(), e);
            throw new AppointmentException("Erro ao verificar disponibilidade no Google Calendar: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encontra os próximos dias disponíveis no Google Calendar
     */
    public List<AvailabilityResponseDTO.AvailableDateTimeDTO> findNextAvailableDates(LocalDate startDate, LocalTime time, int count) {
        try {
            logger.info("Buscando próximas {} datas disponíveis no Google Calendar a partir de {}, hora: {}, Fuso Horário: {}", 
                    count, startDate.plusDays(1), time, timeZone);
            
            List<AvailabilityResponseDTO.AvailableDateTimeDTO> availableDates = new ArrayList<>();
            LocalDate currentDate = startDate.plusDays(1);
            
            while (availableDates.size() < count) {
                // Pular fins de semana
                if (isWeekend(currentDate)) {
                    logger.debug("Pulando fim de semana: {}", currentDate);
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
                
                // Verificar se o horário está disponível no Google Calendar
                if (isTimeSlotAvailable(currentDate, time)) {
                    logger.debug("Data disponível encontrada: {} às {} ({})", currentDate, time, timeZone);
                    availableDates.add(new AvailabilityResponseDTO.AvailableDateTimeDTO(currentDate, time));
                } else {
                    logger.debug("Data indisponível no Google Calendar: {} às {} ({})", currentDate, time, timeZone);
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            logger.info("Total de datas disponíveis encontradas: {}", availableDates.size());
            return availableDates;
        } catch (Exception e) {
            logger.error("Erro ao buscar datas disponíveis no Google Calendar: {}", e.getMessage(), e);
            throw new AppointmentException("Erro ao buscar datas disponíveis no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Cria um novo evento no Google Calendar
     */
    public String createEvent(String phoneNumber, LocalDate date, LocalTime time, String summary) {
        try {
            logger.info("Iniciando criação de evento no Google Calendar - Agenda: {}, Telefone: {}, Data/Hora: {}/{}, Fuso Horário: {}",
                    calendarId, phoneNumber, date, time, timeZone);
            
            Calendar service = getCalendarService();
            
            Event event = new Event()
                    .setSummary("Agendamento: " + phoneNumber)
                    .setDescription(summary);

            // Converter para ZonedDateTime com o fuso horário correto
            LocalDateTime startLocalDateTime = LocalDateTime.of(date, time);
            LocalDateTime endLocalDateTime = startLocalDateTime.plusMinutes(appointmentDurationMinutes);
            
            ZonedDateTime startZonedDateTime = startLocalDateTime.atZone(getZoneId());
            ZonedDateTime endZonedDateTime = endLocalDateTime.atZone(getZoneId());
            
            // Converter para DateTime do Google API
            DateTime start = new DateTime(Date.from(startZonedDateTime.toInstant()));
            DateTime end = new DateTime(Date.from(endZonedDateTime.toInstant()));
            
            event.setStart(new EventDateTime().setDateTime(start).setTimeZone(timeZone));
            event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(timeZone));

            logger.debug("Dados do evento a ser criado - ID Calendário: {}, Início: {}, Fim: {}, Fuso Horário: {}, Resumo: {}",
                    calendarId,
                    startLocalDateTime.format(DATE_TIME_FORMATTER),
                    endLocalDateTime.format(DATE_TIME_FORMATTER),
                    timeZone,
                    event.getSummary());
            
            logger.debug("Enviando requisição para API do Google para criar evento: {}", event.toPrettyString());
            
            event = service.events().insert(calendarId, event).execute();
            
            logger.info("Evento criado com sucesso no Google Calendar - EventID: {}, Link: {}", 
                    event.getId(), event.getHtmlLink());
            
            return event.getId();
        } catch (Exception e) {
            logger.error("Erro ao criar evento no Google Calendar - Agenda: {}, Telefone: {}, Erro: {}", 
                    calendarId, phoneNumber, e.getMessage(), e);
            throw new AppointmentException("Erro ao criar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza um evento existente no Google Calendar
     */
    public void updateEvent(String eventId, String phoneNumber, LocalDate date, LocalTime time, String summary) {
        try {
            logger.info("Iniciando atualização de evento no Google Calendar - Agenda: {}, EventID: {}, Telefone: {}, Nova Data/Hora: {}/{}, Fuso Horário: {}",
                    calendarId, eventId, phoneNumber, date, time, timeZone);
            
            Calendar service = getCalendarService();
            
            Event event = service.events().get(calendarId, eventId).execute();
            logger.debug("Evento encontrado para atualização - Evento atual: {}", event.toPrettyString());
            
            event.setSummary("Agendamento: " + phoneNumber);
            event.setDescription(summary);

            // Converter para ZonedDateTime com o fuso horário correto
            LocalDateTime startLocalDateTime = LocalDateTime.of(date, time);
            LocalDateTime endLocalDateTime = startLocalDateTime.plusMinutes(appointmentDurationMinutes);
            
            ZonedDateTime startZonedDateTime = startLocalDateTime.atZone(getZoneId());
            ZonedDateTime endZonedDateTime = endLocalDateTime.atZone(getZoneId());
            
            // Converter para DateTime do Google API
            DateTime start = new DateTime(Date.from(startZonedDateTime.toInstant()));
            DateTime end = new DateTime(Date.from(endZonedDateTime.toInstant()));
            
            event.setStart(new EventDateTime().setDateTime(start).setTimeZone(timeZone));
            event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(timeZone));

            logger.debug("Novos dados do evento - Início: {}, Fim: {}, Fuso Horário: {}, Resumo: {}",
                    startLocalDateTime.format(DATE_TIME_FORMATTER),
                    endLocalDateTime.format(DATE_TIME_FORMATTER),
                    timeZone,
                    event.getSummary());
            
            Event updatedEvent = service.events().update(calendarId, eventId, event).execute();
            
            logger.info("Evento atualizado com sucesso no Google Calendar - EventID: {}, Link: {}", 
                    updatedEvent.getId(), updatedEvent.getHtmlLink());
            
        } catch (Exception e) {
            logger.error("Erro ao atualizar evento no Google Calendar - Agenda: {}, EventID: {}, Telefone: {}, Erro: {}", 
                    calendarId, eventId, phoneNumber, e.getMessage(), e);
            throw new AppointmentException("Erro ao atualizar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela um evento no Google Calendar
     */
    public void cancelEvent(String eventId) {
        try {
            logger.info("Iniciando cancelamento de evento no Google Calendar - Agenda: {}, EventID: {}", 
                    calendarId, eventId);
            
            Calendar service = getCalendarService();
            
            // Obter informações do evento antes de excluir (para logging)
            Event event = service.events().get(calendarId, eventId).execute();
            logger.debug("Evento a ser cancelado: {}", event.toPrettyString());
            
            service.events().delete(calendarId, eventId).execute();
            
            logger.info("Evento cancelado com sucesso no Google Calendar - EventID: {}", eventId);
            
        } catch (Exception e) {
            logger.error("Erro ao cancelar evento no Google Calendar - Agenda: {}, EventID: {}, Erro: {}", 
                    calendarId, eventId, e.getMessage(), e);
            throw new AppointmentException("Erro ao cancelar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se um evento existe no Google Calendar
     */
    public boolean checkEventExists(String eventId) {
        try {
            logger.info("Verificando existência de evento no Google Calendar - Agenda: {}, EventID: {}", 
                    calendarId, eventId);
            
            Calendar service = getCalendarService();
            Event event = service.events().get(calendarId, eventId).execute();
            
            logger.info("Evento encontrado no Google Calendar - EventID: {}, Status: {}", 
                    eventId, event.getStatus());
            
            return true;
        } catch (Exception e) {
            logger.info("Evento não encontrado no Google Calendar ou erro na consulta - Agenda: {}, EventID: {}, Erro: {}", 
                    calendarId, eventId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Lista todos os calendários disponíveis para a conta
     * @return Mapa com informações dos calendários disponíveis
     */
    public Map<String, String> listAvailableCalendars() {
        try {
            logger.info("Listando calendários disponíveis");
            
            Calendar service = getCalendarService();
            
            CalendarList calendarList = service.calendarList().list().execute();
            
            Map<String, String> calendarsInfo = new HashMap<>();
            
            logger.info("Calendários disponíveis:");
            for (CalendarListEntry calendar : calendarList.getItems()) {
                String calendarDetails = String.format("Nome: %s, Fuso Horário: %s, Papel: %s, Primário: %s", 
                        calendar.getSummary(), 
                        calendar.getTimeZone(),
                        calendar.getAccessRole(),
                        calendar.getPrimary() != null && calendar.getPrimary() ? "Sim" : "Não");
                
                calendarsInfo.put(calendar.getId(), calendarDetails);
                
                logger.info("ID: {}, Nome: {}, Fuso Horário: {}, Papel: {}, Primário: {}", 
                        calendar.getId(), 
                        calendar.getSummary(), 
                        calendar.getTimeZone(),
                        calendar.getAccessRole(),
                        calendar.getPrimary() != null && calendar.getPrimary() ? "Sim" : "Não");
            }
            
            return calendarsInfo;
        } catch (Exception e) {
            logger.error("Erro ao listar calendários disponíveis: {}", e.getMessage(), e);
            throw new AppointmentException("Erro ao listar calendários disponíveis: " + e.getMessage(), e);
        }
    }

    /**
     * Configura o serviço do Google Calendar
     */
    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        logger.debug("Inicializando serviço do Google Calendar - Agenda: {}, Fuso Horário: {}", calendarId, timeZone);
        
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credentials = GoogleCredential
                .fromStream(credentialsFile.getInputStream())
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        logger.debug("Credenciais do Google Calendar carregadas com sucesso");
        
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credentials)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Verifica se uma data é fim de semana (sábado ou domingo)
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
