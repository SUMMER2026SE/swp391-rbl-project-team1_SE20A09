-- Add index for filtering total revenue calculation efficiently
CREATE INDEX idx_payments_payment_status ON payments (payment_status);

-- Add index for filtering open complaints count efficiently
CREATE INDEX idx_complaints_status ON complaints (status);
