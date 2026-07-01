package com.sportvenue.repository;

import com.sportvenue.entity.MaintenanceSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Integer> {

    Page<MaintenanceSchedule> findByStadiumStadiumIdOrderByStartDateDesc(Integer stadiumId, Pageable pageable);

    Page<MaintenanceSchedule> findByComplexComplexIdOrderByStartDateDesc(Integer complexId, Pageable pageable);

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

    /**
     * Tổng quát hoá của {@link #findActiveForStadiumsAndDate} cho một KHOẢNG ngày thay vì
     * 1 ngày — dùng để: (1) batch nhiều sân trong 1 query duy nhất thay vì gọi lặp N lần
     * (getMyStadiums, danh sách public Facility/Court), (2) gộp 7 query/ngày của weekly-slots
     * thành 1 query cho cả tuần, (3) check overlap giữa các MaintenanceSchedule cùng sân khi
     * tạo mới (truyền {@code stadiumIds} chỉ 1 phần tử). Truyền {@code rangeStart == rangeEnd}
     * để check đúng 1 ngày.
     */
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.stadium.stadiumId IN :stadiumIds " +
           "AND ms.startDate <= :rangeEnd AND (ms.endDate IS NULL OR ms.endDate >= :rangeStart)")
    List<MaintenanceSchedule> findActiveForStadiumsInRange(
            @Param("stadiumIds") List<Integer> stadiumIds,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);

    /** Tương tự {@link #findActiveForStadiumsInRange} nhưng ở cấp Complex, cho nhiều complex cùng lúc. */
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.complex.complexId IN :complexIds " +
           "AND ms.startDate <= :rangeEnd AND (ms.endDate IS NULL OR ms.endDate >= :rangeStart)")
    List<MaintenanceSchedule> findActiveForComplexesInRange(
            @Param("complexIds") List<Integer> complexIds,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);
}
