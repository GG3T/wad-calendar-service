package wad_calendar_service.com.br.wad_calendar_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wad_calendar_service.com.br.wad_calendar_service.service.GoogleCalendarService;

import java.util.Map;

/**
 * Controller para diagnóstico e operações de manutenção
 */
@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticsController.class);

    @Autowired
    private GoogleCalendarService googleCalendarService;

    /**
     * Endpoint para listar todos os calendários disponíveis
     */
    @GetMapping("/calendars")
    public ResponseEntity<?> listCalendars() {
        try {
            logger.info("Requisição para listar calendários disponíveis");
            Map<String, String> calendarsInfo = googleCalendarService.listAvailableCalendars();
            return ResponseEntity.ok(calendarsInfo);
        } catch (Exception e) {
            logger.error("Erro ao listar calendários: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao listar calendários: " + e.getMessage()));
        }
    }
}
