package wad_calendar_service.com.br.wad_calendar_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Classe principal da aplicação WAD Calendar Service.
 * Responsável por gerenciar agendamentos e integração com Google Calendar.
 */
@SpringBootApplication
@EnableScheduling  // Habilita agendamento de tarefas (para confirmações automáticas)
@EnableAsync  // Habilita processamento assíncrono
public class WadCalendarServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WadCalendarServiceApplication.class, args);
    }
    
    /**
     * Bean para realizar requisições HTTP (utilizado pelo NotificationService).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
