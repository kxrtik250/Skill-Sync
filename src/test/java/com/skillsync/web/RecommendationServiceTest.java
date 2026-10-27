package com.skillsync.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skillsync.user.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private final UserSkillRepository skills=mock(UserSkillRepository.class);
    private final CareerPreferenceRepository preferences=mock(CareerPreferenceRepository.class);
    private final RecommendationService service=new RecommendationService(skills,preferences);

    @Test void keepsAllExistingPathsAndExplainsTheMatch(){
        User user=new User("student@example.test","encoded","Student");
        when(skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of());
        when(preferences.findByUserId(any())).thenReturn(Optional.empty());
        List<RecommendationService.Recommendation> results=service.forUser(user);
        assertEquals(13,results.size());
        var backend=results.stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow();
        assertEquals(3,backend.coreTotal());
        assertEquals(0,backend.coreMatched());
        assertFalse(backend.whyNotReady().isBlank());
        assertEquals(8,backend.gaps().size());
    }

    @Test void strongerVerifiedCoreSkillsIncreaseWeightedFit(){
        User user=new User("student@example.test","encoded","Student");
        when(skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of());
        when(preferences.findByUserId(any())).thenReturn(Optional.empty());
        int empty=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow().score();
        UserSkill java=new UserSkill(user,"Java","Programming");java.setProficiency(Proficiency.EXPERT);java.setConfidence(5);java.setVerificationStatus(VerificationStatus.ASSESSMENT_VERIFIED);
        UserSkill spring=new UserSkill(user,"Spring Boot","Backend");spring.setProficiency(Proficiency.ADVANCED);spring.setConfidence(4);spring.setVerificationStatus(VerificationStatus.PROJECT_BACKED);
        when(skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of(java,spring));
        int demonstrated=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow().score();
        assertTrue(demonstrated>empty);
    }

    @Test void careerTypeAndDomainPreferencesHaveSmallExplainableEffect(){
        User user=new User("student@example.test","encoded","Student");
        when(skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of());
        when(preferences.findByUserId(any())).thenReturn(Optional.empty());
        int baseline=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow().score();
        CareerPreference preference=new CareerPreference(user);preference.setCareerType("Technical");preference.setDomains("Cloud");
        when(preferences.findByUserId(any())).thenReturn(Optional.of(preference));
        var match=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow();
        assertEquals(Math.min(100,baseline+8),match.score());
        assertEquals(8,match.preferenceAdjustment());
        assertTrue(match.preferenceReason().contains("preferred Cloud domain"));
    }

    @Test void supportedCareerPrioritiesHaveSmallExplainableEffect(){
        User user=new User("student@example.test","encoded","Student");
        when(skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of());
        when(preferences.findByUserId(any())).thenReturn(Optional.empty());
        int baseline=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow().score();
        CareerPreference preference=new CareerPreference(user);preference.setPriorities("Technical depth,Compensation,Remote opportunities");
        when(preferences.findByUserId(any())).thenReturn(Optional.of(preference));
        var match=service.forUser(user).stream().filter(r->r.name().equals("Cloud Backend Engineer")).findFirst().orElseThrow();
        assertEquals(Math.min(100,baseline+2),match.score());
        assertEquals(2,match.preferenceAdjustment());
        assertTrue(match.preferenceReason().contains("technical-depth priority"));
    }

    @Test void roadmapRefreshesGenericTaskTextWithoutLosingCompletion(){
        RoadmapTaskRepository repository=mock(RoadmapTaskRepository.class);
        RoadmapTaskService roadmap=new RoadmapTaskService(repository);
        User user=new User("student@example.test","encoded","Student");
        RoadmapTask oldLearn=new RoadmapTask(user,"Cloud Backend Engineer","Docker","LEARN","Learn Docker foundations","Generic old task text",2);
        oldLearn.setCompleted(true);
        when(repository.findAllByUserIdAndCareerNameOrderByWeekIndexAscIdAsc(user.getId(),"Cloud Backend Engineer"))
            .thenReturn(List.of(oldLearn));
        when(repository.save(any(RoadmapTask.class))).thenAnswer(call->call.getArgument(0));

        roadmap.ensure(user,"Cloud Backend Engineer",List.of("Docker"),List.of());

        assertTrue(oldLearn.isCompleted());
        assertTrue(oldLearn.getDescription().contains("images, containers"));
        assertEquals(1,oldLearn.getWeekIndex());
        verify(repository,times(3)).save(any(RoadmapTask.class));
    }
}
