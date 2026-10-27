package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="job_applications")
public class JobApplication {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false) private String company;
    @Column(nullable=false) private String role;
    private String jobUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ApplicationStatus status=ApplicationStatus.SAVED;
    @Column(length=1200) private String notes;
    private LocalDate appliedDate=LocalDate.now();
    protected JobApplication(){}
    public JobApplication(User user,String company,String role,String jobUrl,String notes){this.user=user;this.company=company;this.role=role;this.jobUrl=jobUrl;this.notes=notes;}
    public Long getId(){return id;} public User getUser(){return user;} public String getCompany(){return company;} public String getRole(){return role;} public String getJobUrl(){return jobUrl;} public ApplicationStatus getStatus(){return status;} public void setStatus(ApplicationStatus v){status=v;} public String getNotes(){return notes;} public LocalDate getAppliedDate(){return appliedDate;}
}
