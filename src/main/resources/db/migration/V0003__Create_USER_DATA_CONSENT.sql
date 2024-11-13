-- When you update this file, make sure you update this file cicd/init/db/dragonfly_init.sql

-- Start of Transaction
START TRANSACTION;

-- Creating the table
CREATE TABLE `USER_DATA_CONSENT`
(
    user_id         int(11) PRIMARY KEY NOT NULL,
    is_consent      boolean NOT NULL,
    utc_created_at  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utc_revoked_at  datetime
);

-- End of Transaction
COMMIT;

