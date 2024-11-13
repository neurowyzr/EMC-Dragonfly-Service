-- These tables are maintained in cognifyx-core

CREATE DATABASE IF NOT EXISTS `cognifyx-core-test` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;
USE `cognifyx-core-test`;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `ASSESSMENTS`
--

DROP TABLE IF EXISTS `ASSESSMENTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ASSESSMENTS`
(
    `id`              int(11)                                                           NOT NULL AUTO_INCREMENT,
    `instructions`    varchar(200) COLLATE utf8mb4_unicode_ci                                    DEFAULT NULL,
    `engagement_id`   int(11)                                                                    DEFAULT NULL,
    `name`            varchar(50) COLLATE utf8mb4_unicode_ci                            NOT NULL,
    `preview_url`     varchar(200) COLLATE utf8mb4_unicode_ci                                    DEFAULT NULL,
    `show_timer`      bit(1)                                                            NOT NULL DEFAULT b'1',
    `grading_profile` json                                                              NOT NULL,
    `rank`            int(11)                                                           NOT NULL,
    `grade_config`    json                                                                       DEFAULT NULL,
    `type`            enum ('NORMAL','PERSONAL_GOAL','VCAT') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`),
    KEY `engagement_id` (`engagement_id`),
    CONSTRAINT `engagement_id` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1774
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ASSESSMENT_QUESTIONS`
--

DROP TABLE IF EXISTS `ASSESSMENT_QUESTIONS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ASSESSMENT_QUESTIONS`
(
    `id`                 int(11)                                NOT NULL AUTO_INCREMENT,
    `question_family_id` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    `time`               int(11)                                NOT NULL,
    `assessment_id`      int(11)                                NOT NULL,
    `rank`               int(11)                                NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `aq_question_family_id_assessment_id` (`question_family_id`, `assessment_id`),
    KEY `question_family_id` (`question_family_id`),
    KEY `assessment_id` (`assessment_id`),
    CONSTRAINT `aq_question_family_id` FOREIGN KEY (`question_family_id`) REFERENCES `QUESTIONS` (`question_family_id`) ON DELETE CASCADE,
    CONSTRAINT `assessment_id` FOREIGN KEY (`assessment_id`) REFERENCES `ASSESSMENTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ASSESSMENT_RESPONSES`
--

DROP TABLE IF EXISTS `ASSESSMENT_RESPONSES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ASSESSMENT_RESPONSES`
(
    `id`                 int(11)                                NOT NULL AUTO_INCREMENT,
    `question_family_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `response_data`      json                                   NOT NULL,
    `timing_data`        json                                   NOT NULL,
    `engagement_id`      int(11)                                NOT NULL,
    `assessment_id`      int(11)                                NOT NULL,
    `user_id`            int(11)                                NOT NULL,
    `user_batch_id`      int(11)                                NOT NULL,
    `answered_at`        datetime(3)                            NOT NULL,
    `start_time`         datetime(3)                            NOT NULL,
    `duration`           bigint(20)                             NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ar_engagement_id` (`engagement_id`),
    KEY `ar_assessment_id` (`assessment_id`),
    KEY `ar_user_batch_id` (`user_batch_id`),
    KEY `ar_user_id` (`user_id`),
    KEY `ar_question_family_id` (`question_family_id`),
    CONSTRAINT `ar_assessment_id` FOREIGN KEY (`assessment_id`) REFERENCES `ASSESSMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ar_engagement_id` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ar_question_family_id` FOREIGN KEY (`question_family_id`) REFERENCES `QUESTIONS` (`question_family_id`) ON DELETE CASCADE,
    CONSTRAINT `ar_user_batch_id` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ar_user_id` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_EXECUTION`
--

DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION`
(
    `JOB_EXECUTION_ID`           bigint(20) NOT NULL,
    `VERSION`                    bigint(20)                               DEFAULT NULL,
    `JOB_INSTANCE_ID`            bigint(20) NOT NULL,
    `CREATE_TIME`                datetime   NOT NULL,
    `START_TIME`                 datetime                                 DEFAULT NULL,
    `END_TIME`                   datetime                                 DEFAULT NULL,
    `STATUS`                     varchar(10) COLLATE utf8mb4_unicode_ci   DEFAULT NULL,
    `EXIT_CODE`                  varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `EXIT_MESSAGE`               varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `LAST_UPDATED`               datetime                                 DEFAULT NULL,
    `JOB_CONFIGURATION_LOCATION` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`JOB_EXECUTION_ID`),
    KEY `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
    CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_EXECUTION_CONTEXT`
--

DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_CONTEXT`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_CONTEXT`
(
    `JOB_EXECUTION_ID`   bigint(20)                               NOT NULL,
    `SHORT_CONTEXT`      varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
    PRIMARY KEY (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_EXECUTION_PARAMS`
--

DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_PARAMS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_PARAMS`
(
    `JOB_EXECUTION_ID` bigint(20)                              NOT NULL,
    `TYPE_CD`          varchar(6) COLLATE utf8mb4_unicode_ci   NOT NULL,
    `KEY_NAME`         varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `STRING_VAL`       varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `DATE_VAL`         datetime                                DEFAULT NULL,
    `LONG_VAL`         bigint(20)                              DEFAULT NULL,
    `DOUBLE_VAL`       double                                  DEFAULT NULL,
    `IDENTIFYING`      char(1) COLLATE utf8mb4_unicode_ci      NOT NULL,
    KEY `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_EXECUTION_SEQ`
--

DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_SEQ`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_SEQ`
(
    `ID`         bigint(20)                         NOT NULL,
    `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_INSTANCE`
--

DROP TABLE IF EXISTS `BATCH_JOB_INSTANCE`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_INSTANCE`
(
    `JOB_INSTANCE_ID` bigint(20)                              NOT NULL,
    `VERSION`         bigint(20) DEFAULT NULL,
    `JOB_NAME`        varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `JOB_KEY`         varchar(32) COLLATE utf8mb4_unicode_ci  NOT NULL,
    PRIMARY KEY (`JOB_INSTANCE_ID`),
    UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`, `JOB_KEY`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_JOB_SEQ`
--

DROP TABLE IF EXISTS `BATCH_JOB_SEQ`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_SEQ`
(
    `ID`         bigint(20)                         NOT NULL,
    `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_STEP_EXECUTION`
--

DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION`
(
    `STEP_EXECUTION_ID`  bigint(20)                              NOT NULL,
    `VERSION`            bigint(20)                              NOT NULL,
    `STEP_NAME`          varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `JOB_EXECUTION_ID`   bigint(20)                              NOT NULL,
    `START_TIME`         datetime                                NOT NULL,
    `END_TIME`           datetime                                 DEFAULT NULL,
    `STATUS`             varchar(10) COLLATE utf8mb4_unicode_ci   DEFAULT NULL,
    `COMMIT_COUNT`       bigint(20)                               DEFAULT NULL,
    `READ_COUNT`         bigint(20)                               DEFAULT NULL,
    `FILTER_COUNT`       bigint(20)                               DEFAULT NULL,
    `WRITE_COUNT`        bigint(20)                               DEFAULT NULL,
    `READ_SKIP_COUNT`    bigint(20)                               DEFAULT NULL,
    `WRITE_SKIP_COUNT`   bigint(20)                               DEFAULT NULL,
    `PROCESS_SKIP_COUNT` bigint(20)                               DEFAULT NULL,
    `ROLLBACK_COUNT`     bigint(20)                               DEFAULT NULL,
    `EXIT_CODE`          varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `EXIT_MESSAGE`       varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `LAST_UPDATED`       datetime                                 DEFAULT NULL,
    PRIMARY KEY (`STEP_EXECUTION_ID`),
    KEY `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_STEP_EXECUTION_CONTEXT`
--

DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION_CONTEXT`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_CONTEXT`
(
    `STEP_EXECUTION_ID`  bigint(20)                               NOT NULL,
    `SHORT_CONTEXT`      varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
    PRIMARY KEY (`STEP_EXECUTION_ID`),
    CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BATCH_STEP_EXECUTION_SEQ`
--

DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION_SEQ`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_SEQ`
(
    `ID`         bigint(20)                         NOT NULL,
    `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CLIENTS`
--

DROP TABLE IF EXISTS `CLIENTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENTS`
(
    `id`            int(11)                                NOT NULL AUTO_INCREMENT,
    `name`          varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    `address`       varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_phone` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_email` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_name`  varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 59
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CYGNUS_EVENT`
--

DROP TABLE IF EXISTS `CYGNUS_EVENT`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CYGNUS_EVENT`
(
    `id`             int(11)      NOT NULL AUTO_INCREMENT,
    `message_type`   VARCHAR(100) NOT NULL,
    `message_id`     varchar(100) NOT NULL,
    `utc_created_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_type_message_id` (`message_type`, `message_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

DELIMITER ;;
/*!50003 CREATE */ /*!50017 DEFINER =`root`@`localhost`*/ /*!50003 TRIGGER before_insert_cygnus_events
    BEFORE INSERT
    ON CYGNUS_EVENT
    FOR EACH ROW
BEGIN
    IF NEW.utc_created_at IS NULL THEN
        SET NEW.utc_created_at = CURRENT_TIMESTAMP;
    END IF;
END */;;
DELIMITER ;

--
-- Table structure for table `EHA_DETAIL`
--

DROP TABLE IF EXISTS `EHA_DETAIL`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `EHA_DETAIL`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`         int(11)             NOT NULL,
    `user_batch_id`   int(11)             NOT NULL,
    `details`         json DEFAULT NULL,
    `test_session_id` bigint(20)          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `eha_user_account_session_unique` (`user_id`, `user_batch_id`, `test_session_id`) USING BTREE,
    KEY `eha_user_id_fk` (`user_id`),
    KEY `eha_user_batch_id_fk` (`user_batch_id`),
    KEY `eha_test_session_id_fk` (`test_session_id`),
    CONSTRAINT `eha_test_session_id_fk` FOREIGN KEY (`test_session_id`) REFERENCES `TEST_SESSION` (`id`) ON DELETE CASCADE,
    CONSTRAINT `eha_user_batch_id_fk` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `eha_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EMAIL_DETAILS`
--

DROP TABLE IF EXISTS `EMAIL_DETAILS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `EMAIL_DETAILS`
(
    `engagement_id` int(11)                                 NOT NULL,
    `host`          varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `port`          int(11)                                 NOT NULL,
    `username`      varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `password`      varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`engagement_id`),
    CONSTRAINT `ed_engagement_id` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ENGAGEMENTS`
--

DROP TABLE IF EXISTS `ENGAGEMENTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ENGAGEMENTS`
(
    `id`                        int(11)                                                 NOT NULL AUTO_INCREMENT,
    `client_id`                 int(11)                                                 NOT NULL,
    `billing_pax`               varchar(45) COLLATE utf8mb4_unicode_ci                  NOT NULL,
    `notes`                     varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `product_id`                int(11)                                                 NOT NULL,
    `status`                    varchar(45) COLLATE utf8mb4_unicode_ci                  NOT NULL,
    `tenant_token`              varchar(255) COLLATE utf8mb4_unicode_ci                 NOT NULL,
    `name`                      varchar(50) COLLATE utf8mb4_unicode_ci                  NOT NULL,
    `app_type`                  varchar(10) COLLATE utf8mb4_unicode_ci                  NOT NULL,
    `server_url`                varchar(100) COLLATE utf8mb4_unicode_ci                 NOT NULL,
    `start_date`                date                                                    NOT NULL,
    `end_date`                  date                                                    NOT NULL,
    `locale`                    enum ('ar_AE','ar_JO','ar_SY','hr_HR','fr_BE','es_PA','mt_MT','es_VE','bg','zh_TW','it','ko','uk','lv','da_DK','es_PR','vi_VN','en_US','sr_ME','sv_SE','es_BO','en_SG','ar_BH','pt','ar_SA','sk','ar_YE','hi_IN','ga','en_MT','fi_FI','et','sv','cs','sr_BA_#Latn','el','uk_UA','hu','fr_CH','in','es_AR','ar_EG','ja_JP_JP_#u-ca-japanese','es_SV','pt_BR','be','is_IS','cs_CZ','es','pl_PL','tr','ca_ES','sr_CS','ms_MY','hr','lt','es_ES','es_CO','bg_BG','sq','fr','ja','sr_BA','is','es_PY','de','es_EC','es_US','ar_SD','en','ro_RO','en_PH','ca','ar_TN','sr_ME_#Latn','es_GT','sl','ko_KR','el_CY','es_MX','ru_RU','es_HN','zh_HK','no_NO_NY','hu_HU','th_TH','ar_IQ','es_CL','fi','ar_MA','ga_IE','mk','tr_TR','et_EE','ar_QA','sr__#Latn','pt_PT','fr_LU','ar_OM','th','sq_AL','es_DO','es_CU','ar','ru','en_NZ','sr_RS','de_CH','es_UY','ms','el_GR','iw_IL','en_ZA','th_TH_TH_#u-nu-thai','hi','fr_FR','de_AT','nl','no_NO','en_AU','vi','nl_NL','fr_CA','lv_LV','de_LU','es_CR','ar_KW','sr','ar_LY','mt','it_CH','da','de_DE','ar_DZ','sk_SK','lt_LT','it_IT','en_IE','zh_SG','ro','en_CA','nl_BE','no','pl','zh_CN','ja_JP','de_GR','sr_RS_#Latn','iw','en_IN','ar_LB','es_NI','zh','mk_MK','be_BY','sl_SI','es_PE','in_ID','en_GB','ta') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `assessments_config`        json DEFAULT NULL,
    `type`                      enum ('PRIMARY','SECONDARY') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SECONDARY',
    `sub_type`                  enum ('NORMAL','HEALTH_SCREENING','E_PRESCRIPTION','HNA','EHA') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `content_config`            json DEFAULT NULL,
    `product_config`            json DEFAULT NULL,
    `decision_question_mapping` json DEFAULT NULL,
    `enable_magic_link`         tinyint(1)                                              NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `e_tenant_token_uk_idx` (`tenant_token`),
    KEY `client_id` (`client_id`),
    KEY `product_id` (`product_id`),
    FULLTEXT KEY `engagements_full_text_name_idx` (`name`),
    CONSTRAINT `client_id` FOREIGN KEY (`client_id`) REFERENCES `CLIENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `product_id` FOREIGN KEY (`product_id`) REFERENCES `PRODUCTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 178
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ENGAGEMENT_CONTENTS`
--

DROP TABLE IF EXISTS `ENGAGEMENT_CONTENTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ENGAGEMENT_CONTENTS`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `engagement_id` int(11)             NOT NULL,
    `content_id`    bigint(20) unsigned NOT NULL,
    `rank`          int(11)             NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ec_engagement_id_content_id` (`engagement_id`, `content_id`),
    UNIQUE KEY `ec_engagement_id_rank` (`engagement_id`, `rank`),
    KEY `fk_ec_engagement_id` (`engagement_id`),
    KEY `fk_ec_content_id` (`content_id`),
    CONSTRAINT `fk_ec_engagement_id` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EPISODE`
--

DROP TABLE IF EXISTS `EPISODE`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `EPISODE`
(
    `id`              int(11)                                 NOT NULL AUTO_INCREMENT,
    `user_id`         int(11)                                 NOT NULL,
    `episode_ref`     varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `test_session_id` bigint(20)                              NOT NULL,
    `source`          varchar(50) COLLATE utf8mb4_unicode_ci           DEFAULT NULL,
    `message_id`      varchar(50) COLLATE utf8mb4_unicode_ci           DEFAULT NULL,
    `is_invalidated`  tinyint(1)                              NOT NULL DEFAULT '0',
    `utc_start_date`  datetime                                         DEFAULT NULL,
    `utc_expiry_date` datetime                                         DEFAULT NULL,
    `utc_created_at`  datetime                                         DEFAULT NULL,
    `utc_updated_at`  datetime                                         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_source_user_id_episode_ref_test_session_id` (`source`, `user_id`, `episode_ref`, `test_session_id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `ul_test_session_id_fk` (`test_session_id`),
    KEY `idx_user_id_fk` (`user_id`) USING BTREE,
    CONSTRAINT `ul_test_session_id_fk` FOREIGN KEY (`test_session_id`) REFERENCES `TEST_SESSION` (`id`),
    CONSTRAINT `ul_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `NON_ASSESSMENTS`
--

DROP TABLE IF EXISTS `NON_ASSESSMENTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `NON_ASSESSMENTS`
(
    `id`          int(11)                                NOT NULL AUTO_INCREMENT,
    `name`        varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    `module_type` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PASSWORD_RESET_TOKENS`
--

DROP TABLE IF EXISTS `PASSWORD_RESET_TOKENS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PASSWORD_RESET_TOKENS`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `token`        varchar(250) NOT NULL,
    `email`        varchar(100) NOT NULL,
    `created_at`   datetime     NOT NULL,
    `expires_on`   datetime     NOT NULL,
    `refreshed_at` datetime DEFAULT NULL,
    `reset_at`     datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_p_token_idx` (`token`) USING BTREE,
    KEY `token_email_idx` (`email`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1 COMMENT ='The table which holds the tokens to reset the passsword after new user is added from the system and forgot password';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PERSONAL_GOALS`
--

DROP TABLE IF EXISTS `PERSONAL_GOALS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL_GOALS`
(
    `id`            int(11) unsigned                       NOT NULL AUTO_INCREMENT,
    `user_id`       int(11)                                NOT NULL,
    `user_batch_id` int(11)                                NOT NULL,
    `name`          varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `display_name`  varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `started_at`    datetime                               NOT NULL,
    `set_at`        datetime                               NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `pg_user_id_fk_idx` (`user_id`),
    KEY `pg_user_batch_id_fk_idx` (`user_batch_id`),
    CONSTRAINT `pg_user_batch_id_fk_idx` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `pg_user_id_fk_idx` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PERSONAL_GOAL_ASSESSMENT_RESPONSES`
--

DROP TABLE IF EXISTS `PERSONAL_GOAL_ASSESSMENT_RESPONSES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL_GOAL_ASSESSMENT_RESPONSES`
(
    `id`                 int(11)                                NOT NULL AUTO_INCREMENT,
    `goal_id`            int(11) unsigned                       NOT NULL,
    `question_family_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `response_data`      json                                   NOT NULL,
    `timing_data`        json                                   NOT NULL,
    `engagement_id`      int(11)                                NOT NULL,
    `assessment_id`      int(11)                                NOT NULL,
    `answered_at`        datetime                               NOT NULL,
    `start_time`         datetime                               NOT NULL,
    `duration`           bigint(20)                             NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `pgar_goal_assessment_question_uk_idx` (`goal_id`, `question_family_id`, `assessment_id`),
    KEY `pgar_engagement_id_fk_idx` (`engagement_id`) USING BTREE,
    KEY `pgar_assessment_id_fk_idx` (`assessment_id`) USING BTREE,
    KEY `pgar_question_family_id_fk_idx` (`question_family_id`) USING BTREE,
    KEY `pgar_goal_id_fk_idx` (`goal_id`) USING BTREE,
    CONSTRAINT `pgar_assessment_id_fk_idx` FOREIGN KEY (`assessment_id`) REFERENCES `ASSESSMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `pgar_engagement_id_fk_idx` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `pgar_goal_id_fk_idx` FOREIGN KEY (`goal_id`) REFERENCES `PERSONAL_GOALS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `pgar_question_family_id_fk_idx` FOREIGN KEY (`question_family_id`) REFERENCES `QUESTIONS` (`question_family_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PERSONAL_GOAL_OUTCOMES`
--

DROP TABLE IF EXISTS `PERSONAL_GOAL_OUTCOMES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL_GOAL_OUTCOMES`
(
    `id`      int(11) unsigned                                                            NOT NULL AUTO_INCREMENT,
    `goal_id` int(11) unsigned                                                            NOT NULL,
    `name`    varchar(50) COLLATE utf8mb4_unicode_ci                                      NOT NULL,
    `type`    enum ('ARTICLE','HABITS','NUTRITION','EXERCISE') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `pgo_goal_id_fk_idx` (`goal_id`) USING BTREE,
    CONSTRAINT `pgo_goal_id_fk_idx` FOREIGN KEY (`goal_id`) REFERENCES `PERSONAL_GOALS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PERSONAL_GOAL_USERS`
--

DROP TABLE IF EXISTS `PERSONAL_GOAL_USERS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL_GOAL_USERS`
(
    `id`           int(11) unsigned                                                            NOT NULL AUTO_INCREMENT,
    `content_id`   bigint(20)                                                                  NOT NULL,
    `content_type` enum ('ARTICLE','HABITS','NUTRITION','EXERCISE') COLLATE utf8mb4_unicode_ci NOT NULL,
    `goal_id`      int(11) unsigned                                                            NOT NULL,
    `presented_on` datetime                                                                    NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `pgu_goal_id_fk_idx` (`goal_id`) USING BTREE,
    CONSTRAINT `pgu_goal_id_fk_idx` FOREIGN KEY (`goal_id`) REFERENCES `PERSONAL_GOALS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PERSONAL_GOAL_USER_ACTIONS`
--

DROP TABLE IF EXISTS `PERSONAL_GOAL_USER_ACTIONS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PERSONAL_GOAL_USER_ACTIONS`
(
    `id`          int(10) unsigned NOT NULL AUTO_INCREMENT,
    `goal_id`     int(11) unsigned NOT NULL,
    `action_date` datetime         NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `pgua_goal_id_fk_idx` (`goal_id`),
    CONSTRAINT `pgua_goal_id_fk_idx` FOREIGN KEY (`goal_id`) REFERENCES `PERSONAL_GOALS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PRODUCTS`
--

DROP TABLE IF EXISTS `PRODUCTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PRODUCTS`
(
    `id`          int(11)                                NOT NULL AUTO_INCREMENT,
    `name`        varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    `description` varchar(550) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QUESTIONS`
--

DROP TABLE IF EXISTS `QUESTIONS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QUESTIONS`
(
    `id`                     int(11)                                                                                                                                                                                                                                                                                                   NOT NULL AUTO_INCREMENT,
    `question_family_id`     varchar(45) COLLATE utf8mb4_unicode_ci                                                                                                                                                                                                                                                                    NOT NULL,
    `question_type`          enum ('PVT','MCQ','TRAIL_MAKING','STROOP','INHIBITION','N_BACK','MEMORY_TILES','WORD_MEMORY','COLOR_SEQUENCE','ODD_ONE','SHAPE_SEARCH','DND','GRID_PATTERN','WORD_PAIR_MEMORY','ESSAY','GRID_SEARCH','NUMBER_RECALL','ISHIHARA','SHAPE_SEQUENCE','FOOD_COURT','PERSONAL_GOAL') COLLATE utf8mb4_unicode_ci NOT NULL,
    `question_configuration` json DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `question_family_id` (`question_family_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 462
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QUESTION_TAGS`
--

DROP TABLE IF EXISTS `QUESTION_TAGS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QUESTION_TAGS`
(
    `question_id` int(11) NOT NULL,
    `tag_id`      int(11) NOT NULL,
    KEY `qt_question_id` (`question_id`),
    KEY `qt_tag_id` (`tag_id`),
    CONSTRAINT `qt_question_id` FOREIGN KEY (`question_id`) REFERENCES `QUESTIONS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `qt_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `TAGS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RABBITMQ_PUBLISH_FAILURE`
--

DROP TABLE IF EXISTS `RABBITMQ_PUBLISH_FAILURE`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RABBITMQ_PUBLISH_FAILURE`
(
    `id`           int(11) unsigned                        NOT NULL AUTO_INCREMENT,
    `message_id`   varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `full_message` text COLLATE utf8mb4_unicode_ci         NOT NULL,
    `message_body` text COLLATE utf8mb4_unicode_ci         NOT NULL,
    `failure_date` datetime                                NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `REVINFO`
--

DROP TABLE IF EXISTS `REVINFO`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REVINFO`
(
    `REV`      int(11) NOT NULL AUTO_INCREMENT,
    `REVTSTMP` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`REV`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2110
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ROLES`
--

DROP TABLE IF EXISTS `ROLES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ROLES`
(
    `id`   int(11)                                NOT NULL AUTO_INCREMENT,
    `name` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO ROLES (name) VALUES ('ADMIN'), ('USER'), ('SCIENCE_TEAM'), ('PATIENT'), ('PROVIDER');
--
-- Table structure for table `SPRING_SESSION`
--

DROP TABLE IF EXISTS `SPRING_SESSION`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPRING_SESSION`
(
    `SESSION_ID`            char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
    `CREATION_TIME`         bigint(20)                          NOT NULL,
    `LAST_ACCESS_TIME`      bigint(20)                          NOT NULL,
    `MAX_INACTIVE_INTERVAL` int(11)                             NOT NULL,
    `PRINCIPAL_NAME`        varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`SESSION_ID`),
    KEY `SPRING_SESSION_IX1` (`LAST_ACCESS_TIME`),
    KEY `SPRING_SESSION_IX2` (`PRINCIPAL_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SPRING_SESSION_ATTRIBUTES`
--

DROP TABLE IF EXISTS `SPRING_SESSION_ATTRIBUTES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPRING_SESSION_ATTRIBUTES`
(
    `SESSION_ID`      char(36) COLLATE utf8mb4_unicode_ci     NOT NULL,
    `ATTRIBUTE_NAME`  varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `ATTRIBUTE_BYTES` blob                                    NOT NULL,
    PRIMARY KEY (`SESSION_ID`, `ATTRIBUTE_NAME`),
    KEY `SPRING_SESSION_ATTRIBUTES_IX1` (`SESSION_ID`),
    CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` FOREIGN KEY (`SESSION_ID`) REFERENCES `SPRING_SESSION` (`SESSION_ID`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TAGS`
--

DROP TABLE IF EXISTS `TAGS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TAGS`
(
    `id`   int(11)                                NOT NULL AUTO_INCREMENT,
    `name` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 153
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TEST_SESSION`
--

DROP TABLE IF EXISTS `TEST_SESSION`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TEST_SESSION`
(
    `id`                 bigint(20)                             NOT NULL AUTO_INCREMENT,
    `user_id`            int(11)                                NOT NULL,
    `user_batch_id`      int(11)                                NOT NULL,
    `engagement_id`      int(11)                                NOT NULL,
    `start_date`         datetime(3)                                     DEFAULT NULL,
    `end_date`           datetime(3)                                     DEFAULT NULL,
    `completed_at`       datetime(3)                                     DEFAULT NULL,
    `frequency`          varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    `test_session_order` int(11)                                NOT NULL,
    `uac_revision`       int(11)                                         DEFAULT NULL,
    `find_score`         json                                            DEFAULT NULL,
    `aggregate_score`    json                                            DEFAULT NULL,
    `mcq_v2`             json                                            DEFAULT NULL,
    `z_score`            json                                            DEFAULT NULL,
    `is_voided`          tinyint(1)                             NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `as_fk_user_id_idx` (`user_id`),
    KEY `as_fk_user_batch_id_idx` (`user_batch_id`),
    KEY `as_fk_engagement_id_idx` (`engagement_id`),
    CONSTRAINT `as_fk_engagement_id_idx` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `as_fk_user_batch_id_idx` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `as_fk_user_id_idx` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 13144
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `URLS`
--

DROP TABLE IF EXISTS `URLS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `URLS`
(
    `id`          int(10) unsigned                                                                               NOT NULL AUTO_INCREMENT,
    `url_pattern` varchar(250) COLLATE utf8mb4_unicode_ci                                                        NOT NULL,
    `http_method` enum ('GET','HEAD','POST','PUT','PATCH','DELETE','OPTIONS','TRACE') COLLATE utf8mb4_unicode_ci NOT NULL,
    `type`        enum ('REST','WEB') COLLATE utf8mb4_unicode_ci                                                 NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `u_uk_url_method_idx` (`url_pattern`, `http_method`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 457
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `URL_ROLES`
--

DROP TABLE IF EXISTS `URL_ROLES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `URL_ROLES`
(
    `url_id`  int(11) unsigned NOT NULL,
    `role_id` int(11)          NOT NULL,
    KEY `FK_URL_ROLES_URLS` (`url_id`),
    KEY `FK_URL_ROLES_ROLES` (`role_id`),
    CONSTRAINT `FK_URL_ROLES_ROLES` FOREIGN KEY (`role_id`) REFERENCES `ROLES` (`id`) ON DELETE CASCADE,
    CONSTRAINT `FK_URL_ROLES_URLS` FOREIGN KEY (`url_id`) REFERENCES `URLS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `URL_TAGS`
--

DROP TABLE IF EXISTS `URL_TAGS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `URL_TAGS`
(
    `id`   int(10) unsigned                       NOT NULL AUTO_INCREMENT,
    `name` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ut_name_idx` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 87
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `URL_TAG_CONNECTIONS`
--

DROP TABLE IF EXISTS `URL_TAG_CONNECTIONS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `URL_TAG_CONNECTIONS`
(
    `url_id` int(10) unsigned NOT NULL,
    `tag_id` int(10) unsigned NOT NULL,
    KEY `fk_utc_url_id_idx` (`url_id`) USING BTREE,
    KEY `fk_utc_tag_id_idx` (`tag_id`) USING BTREE,
    CONSTRAINT `fk_utc_tag_id_idx` FOREIGN KEY (`tag_id`) REFERENCES `URL_TAGS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_utc_url_id_idx` FOREIGN KEY (`url_id`) REFERENCES `URLS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USERS`
--

DROP TABLE IF EXISTS `USERS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USERS`
(
    `id`                    int(11)                                 NOT NULL AUTO_INCREMENT,
    `username`              varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `password`              varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `email`                 varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `first_name`            varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `last_name`             varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `dob`                   datetime                                DEFAULT NULL,
    `mobile_number`         bigint(20)                              DEFAULT NULL,
    `country`               varchar(45) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    `nationality`           varchar(45) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    `postal_code`           int(11)                                 DEFAULT NULL,
    `address`               varchar(45) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    `gender`                varchar(45) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    `user_status`           varchar(45) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    `user_profile`          json                                    DEFAULT NULL,
    `ic_or_passport_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`            datetime                                DEFAULT NULL,
    `updated_at`            datetime                                DEFAULT NULL,
    `external_patient_ref`  varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `source`                varchar(30) COLLATE utf8mb4_unicode_ci  DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`),
    UNIQUE KEY `uk_u_ic_or_passport_number_idx` (`ic_or_passport_number`),
    UNIQUE KEY `uk_u_email_idx` (`email`) USING BTREE,
    KEY `idx_external_patient_ref` (`external_patient_ref`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 39491
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_ACCOUNTS`
--

DROP TABLE IF EXISTS `USER_ACCOUNTS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ACCOUNTS`
(
    `id`                  int(11) NOT NULL AUTO_INCREMENT,
    `user_id`             int(11) NOT NULL,
    `user_profile`        json             DEFAULT NULL,
    `user_batch_id`       int(11) NOT NULL,
    `is_demo_account`     bit(1)  NOT NULL DEFAULT b'0',
    `user_account_config` json             DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ua_user_id_batch_id_idx` (`user_id`, `user_batch_id`),
    KEY `ua_user_batch_id` (`user_batch_id`),
    CONSTRAINT `ua_user_batch_id` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `user_id` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_ACCOUNTS_AUD`
--

DROP TABLE IF EXISTS `USER_ACCOUNTS_AUD`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ACCOUNTS_AUD`
(
    `id`                  bigint(20) NOT NULL,
    `REV`                 int(11)    NOT NULL,
    `REVTYPE`             tinyint(4) DEFAULT NULL,
    `user_account_config` json       DEFAULT NULL,
    PRIMARY KEY (`id`, `REV`),
    KEY `fk_uaa_revinfo_idx` (`REV`),
    CONSTRAINT `fk_uaa_revinfo_idx` FOREIGN KEY (`REV`) REFERENCES `REVINFO` (`REV`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_BATCH`
--

DROP TABLE IF EXISTS `USER_BATCH`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_BATCH`
(
    `id`            int(11)                                NOT NULL AUTO_INCREMENT,
    `name`          varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `description`   varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `start_date`    date                                   NOT NULL,
    `end_date`      date                                   NOT NULL,
    `engagement_id` int(11)                                NOT NULL,
    `status`        varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
    `code`          varchar(10) COLLATE utf8mb4_unicode_ci   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `code_UNIQUE` (`code`),
    KEY `ub_engagement_id` (`engagement_id`),
    CONSTRAINT `ub_engagement_id` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 512
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_CONTENT_VISIT_HISTORY`
--

DROP TABLE IF EXISTS `USER_CONTENT_VISIT_HISTORY`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_CONTENT_VISIT_HISTORY`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`       int(11)             NOT NULL,
    `user_batch_id` int(11)             NOT NULL,
    `content_id`    bigint(20) unsigned NOT NULL,
    `engagement_id` int(11)             NOT NULL,
    `visited_at`    datetime            NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_ucvh_user_batch_idx` (`user_batch_id`),
    KEY `fk_ucvh_user_idx` (`user_id`),
    KEY `fk_ucvh_engagement_idx` (`engagement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_LOGINS`
--

DROP TABLE IF EXISTS `USER_LOGINS`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_LOGINS`
(
    `id`            int(11)                                 NOT NULL AUTO_INCREMENT,
    `username`      varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `login_time`    datetime                                NOT NULL,
    `user_agent`    varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_batch_id` int(11)                                 DEFAULT NULL,
    `engagement_id` int(11)                                 DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `ul_user_batch_fk` (`user_batch_id`),
    KEY `ul_engagement_fk` (`engagement_id`),
    KEY `ul_username_fk` (`username`),
    CONSTRAINT `ul_engagement_fk` FOREIGN KEY (`engagement_id`) REFERENCES `ENGAGEMENTS` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ul_user_batch_fk` FOREIGN KEY (`user_batch_id`) REFERENCES `USER_BATCH` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ul_username_fk` FOREIGN KEY (`username`) REFERENCES `USERS` (`username`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `USER_ROLES`
--

DROP TABLE IF EXISTS `USER_ROLES`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ROLES`
(
    `user_id` int(11) NOT NULL,
    `role_id` int(11) NOT NULL,
    KEY `ur_role_id` (`role_id`),
    KEY `ur_user_id` (`user_id`),
    CONSTRAINT `ur_role_id` FOREIGN KEY (`role_id`) REFERENCES `ROLES` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ur_user_id` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `SESSION_OTP`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SESSION_OTP` (
   `id` int NOT NULL AUTO_INCREMENT,
   `session_id` varchar(100) NOT NULL,
   `email` varchar(100) NOT NULL,
   `otp_value` varchar(100) NOT NULL,
   `attempt_count` int NOT NULL DEFAULT '0',
   `utc_created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `utc_expired_at` datetime NOT NULL,
   `utc_invalidated_at` datetime NULL,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_session_id_email` (`session_id`,`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history`
(
    `installed_rank` int(11)                                  NOT NULL,
    `version`        varchar(50) COLLATE utf8mb4_unicode_ci            DEFAULT NULL,
    `description`    varchar(200) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `type`           varchar(20) COLLATE utf8mb4_unicode_ci   NOT NULL,
    `script`         varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
    `checksum`       int(11)                                           DEFAULT NULL,
    `installed_by`   varchar(100) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `installed_on`   timestamp                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `execution_time` int(11)                                  NOT NULL,
    `success`        tinyint(1)                               NOT NULL,
    PRIMARY KEY (`installed_rank`),
    KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shedlock`
--

DROP TABLE IF EXISTS `shedlock`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock`
(
    `name`       varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `lock_until` timestamp(3)                            NULL DEFAULT NULL,
    `locked_at`  timestamp(3)                            NULL DEFAULT NULL,
    `locked_by`  varchar(255) COLLATE utf8mb4_unicode_ci      DEFAULT NULL,
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

DELIMITER ;;
/*!50003 CREATE */ /*!50017 DEFINER =`root`@`localhost`*/ /*!50003 TRIGGER before_insert_episode
    BEFORE INSERT
    ON EPISODE
    FOR EACH ROW
BEGIN
    IF NEW.utc_created_at IS NULL THEN
        SET NEW.utc_created_at = CURRENT_TIMESTAMP;
    END IF;
END */;;
DELIMITER ;

DELIMITER ;;
/*!50003 CREATE */ /*!50017 DEFINER =`root`@`localhost`*/ /*!50003 TRIGGER before_update_episode
    BEFORE UPDATE
    ON EPISODE
    FOR EACH ROW
BEGIN
    SET NEW.utc_updated_at = CURRENT_TIMESTAMP;
END */;;
DELIMITER ;


/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

CREATE EVENT delete_old_cygnus_events
    ON SCHEDULE EVERY 1 DAY
        STARTS '2023-12-14 00:00:00.000'
    ON COMPLETION NOT PRESERVE
    ENABLE
    DO DELETE
       FROM CYGNUS_EVENT
       WHERE utc_created_at < NOW() - INTERVAL 1 MONTH;

