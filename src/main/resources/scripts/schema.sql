-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: localhost    Database: ssafyhome
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `author_mno` int NOT NULL,
  `author_name` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `depth` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `likes` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_author_mno` (`author_mno`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`author_mno`) REFERENCES `user` (`mno`) ON DELETE CASCADE,
  CONSTRAINT `comments_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `daily_price`
--

DROP TABLE IF EXISTS `daily_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_price` (
  `date` date NOT NULL,
  `closing_price` decimal(20,8) NOT NULL,
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dong_code_superman`
--

DROP TABLE IF EXISTS `dong_code_superman`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dong_code_superman` (
  `dong_code` varchar(10) NOT NULL,
  `sido_name` varchar(30) DEFAULT NULL,
  `gugun_name` varchar(30) DEFAULT NULL,
  `dong_name` varchar(30) DEFAULT NULL,
  `lat` double DEFAULT NULL,
  `lng` double DEFAULT NULL,
  `avg_price` bigint DEFAULT NULL,
  `apt_count` int DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`dong_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dongcodes`
--

DROP TABLE IF EXISTS `dongcodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dongcodes` (
  `dong_code` varchar(10) NOT NULL COMMENT '???????ڵ',
  `sido_name` varchar(30) DEFAULT NULL COMMENT '?õ??̸?',
  `gugun_name` varchar(30) DEFAULT NULL COMMENT '?????̸?',
  `dong_name` varchar(30) DEFAULT NULL COMMENT '???̸?',
  PRIMARY KEY (`dong_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???????ڵ????̺';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorite_housedeals`
--

DROP TABLE IF EXISTS `favorite_housedeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_housedeals` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_mno` int NOT NULL,
  `apt_seq` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_housedeal` (`user_mno`,`apt_seq`),
  KEY `idx_user_mno` (`user_mno`),
  KEY `idx_apt_seq` (`apt_seq`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `favorite_housedeals_ibfk_1` FOREIGN KEY (`user_mno`) REFERENCES `user` (`mno`) ON DELETE CASCADE,
  CONSTRAINT `favorite_housedeals_ibfk_2` FOREIGN KEY (`apt_seq`) REFERENCES `housedeals` (`apt_seq`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `housedeals`
--

DROP TABLE IF EXISTS `housedeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `housedeals` (
  `no` int NOT NULL AUTO_INCREMENT COMMENT '?ŷ??',
  `apt_seq` varchar(20) DEFAULT NULL COMMENT '????Ʈ?ڵ',
  `apt_dong` varchar(40) DEFAULT NULL COMMENT '????Ʈ??',
  `floor` varchar(3) DEFAULT NULL COMMENT '????Ʈ?',
  `deal_year` int DEFAULT NULL COMMENT '?ŷ??⵵',
  `deal_month` int DEFAULT NULL COMMENT '?ŷ??',
  `deal_day` int DEFAULT NULL COMMENT '?ŷ??',
  `exclu_use_ar` decimal(7,2) DEFAULT NULL COMMENT '????Ʈ???',
  `deal_amount` varchar(10) DEFAULT NULL COMMENT '?ŷ????',
  PRIMARY KEY (`no`),
  KEY `apt_seq_to_house_info_idx` (`apt_seq`),
  KEY `idx_housedeals_apt_seq` (`apt_seq`),
  KEY `idx_housedeals_deal_amount` (`deal_amount`),
  KEY `idx_housedeals_area` (`exclu_use_ar`),
  KEY `idx_housedeals_date` (`deal_year`,`deal_month`,`deal_day`),
  KEY `idx_housedeals_complex` (`apt_seq`,`deal_amount`,`exclu_use_ar`),
  KEY `idx_apt_seq_deal_date` (`apt_seq`,`deal_year`,`deal_month`,`deal_day`),
  KEY `idx_apt_seq` (`apt_seq`),
  KEY `idx_housedeals_aptseq_date` (`apt_seq`,`deal_year`,`deal_month`,`deal_day`),
  CONSTRAINT `apt_seq_to_house_info` FOREIGN KEY (`apt_seq`) REFERENCES `houseinfos` (`apt_seq`)
) ENGINE=InnoDB AUTO_INCREMENT=7262332 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???ðŷ????????̺';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `houseinfos`
--

DROP TABLE IF EXISTS `houseinfos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `houseinfos` (
  `apt_seq` varchar(20) NOT NULL COMMENT '????Ʈ?ڵ',
  `sgg_cd` varchar(5) DEFAULT NULL COMMENT '?ñ????ڵ',
  `umd_cd` varchar(5) DEFAULT NULL COMMENT '???鵿?ڵ',
  `umd_nm` varchar(20) DEFAULT NULL COMMENT '???鵿?̸?',
  `jibun` varchar(10) DEFAULT NULL COMMENT '?',
  `road_nm_sgg_cd` varchar(5) DEFAULT NULL COMMENT '???θ??ñ????ڵ',
  `road_nm` varchar(20) DEFAULT NULL COMMENT '???θ',
  `road_nm_bonbun` varchar(10) DEFAULT NULL COMMENT '???θ????ʹ',
  `road_nm_bubun` varchar(10) DEFAULT NULL COMMENT '???θ??߰??',
  `apt_nm` varchar(40) DEFAULT NULL COMMENT '????Ʈ?̸?',
  `build_year` int DEFAULT NULL COMMENT '?ذ??⵵',
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  PRIMARY KEY (`apt_seq`),
  KEY `idx_houseinfos_apt_nm` (`apt_nm`),
  KEY `idx_houseinfos_umd_nm` (`umd_nm`),
  KEY `idx_houseinfos_build_year` (`build_year`),
  KEY `idx_houseinfos_latlng` (`latitude`,`longitude`),
  KEY `idx_houseinfos_aptseq` (`apt_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???????????̺';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interest_areas`
--

DROP TABLE IF EXISTS `interest_areas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interest_areas` (
  `id` int NOT NULL AUTO_INCREMENT,
  `mno` int NOT NULL,
  `dong_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_dong` (`mno`,`dong_code`),
  KEY `idx_dong_code` (`dong_code`),
  CONSTRAINT `interest_areas_ibfk_1` FOREIGN KEY (`mno`) REFERENCES `user` (`mno`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 관심지역';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `latest_housedeals`
--

DROP TABLE IF EXISTS `latest_housedeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `latest_housedeals` (
  `apt_seq` varchar(20) NOT NULL,
  `deal_year` int DEFAULT NULL,
  `deal_month` int DEFAULT NULL,
  `deal_day` int DEFAULT NULL,
  `deal_amount` varchar(10) DEFAULT NULL,
  `exclu_use_ar` decimal(7,2) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`apt_seq`),
  KEY `apt_seq` (`apt_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_mno` int NOT NULL,
  `author_name` varchar(100) NOT NULL,
  `category` varchar(50) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `tags` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `views` int DEFAULT '0',
  `likes` int DEFAULT '0',
  `dislikes` int DEFAULT '0',
  `comments_count` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_author_mno` (`author_mno`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_views` (`views`),
  KEY `idx_likes` (`likes`),
  CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`author_mno`) REFERENCES `user` (`mno`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_info`
--

DROP TABLE IF EXISTS `store_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_info` (
  `store_id` varchar(20) NOT NULL COMMENT '상가업소번호 (PK)',
  `store_name` varchar(100) DEFAULT NULL COMMENT '상호명',
  `branch_name` varchar(100) DEFAULT NULL COMMENT '지점명',
  `category_major_name` varchar(50) DEFAULT NULL COMMENT '상권업종대분류명',
  `category_mid_name` varchar(50) DEFAULT NULL COMMENT '상권업종중분류명',
  `category_minor_name` varchar(50) DEFAULT NULL COMMENT '상권업종소분류명',
  `city` varchar(20) DEFAULT NULL COMMENT '시도명',
  `district` varchar(20) DEFAULT NULL COMMENT '시군구명',
  `legal_dong_code` varchar(10) DEFAULT NULL COMMENT '법정동코드 (외래키)',
  `road_address` varchar(100) DEFAULT NULL COMMENT '도로명주소',
  `building_name` varchar(100) DEFAULT NULL COMMENT '건물명',
  PRIMARY KEY (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='주변 상권 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `mno` int NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `profile_image` varchar(512) DEFAULT NULL,
  `refresh` varchar(512) DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT '0',
  `verification_token` varchar(255) DEFAULT NULL,
  `token_expiry_date` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`mno`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_deleted`
--

DROP TABLE IF EXISTS `user_deleted`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_deleted` (
  `email` varchar(255) NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`email`),
  CONSTRAINT `fk_user_deleted_user` FOREIGN KEY (`email`) REFERENCES `user` (`email`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-05-28  2:04:05
