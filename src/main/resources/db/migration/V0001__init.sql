-- When you update this file, make sure you update this file cicd/init/db/dragonfly_init.sql

-- Start of Transaction
START TRANSACTION;

-- Creating the table
CREATE TABLE sample
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           TEXT NOT NULL,
    utc_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- End of Transaction
COMMIT;

