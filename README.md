# 🎮 Arena Hub

**Arena Hub** is a full-stack esports arena and gaming lounge management platform designed for university campuses, gaming cafes, and competitive gaming centers. It centralizes station reservations, dynamic pricing, tournament bracket management, player ELO rankings, loyalty rewards, and front-desk QR check-ins into a unified, responsive web application.

---

## 🌟 Key Features

### 🖥️ Station Booking & Availability
- **Interactive Reservation Wizard**: Book 1-hour slots across gaming stations (High-End PC VIP, Standard PC, PS5, Racing Simulators).
- **Timeline Heatmap**: Visual real-time availability grid showing occupied, reserved, and open slots.
- **Dynamic Pricing**: Automated pricing multipliers based on time of day (Morning Off-Peak discounts, Evening Peak rates, and Member discounts).
- **Automated Waitlists**: Join waitlist queues for fully booked slots; waiters are automatically notified if a reservation is cancelled.

### 📱 QR Check-In & Live Status Board
- **Contactless QR Check-In**: Instant QR code generation on player booking receipts with a dedicated front-desk camera scanner and manual code lookup for arena staff.
- **Live Arena Status Board**: Full-screen digital display showing real-time station occupancy, current player handles, and live countdown timers.
- **Automated Status Transitions**: Background scheduler automatically updates reservations from confirmed to active and completed sessions.

### 🏆 Esports Tournaments & Bracket Engine
- **Tournament Registration**: Cap-enforced player signups with fee tracking and prize pool allocation.
- **Automated Bracket Generation**: Single-elimination tournament tree generation with random or seeded matchups.
- **Match Progression & Disputes**: Players submit match scores, with built-in dispute flagging and administrative override capabilities.

### ⚔️ Competitive ELO Matchmaking & Gamification
- **Dynamic ELO Rating System**: Per-game skill ratings starting at 1200 with dynamic K-factors ($K=32$ for newcomers, $K=16$ for established players).
- **Player Leaderboards**: Real-time game rankings, match records (wins/losses/draws), and win streaks.
- **Achievement Badges**: Automated rewards for competitive milestones (First Win, 5-Win Streak, Top 10 Player).

### 🎁 Loyalty Points & Rewards Store
- **Points Ledger**: Transparent transaction history crediting points on completed bookings and tournament participations.
- **Rewards Redemption Store**: Redeem points for free gaming passes, branded merchandise, and cafe snacks with strict inventory race condition protection.

### 📊 Admin Operations & Analytics
- **Business KPI Dashboard**: Revenue summaries, daily booking counts, and active station utilization metrics.
- **Analytics & CSV Export**: Interactive charts for daily revenue and peak hours with one-click streaming CSV exports.
- **User Governance & Audit Trails**: Role-based access control (`STUDENT`, `STAFF`, `ADMIN`), account status management, and complete JSON audit logs for administrative actions.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Java 17, Spring Boot 3.5.x |
| **Security & Auth** | Spring Security 6 (BCrypt password hashing, RBAC, CSRF protection) |
| **Persistence & ORM** | Spring Data JPA, Hibernate (`ddl-auto=validate`), MySQL 8.0 |
| **Database Migrations** | Flyway (Append-only versioned migrations V001–V016) |
| **Frontend UI** | Spring MVC, Thymeleaf, Bootstrap 5, Bootstrap Icons |
| **Client-Side Libraries** | Chart.js (analytics), QRCode.js (ticket generation), html5-qrcode (desk camera scanner) |

---

## 📐 System Architecture & Diagrams

Comprehensive system diagrams and architectural models are available in the [`diagrams/`](diagrams/README.md) directory:

- 🏛️ **[System Architecture](diagrams/system-architecture.mmd)**: Layered client, security, domain service, and persistence architecture.
- 👤 **[Use Case Diagram](diagrams/use-case-diagram.mmd)**: Actor interactions across Student, Staff, and Admin roles.
- 🧩 **[Class Diagram](diagrams/class-diagram.mmd)**: JPA entity models, field definitions, and domain relationships.
- 🔄 **[Activity Diagram](diagrams/activity-diagram.mmd)**: End-to-end booking, dynamic pricing, waitlist, and check-in lifecycle.
- 🗄️ **[Entity-Relationship (ER) Diagram](diagrams/er-diagram.mmd)**: Full database schema detailing all 18 tables, relationships, and analytical views.

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: 17 or higher
- **Apache Maven**: 3.8+
- **MySQL Server**: 8.0.16 or higher

### 1. Database Setup
Create a dedicated UTF-8 MySQL database:
```sql
CREATE DATABASE arenahub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### 2. Environment Variables
Configure your database credentials (or define them in `src/main/resources/application.yml`):
```bash
# Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/arenahub?useSSL=false&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"

# Linux / macOS
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/arenahub?useSSL=false&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="your_password"
```

### 3. Run the Application
Start the Spring Boot server (Flyway will automatically execute migrations `V001` through `V016` on initial boot):
```bash
mvn spring-boot:run
```
Once started, open your browser and navigate to:
```
http://localhost:8080
```

---

## 👥 Default Demo Accounts

The database comes pre-seeded with sample user accounts for all three system roles:

| Role | Email | Password | Primary Permissions |
|---|---|---|---|
| **Admin** | `admin@arenahub.com` | `admin123` | Full system control, reports, tournament brackets, pricing rules, user governance |
| **Staff** | `staff@arenahub.com` | `staff123` | Front-desk check-in desk, station status monitoring, tournament score recording |
| **Student** | `student@arenahub.com` | `student123` | Booking stations, joining waitlists, playing matches, rewards redemption |

---

## 🏷️ Database Migrations (V001 – V016)

All database evolution is managed through immutable Flyway SQL migrations:

- `V001__core_users_roles.sql`: User authentication tables and role assignments (`roles`, `users`).
- `V002__seed_demo_users.sql`: Seed data for initial demo user accounts.
- `V003__catalog.sql`: Hardware stations, game titles, and station installations (`stations`, `games`, `station_games`).
- `V004__bookings.sql`: Reservation management with active slot conflict protection (`bookings`).
- `V005__rewards.sql`: Loyalty rewards catalog and inventory stock (`rewards`).
- `V006__points_ledger.sql`: Append-only loyalty transaction ledger (`points_ledger`).
- `V007__audit_logs.sql`: Administrative audit logs with before/after JSON state tracking (`audit_logs`).
- `V008__tournaments.sql`: Esports tournament definitions and player rosters (`tournaments`, `tournament_players`).
- `V009__elo_matches.sql`: Competitive matches, scores, and ELO ratings (`matches`, `match_results`, `player_ratings`).
- `V010__bookings_checks.sql`: Business integrity constraints on 1-hour durations and top-of-the-hour scheduling.
- `V011__waitlist.sql`: Reservation queue system for booked station slots (`waitlist`).
- `V012__pricing_rules.sql`: Peak, off-peak, and member pricing multipliers (`pricing_rules`).
- `V013__player_match_stats_view.sql`: Analytical view summarizing player match win/loss rates (`player_match_stats`).
- `V014__reporting_views.sql`: Aggregated views for station revenues and peak hours (`daily_revenue`, `peak_hours`).
- `V015__badges.sql`: Competitive badge catalog and user achievements (`badges`, `user_badges`).
- `V016__booking_checkin.sql`: Unique booking check-in codes and check-in timestamp tracking (`bookings`).
