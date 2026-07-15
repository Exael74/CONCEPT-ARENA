CREATE TABLE IF NOT EXISTS session_results (
    id VARCHAR(255) PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    incorrect_answers INT NOT NULL DEFAULT 0,
    total_time_ms BIGINT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP NOT NULL
);
