-- Local read-model populated by consuming concept-bank-service's enriched ConceptBankCreated
-- (carries the full concept list) — see ADR-004.
CREATE TABLE IF NOT EXISTS conceptbank_read_model (
    bank_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS concept_read_model (
    id VARCHAR(255) PRIMARY KEY,
    bank_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT NOT NULL,
    difficulty INT NOT NULL
);
