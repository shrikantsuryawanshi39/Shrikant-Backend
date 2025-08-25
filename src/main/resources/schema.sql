-- USER TABLE
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ORGANIZATION TABLE
CREATE TABLE IF NOT EXISTS organization (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- USER_ORG TABLE
CREATE TABLE IF NOT EXISTS user_org (
    user_name VARCHAR(255) NOT NULL,
    org_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user FOREIGN KEY (user_name) REFERENCES "user"(email) ON DELETE CASCADE,
    CONSTRAINT fk_org FOREIGN KEY (org_id) REFERENCES organization(id) ON DELETE CASCADE
);

-- CLUSTER TABLE
CREATE TABLE IF NOT EXISTS cluster (
    id BIGSERIAL PRIMARY KEY,
    org_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_org_cluster_name UNIQUE (org_id, name)
);

-- CLUSTER ASSOCIATION TABLE
CREATE TABLE IF NOT EXISTS cluster_association (
    source_name VARCHAR NOT NULL,
    source_type VARCHAR NOT NULL CHECK (source_type IN ('USER', 'GROUP')),
    cluster_name VARCHAR(100) NOT NULL,
    org_id BIGINT NOT NULL,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_org FOREIGN KEY (org_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_cluster FOREIGN KEY (org_id, cluster_name) REFERENCES cluster(org_id, name) ON DELETE CASCADE,
    CONSTRAINT uq_source_cluster UNIQUE (source_name, source_type, cluster_name, org_id)
);

-- GROUP TABLE
CREATE TABLE IF NOT EXISTS "group" (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    org_id BIGINT NOT NULL,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_org FOREIGN KEY (org_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT unique_group_name_per_org UNIQUE (name, org_id)
);

-- USER_GROUP TABLE
CREATE TABLE IF NOT EXISTS user_group (
    user_name VARCHAR(255) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    org_id BIGINT NOT NULL,
    creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_group PRIMARY KEY (user_name, org_id, group_name),
    CONSTRAINT fk_user FOREIGN KEY (user_name) REFERENCES "user"(email) ON DELETE CASCADE,
    CONSTRAINT fk_group FOREIGN KEY (org_id, group_name) REFERENCES "group"(org_id, name) ON DELETE CASCADE
);