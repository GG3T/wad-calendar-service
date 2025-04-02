package wad_calendar_service.com.br.wad_calendar_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wad_calendar_service.com.br.wad_calendar_service.exception.AppointmentException;
import wad_calendar_service.com.br.wad_calendar_service.service.AppointmentService;

import java.util.Map;

/**
 * Controller para receber confirmações de agendamento
 */
@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmationController.class);

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Endpoint para confirmar um agendamento
     */
    @PostMapping
    public ResponseEntity<?> confirmAppointment(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String response = request.get("response");
        
        logger.info("Recebida resposta de confirmação - Telefone: {}, Resposta: {}", phoneNumber, response);
        
        if (phoneNumber == null || response == null) {
            logger.warn("Requisição de confirmação inválida - Telefone: {}, Resposta: {}", phoneNumber, response);
            return ResponseEntity.badRequest().body(Map.of("error", "Número de telefone e resposta são obrigatórios"));
        }
        
        try {
            if ("SIM".equalsIgnoreCase(response)) {
                logger.info("Confirmando agendamento - Telefone: {}", phoneNumber);
                appointmentService.confirmAppointment(phoneNumber);
                logger.info("Agendamento confirmado com sucesso - Telefone: {}", phoneNumber);
                return ResponseEntity.ok(Map.of("message", "Agendamento confirmado com sucesso"));
            } else if ("NAO".equalsIgnoreCase(response) || "NÃO".equalsIgnoreCase(response) || "NO".equalsIgnoreCase(response)) {
                logger.info("Cancelando agendamento por resposta negativa - Telefone: {}", phoneNumber);
                appointmentService.cancelAppointment(phoneNumber);
                logger.info("Agendamento cancelado por resposta negativa - Telefone: {}", phoneNumber);
                return ResponseEntity.ok(Map.of("message", "Agendamento cancelado com sucesso"));
            } else {
                logger.warn("Resposta de confirmação inválida - Telefone: {}, Resposta: {}", phoneNumber, response);
                return ResponseEntity.badRequest().body(Map.of("error", "Resposta inválida. Use SIM para confirmar ou NÃO para cancelar"));
            }
        } catch (AppointmentException e) {
            logger.warn("Erro de negócio ao processar confirmação - Telefone: {}, Resposta: {}, Erro: {}", 
                    phoneNumber, response, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno ao processar confirmação - Telefone: {}, Resposta: {}, Erro: {}", 
                    phoneNumber, response, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao processar confirmação: " + e.getMessage()));
        }
    }
}
