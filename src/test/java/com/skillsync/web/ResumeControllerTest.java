package com.skillsync.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skillsync.user.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

class ResumeControllerTest {
    @Test void extractsCatalogSkillsWithoutPersistingResume(){
        User user=new User("resume-test@example.test","not-a-real-password","Resume Test");
        UserRepository users=mock(UserRepository.class);when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        UserSkillRepository userSkills=mock(UserSkillRepository.class);when(userSkills.findAllByUserIdOrderByCategoryAscSkillNameAsc(any())).thenReturn(List.of());
        CareerPreferenceRepository prefs=mock(CareerPreferenceRepository.class);when(prefs.findByUserId(any())).thenReturn(Optional.empty());
        ResumeController controller=new ResumeController(users,new RecommendationService(userSkills,prefs));
        byte[] contents="Software engineer\nSkills: Java, Spring Boot, SQL\nProjects\nEducation".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile upload=new MockMultipartFile("resume","sample.txt","text/plain",contents);
        ExtendedModelMap model=new ExtendedModelMap();
        String view=controller.analyze(upload,"Cloud Backend Engineer",new UsernamePasswordAuthenticationToken(user.getEmail(),""),model);
        ResumeController.ResumeResult result=(ResumeController.ResumeResult)model.getAttribute("result");
        assertEquals("resume-intelligence",view);assertNotNull(result);assertTrue(result.detectedSkills().containsAll(List.of("Java","Spring Boot","SQL")));
        assertTrue(result.missing().contains("Cloud Computing"));assertFalse(result.detectedSections().contains("experience"));
        verify(users,never()).save(any());verify(userSkills,never()).save(any());
    }
}
