package wad_calendar_service.com.br.wad_calendar_service.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wad_calendar_service.com.br.wad_calendar_service.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Componente responsável por agendar tarefas periódicas
 */
@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Tarefa agendada para processar confirmações de agendamentos 
     * Executa diariamente às 10:00 para enviar confirmações para o dia seguinte
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void processConfirmations() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        logger.info("==== INICIANDO TAREFA AGENDADA: Processamento de confirmações para {} ====", tomorrow);
        logger.info("Horário de execução: {}", LocalDateTime.now().format(DATE_FORMATTER));
        
        try {
            appointmentService.processConfirmations();
            logger.info("Processamento de confirmações concluído com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao processar confirmações automáticas: {}", e.getMessage(), e);
        }
        
        logger.info("==== TAREFA AGENDADA FINALIZADA ====");
    }
}
