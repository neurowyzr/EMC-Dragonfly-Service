
CREATE DATABASE IF NOT EXISTS `nw-apollo-dragon-service-db-test` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;
USE `nw-apollo-dragon-service-db-test`;

-- Creating the table
CREATE TABLE sample
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           TEXT NOT NULL,
    utc_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS `USER_SURVEYS`;
CREATE TABLE `USER_SURVEYS`
(
    session_id        varchar(100) PRIMARY KEY NOT NULL,
    user_id           int(11) NOT NULL,
    survey_selections json,
    utc_created_at    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS `USER_DATA_CONSENT`;
CREATE TABLE `USER_DATA_CONSENT`
(
  user_id         int(11) PRIMARY KEY NOT NULL,
  is_consent      boolean NOT NULL,
  utc_created_at  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  utc_revoked_at  datetime
);

DROP TABLE IF EXISTS `USER_BATCH_LOOKUP`;
CREATE TABLE `USER_BATCH_LOOKUP`
(
    `key`             VARCHAR(10) PRIMARY KEY NOT NULL CHECK (TRIM(`key`) != ''),
    `value`           VARCHAR(10) NOT NULL CHECK (TRIM(`value`) != '')
);
