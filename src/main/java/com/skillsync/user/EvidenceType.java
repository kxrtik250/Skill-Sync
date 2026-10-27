package com.skillsync.user;

public enum EvidenceType {
    PROJECT("Project"), INTERNSHIP("Internship"), CERTIFICATION("Certification"), COURSE("Course"), WORK_EXPERIENCE("Work experience"), ASSESSMENT("Assessment");
    private final String label;
    EvidenceType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
