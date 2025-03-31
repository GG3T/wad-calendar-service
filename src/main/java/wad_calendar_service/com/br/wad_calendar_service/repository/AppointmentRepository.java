package wad_calendar_service.com.br.wad_calendar_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wad_calendar_service.com.br.wad_calendar_service.model.Appointment;
import wad_calendar_service.com.br.wad_calendar_service.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de banco de dados com agendamentos
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Busca um agendamento pelo número de telefone que não esteja cancelado
     */
    @Query("SELECT a FROM Appointment a WHERE a.phoneNumber = :phoneNumber AND a.status != 'CANCELADA'")
    Optional<Appointment> findActiveByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    /**
     * Verifica se existe um agendamento para a data e hora específica
     */
    boolean existsByDateAndTimeAndStatusNot(LocalDate date, LocalTime time, AppointmentStatus notStatus);
    
    /**
     * Busca um agendamento pelo ID do evento no Google Calendar
     */
    Optional<Appointment> findByGoogleEventId(String googleEventId);
    
    /**
     * Lista agendamentos com data futura que ainda não foram confirmados
     */
    @Query("SELECT a FROM Appointment a WHERE a.date = :date AND a.status = 'AGENDADA' AND a.confirmationSent = false")
    List<Appointment> findUpcomingAppointmentsForConfirmation(@Param("date") LocalDate date);
}
