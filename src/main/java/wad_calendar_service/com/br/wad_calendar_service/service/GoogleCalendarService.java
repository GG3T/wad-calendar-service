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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

/**
 * Serviço para integração com o Google Calendar
 */
@Service
public class GoogleCalendarService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "WAD Calendar Service";

    @Value("${google.calendar.id}")
    private String calendarId;

    @Value("${google.credentials.file}")
    private Resource credentialsFile;

    /**
     * Cria um novo evento no Google Calendar
     */
    public String createEvent(String phoneNumber, LocalDate date, LocalTime time, String summary) {
        try {
            Calendar service = getCalendarService();
            
            Event event = new Event()
                    .setSummary("Agendamento: " + phoneNumber)
                    .setDescription(summary);

            LocalDateTime startDateTime = LocalDateTime.of(date, time);
            LocalDateTime endDateTime = startDateTime.plusHours(1); // Assumindo que cada agendamento dura 1 hora
            
            DateTime start = new DateTime(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            event.setStart(new EventDateTime().setDateTime(start).setTimeZone(TimeZone.getDefault().getID()));
            
            DateTime end = new DateTime(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(TimeZone.getDefault().getID()));

            event = service.events().insert(calendarId, event).execute();
            return event.getId();
        } catch (Exception e) {
            throw new AppointmentException("Erro ao criar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza um evento existente no Google Calendar
     */
    public void updateEvent(String eventId, String phoneNumber, LocalDate date, LocalTime time, String summary) {
        try {
            Calendar service = getCalendarService();
            
            Event event = service.events().get(calendarId, eventId).execute();
            event.setSummary("Agendamento: " + phoneNumber);
            event.setDescription(summary);

            LocalDateTime startDateTime = LocalDateTime.of(date, time);
            LocalDateTime endDateTime = startDateTime.plusHours(1);
            
            DateTime start = new DateTime(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            event.setStart(new EventDateTime().setDateTime(start).setTimeZone(TimeZone.getDefault().getID()));
            
            DateTime end = new DateTime(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(TimeZone.getDefault().getID()));

            service.events().update(calendarId, eventId, event).execute();
        } catch (Exception e) {
            throw new AppointmentException("Erro ao atualizar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela um evento no Google Calendar
     */
    public void cancelEvent(String eventId) {
        try {
            Calendar service = getCalendarService();
            service.events().delete(calendarId, eventId).execute();
        } catch (Exception e) {
            throw new AppointmentException("Erro ao cancelar evento no Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Configura o serviço do Google Calendar
     */
    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credentials = GoogleCredential
                .fromStream(credentialsFile.getInputStream())
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credentials)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
