package com.sportvenue.repository;

import com.sportvenue.entity.TimeSlotException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotExceptionRepository extends JpaRepository<TimeSlotException, Integer> {

    Optional<TimeSlotException> findBySlotSlotIdAndExceptionDate(Integer slotId, LocalDate exceptionDate);

    @Query("SELECT e FROM TimeSlotException e JOIN FETCH e.slot s " +
           "WHERE s.stadium.stadiumId = :stadiumId " +
           "AND e.exceptionDate BETWEEN :start AND :end")
    List<TimeSlotException> findByStadiumAndDateRange(
        @Param("stadiumId") Integer stadiumId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end);
}
