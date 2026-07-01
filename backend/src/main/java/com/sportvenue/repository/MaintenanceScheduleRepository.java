package com.sportvenue.repository;

import com.sportvenue.entity.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Integer> {

    List<MaintenanceSchedule> findByStadiumStadiumIdOrderByStartDateDesc(Integer stadiumId);

    List<MaintenanceSchedule> findByComplexComplexIdOrderByStartDateDesc(Integer complexId);

    /**
     * Các khung bảo trì đang active (bao trùm {@code date}) trên 1 hoặc nhiều sân
     * (dùng để check cascade FACILITY -> COURT: {@code stadiumIds} truyền vào có thể
     * gồm cả court đang xét lẫn facility cha của nó).
     */
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.stadium.stadiumId IN :stadiumIds " +
           "AND ms.startDate <= :date AND (ms.endDate IS NULL OR ms.endDate >= :date)")
    List<MaintenanceSchedule> findActiveForStadiumsAndDate(
            @Param("stadiumIds") List<Integer> stadiumIds,
            @Param("date") LocalDate date);

    /**
     * Khung bảo trì đang active gắn trực tiếp ở cấp Complex (không phải Stadium) —
     * dùng bởi {@code isStadiumUnderMaintenance} khi cascade từ Court/Facility lên Complex cha.
     */
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.complex.complexId = :complexId " +
           "AND ms.startDate <= :date AND (ms.endDate IS NULL OR ms.endDate >= :date)")
    List<MaintenanceSchedule> findActiveForComplexAndDate(
            @Param("complexId") Integer complexId,
            @Param("date") LocalDate date);
}
