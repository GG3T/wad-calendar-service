package wad_calendar_service.com.br.wad_calendar_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import wad_calendar_service.com.br.wad_calendar_service.dto.AppointmentDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.AvailabilityResponseDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.RescheduleDTO;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;
import wad_calendar_service.com.br.wad_calendar_service.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    void testCheckAvailability_Available() throws Exception {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);
        
        AvailabilityResponseDTO responseDTO = new AvailabilityResponseDTO(true, date, time);

        when(appointmentService.checkAvailability(eq(date), eq(time))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/appointments/availability")
                .param("date", date.toString())
                .param("time", time.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.requestedDate").value(date.toString()))
                .andExpect(jsonPath("$.requestedTime").value(time.toString()));
    }

    @Test
    void testCheckAvailability_Unavailable() throws Exception {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);
        
        AvailabilityResponseDTO responseDTO = new AvailabilityResponseDTO(false, date, time);
        responseDTO.setAlternativeDates(new ArrayList<>());
        
        when(appointmentService.checkAvailability(eq(date), eq(time))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/appointments/availability")
                .param("date", date.toString())
                .param("time", time.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.requestedDate").value(date.toString()))
                .andExpect(jsonPath("$.requestedTime").value(time.toString()))
                .andExpect(jsonPath("$.alternativeDates").exists());
    }

    @Test
    void testCheckAvailability_Error() throws Exception {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);
        
        when(appointmentService.checkAvailability(eq(date), eq(time)))
            .thenThrow(new AppointmentException("Erro ao verificar disponibilidade"));

        // Act & Assert
        mockMvc.perform(get("/api/appointments/availability")
                .param("date", date.toString())
                .param("time", time.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro ao verificar disponibilidade"));
    }

    @Test
    void testCreateAppointment_Success() throws Exception {
        // Arrange
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setPhoneNumber("11999887766");
        appointmentDTO.setDate(LocalDate.now().plusDays(1));
        appointmentDTO.setTime(LocalTime.of(14, 0));
        appointmentDTO.setSummary("Teste de agendamento");

        // Act & Assert
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentDTO))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Agendamento criado com sucesso"));
    }

    @Test
    void testCreateAppointment_Error() throws Exception {
        // Arrange
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setPhoneNumber("11999887766");
        appointmentDTO.setDate(LocalDate.now().plusDays(1));
        appointmentDTO.setTime(LocalTime.of(14, 0));
        appointmentDTO.setSummary("Teste de agendamento");
        
        doThrow(new AppointmentException("Horário não disponível"))
            .when(appointmentService).createAppointment(any(AppointmentDTO.class));

        // Act & Assert
        mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentDTO))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Horário não disponível"));
    }

    @Test
    void testCancelAppointment_Success() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("phoneNumber", "11999887766");

        // Act & Assert
        mockMvc.perform(post("/api/appointments/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Agendamento cancelado com sucesso"));
    }

    @Test
    void testCancelAppointment_Error() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("phoneNumber", "11999887766");
        
        doThrow(new AppointmentException("Agendamento não encontrado"))
            .when(appointmentService).cancelAppointment(eq("11999887766"));

        // Act & Assert
        mockMvc.perform(post("/api/appointments/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Agendamento não encontrado"));
    }

    @Test
    void testRescheduleAppointment_Success() throws Exception {
        // Arrange
        RescheduleDTO rescheduleDTO = new RescheduleDTO();
        rescheduleDTO.setPhoneNumber("11999887766");
        rescheduleDTO.setDate(LocalDate.now().plusDays(2));
        rescheduleDTO.setTime(LocalTime.of(15, 0));
        rescheduleDTO.setSummary("Teste de reagendamento");

        // Act & Assert
        mockMvc.perform(post("/api/appointments/reschedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rescheduleDTO))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Agendamento reagendado com sucesso"));
    }

    @Test
    void testGetAppointmentByPhone_Success() throws Exception {
        // Arrange
        String phoneNumber = "11999887766";
        
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setPhoneNumber(phoneNumber);
        appointmentDTO.setDate(LocalDate.now().plusDays(1));
        appointmentDTO.setTime(LocalTime.of(14, 0));
        appointmentDTO.setSummary("Teste de consulta");
        appointmentDTO.setStatus("AGENDADA");
        
        when(appointmentService.getAppointmentByPhone(eq(phoneNumber))).thenReturn(appointmentDTO);

        // Act & Assert
        mockMvc.perform(get("/api/appointments")
                .param("phoneNumber", phoneNumber)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber))
                .andExpect(jsonPath("$.date").value(appointmentDTO.getDate().toString()))
                .andExpect(jsonPath("$.time").value(appointmentDTO.getTime().toString()))
                .andExpect(jsonPath("$.summary").value(appointmentDTO.getSummary()))
                .andExpect(jsonPath("$.status").value(appointmentDTO.getStatus()));
    }

    @Test
    void testGetAppointmentByPhone_NotFound() throws Exception {
        // Arrange
        String phoneNumber = "11999887766";
        
        when(appointmentService.getAppointmentByPhone(eq(phoneNumber)))
            .thenThrow(new AppointmentException("Agendamento não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/appointments")
                .param("phoneNumber", phoneNumber)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Agendamento não encontrado"));
    }
}
