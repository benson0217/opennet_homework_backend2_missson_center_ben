
# 📬 Mission Center Service – Backend Homework

This is a technical assignment for backend engineer candidates. You are expected to build a RESTful mission center service using **Spring Boot**, integrating **MySQL**, **Redis**, and **RocketMQ**.


---

## 🎯 Objective

You are required to implement a 30-day mission system for new users.  
The goal is to track user activity and automatically distribute rewards once specific missions are completed.  
All user gameplay actions are triggered via an API and stored in the database.  
The system should be designed with performance, scalability, and clean architecture in mind.  
In addition, the system must include a Mission Center where users can view the current progress of each mission in real-time.

---

## 🧰 Tech Requirements

You **must use** the following technologies:

- **Java 21+**
- **Spring Boot**
- **MySQL** (for persistence)
- **Redis** (for caching)
- **RocketMQ** (for event messaging)

You may use starter dependencies such as:
- Spring Web
- Spring Data JPA
- Spring Cache
- RocketMQ Spring Boot Starter

---

## 🔧 Features to Implement

### Implement a RESTful backend service that supports the following features:

### 1️⃣  There are three missions to complete 
1. Log in for three consecutive days.
2. Launch at least three different games.
3. Play at least three game sessions with a combined score of over 1,000 points.

Once all missions are completed, the user should receive a 777-point reward.  
The system should expose a Mission Center view that returns the user’s current mission status and progress.  

### 2️⃣  You are required to implement at least the following APIs (additional APIs are welcome):
- POST /login – Simulate a user login event
- POST /launchGame – Record a game launch event
- POST /play – Record a gameplay session
- GET /missions – Get the missions list including progress.

### 3️⃣ You are required to implement at least the following database tables:
- **users** – User information
- **games** – Game metadata
- **games_play_record** – Game play records
- **missions** – Track user mission progress and reward status

### 🟩 You are encouraged to design additional tables or services as needed to support a clean and maintainable architecture.

⸻

🧪 Bonus (Optional)
- Use Spring Cache abstraction or RedisTemplate encapsulation
- Apply proper error handling with meaningful status codes
- Define your own DTO and message format for RocketMQ
- Use consistent and modular code structure (controller, service, repository, config, etc.)
- Test case coverage: as much as possible

⸻

🐳 Environment Setup

Use the provided docker-compose.yaml file to start required services:

Service	Port  
MySQL	3306  
Redis	6379  
RocketMQ Namesrv	9876  
RocketMQ Broker	10911  
RocketMQ Console	8088  

To start the services:

```commandline
docker-compose up -d
```

MySQL credentials:
- User: taskuser
- Password: taskpass
- Database: taskdb

You may edit init.sql to create required tables automatically.

⸻

🚀 Getting Started

To run the application:

./mvn spring-boot:run

Make sure to update your application.yml with the proper connections for:
- spring.datasource.url
- spring.redis.host
- rocketmq.name-server

⸻

📤 Submission

Please submit a `public Github repository` that includes:
- ✅ Complete and executable source code
- ✅ README.md (this file)
- ✅ Any necessary setup or data scripts please add them in HELP.md
- ✅ Optional: Postman collection or curl samples  

⸻

📌 Notes
- Focus on API correctness, basic error handling, and proper use of each technology
- You may use tools like Vibe Coding / ChatGPT to assist, but please write and understand your own code
- The expected time to complete is around 3 hours

Good luck!

---

## 專案設計理念與功能說明

### 設計理念

本專案採用現代化的響應式（Reactive）架構，使用 **Java 21** 和 **Spring Boot 3**，整合 **Project Reactor**。設計概念是建立一個高效能、高擴展性且易於維護的非阻塞系統。

1.  **響應式與非阻塞**：
    *   整個應用程式從 API 層（WebFlux）到底層資料存取（R2DBC、Reactive Redis）均採用非阻塞 I/O 模型。可以讓系統能用較少的執行緒處理大量的併發請求，提升資源利用率和系統吞吐量。

2.  **事件驅動架構 (EDA)**：
    *   透過 **RocketMQ** 實現核心業務的解耦。使用者登入、遊戲啟動和遊玩等核心操作會作為事件發布到訊息佇列。任務中心作為消費者監聽這些事件，並非同步地更新任務進度。

3.  **領域驅動設計 (DDD) 分層**：
    *   將業務邏輯劃分為 `interfaces`（介面層）、`application`（應用層）、`domain`（領域層）和 `infrastructure`（基礎設施層）。

### 功能說明

本專案實現了一個使用者任務中心，主要功能如下：

1.  **使用者登入與自動註冊**：
    *   `POST /login`：當使用者登入時，如果使用者不存在，系統會自動為其建立新帳戶。同時，系統會記錄每日登入，並為符合資格（註冊30天內）的使用者發布登入事件。

2.  **遊戲事件處理**：
    *   `POST /launchGame`：記錄使用者啟動遊戲的行為，並發布遊戲啟動事件。
    *   `POST /play`：記錄使用者遊玩遊戲的得分和時長，並發布遊戲遊玩事件。

3.  **非同步任務進度更新**：
    *   系統中的 `GameLaunchEventConsumer`、`GamePlayEventConsumer` 和 `UserLoginEventConsumer` 會非同步地消費 RocketMQ 中的事件。
    *   收到事件後，`MissionCommandService` 會被觸發，根據事件內容更新相關任務（連續登入、啟動不同遊戲、遊玩遊戲得分）的進度。

4.  **幂等性處理**：
    *   所有事件消費者都實現了基於 Redis 的幂等性處理。透過 `setIfAbsent` 原子操作，確保即使 RocketMQ 發送重複訊息，業務邏輯也只會被執行一次，保證了資料的一致性。

5.  **任務查詢與獎勵**：
    *   `GET /missions`：提供 API 讓使用者查詢自己所有任務的當前進度。
    *   當所有任務都完成時，系統會自動為使用者增加 777 點數作為獎勵。

