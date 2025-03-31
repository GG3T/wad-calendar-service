package wad_calendar_service.com.br.wad_calendar_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para envio de notificações
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:}")
    private String notificationServiceUrl;

    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envia requisição de confirmação para um agendamento
     */
    public void sendConfirmationRequest(String phoneNumber, LocalDate date, LocalTime time) {
        if (notificationServiceUrl.isEmpty()) {
            logger.info("Notificação simulada para o número {} - Confirmação de agendamento para {}, às {}", 
                     phoneNumber, date.format(DATE_FORMATTER), time.format(TIME_FORMATTER));
            return;
        }

        try {
            Map<String, String> request = new HashMap<>();
            request.put("phoneNumber", phoneNumber);
            request.put("message", String.format(
                    "Confirmação de agendamento para amanhã (%s) às %s. Responda SIM para confirmar ou NÃO para cancelar.",
                    date.format(DATE_FORMATTER),
                    time.format(TIME_FORMATTER)
            ));

            restTemplate.postForEntity(notificationServiceUrl + "/send", request, String.class);
            logger.info("Notificação de confirmação enviada para: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de confirmação: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de confirmação de agendamento
     */
    public void sendAppointmentConfirmation(String phoneNumber, LocalDate date, LocalTime time) {
        if (notificationServiceUrl.isEmpty()) {
            logger.info("Notificação simulada para o número {} - Agendamento confirmado para {}, às {}", 
                     phoneNumber, date.format(DATE_FORMATTER), time.format(TIME_FORMATTER));
            return;
        }

        try {
            Map<String, String> request = new HashMap<>();
            request.put("phoneNumber", phoneNumber);
            request.put("message", String.format(
                    "Seu agendamento para %s às %s foi confirmado. Agradecemos a preferência!",
                    date.format(DATE_FORMATTER),
                    time.format(TIME_FORMATTER)
            ));

            restTemplate.postForEntity(notificationServiceUrl + "/send", request, String.class);
            logger.info("Notificação de agendamento confirmado enviada para: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de agendamento confirmado: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de cancelamento de agendamento
     */
    public void sendCancellationNotification(String phoneNumber, LocalDate date, LocalTime time) {
        if (notificationServiceUrl.isEmpty()) {
            logger.info("Notificação simulada para o número {} - Agendamento cancelado para {}, às {}", 
                     phoneNumber, date.format(DATE_FORMATTER), time.format(TIME_FORMATTER));
            return;
        }

        try {
            Map<String, String> request = new HashMap<>();
            request.put("phoneNumber", phoneNumber);
            request.put("message", String.format(
                    "Seu agendamento para %s às %s foi cancelado. Entre em contato caso deseje reagendar.",
                    date.format(DATE_FORMATTER),
                    time.format(TIME_FORMATTER)
            ));

            restTemplate.postForEntity(notificationServiceUrl + "/send", request, String.class);
            logger.info("Notificação de cancelamento enviada para: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de cancelamento: {}", e.getMessage(), e);
        }
    }
}
