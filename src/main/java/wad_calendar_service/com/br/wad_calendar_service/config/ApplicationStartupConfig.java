package wad_calendar_service.com.br.wad_calendar_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import wad_calendar_service.com.br.wad_calendar_service.service.GoogleCalendarService;

/**
 * Configuração para inicialização da aplicação
 */
@Configuration
public class ApplicationStartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupConfig.class);

    @Autowired
    private GoogleCalendarService googleCalendarService;

    /**
     * Executado quando a aplicação está pronta para uso
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Aplicação inicializada. Verificando calendários disponíveis...");
        
        try {
            googleCalendarService.listAvailableCalendars();
        } catch (Exception e) {
            logger.warn("Não foi possível listar os calendários na inicialização: {}", e.getMessage());
        }
    }
}
