package wad_calendar_service.com.br.wad_calendar_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO para resposta de verificação de disponibilidade
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityResponseDTO {

    private boolean available;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestedDate;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime requestedTime;
    
    private List<AvailableDateTimeDTO> alternativeDates;

    public AvailabilityResponseDTO() {
    }

    public AvailabilityResponseDTO(boolean available, LocalDate requestedDate, LocalTime requestedTime) {
        this.available = available;
        this.requestedDate = requestedDate;
        this.requestedTime = requestedTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalTime getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(LocalTime requestedTime) {
        this.requestedTime = requestedTime;
    }

    public List<AvailableDateTimeDTO> getAlternativeDates() {
        return alternativeDates;
    }

    public void setAlternativeDates(List<AvailableDateTimeDTO> alternativeDates) {
        this.alternativeDates = alternativeDates;
    }

    /**
     * DTO interno para representar datas e horários disponíveis
     */
    public static class AvailableDateTimeDTO {
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;

        public AvailableDateTimeDTO() {
        }

        public AvailableDateTimeDTO(LocalDate date, LocalTime time) {
            this.date = date;
            this.time = time;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }
    }
}
