package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="career_preferences")
public class CareerPreference {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", unique=true) private User user;
    @Column(nullable=false) private String careerType="Exploring";
    @Column(length=1000) private String priorities="";
    @Column(length=1000) private String domains="";
    private String targetCareer;
    private LocalDate targetDate;
    protected CareerPreference() {}
    public CareerPreference(User user){this.user=user;}
    public Long getId(){return id;} public User getUser(){return user;} public String getCareerType(){return careerType;} public void setCareerType(String v){careerType=v;}
    public String getPriorities(){return priorities==null?"":priorities;} public void setPriorities(String v){priorities=v;}
    public String getDomains(){return domains==null?"":domains;} public void setDomains(String v){domains=v;}
    public String getTargetCareer(){return targetCareer;} public void setTargetCareer(String v){targetCareer=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;}
}
