package io.coreplatform.storage.api.response;

import java.util.Map;

/**
 * 平台仪表板响应 DTO。
 */
public class PlatformDashboardResponse {

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