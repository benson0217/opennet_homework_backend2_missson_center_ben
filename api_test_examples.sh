#!/bin/bash

# --- API Test Examples ---
# 在終端機中執行此腳本以測試應用程式的 API。
# 執行前請確保 Spring Boot 應用程式正在運行。

echo -e "\n"


# 1. 測試使用者登入 API
echo "--- 1. 測試使用者登入 API (testuser) ---"
curl -X POST http://localhost:8080/api/users/login \
-H "Content-Type: application/json" \
-d '{"username": "testuser"}'
echo -e "\n\n"


# 2. 測試獲取任務列表 API
echo "--- 2. 測試獲取任務列表 API (testuser) ---"
curl -X GET "http://localhost:8080/api/missions?username=testuser"
echo -e "\n\n"


# 3. 測試啟動遊戲 API
echo "--- 3. 測試啟動遊戲 API (GAME003) ---"
curl -X POST http://localhost:8080/api/games/launchGame \
-H "Content-Type: application/json" \
-d '{
  "username": "testuser",
  "gameCode": "GAME003"
}'
echo -e "\n\n"


# 4. 測試遊玩遊戲 API
echo "--- 4. 測試遊玩遊戲 API (GAME001, score: 700) ---"
curl -X POST http://localhost:8080/api/games/play \
-H "Content-Type: application/json" \
-d '{
  "username": "testuser",
  "gameCode": "GAME001",
  "score": 700,
  "playDuration": 9
}'
echo -e "\n"
