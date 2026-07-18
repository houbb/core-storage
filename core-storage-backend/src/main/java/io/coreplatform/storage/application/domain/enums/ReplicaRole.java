package io.coreplatform.storage.application.domain.enums;

/**
 * 副本角色 — 描述 Replica 在主从复制拓扑中的位置。
 * <p>
 * 设计原则：PRIMARY/SECONDARY/BACKUP 是角色，不是 Driver。
 * 任何 Driver 都可以承担任意角色。
 */
public enum ReplicaRole {

    /** 主副本 — 业务读写目标 */
    PRIMARY,

    /** 从副本 — 接收 PRIMARY 的同步数据 */
    SECONDARY,

    /** 备份副本 — 独立于主从链路的离线备份 */
    BACKUP,

    /** 归档副本 — 长期冷存储 */
    ARCHIVE,

    /** 缓存副本 — 就近加速读取 */
    CACHE
}