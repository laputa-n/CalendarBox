ALTER TABLE expense
    ALTER COLUMN name TYPE varchar(255);

ALTER TABLE expense_line
    ALTER COLUMN label TYPE varchar(255);

ALTER TABLE expense_ocr_task
    ALTER COLUMN request_hash SET NOT NULL;