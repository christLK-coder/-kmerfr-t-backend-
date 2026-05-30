-- Activation extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- TABLE users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name     VARCHAR(150)        NOT NULL,
    phone         VARCHAR(20)         NOT NULL UNIQUE,
    email         VARCHAR(255)        UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    role          VARCHAR(20)         NOT NULL CHECK (role IN ('IMPORTER','DRIVER','ADMIN')),
    avatar_url    TEXT,
    is_verified   BOOLEAN             DEFAULT FALSE,
    is_active     BOOLEAN             DEFAULT TRUE,
    created_at    TIMESTAMPTZ         DEFAULT NOW(),
    updated_at    TIMESTAMPTZ         DEFAULT NOW()
);

-- TABLE trucks
CREATE TABLE trucks (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plate_number     VARCHAR(20)  NOT NULL UNIQUE,
    brand            VARCHAR(80),
    model            VARCHAR(80),
    capacity_tons    DECIMAL(6,2) NOT NULL,
    truck_type       VARCHAR(40)  NOT NULL CHECK (truck_type IN (
                         'FLATBED','TANKER','REFRIGERATED',
                         'CONTAINER_20','CONTAINER_40','TIPPER'
                     )),
    insurance_expiry DATE,
    docs_verified    BOOLEAN      DEFAULT FALSE,
    created_at       TIMESTAMPTZ  DEFAULT NOW()
);

-- TABLE missions
CREATE TABLE missions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    importer_id         UUID            NOT NULL REFERENCES users(id),
    driver_id           UUID            REFERENCES users(id),
    truck_id            UUID            REFERENCES trucks(id),
    origin_point        GEOGRAPHY(POINT, 4326) NOT NULL,
    destination_point   GEOGRAPHY(POINT, 4326) NOT NULL,
    origin_label        VARCHAR(255)    NOT NULL,
    destination_label   VARCHAR(255)    NOT NULL,
    distance_km         DECIMAL(8,2),
    cargo_description   TEXT            NOT NULL,
    cargo_weight_tons   DECIMAL(6,2)    NOT NULL,
    cargo_type          VARCHAR(40)     NOT NULL CHECK (cargo_type IN (
                            'GENERAL','DANGEROUS','PERISHABLE',
                            'OVERSIZED','LIQUID','CONTAINER'
                        )),
    special_instructions TEXT,
    total_price         DECIMAL(12,2)   NOT NULL,
    commission_rate     DECIMAL(4,3)    DEFAULT 0.075,
    commission_amount   DECIMAL(12,2)   GENERATED ALWAYS AS
                            (total_price * commission_rate) STORED,
    driver_payout       DECIMAL(12,2)   GENERATED ALWAYS AS
                            (total_price - total_price * commission_rate) STORED,
    payment_status      VARCHAR(20)     DEFAULT 'PENDING' CHECK (payment_status IN (
                            'PENDING','ESCROWED','RELEASED','REFUNDED','DISPUTED'
                        )),
    payment_method      VARCHAR(20)     CHECK (payment_method IN (
                            'MTN_MOMO','ORANGE_MONEY','STRIPE','PAYPAL'
                        )),
    payment_reference   VARCHAR(255),
    status              VARCHAR(20)     DEFAULT 'OPEN' CHECK (status IN (
                            'OPEN','ASSIGNED','IN_TRANSIT',
                            'DELIVERED','CANCELLED','DISPUTED'
                        )),
    qr_delivery_token   VARCHAR(255)    UNIQUE,
    qr_scanned_at       TIMESTAMPTZ,
    pickup_scheduled_at TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     DEFAULT NOW()
);

CREATE INDEX idx_missions_origin    ON missions USING GIST(origin_point);
CREATE INDEX idx_missions_dest      ON missions USING GIST(destination_point);
CREATE INDEX idx_missions_status    ON missions(status);
CREATE INDEX idx_missions_driver    ON missions(driver_id);

-- TABLE road_hazards
CREATE TABLE road_hazards (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reported_by     UUID            NOT NULL REFERENCES users(id),
    mission_id      UUID            REFERENCES missions(id),
    location        GEOGRAPHY(POINT, 4326) NOT NULL,
    altitude_m      DECIMAL(8,2),
    accuracy_m      DECIMAL(6,2),
    shock_magnitude DECIMAL(8,4)    NOT NULL,
    shock_axis      VARCHAR(5)      DEFAULT 'Z',
    speed_kmh       DECIMAL(5,2),
    severity        VARCHAR(10)     NOT NULL CHECK (severity IN (
                        'LOW','MEDIUM','HIGH','CRITICAL'
                    )),
    hazard_type     VARCHAR(30)     DEFAULT 'POTHOLE' CHECK (hazard_type IN (
                        'POTHOLE','CRACK','BUMP','FLOODING',
                        'LANDSLIDE','BROKEN_ROAD'
                    )),
    confirmation_count INTEGER       DEFAULT 1,
    is_validated    BOOLEAN         DEFAULT FALSE,
    is_repaired     BOOLEAN         DEFAULT FALSE,
    recorded_at     TIMESTAMPTZ     NOT NULL,
    synced_at       TIMESTAMPTZ     DEFAULT NOW(),
    was_offline     BOOLEAN         DEFAULT FALSE
);

CREATE INDEX idx_road_hazards_location ON road_hazards USING GIST(location);
CREATE INDEX idx_road_hazards_severity ON road_hazards(severity);
CREATE INDEX idx_road_hazards_recorded ON road_hazards(recorded_at);

-- TABLE telemetry_positions
CREATE TABLE telemetry_positions (
    id          BIGSERIAL PRIMARY KEY,
    mission_id  UUID        NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    driver_id   UUID        NOT NULL REFERENCES users(id),
    position    GEOGRAPHY(POINT, 4326) NOT NULL,
    speed_kmh   DECIMAL(5,2),
    heading_deg DECIMAL(6,2),
    accuracy_m  DECIMAL(6,2),
    recorded_at TIMESTAMPTZ NOT NULL,
    was_offline BOOLEAN     DEFAULT FALSE
);

CREATE INDEX idx_telemetry_mission   ON telemetry_positions(mission_id);
CREATE INDEX idx_telemetry_position  ON telemetry_positions USING GIST(position);
CREATE INDEX idx_telemetry_time      ON telemetry_positions(recorded_at DESC);

-- TABLE mission_documents
CREATE TABLE mission_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id      UUID        NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    uploaded_by     UUID        NOT NULL REFERENCES users(id),
    doc_type        VARCHAR(30) NOT NULL CHECK (doc_type IN (
                        'LETTRE_VOITURE','BON_SORTIE','FACTURE',
                        'INSURANCE','ID_CARD','PERMIT','OTHER'
                    )),
    file_url        TEXT        NOT NULL,
    ocr_raw_text    TEXT,
    ocr_confidence  DECIMAL(5,2),
    is_verified     BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- TABLE alerts
CREATE TABLE alerts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id       UUID        NOT NULL REFERENCES users(id),
    mission_id      UUID        REFERENCES missions(id),
    alert_type      VARCHAR(20) NOT NULL CHECK (alert_type IN (
                        'DISTRESS','BREAKDOWN','ACCIDENT',
                        'DELAY','ROUTE_CHANGE','SYSTEM'
                    )),
    location        GEOGRAPHY(POINT, 4326),
    message         TEXT,
    sms_sent        BOOLEAN     DEFAULT FALSE,
    sms_reference   VARCHAR(100),
    is_resolved     BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- TABLE reviews
CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mission_id      UUID        NOT NULL REFERENCES missions(id),
    reviewer_id     UUID        NOT NULL REFERENCES users(id),
    reviewed_id     UUID        NOT NULL REFERENCES users(id),
    rating          SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(mission_id, reviewer_id)
);
