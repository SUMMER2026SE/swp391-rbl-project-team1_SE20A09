-- Rename PENDING_ADMIN_REVIEW to AWAITING_CUSTOMER_RESPONSE
-- Drop the old constraint
ALTER TABLE complaints DROP CONSTRAINT IF EXISTS complaints_status_check;

-- Widen the status column to accommodate longer names
ALTER TABLE complaints ALTER COLUMN status TYPE VARCHAR(50);

-- Update existing data
UPDATE complaints SET status = 'AWAITING_CUSTOMER_RESPONSE' WHERE status = 'PENDING_ADMIN_REVIEW';

-- Add new constraint
ALTER TABLE complaints 
ADD CONSTRAINT complaints_status_check 
CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED', 'AWAITING_CUSTOMER_RESPONSE', 'CUSTOMER_WITHDRAWN'));
