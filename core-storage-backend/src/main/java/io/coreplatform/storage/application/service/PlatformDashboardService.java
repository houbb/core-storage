package io.coreplatform.storage.application.service;

import io.coreplatform.storage.application.domain.enums.ScanStatus;
import io.coreplatform.storage.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台仪表板服务 — 提供企业级监控面板的聚合数据。
 */
@Service
public class PlatformDashboardService {

    private static final Logger log = LoggerFactory.getLogger(PlatformDashboardService.class);

    private final StorageResourceRepository resourceRepo;
    private final StorageTenantRepository tenantRepo;
    private final StorageDriverRepository driverRepo;
    private final StorageAuditRepository auditRepo;
    private final StorageScanRepository scanRepo;

    public PlatformDashboardService(StorageResourceRepository resourceRepo,
                                     StorageTenantRepository tenantRepo,
                                     StorageDriverRepository driverRepo,
                                     StorageAuditRepository auditRepo,
                                     StorageScanRepository scanRepo) {
        this.resourceRepo = resourceRepo;
        this.tenantRepo = tenantRepo;
        this.driverRepo = driverRepo;
        this.auditRepo = auditRepo;
        this.scanRepo = scanRepo;
    }

    /**
     * 构建系统级仪表板数据。
     */
    public DashboardData getDashboard() {
        DashboardData d = new DashboardData();

        // 资源统计
        d.setTotalResources(resourceRepo.countSearch(null, null, null, null, null, null, null, null));
        d.setActiveTenants(tenantRepo.listAll().stream()
                .filter(t -> t.getStatus() == io.coreplatform.storage.application.domain.enums.TenantStatus.ACTIVE).count());

        // 驱动健康
        var drivers = driverRepo.findAll();
        d.setTotalDrivers(drivers.size());
        d.setHealthyDrivers(drivers.stream().filter(drv -> drv.getHealthStatus() == io.coreplatform.storage.application.domain.enums.DriverHealth.HEALTHY).count());

        // 副本统计：汇总所有资源的副本数（resource count * replica count）
        d.setTotalReplicas(0);
        d.setHealthyReplicas(0);

        // 今日审计统计
        d.setAuditsToday(auditRepo.countSearch(null, null, null, null));
        d.setScansPending(scanRepo.countSearch(null, null, ScanStatus.PENDING.name()));

        // 按阶段统计
        Map<String, Long> stageCounts = new LinkedHashMap<>();
        for (var stage : io.coreplatform.storage.application.domain.enums.LifecycleStage.values()) {
            stageCounts.put(stage.name(), (long) resourceRepo.countByLifecycleStage(stage.name()));
        }
        d.setStageCounts(stageCounts);

        log.debug("Dashboard built: resources={}, tenants={}, drivers={}/{}, audits={}",
                d.getTotalResources(), d.getActiveTenants(),
                d.getHealthyDrivers(), d.getTotalDrivers(), d.getAuditsToday());

        return d;
    }

    /**
     * 仪表板数据值对象。
     */
    public static class DashboardData {
        private long totalResources;
        private long activeTenants;
        private int totalDrivers;
        private long healthyDrivers;
        private int totalReplicas;
        private long healthyReplicas;
        private int auditsToday;
        private int scansPending;
        private Map<String, Long> stageCounts;

        public long getTotalResources() { return totalResources; }
        public void setTotalResources(long totalResources) { this.totalResources = totalResources; }

        public long getActiveTenants() { return activeTenants; }
        public void setActiveTenants(long activeTenants) { this.activeTenants = activeTenants; }

        public int getTotalDrivers() { return totalDrivers; }
        public void setTotalDrivers(int totalDrivers) { this.totalDrivers = totalDrivers; }

        public long getHealthyDrivers() { return healthyDrivers; }
        public void setHealthyDrivers(long healthyDrivers) { this.healthyDrivers = healthyDrivers; }

        public int getTotalReplicas() { return totalReplicas; }
        public void setTotalReplicas(int totalReplicas) { this.totalReplicas = totalReplicas; }

        public long getHealthyReplicas() { return healthyReplicas; }
        public void setHealthyReplicas(long healthyReplicas) { this.healthyReplicas = healthyReplicas; }

        public int getAuditsToday() { return auditsToday; }
        public void setAuditsToday(int auditsToday) { this.auditsToday = auditsToday; }

        public int getScansPending() { return scansPending; }
        public void setScansPending(int scansPending) { this.scansPending = scansPending; }

        public Map<String, Long> getStageCounts() { return stageCounts; }
        public void setStageCounts(Map<String, Long> stageCounts) { this.stageCounts = stageCounts; }
    }
}