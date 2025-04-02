package wad_calendar_service.com.br.wad_calendar_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wad_calendar_service.com.br.wad_calendar_service.service.AppointmentService;

import java.util.Map;

/**
 * Controller para receber notificações do Google Calendar via webhook
 */
@RestController
@RequestMapping("/api/webhooks/google-calendar")
public class GoogleCalendarWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarWebhookController.class);

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Endpoint para receber notificações do Google Calendar
     */
    @PostMapping
    public ResponseEntity<?> handleGoogleCalendarNotification(
            @RequestHeader("X-Goog-Channel-ID") String channelId,
            @RequestHeader("X-Goog-Resource-ID") String resourceId,
            @RequestHeader("X-Goog-Resource-State") String resourceState,
            @RequestBody Map<String, Object> notification) {
        
        logger.info("Recebida notificação do Google Calendar - ChannelID: {}, ResourceID: {}, State: {}, Payload: {}", 
                channelId, resourceId, resourceState, notification);
        
        try {
            if ("exists".equals(resourceState) || "update".equals(resourceState)) {
                Map<String, Object> eventData = (Map<String, Object>) notification.get("event");
                if (eventData != null) {
                    String eventId = (String) eventData.get("id");
                    String eventStatus = (String) eventData.get("status");
                    
                    logger.info("Processando notificação do Google Calendar - EventID: {}, Status: {}", 
                            eventId, eventStatus);
                    
                    appointmentService.processGoogleCalendarNotification(eventId, eventStatus);
                    
                    logger.info("Notificação do Google Calendar processada com sucesso - EventID: {}", eventId);
                } else {
                    logger.warn("Notificação do Google Calendar sem dados de evento - ChannelID: {}", channelId);
                }
            } else {
                logger.info("Estado do recurso não requer processamento - State: {}", resourceState);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erro ao processar notificação do Google Calendar - ChannelID: {}, Erro: {}", 
                    channelId, e.getMessage(), e);
            // Sempre retornar 200 para o Google (requisito da API)
            return ResponseEntity.ok().build();
        }
    }
}
