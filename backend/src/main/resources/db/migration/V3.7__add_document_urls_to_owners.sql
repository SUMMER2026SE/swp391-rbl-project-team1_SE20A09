-- Migration to add business license and identity card URLs to owners table
ALTER TABLE owners ADD COLUMN business_license_url VARCHAR(255);
ALTER TABLE owners ADD COLUMN identity_card_url VARCHAR(255);
