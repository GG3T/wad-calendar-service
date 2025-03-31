package wad_calendar_service.com.br.wad_calendar_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import wad_calendar_service.com.br.wad_calendar_service.dto.AppointmentDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.AvailabilityResponseDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.RescheduleDTO;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;
import wad_calendar_service.com.br.wad_calendar_service.model.Appointment;
import wad_calendar_service.com.br.wad_calendar_service.model.AppointmentStatus;
import wad_calendar_service.com.br.wad_calendar_service.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckAvailability_Available() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);
        
        when(appointmentRepository.existsByDateAndTimeAndStatusNot(eq(date), eq(time), any(AppointmentStatus.class)))
            .thenReturn(false);

        // Act
        AvailabilityResponseDTO result = appointmentService.checkAvailability(date, time);

        // Assert
        assertTrue(result.isAvailable());
        assertEquals(date, result.getRequestedDate());
        assertEquals(time, result.getRequestedTime());
        assertNull(result.getAlternativeDates());
    }

    @Test
    void testCheckAvailability_Unavailable() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);
        
        when(appointmentRepository.existsByDateAndTimeAndStatusNot(eq(date), eq(time), any(AppointmentStatus.class)))
            .thenReturn(true);
        
        // Simulando disponibilidade para datas alternativas
        when(appointmentRepository.existsByDateAndTimeAndStatusNot(any(LocalDate.class), eq(time), any(AppointmentStatus.class)))
            .thenReturn(false);

        // Act
        AvailabilityResponseDTO result = appointmentService.checkAvailability(date, time);

        // Assert
        assertFalse(result.isAvailable());
        assertEquals(date, result.getRequestedDate());
        assertEquals(time, result.getRequestedTime());
        assertNotNull(result.getAlternativeDates());
        assertEquals(4, result.getAlternativeDates().size());
    }

    @Test
    void testCreateAppointment_Success() {
        // Arrange
        AppointmentDTO dto = new AppointmentDTO();
        dto.setPhoneNumber("11999887766");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setTime(LocalTime.of(14, 0));
        dto.setSummary("Teste de agendamento");
        
        when(appointmentRepository.findActiveByPhoneNumber(dto.getPhoneNumber()))
            .thenReturn(Optional.empty());
        
        when(appointmentRepository.existsByDateAndTimeAndStatusNot(eq(dto.getDate()), eq(dto.getTime()), any(AppointmentStatus.class)))
            .thenReturn(false);
        
        when(googleCalendarService.createEvent(anyString(), any(LocalDate.class), any(LocalTime.class), anyString()))
            .thenReturn("google-event-id-123");
        
        when(appointmentRepository.save(any(Appointment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        appointmentService.createAppointment(dto);

        // Assert
        verify(appointmentRepository).findActiveByPhoneNumber(dto.getPhoneNumber());
        verify(appointmentRepository).existsByDateAndTimeAndStatusNot(eq(dto.getDate()), eq(dto.getTime()), any(AppointmentStatus.class));
        verify(googleCalendarService).createEvent(eq(dto.getPhoneNumber()), eq(dto.getDate()), eq(dto.getTime()), eq(dto.getSummary()));
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testCreateAppointment_AlreadyExists() {
        // Arrange
        AppointmentDTO dto = new AppointmentDTO();
        dto.setPhoneNumber("11999887766");
        
        Appointment existingAppointment = new Appointment();
        existingAppointment.setPhoneNumber("11999887766");
        
        when(appointmentRepository.findActiveByPhoneNumber(dto.getPhoneNumber()))
            .thenReturn(Optional.of(existingAppointment));

        // Act & Assert
        Exception exception = assertThrows(AppointmentException.class, () -> {
            appointmentService.createAppointment(dto);
        });
        
        assertEquals("Já existe um agendamento para este número de telefone", exception.getMessage());
        verify(appointmentRepository).findActiveByPhoneNumber(dto.getPhoneNumber());
        verify(googleCalendarService, never()).createEvent(anyString(), any(LocalDate.class), any(LocalTime.class), anyString());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testCancelAppointment_Success() {
        // Arrange
        String phoneNumber = "11999887766";
        
        Appointment appointment = new Appointment();
        appointment.setPhoneNumber(phoneNumber);
        appointment.setStatus(AppointmentStatus.AGENDADA);
        appointment.setGoogleEventId("google-event-id-123");
        
        when(appointmentRepository.findActiveByPhoneNumber(phoneNumber))
            .thenReturn(Optional.of(appointment));
        
        when(appointmentRepository.save(any(Appointment.class)))
            .thenReturn(appointment);

        // Act
        appointmentService.cancelAppointment(phoneNumber);

        // Assert
        assertEquals(AppointmentStatus.CANCELADA, appointment.getStatus());
        verify(appointmentRepository).findActiveByPhoneNumber(phoneNumber);
        verify(googleCalendarService).cancelEvent(appointment.getGoogleEventId());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void testCancelAppointment_NotFound() {
        // Arrange
        String phoneNumber = "11999887766";
        
        when(appointmentRepository.findActiveByPhoneNumber(phoneNumber))
            .thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(AppointmentException.class, () -> {
            appointmentService.cancelAppointment(phoneNumber);
        });
        
        assertEquals("Agendamento não encontrado para o número de telefone: " + phoneNumber, exception.getMessage());
        verify(appointmentRepository).findActiveByPhoneNumber(phoneNumber);
        verify(googleCalendarService, never()).cancelEvent(anyString());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testRescheduleAppointment_Success() {
        // Arrange
        RescheduleDTO dto = new RescheduleDTO();
        dto.setPhoneNumber("11999887766");
        dto.setDate(LocalDate.now().plusDays(2));
        dto.setTime(LocalTime.of(15, 0));
        dto.setSummary("Teste de reagendamento");
        
        Appointment appointment = new Appointment();
        appointment.setPhoneNumber(dto.getPhoneNumber());
        appointment.setDate(LocalDate.now().plusDays(1));
        appointment.setTime(LocalTime.of(14, 0));
        appointment.setStatus(AppointmentStatus.AGENDADA);
        appointment.setGoogleEventId("google-event-id-123");
        
        when(appointmentRepository.findActiveByPhoneNumber(dto.getPhoneNumber()))
            .thenReturn(Optional.of(appointment));
        
        when(appointmentRepository.existsByDateAndTimeAndStatusNot(eq(dto.getDate()), eq(dto.getTime()), any(AppointmentStatus.class)))
            .thenReturn(false);
        
        when(appointmentRepository.save(any(Appointment.class)))
            .thenReturn(appointment);

        // Act
        appointmentService.rescheduleAppointment(dto);

        // Assert
        assertEquals(AppointmentStatus.REAGENDADA, appointment.getStatus());
        assertEquals(dto.getDate(), appointment.getDate());
        assertEquals(dto.getTime(), appointment.getTime());
        assertEquals(dto.getSummary(), appointment.getSummary());
        
        verify(appointmentRepository).findActiveByPhoneNumber(dto.getPhoneNumber());
        verify(appointmentRepository).existsByDateAndTimeAndStatusNot(eq(dto.getDate()), eq(dto.getTime()), any(AppointmentStatus.class));
        verify(googleCalendarService).updateEvent(eq(appointment.getGoogleEventId()), eq(dto.getPhoneNumber()), eq(dto.getDate()), eq(dto.getTime()), eq(dto.getSummary()));
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void testGetAppointmentByPhone_Success() {
        // Arrange
        String phoneNumber = "11999887766";
        
        Appointment appointment = new Appointment();
        appointment.setPhoneNumber(phoneNumber);
        appointment.setDate(LocalDate.now().plusDays(1));
        appointment.setTime(LocalTime.of(14, 0));
        appointment.setSummary("Teste de consulta");
        appointment.setStatus(AppointmentStatus.AGENDADA);
        
        when(appointmentRepository.findActiveByPhoneNumber(phoneNumber))
            .thenReturn(Optional.of(appointment));

        // Act
        AppointmentDTO result = appointmentService.getAppointmentByPhone(phoneNumber);

        // Assert
        assertNotNull(result);
        assertEquals(phoneNumber, result.getPhoneNumber());
        assertEquals(appointment.getDate(), result.getDate());
        assertEquals(appointment.getTime(), result.getTime());
        assertEquals(appointment.getSummary(), result.getSummary());
        assertEquals(appointment.getStatus().toString(), result.getStatus());
        
        verify(appointmentRepository).findActiveByPhoneNumber(phoneNumber);
    }

    @Test
    void testGetAppointmentByPhone_NotFound() {
        // Arrange
        String phoneNumber = "11999887766";
        
        when(appointmentRepository.findActiveByPhoneNumber(phoneNumber))
            .thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(AppointmentException.class, () -> {
            appointmentService.getAppointmentByPhone(phoneNumber);
        });
        
        assertEquals("Agendamento não encontrado para o número de telefone: " + phoneNumber, exception.getMessage());
        verify(appointmentRepository).findActiveByPhoneNumber(phoneNumber);
    }
}
