package wad_calendar_service.com.br.wad_calendar_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wad_calendar_service.com.br.wad_calendar_service.dto.AppointmentDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.AvailabilityResponseDTO;
import wad_calendar_service.com.br.wad_calendar_service.dto.RescheduleDTO;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;
import wad_calendar_service.com.br.wad_calendar_service.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Controller para gerenciamento de agendamentos
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Verifica disponibilidade para um dia e horário específico
     * Se não houver disponibilidade, retorna os próximos 4 dias disponíveis (excluindo sábados e domingos)
     */
    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        try {
            AvailabilityResponseDTO response = appointmentService.checkAvailability(date, time);
            return ResponseEntity.ok(response);
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao verificar disponibilidade: " + e.getMessage()));
        }
    }

    /**
     * Cria um novo agendamento
     */
    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        try {
            appointmentService.createAppointment(appointmentDTO);
            return ResponseEntity.ok(Map.of("message", "Agendamento criado com sucesso"));
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao criar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Cancela um agendamento pelo número de telefone
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAppointment(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            appointmentService.cancelAppointment(phoneNumber);
            return ResponseEntity.ok(Map.of("message", "Agendamento cancelado com sucesso"));
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao cancelar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Reagenda um agendamento existente
     */
    @PostMapping("/reschedule")
    public ResponseEntity<?> rescheduleAppointment(@RequestBody RescheduleDTO rescheduleDTO) {
        try {
            appointmentService.rescheduleAppointment(rescheduleDTO);
            return ResponseEntity.ok(Map.of("message", "Agendamento reagendado com sucesso"));
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao reagendar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Consulta um agendamento pelo número de telefone
     */
    @GetMapping
    public ResponseEntity<?> getAppointmentByPhone(@RequestParam String phoneNumber) {
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentByPhone(phoneNumber);
            return ResponseEntity.ok(appointment);
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao consultar o agendamento: " + e.getMessage()));
        }
    }
}
