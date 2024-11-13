-- When you update this file, make sure you update this file cicd/init/db/dragonfly_init.sql

-- Start of Transaction
START TRANSACTION;

-- Creating the table
CREATE TABLE `USER_BATCH_LOOKUP`
(
    `key`             VARCHAR(10) PRIMARY KEY NOT NULL CHECK (TRIM(`key`) != ''),
    `value`           VARCHAR(10) NOT NULL CHECK (TRIM(`value`) != '')
);

-- End of Transaction
COMMIT;

