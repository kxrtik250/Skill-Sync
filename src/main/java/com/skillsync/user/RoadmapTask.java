package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="roadmap_tasks",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","career_name","skill_name","phase"}))
public class RoadmapTask {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Column(name="career_name",nullable=false) private String careerName;
    @Column(name="skill_name",nullable=false) private String skillName;
    @Column(nullable=false) private String phase;
    @Column(nullable=false) private String title;
    @Column(length=1200,nullable=false) private String description;
    @Column(name="week_index",nullable=false) private int weekIndex;
    @Column(nullable=false) private boolean completed;
    private LocalDateTime completedAt;
    protected RoadmapTask(){}
    public RoadmapTask(User user,String careerName,String skillName,String phase,String title,String description,int weekIndex){this.user=user;this.careerName=careerName;this.skillName=skillName;this.phase=phase;this.title=title;this.description=description;this.weekIndex=weekIndex;}
    public Long getId(){return id;} public User getUser(){return user;} public String getCareerName(){return careerName;} public String getSkillName(){return skillName;} public String getPhase(){return phase;} public String getTitle(){return title;} public String getDescription(){return description;} public int getWeekIndex(){return weekIndex;} public boolean isCompleted(){return completed;}
    public void setCompleted(boolean v){completed=v;completedAt=v?LocalDateTime.now():null;}
    public void refreshGuidance(String title,String description,int weekIndex){this.title=title;this.description=description;this.weekIndex=weekIndex;}
}
