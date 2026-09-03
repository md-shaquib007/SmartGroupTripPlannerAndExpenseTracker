-- TripSync Database Schema
-- MySQL 8+

CREATE DATABASE IF NOT EXISTS tripsync CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tripsync;

-- Users
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    email_verified  BOOLEAN DEFAULT FALSE,
    reset_token     VARCHAR(100),
    reset_token_expires DATETIME,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Trips
CREATE TABLE trips (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    destination     VARCHAR(200) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    budget          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    currency        VARCHAR(10) DEFAULT 'INR',
    max_members     INT NOT NULL DEFAULT 20,
    description     TEXT,
    invite_code     VARCHAR(20) NOT NULL UNIQUE,
    leader_id       BIGINT NOT NULL,
    status          ENUM('PLANNING', 'ACTIVE', 'COMPLETED', 'ARCHIVED') DEFAULT 'PLANNING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Trip Members
CREATE TABLE trip_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    role            ENUM('LEADER', 'CO_LEADER', 'MEMBER') DEFAULT 'MEMBER',
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trip_user (trip_id, user_id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Expenses
CREATE TABLE expenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    category        ENUM('FOOD', 'FUEL', 'HOTEL', 'SHOPPING', 'TICKETS', 'TOLL', 'MEDICAL', 'MISCELLANEOUS') NOT NULL,
    paid_by         BIGINT NOT NULL,
    expense_date    DATE NOT NULL,
    notes           TEXT,
    receipt_url     VARCHAR(500),
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (paid_by) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Expense Splits
CREATE TABLE expense_splits (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    share_amount    DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_expense_user (expense_id, user_id)
);

-- Itineraries (day-wise)
CREATE TABLE itineraries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    day_number      INT NOT NULL,
    title           VARCHAR(200),
    itinerary_date  DATE,
    UNIQUE KEY uk_trip_day (trip_id, day_number),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Itinerary Items
CREATE TABLE itinerary_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id    BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    start_time      TIME,
    end_time        TIME,
    location        VARCHAR(200),
    sort_order      INT DEFAULT 0,
    status          ENUM('APPROVED', 'SUGGESTED', 'REJECTED') DEFAULT 'APPROVED',
    suggested_by    BIGINT,
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    FOREIGN KEY (suggested_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Checklist Items
CREATE TABLE checklist_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    item_name       VARCHAR(200) NOT NULL,
    category        VARCHAR(50),
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Checklist Assignments (who is bringing what)
CREATE TABLE checklist_assignments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    checklist_item_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    status          ENUM('BRINGING', 'NOT_BRINGING') NOT NULL,
    UNIQUE KEY uk_item_user (checklist_item_id, user_id),
    FOREIGN KEY (checklist_item_id) REFERENCES checklist_items(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Trusted Contacts
CREATE TABLE trusted_contacts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150),
    phone           VARCHAR(20) NOT NULL,
    relationship    VARCHAR(50),
    access_token    VARCHAR(100) NOT NULL UNIQUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Emergency Information
CREATE TABLE emergency_information (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL UNIQUE,
    hotel_details   TEXT,
    vehicle_details TEXT,
    driver_contact  VARCHAR(50),
    leader_contact  VARCHAR(50),
    emergency_numbers TEXT,
    live_location_url VARCHAR(500),
    medical_info    TEXT,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Memories
CREATE TABLE memories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    caption         TEXT,
    memory_date     DATE,
    location        VARCHAR(200),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Media Files
CREATE TABLE media_files (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    memory_id       BIGINT NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_type       ENUM('IMAGE', 'VIDEO') NOT NULL,
    file_name       VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE
);

-- Comments
CREATE TABLE comments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    memory_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Likes
CREATE TABLE likes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    memory_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_memory_user (memory_id, user_id),
    FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Notifications
CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    trip_id         BIGINT,
    type            VARCHAR(50) NOT NULL,
    message         TEXT NOT NULL,
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Activity Logs (Timeline)
CREATE TABLE activity_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    user_id         BIGINT,
    action_type     VARCHAR(50) NOT NULL,
    description     TEXT NOT NULL,
    metadata        JSON,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Favorite Destinations
CREATE TABLE favorite_destinations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    destination     VARCHAR(200) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Trip Ratings
CREATE TABLE trip_ratings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    rating          TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review          TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trip_user_rating (trip_id, user_id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for search and filters
CREATE INDEX idx_trips_destination ON trips(destination);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_leader ON trips(leader_id);
CREATE INDEX idx_expenses_trip ON expenses(trip_id);
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expense_splits_expense ON expense_splits(expense_id);
CREATE INDEX idx_trip_members_user ON trip_members(user_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_activity_trip ON activity_logs(trip_id, created_at);
CREATE INDEX idx_users_reset_token ON users(reset_token);
CREATE INDEX idx_memories_trip ON memories(trip_id);

