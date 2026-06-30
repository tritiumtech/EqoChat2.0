UPDATE credit_record
SET subject_type = 'HUMAN'
WHERE subject_type = 'USER';

UPDATE violation_record
SET subject_type = 'HUMAN'
WHERE subject_type = 'USER';
