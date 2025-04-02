package wad_calendar_service.com.br.wad_calendar_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

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
        
        logger.info("Recebida requisição para verificar disponibilidade - Data: {}, Hora: {}", date, time);
        
        try {
            AvailabilityResponseDTO response = appointmentService.checkAvailability(date, time);
            
            logger.info("Verificação de disponibilidade concluída - Data: {}, Hora: {}, Disponível: {}", 
                    date, time, response.isAvailable());
            
            if (!response.isAvailable() && response.getAlternativeDates() != null) {
                logger.info("Retornando {} datas alternativas", response.getAlternativeDates().size());
            }
            
            return ResponseEntity.ok(response);
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao verificar disponibilidade - Data: {}, Hora: {}, Erro: {}", 
                    date, time, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao verificar disponibilidade - Data: {}, Hora: {}, Erro: {}", 
                    date, time, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao verificar disponibilidade: " + e.getMessage()));
        }
    }

    /**
     * Cria um novo agendamento
     */
    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Recebida requisição para criar agendamento - Telefone: {}, Data: {}, Hora: {}", 
                appointmentDTO.getPhoneNumber(), appointmentDTO.getDate(), appointmentDTO.getTime());
        
        try {
            appointmentService.createAppointment(appointmentDTO);
            
            logger.info("Agendamento criado com sucesso - Telefone: {}", appointmentDTO.getPhoneNumber());
            return ResponseEntity.ok(Map.of("message", "Agendamento criado com sucesso"));
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao criar agendamento - Telefone: {}, Erro: {}", 
                    appointmentDTO.getPhoneNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao criar agendamento - Telefone: {}, Erro: {}", 
                    appointmentDTO.getPhoneNumber(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao criar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Cancela um agendamento pelo número de telefone
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAppointment(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        logger.info("Recebida requisição para cancelar agendamento - Telefone: {}", phoneNumber);
        
        try {
            appointmentService.cancelAppointment(phoneNumber);
            
            logger.info("Agendamento cancelado com sucesso - Telefone: {}", phoneNumber);
            return ResponseEntity.ok(Map.of("message", "Agendamento cancelado com sucesso"));
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao cancelar agendamento - Telefone: {}, Erro: {}", 
                    phoneNumber, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao cancelar agendamento - Telefone: {}, Erro: {}", 
                    phoneNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao cancelar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Reagenda um agendamento existente
     */
    @PostMapping("/reschedule")
    public ResponseEntity<?> rescheduleAppointment(@RequestBody RescheduleDTO rescheduleDTO) {
        logger.info("Recebida requisição para reagendar agendamento - Telefone: {}, Nova Data: {}, Nova Hora: {}", 
                rescheduleDTO.getPhoneNumber(), rescheduleDTO.getDate(), rescheduleDTO.getTime());
        
        try {
            appointmentService.rescheduleAppointment(rescheduleDTO);
            
            logger.info("Agendamento reagendado com sucesso - Telefone: {}", rescheduleDTO.getPhoneNumber());
            return ResponseEntity.ok(Map.of("message", "Agendamento reagendado com sucesso"));
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao reagendar agendamento - Telefone: {}, Erro: {}", 
                    rescheduleDTO.getPhoneNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao reagendar agendamento - Telefone: {}, Erro: {}", 
                    rescheduleDTO.getPhoneNumber(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao reagendar o agendamento: " + e.getMessage()));
        }
    }

    /**
     * Consulta um agendamento pelo número de telefone
     */
    @GetMapping
    public ResponseEntity<?> getAppointmentByPhone(@RequestParam String phoneNumber) {
        logger.info("Recebida requisição para consultar agendamento - Telefone: {}", phoneNumber);
        
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentByPhone(phoneNumber);
            
            logger.info("Agendamento consultado com sucesso - Telefone: {}, Status: {}", 
                    phoneNumber, appointment.getStatus());
            return ResponseEntity.ok(appointment);
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao consultar agendamento - Telefone: {}, Erro: {}", 
                    phoneNumber, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao consultar agendamento - Telefone: {}, Erro: {}", 
                    phoneNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro ao consultar o agendamento: " + e.getMessage()));
        }
    }
}
