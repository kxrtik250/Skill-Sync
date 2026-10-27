package com.skillsync.user;

public enum VerificationStatus {
    SELF_REPORTED("Self-reported", 0.75), PROJECT_BACKED("Project-backed", 0.90), CERTIFICATION_BACKED("Certification-backed", 0.95), ASSESSMENT_VERIFIED("Assessment-verified", 1.0);
    private final String label;
    private final double factor;
    VerificationStatus(String label, double factor) { this.label = label; this.factor = factor; }
    public String getLabel() { return label; }
    public double getFactor() { return factor; }
}
