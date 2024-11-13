-- When you update this file, make sure you update this file cicd/init/db/dragonfly_init.sql

-- Start of Transaction
START TRANSACTION;

-- Creating the table
CREATE TABLE `USER_SURVEYS`
(
    session_id        varchar(100) PRIMARY KEY NOT NULL,
    user_id           int(11) NOT NULL,
    survey_selections json,
    utc_created_at    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- End of Transaction
COMMIT;

