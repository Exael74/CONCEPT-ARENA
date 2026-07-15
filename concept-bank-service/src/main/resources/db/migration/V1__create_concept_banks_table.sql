CREATE TABLE IF NOT EXISTS concept_banks (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS concepts (
    id VARCHAR(255) PRIMARY KEY,
    bank_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT NOT NULL,
    difficulty INT NOT NULL,
    FOREIGN KEY (bank_id) REFERENCES concept_banks(id)
);
