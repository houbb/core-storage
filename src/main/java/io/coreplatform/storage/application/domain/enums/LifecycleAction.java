package io.coreplatform.storage.application.domain.enums;

/**
 * 生命周期操作类型。
 * <p>
 * 生命周期不是简单的"删除文件"，而是一系列治理操作。
 * 新增 Action 无需修改架构。
 */
public enum LifecycleAction {

    /** 无需操作（资源处于正确阶段） */
    NOTHING,

    /** 移动到下一存储层（如 SSD→NAS→OSS Archive） */
    MOVE,

    /** 归档（不可修改，只读） */
    ARCHIVE,

    /** 冻结（临时阻止所有生命周期操作） */
    FREEZE,

    /** 进入删除流程（两阶段：软删除→宽限期→物理删除） */
    DELETE,

    /** 校验（检查数据完整性） */
    VERIFY
}