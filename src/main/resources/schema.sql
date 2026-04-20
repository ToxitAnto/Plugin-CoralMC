CREATE TABLE IF NOT EXISTS `cs_clans` (
    `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(20)     NOT NULL,
    `tag`         VARCHAR(5)      NOT NULL,
    `leader_uuid` VARCHAR(36)     NOT NULL,
    `description` VARCHAR(255)    DEFAULT NULL,
    `home_world`  VARCHAR(64)     DEFAULT NULL,
    `home_x`      DOUBLE          DEFAULT NULL,
    `home_y`      DOUBLE          DEFAULT NULL,
    `home_z`      DOUBLE          DEFAULT NULL,
    `home_yaw`    FLOAT           DEFAULT NULL,
    `home_pitch`  FLOAT           DEFAULT NULL,
    `created_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_clan_name` (`name`),
    UNIQUE KEY `uq_clan_tag`  (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cs_members` (
    `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `clan_id`     INT UNSIGNED    NOT NULL,
    `player_uuid` VARCHAR(36)     NOT NULL,
    `player_name` VARCHAR(16)     NOT NULL,
    `role`        ENUM('LEADER','OFFICER','MEMBER') NOT NULL DEFAULT 'MEMBER',
    `joined_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_member_clan` (`clan_id`, `player_uuid`),
    UNIQUE KEY `uq_player`      (`player_uuid`),
    CONSTRAINT `fk_member_clan`
        FOREIGN KEY (`clan_id`) REFERENCES `cs_clans` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cs_territories` (
    `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `clan_id`     INT UNSIGNED    NOT NULL,
    `world`       VARCHAR(64)     NOT NULL,
    `chunk_x`     INT             NOT NULL,
    `chunk_z`     INT             NOT NULL,
    `claimed_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_chunk` (`world`, `chunk_x`, `chunk_z`),
    CONSTRAINT `fk_territory_clan`
        FOREIGN KEY (`clan_id`) REFERENCES `cs_clans` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cs_invites` (
    `id`           INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    `clan_id`      INT UNSIGNED   NOT NULL,
    `inviter_uuid` VARCHAR(36)    NOT NULL,
    `invitee_uuid` VARCHAR(36)    NOT NULL,
    `expires_at`   TIMESTAMP      NOT NULL,
    `created_at`   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_invite` (`clan_id`, `invitee_uuid`),
    CONSTRAINT `fk_invite_clan`
        FOREIGN KEY (`clan_id`) REFERENCES `cs_clans` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_member_uuid    ON `cs_members`    (`player_uuid`);
CREATE INDEX idx_territory_clan ON `cs_territories`(`clan_id`);
CREATE INDEX idx_invite_invitee ON `cs_invites`    (`invitee_uuid`);
CREATE INDEX idx_invite_expires ON `cs_invites`    (`expires_at`);
