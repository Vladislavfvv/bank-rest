-- Script for creating PostgreSQL database
-- Execute this script as postgres superuser

-- Creating database
CREATE DATABASE bankcards
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

-- Connecting to bankcards database
\c bankcards;

-- Granting all privileges to postgres user for all operations
GRANT ALL PRIVILEGES ON DATABASE bankcards TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA public TO postgres;