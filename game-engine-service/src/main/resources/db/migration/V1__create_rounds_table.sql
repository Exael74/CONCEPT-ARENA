CREATE TABLE IF NOT EXISTS rounds (
    id VARCHAR(255) PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    concept_question TEXT NOT NULL,
    expected_answer VARCHAR(1000) NOT NULL,
    difficulty INT NOT NULL,
    duration_seconds BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS answers (
    id VARCHAR(255) PRIMARY KEY,
    round_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    result VARCHAR(20) NOT NULL,
    FOREIGN KEY (round_id) REFERENCES rounds(id)
);
