-- Cho phép MaintenanceSchedule gắn vào cấp Complex (L1), không chỉ Stadium (Facility/Court).
-- stadium_id trở thành nullable; thêm complex_id nullable — đúng 1 trong 2 cột phải có giá trị.
ALTER TABLE maintenance_schedules
    ALTER COLUMN stadium_id DROP NOT NULL;

ALTER TABLE maintenance_schedules
    ADD COLUMN complex_id INT REFERENCES stadium_complexes(complex_id) ON DELETE CASCADE;

ALTER TABLE maintenance_schedules
    ADD CONSTRAINT chk_maintenance_schedule_target CHECK (
        (stadium_id IS NOT NULL AND complex_id IS NULL) OR
        (stadium_id IS NULL AND complex_id IS NOT NULL)
    );

CREATE INDEX idx_maintenance_schedules_complex ON maintenance_schedules(complex_id);
