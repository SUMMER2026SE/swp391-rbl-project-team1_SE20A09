package com.sportvenue.service;

public interface ComplaintEscalationService {
    
    /**
     * Escalate complaint to admin manually (customer or admin action)
     */
    void escalateToAdmin(Integer complaintId, String reason, String requestedByEmail);
    
    
    /**
     * Finalize complaints after 48h customer objection period expires
     */
    void processCustomerResponseDeadlines();
    
    /**
     * Check and mark SLA violations for owner response time
     */
    void checkSlaViolations();
    
    /**
     * Owner resolves complaint - starts 48h customer objection period
     */
    void startOwnerResolution(Integer complaintId, String resolution, String ownerEmail);
    
    /**
     * Customer objects to owner resolution - escalates to admin
     */
    void customerObjectToResolution(Integer complaintId, String objectionReason, String customerEmail);
    
    /**
     * Admin approves owner resolution
     */
    void adminApproveResolution(Integer complaintId, String adminEmail);
    
    /**
     * Admin overrides owner resolution
     */
    void adminOverrideResolution(Integer complaintId, String newResolution, String adminEmail);
}