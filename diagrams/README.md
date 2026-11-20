# Arena Hub - System Diagrams

This directory contains standalone Mermaid source files (`.mmd`) and rendered previews for the Arena Hub platform report.

## Table of Contents
1. [System Architecture](#1-system-architecture) (`system-architecture.mmd`)
2. [Use Case Diagram](#2-use-case-diagram) (`use-case-diagram.mmd`)
3. [Class Diagram](#3-class-diagram) (`class-diagram.mmd`)
4. [Activity Diagram](#4-activity-diagram) (`activity-diagram.mmd`)
5. [Entity-Relationship Diagram](#5-entity-relationship-diagram) (`er-diagram.mmd`)

---

## 1. System Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer (Web Browser)"]
        UI["Thymeleaf SSR Views + Bootstrap 5"]
        JS["Client Scripts (Chart.js, QRCode.js, html5-qrcode)"]
        POLL["Status Board Polling (/status-board/data)"]
    end

    subgraph Security["Security & Web Layer (Spring Security + Spring MVC)"]
        SEC["SecurityConfig (RBAC: STUDENT / STAFF / ADMIN, BCrypt, CSRF)"]
        CTRL["Feature Controllers (auth, catalog, booking, loyalty, elo, tournament, admin, audit)"]
        API["JSON & Export Endpoints (/status-board/data, /admin/reports/export.csv)"]
    end

    subgraph Service["Business Logic & Background Processing Layer"]
        SVC["Domain Services (Transactional + Pessimistic Locking)"]
        PRICE["PricingService (Peak / Off-Peak / Member Rules)"]
        ELO["EloService (Dynamic K=32/16 & Badge Evaluation)"]
        SCHED["BookingStatusScheduler (Auto CONFIRMED -> ACTIVE -> COMPLETED)"]
        AUD["AuditService (JSON Old/New State Trail)"]
    end

    subgraph Data["Persistence & Database Layer"]
        JPA["Spring Data JPA / Hibernate (ddl-auto=validate)"]
        JDBC["JdbcTemplate (SQL Reporting Views)"]
        FLY["Flyway Schema Migrations (V001 to V016)"]
        DB[("MySQL 8.0 Database (18 Tables + 3 Views + CHECK/UNIQUE Constraints)")]
    end

    UI --> SEC
    JS --> SEC
    POLL --> SEC
    SEC --> CTRL
    SEC --> API

    CTRL --> SVC
    API --> SVC
    SVC --> PRICE
    SVC --> ELO
    SVC --> AUD
    SCHED --> JPA

    SVC --> JPA
    SVC --> JDBC
    AUD --> JPA
    FLY --> DB
    JPA --> DB
    JDBC --> DB
```

---

## 2. Use Case Diagram

```mermaid
flowchart LR
    Student(["Student / Player"])
    Staff(["Staff / Desk Operator"])
    Admin(["Admin / Arena Manager"])
    Scheduler(["System Scheduler"])

    subgraph ArenaHub["Arena Hub System"]
        UC1(["Register & Login"])
        UC2(["Browse Stations & Availability Heatmap"])
        UC3(["Book / Cancel / Rebook 1-Hour Slot"])
        UC4(["View Booking QR Code"])
        UC5(["Join Waitlist & Receive Notification"])
        UC6(["View Points Ledger & Redeem Rewards"])
        UC7(["Record Ranked Match & View Leaderboard/Badges"])
        UC8(["Join Tournament, Report Score & Raise Dispute"])

        UC9(["Scan QR / Manual Desk Check-in"])
        UC10(["Monitor Live Status Board"])

        UC11(["Manage Stations & Games Catalog"])
        UC12(["Configure Dynamic Pricing Rules"])
        UC13(["Manage Rewards & Stock"])
        UC14(["Create Tournament, Generate Bracket & Resolve Disputes"])
        UC15(["Ban / Unban Users & Reset Points"])
        UC16(["View Revenue Reports & Export CSV"])
        UC17(["Inspect Audit Logs"])

        UC18(["Auto-Transition Booking Statuses"])
    end

    Student --> UC1
    Student --> UC2
    Student --> UC3
    Student --> UC4
    Student --> UC5
    Student --> UC6
    Student --> UC7
    Student --> UC8
    Student --> UC10

    Staff --> UC1
    Staff --> UC2
    Staff --> UC9
    Staff --> UC10
    Staff --> UC7
    Staff --> UC8

    Admin --> UC1
    Admin --> UC10
    Admin --> UC11
    Admin --> UC12
    Admin --> UC13
    Admin --> UC14
    Admin --> UC15
    Admin --> UC16
    Admin --> UC17

    Scheduler --> UC18
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class Role {
        +Long id
        +String name
    }

    class User {
        +Long id
        +String email
        +String passwordHash
        +String fullName
        +String phone
        +Boolean isActive
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Station {
        +Long id
        +String stationNumber
        +String stationType
        +String status
        +BigDecimal hourlyRate
        +LocalDateTime createdAt
    }

    class Game {
        +Long id
        +String title
        +String genre
        +String platform
        +Integer minAge
        +Boolean isActive
    }

    class StationGame {
        +Long id
        +LocalDateTime installedAt
    }

    class Booking {
        +Long id
        +LocalDateTime startTime
        +LocalDateTime endTime
        +String status
        +BigDecimal totalPrice
        +String checkinCode
        +LocalDateTime checkedInAt
        +LocalDateTime createdAt
    }

    class WaitlistEntry {
        +Long id
        +LocalDate requestedDate
        +Integer requestedHour
        +String status
        +LocalDateTime notifiedAt
    }

    class PricingRule {
        +Long id
        +String ruleName
        +String stationType
        +Integer startHour
        +Integer endHour
        +String rateType
        +BigDecimal multiplier
        +Boolean isActive
    }

    class Reward {
        +Long id
        +String name
        +String description
        +Integer pointsCost
        +Integer stockQuantity
        +Boolean isActive
    }

    class PointsLedger {
        +Long id
        +Integer pointsChange
        +String transactionType
        +String description
        +LocalDateTime createdAt
    }

    class Tournament {
        +Long id
        +String name
        +String format
        +String status
        +Integer maxParticipants
        +BigDecimal entryFee
        +BigDecimal prizePool
        +LocalDateTime startTime
        +LocalDateTime endTime
    }

    class TournamentPlayer {
        +Long id
        +Integer seed
        +Integer finalRank
        +String status
        +LocalDateTime registeredAt
    }

    class Match {
        +Long id
        +Integer roundNumber
        +Integer bracketPosition
        +String matchType
        +String status
        +LocalDateTime scheduledAt
        +LocalDateTime completedAt
    }

    class MatchResult {
        +Long id
        +String outcome
        +Integer score
        +Integer ratingChange
        +Integer rankPlacement
    }

    class PlayerRating {
        +Long id
        +Integer currentRating
        +Integer matchesPlayed
        +LocalDateTime lastUpdatedAt
    }

    class Badge {
        +Long id
        +String code
        +String name
        +String description
        +String icon
    }

    class UserBadge {
        +Long id
        +LocalDateTime earnedAt
    }

    class AuditLog {
        +Long id
        +String action
        +String entityName
        +Long entityId
        +String oldValue
        +String newValue
        +String ipAddress
        +LocalDateTime createdAt
    }

    Role "1" <-- "*" User : assigned
    Station "1" <-- "*" StationGame
    Game "1" <-- "*" StationGame

    User "1" <-- "*" Booking
    Station "1" <-- "*" Booking
    User "1" <-- "*" WaitlistEntry
    Station "1" <-- "*" WaitlistEntry

    User "1" <-- "*" PointsLedger
    Booking "0..1" <-- "*" PointsLedger
    Reward "0..1" <-- "*" PointsLedger

    Game "1" <-- "*" Tournament
    Tournament "1" <-- "*" TournamentPlayer
    User "1" <-- "*" TournamentPlayer

    Game "1" <-- "*" Match
    Tournament "0..1" <-- "*" Match
    Match "0..1" <-- "*" Match : nextMatch
    Match "1" <-- "*" MatchResult
    User "1" <-- "*" MatchResult

    User "1" <-- "*" PlayerRating
    Game "1" <-- "*" PlayerRating

    User "1" <-- "*" UserBadge
    Badge "1" <-- "*" UserBadge

    User "0..1" <-- "*" AuditLog : actor
```

---

## 4. Activity Diagram

```mermaid
flowchart TD
    Start(["Start: Player Selects Station & 1-Hour Slot"]) --> CheckAvail{"Is Slot Available?"}

    CheckAvail -- No --> OfferWL["Prompt to Join Waitlist"]
    OfferWL --> JoinWL["Insert WaitlistEntry (status = WAITING)"]
    JoinWL --> WaitCancel["Wait for Existing Booking Cancellation"]
    WaitCancel --> NotifyWL["Auto-Promote First Waiter (status = NOTIFIED)"]
    NotifyWL --> EndWL(["End: User Notified to Book"])

    CheckAvail -- Yes --> CalcPrice["PricingService Evaluates Active Rules (Peak / Off-Peak / Member)"]
    CalcPrice --> LockTx["Begin Transaction & Attempt Insert (DB Unique Active Slot Guard)"]
    LockTx --> Conflict{"Slot Conflict Detected?"}
    Conflict -- Yes --> Reject["Return SlotAlreadyBookedException"]
    Reject --> EndErr(["End: Prompt Alternate Slot"])

    Conflict -- No --> GenCode["Generate Unique 10-Char Check-in Code & Save Booking (CONFIRMED)"]
    GenCode --> AwardPts["Credit Loyalty Points in PointsLedger (EARNED_BOOKING)"]
    AwardPts --> AuditBook["Write AuditLog Entry"]
    AuditBook --> ActionChoice{"Before Session Starts"}

    ActionChoice -- Player Cancels --> CancelTx["Set Status = CANCELLED (Frees active_slot_start)"]
    CancelTx --> TriggerWL["WaitlistService Notifies Next Waiting User"]
    TriggerWL --> EndCanc(["End: Booking Cancelled"])

    ActionChoice -- Player Arrives --> ScanQR["Staff Scans QR Code or Enters Code at /checkin"]
    ScanQR --> MarkCI["Validate Code & Set checked_in_at Timestamp"]
    MarkCI --> AutoActive["BookingStatusScheduler Transitions CONFIRMED -> ACTIVE"]
    AutoActive --> PlaySession["Session Displayed on Live /status-board with Countdown"]
    PlaySession --> AutoComplete["BookingStatusScheduler Transitions ACTIVE -> COMPLETED at end_time"]
    AutoComplete --> EndDone(["End: Session Completed"])
```

---

## 5. Entity-Relationship Diagram

```mermaid
erDiagram
    roles {
        BIGINT id PK
        VARCHAR name UK
    }

    users {
        BIGINT id PK
        BIGINT role_id FK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR full_name
        VARCHAR phone UK
        BOOLEAN is_active
        DATETIME created_at
        DATETIME updated_at
    }

    games {
        BIGINT id PK
        VARCHAR title UK
        VARCHAR genre
        VARCHAR platform
        INT min_age
        BOOLEAN is_active
        DATETIME created_at
    }

    stations {
        BIGINT id PK
        VARCHAR station_number UK
        VARCHAR station_type
        VARCHAR status
        DECIMAL hourly_rate
        DATETIME created_at
    }

    station_games {
        BIGINT id PK
        BIGINT station_id FK
        BIGINT game_id FK
        DATETIME installed_at
    }

    bookings {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT station_id FK
        DATETIME start_time
        DATETIME end_time
        VARCHAR status
        DECIMAL total_price
        DATETIME active_slot_start
        VARCHAR checkin_code UK
        DATETIME checked_in_at
        DATETIME created_at
    }

    waitlist {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT station_id FK
        DATE requested_date
        TINYINT requested_hour
        VARCHAR status
        DATETIME created_at
        DATETIME notified_at
    }

    pricing_rules {
        BIGINT id PK
        VARCHAR rule_name
        VARCHAR station_type
        TINYINT start_hour
        TINYINT end_hour
        VARCHAR rate_type
        DECIMAL multiplier
        BOOLEAN is_active
        DATETIME created_at
    }

    rewards {
        BIGINT id PK
        VARCHAR name UK
        TEXT description
        INT points_cost
        INT stock_quantity
        BOOLEAN is_active
        DATETIME created_at
    }

    points_ledger {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT booking_id FK
        BIGINT reward_id FK
        INT points_change
        VARCHAR transaction_type
        VARCHAR description
        DATETIME created_at
    }

    audit_logs {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR action
        VARCHAR entity_name
        BIGINT entity_id
        JSON old_value
        JSON new_value
        VARCHAR ip_address
        DATETIME created_at
    }

    tournaments {
        BIGINT id PK
        BIGINT game_id FK
        VARCHAR name
        VARCHAR format
        VARCHAR status
        INT max_participants
        DECIMAL entry_fee
        DECIMAL prize_pool
        DATETIME start_time
        DATETIME end_time
        DATETIME created_at
    }

    tournament_players {
        BIGINT id PK
        BIGINT tournament_id FK
        BIGINT user_id FK
        INT seed
        INT final_rank
        VARCHAR status
        DATETIME registered_at
    }

    player_ratings {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT game_id FK
        INT current_rating
        INT matches_played
        DATETIME last_updated_at
    }

    matches {
        BIGINT id PK
        BIGINT game_id FK
        BIGINT tournament_id FK
        BIGINT next_match_id FK
        INT round_number
        INT bracket_position
        VARCHAR match_type
        VARCHAR status
        DATETIME scheduled_at
        DATETIME completed_at
    }

    match_results {
        BIGINT id PK
        BIGINT match_id FK
        BIGINT user_id FK
        VARCHAR outcome
        INT score
        INT rating_change
        INT rank_placement
        DATETIME created_at
    }

    badges {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        VARCHAR description
        VARCHAR icon
    }

    user_badges {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT badge_id FK
        DATETIME earned_at
    }

    roles ||--o{ users : "has"
    stations ||--o{ station_games : "installs"
    games ||--o{ station_games : "installed_on"

    users ||--o{ bookings : "makes"
    stations ||--o{ bookings : "reserved_in"
    users ||--o{ waitlist : "joins"
    stations ||--o{ waitlist : "queued_for"

    users ||--o{ points_ledger : "owns"
    bookings ||--o{ points_ledger : "earns_from"
    rewards ||--o{ points_ledger : "redeemed_in"

    users ||--o{ audit_logs : "performs"

    games ||--o{ tournaments : "hosts"
    tournaments ||--o{ tournament_players : "registers"
    users ||--o{ tournament_players : "participates"

    games ||--o{ matches : "played_in"
    tournaments ||--o{ matches : "contains"
    matches ||--o{ matches : "advances_to"
    matches ||--o{ match_results : "records"
    users ||--o{ match_results : "achieves"

    users ||--o{ player_ratings : "holds"
    games ||--o{ player_ratings : "rated_in"

    users ||--o{ user_badges : "earns"
    badges ||--o{ user_badges : "awarded_as"
```
