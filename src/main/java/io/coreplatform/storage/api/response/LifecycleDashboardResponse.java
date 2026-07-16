package io.coreplatform.storage.api.response;

import java.util.Map;

/**
 * 生命周期仪表盘响应 DTO。
 */
public class LifecycleDashboardResponse {

    private Map<String, Long> stageCounts;
    private int activeHolds;
    private int pendingTasks;
    private int totalPolicies;

    // ---- getters & setters ----

    public Map<String, Long> getStageCounts() { return stageCounts; }
    public void setStageCounts(Map<String, Long> stageCounts) { this.stageCounts = stageCounts; }

    public int getActiveHolds() { return activeHolds; }
    public void setActiveHolds(int activeHolds) { this.activeHolds = activeHolds; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }

    public int getTotalPolicies() { return totalPolicies; }
    public void setTotalPolicies(int totalPolicies) { this.totalPolicies = totalPolicies; }
}