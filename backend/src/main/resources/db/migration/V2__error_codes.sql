-- A stable code next to the English error message, so the UI can show the error in the user's language
ALTER TABLE library_document ADD COLUMN error_code VARCHAR(40);
ALTER TABLE compliance_report ADD COLUMN error_code VARCHAR(40);
