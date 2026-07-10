-- Update complaint status enum to support new escalation workflow
-- V13: Complaint System Enhancement

-- Drop the old constraint
ALTER TABLE complaints DROP CONSTRAINT IF EXISTS complaints_status_check;

-- Add new status values with updated constraint
ALTER TABLE complaints 
ADD CONSTRAINT complaints_status_check 
CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED', 'PENDING_ADMIN_REVIEW', 'CUSTOMER_WITHDRAWN'));

-- Add new fields for escalation workflow
ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS customer_response_deadline TIMESTAMP,
ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS escalation_reason TEXT,
ADD COLUMN IF NOT EXISTS admin_reviewed_by INT REFERENCES users(user_id),
ADD COLUMN IF NOT EXISTS admin_reviewed_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS sla_violated BOOLEAN DEFAULT FALSE;

-- Add indexes for new fields
CREATE INDEX IF NOT EXISTS idx_complaints_resolved_at ON complaints(resolved_at);
CREATE INDEX IF NOT EXISTS idx_complaints_customer_response_deadline ON complaints(customer_response_deadline);
CREATE INDEX IF NOT EXISTS idx_complaints_escalated_at ON complaints(escalated_at);
CREATE INDEX IF NOT EXISTS idx_complaints_sla_violated ON complaints(sla_violated);

-- Comment
COMMENT ON COLUMN complaints.resolved_at IS 'Timestamp when complaint was resolved by Owner';
COMMENT ON COLUMN complaints.customer_response_deadline IS 'Deadline for customer to object to resolution (48h from resolved_at)';
COMMENT ON COLUMN complaints.escalated_at IS 'Timestamp when complaint was escalated to Admin';
COMMENT ON COLUMN complaints.escalation_reason IS 'Reason for escalation (auto/manual)';
COMMENT ON COLUMN complaints.admin_reviewed_by IS 'Admin user who reviewed the complaint';
COMMENT ON COLUMN complaints.admin_reviewed_at IS 'Timestamp when Admin reviewed';
COMMENT ON COLUMN complaints.sla_violated IS 'Flag if SLA response time was violated';