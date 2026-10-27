package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="skill_evidence")
public class SkillEvidence {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_skill_id") private UserSkill userSkill;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private EvidenceType type;
    @Column(nullable=false, length=120) private String title;
    @Column(length=1000) private String description;
    @Column(length=500) private String url;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected SkillEvidence() {}
    public SkillEvidence(EvidenceType type,String title,String description,String url){this.type=type;this.title=title;this.description=description;this.url=url;}
    public Long getId(){return id;} public UserSkill getUserSkill(){return userSkill;} void setUserSkill(UserSkill v){userSkill=v;}
    public EvidenceType getType(){return type;} public String getTitle(){return title;} public String getDescription(){return description;} public String getUrl(){return url;} public LocalDateTime getCreatedAt(){return createdAt;}
}
