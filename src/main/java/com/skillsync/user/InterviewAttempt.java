package com.skillsync.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="interview_attempts")
public class InterviewAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false) private String category;
    @Column(nullable=false) private String careerName;
    @Column(nullable=false,length=800) private String question;
    @Column(length=5000) private String answer;
    @Column(nullable=false) private int selfRating;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected InterviewAttempt(){}
    public InterviewAttempt(User u,String category,String career,String question,String answer,int rating){user=u;this.category=category;careerName=career;this.question=question;this.answer=answer;selfRating=rating;}
    public Long getId(){return id;} public User getUser(){return user;} public String getCategory(){return category;} public String getCareerName(){return careerName;} public String getQuestion(){return question;} public String getAnswer(){return answer;} public int getSelfRating(){return selfRating;} public LocalDateTime getCreatedAt(){return createdAt;}
}
