package wad_calendar_service.com.br.wad_calendar_service.controller;

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

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Endpoint para confirmar um agendamento
     */
    @PostMapping
    public ResponseEntity<?> confirmAppointment(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            String response = request.get("response");
            
            if (phoneNumber == null || response == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Número de telefone e resposta são obrigatórios"));
            }
            
            if ("SIM".equalsIgnoreCase(response)) {
                appointmentService.confirmAppointment(phoneNumber);
                return ResponseEntity.ok(Map.of("message", "Agendamento confirmado com sucesso"));
            } else if ("NAO".equalsIgnoreCase(response) || "NÃO".equalsIgnoreCase(response) || "NO".equalsIgnoreCase(response)) {
                appointmentService.cancelAppointment(phoneNumber);
                return ResponseEntity.ok(Map.of("message", "Agendamento cancelado com sucesso"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Resposta inválida. Use SIM para confirmar ou NÃO para cancelar"));
            }
        } catch (AppointmentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao processar confirmação: " + e.getMessage()));
        }
    }
}
