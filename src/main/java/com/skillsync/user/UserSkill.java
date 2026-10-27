package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_skills", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_name"}))
public class UserSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "skill_name", nullable = false) private String skillName;
    @Column(nullable = false) private String category;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Proficiency proficiency = Proficiency.BEGINNER;
    @Column(nullable = false) private int confidence = 3;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private VerificationStatus verificationStatus = VerificationStatus.SELF_REPORTED;
    private LocalDate lastPracticed;
    @OneToMany(mappedBy = "userSkill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC") private List<SkillEvidence> evidence = new ArrayList<>();

    protected UserSkill() {}
    public UserSkill(User user, String skillName, String category) { this.user=user; this.skillName=skillName; this.category=category; }
    public Long getId(){return id;} public User getUser(){return user;} public String getSkillName(){return skillName;} public String getCategory(){return category;}
    public Proficiency getProficiency(){return proficiency;} public void setProficiency(Proficiency v){proficiency=v;}
    public int getConfidence(){return confidence;} public void setConfidence(int v){confidence=v;}
    public VerificationStatus getVerificationStatus(){return verificationStatus;} public void setVerificationStatus(VerificationStatus v){verificationStatus=v;}
    public LocalDate getLastPracticed(){return lastPracticed;} public void setLastPracticed(LocalDate v){lastPracticed=v;}
    public List<SkillEvidence> getEvidence(){return evidence;}
    public double getConfidenceScore(){return Math.round(100 * proficiency.getFactor() * verificationStatus.getFactor() * (0.7 + confidence * 0.06));}
    public void addEvidence(SkillEvidence item){evidence.add(item);item.setUserSkill(this);}
}
