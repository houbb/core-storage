package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.infrastructure.persistence.repository.LifecyclePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 生命周期策略管理服务。
 */
@Service
public class LifecyclePolicyService {

    private static final Logger log = LoggerFactory.getLogger(LifecyclePolicyService.class);

    private final LifecyclePolicyRepository policyRepo;

    public LifecyclePolicyService(LifecyclePolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    /**
     * 创建生命周期策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public LifecyclePolicy createPolicy(String policyName, String resourceType, String category,
                                         int activeDays, int warmDays, int coldDays,
                                         int archiveDays, int deleteDays, String description) {
        if (policyRepo.existsByTypeAndCategory(resourceType, category)) {
            throw new PolicyAlreadyExistsException(
                    "Policy already exists for type=" + resourceType + ", category=" + category);
        }

        LifecyclePolicy policy = LifecyclePolicy.create(policyName, resourceType, category,
                activeDays, warmDays, coldDays, archiveDays, deleteDays, description);

        LifecyclePolicy saved = policyRepo.save(policy);
        log.info("Lifecycle policy created: id={}, name={}, type={}, category={}",
                saved.getId(), saved.getPolicyName(), resourceType, category);
        return saved;
    }

    /**
     * 查询所有策略。
     */
    public List<LifecyclePolicy> listPolicies() {
        return policyRepo.findAll();
    }

    /**
     * 根据 ID 查询策略。
     */
    public LifecyclePolicy getById(Long id) {
        return policyRepo.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: id=" + id));
    }

    /**
     * 更新策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public LifecyclePolicy updatePolicy(Long id, String policyName, Integer activeDays,
                                         Integer warmDays, Integer coldDays, Integer archiveDays,
                                         Integer deleteDays, Boolean enabled, String description) {
        LifecyclePolicy policy = policyRepo.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: id=" + id));

        if (policyName != null) policy.setPolicyName(policyName);
        if (activeDays != null) policy.setActiveDays(activeDays);
        if (warmDays != null) policy.setWarmDays(warmDays);
        if (coldDays != null) policy.setColdDays(coldDays);
        if (archiveDays != null) policy.setArchiveDays(archiveDays);
        if (deleteDays != null) policy.setDeleteDays(deleteDays);
        if (enabled != null) policy.setEnabled(enabled);
        if (description != null) policy.setDescription(description);

        policyRepo.update(policy);
        log.info("Lifecycle policy updated: id={}", id);
        return getById(id);
    }

    /**
     * 删除策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(Long id) {
        policyRepo.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: id=" + id));
        policyRepo.deleteById(id);
        log.info("Lifecycle policy deleted: id={}", id);
    }

    // ---- inner exceptions ----

    public static class PolicyNotFoundException extends RuntimeException {
        public PolicyNotFoundException(String message) {
            super(message);
        }
    }

    public static class PolicyAlreadyExistsException extends RuntimeException {
        public PolicyAlreadyExistsException(String message) {
            super(message);
        }
    }
}