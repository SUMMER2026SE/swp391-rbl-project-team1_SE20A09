-- V3.2: Add index on matching_type column in match_requests table
CREATE INDEX idx_match_requests_matching_type ON match_requests(matching_type);
