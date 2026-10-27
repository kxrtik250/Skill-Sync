package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="career_projects")
public class CareerProject {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false,length=120) private String name;
    @Column(length=2000) private String description;
    @Column(length=800) private String technologies;
    @Column(length=1000) private String skills;
    private String githubUrl;
    private String demoUrl;
    private String duration;
    private String role;
    @Column(length=1000) private String outcome;
    private String projectType;
    private boolean featured;
    private LocalDateTime createdAt=LocalDateTime.now();
    protected CareerProject(){}
    public CareerProject(User user,String name,String description,String technologies,String skills,String githubUrl,String demoUrl,String duration,String role,String outcome,String projectType){this.user=user;this.name=name;this.description=description;this.technologies=technologies;this.skills=skills;this.githubUrl=githubUrl;this.demoUrl=demoUrl;this.duration=duration;this.role=role;this.outcome=outcome;this.projectType=projectType;}
    public Long getId(){return id;} public User getUser(){return user;} public String getName(){return name;} public String getDescription(){return description;} public String getTechnologies(){return technologies;} public String getSkills(){return skills;} public String getGithubUrl(){return githubUrl;} public String getDemoUrl(){return demoUrl;} public String getDuration(){return duration;} public String getRole(){return role;} public String getOutcome(){return outcome;} public String getProjectType(){return projectType;} public boolean isFeatured(){return featured;} public LocalDateTime getCreatedAt(){return createdAt;} public void setFeatured(boolean v){featured=v;}
}
