package wad_calendar_service.com.br.wad_calendar_service.exception;

/**
 * Exceção personalizada para operações de agendamento
 */
public class AppointmentException extends RuntimeException {

    public AppointmentException(String message) {
        super(message);
    }

    public AppointmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
