-- Alter response column type to TEXT to accommodate large JSON payloads for conversation threads
ALTER TABLE complaints ALTER COLUMN response TYPE TEXT;

-- Add subject column to complaints
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS subject VARCHAR(255) DEFAULT 'No Subject';
