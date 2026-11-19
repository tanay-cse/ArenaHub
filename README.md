# Arena Hub

## Prerequisites
- JDK 17+
- Maven
- MySQL 8.0.16+

## Database Setup
```sql
CREATE DATABASE arenahub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

## Environment Variables
- `SPRING_DATASOURCE_URL` (or `DB_URL`)
- `SPRING_DATASOURCE_USERNAME` (or `DB_USERNAME`)
- `SPRING_DATASOURCE_PASSWORD` (or `DB_PASSWORD`)

## Run Command
```bash
mvn spring-boot:run
```

## Demo Accounts
- Admin: `admin@arenahub.com` / `admin123`
- Staff: `staff@arenahub.com` / `staff123`
- Student: `student@arenahub.com` / `student123`

## Database Migrations
- `V001__core_users_roles.sql`: Core user accounts and roles tables (`roles`, `users`).
- `V002__seed_demo_users.sql`: Seed data for demo admin, staff, and student accounts.
- `V003__catalog.sql`: Catalog management for games, stations, and station game installations (`games`, `stations`, `station_games`).
- `V004__bookings.sql`: Booking reservations with virtual column double-booking protection (`bookings`).
- `V005__rewards.sql`: Loyalty rewards catalog (`rewards`).
- `V006__points_ledger.sql`: Loyalty points ledger history and transactions (`points_ledger`).
- `V007__audit_logs.sql`: Administrative audit logs with JSON change tracking (`audit_logs`).
- `V008__tournaments.sql`: Esports tournaments and player registrations (`tournaments`, `tournament_players`).
- `V009__elo_matches.sql`: ELO ratings, match tracking, and match results (`player_ratings`, `matches`, `match_results`).
