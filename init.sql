
-- Users table: stores user information and points
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用戶ID',
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '用戶名稱',
    points INT NOT NULL DEFAULT 0 COMMENT '用戶積分',
    registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '註冊日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_username (username),
    INDEX idx_registration_date (registration_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Games table: stores game metadata
CREATE TABLE IF NOT EXISTS games (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '遊戲ID',
    game_code VARCHAR(50) NOT NULL UNIQUE COMMENT '遊戲代碼',
    game_name VARCHAR(200) NOT NULL COMMENT '遊戲名稱',
    description TEXT COMMENT '遊戲描述',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_game_code (game_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Login record table: tracks user login history for consecutive login mission
CREATE TABLE IF NOT EXISTS login_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '登入記錄ID',
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    login_date DATE NOT NULL COMMENT '登入日期',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登入時間',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    UNIQUE KEY uk_user_login_date (user_id, login_date),
    INDEX idx_user_id (user_id),
    INDEX idx_login_date (login_date),
    INDEX idx_user_date (user_id, login_date DESC),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game launch record table: tracks which games users have launched
CREATE TABLE IF NOT EXISTS game_launch_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '遊戲啟動記錄ID',
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    game_id BIGINT NOT NULL COMMENT '遊戲ID',
    launch_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '啟動時間',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_user_game (user_id, game_id),
    INDEX idx_launch_time (launch_time),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Games play record table: stores game play sessions with scores
CREATE TABLE IF NOT EXISTS games_play_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '遊戲遊玩記錄ID',
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    game_id BIGINT NOT NULL COMMENT '遊戲ID',
    score INT NOT NULL DEFAULT 0 COMMENT '遊戲分數',
    play_duration INT COMMENT '遊玩時長(秒)',
    play_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '遊玩時間',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_user_game (user_id, game_id),
    INDEX idx_play_time (play_time),
    INDEX idx_user_score (user_id, score),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Missions table: tracks user mission progress and reward status
CREATE TABLE IF NOT EXISTS missions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任務ID',
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    mission_type VARCHAR(50) NOT NULL COMMENT '任務類型 (CONSECUTIVE_LOGIN:連續登入, LAUNCH_GAMES:啟動遊戲, PLAY_GAMES:遊玩遊戲)',
    current_progress INT NOT NULL DEFAULT 0 COMMENT '當前進度',
    target_progress INT NOT NULL COMMENT '目標進度',
    is_completed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否完成',
    completed_at DATETIME COMMENT '完成時間',
    is_rewarded BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已領取獎勵',
    rewarded_at DATETIME COMMENT '領取獎勵時間',
    reward_points INT NOT NULL DEFAULT 0 COMMENT '獎勵積分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    UNIQUE KEY uk_user_mission_type (user_id, mission_type),
    INDEX idx_user_id (user_id),
    INDEX idx_mission_type (mission_type),
    INDEX idx_is_completed (is_completed),
    INDEX idx_is_rewarded (is_rewarded),
    INDEX idx_user_completed (user_id, is_completed),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample games for testing
INSERT INTO games (game_code, game_name, description, is_active) VALUES
('GAME001', 'Adventure Quest', 'Epic adventure game', TRUE),
('GAME002', 'Racing Mania', 'Fast-paced racing game', TRUE),
('GAME003', 'Puzzle Master', 'Brain-teasing puzzle game', TRUE),
('GAME004', 'Space Shooter', 'Intense space combat', TRUE),
('GAME005', 'Strategy Empire', 'Build your empire', TRUE)
ON DUPLICATE KEY UPDATE game_name=VALUES(game_name);

INSERT INTO taskdb.users (username, points, registration_date, created_at, updated_at)
VALUES ('testuser', 0, '2025-10-31 18:29:34', '2025-10-31 18:29:38', '2025-10-31 18:29:39')
ON DUPLICATE KEY UPDATE username=VALUES(username);


