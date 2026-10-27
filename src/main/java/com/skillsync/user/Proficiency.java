package com.skillsync.user;

public enum Proficiency {
    BEGINNER("Beginner", 0.50), INTERMEDIATE("Intermediate", 0.72), ADVANCED("Advanced", 0.90), EXPERT("Expert", 1.0);
    private final String label;
    private final double factor;
    Proficiency(String label, double factor) { this.label = label; this.factor = factor; }
    public String getLabel() { return label; }
    public double getFactor() { return factor; }
}
