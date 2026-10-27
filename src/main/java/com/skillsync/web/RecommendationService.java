package com.skillsync.web;

import com.skillsync.user.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    public static final List<Skill> SKILLS = List.of(
        new Skill("Java","Programming"),new Skill("Spring Boot","Backend"),new Skill("Python","Programming"),new Skill("JavaScript","Frontend"),new Skill("TypeScript","Frontend"),new Skill("SQL","Data"),new Skill("Data Structures","Computer Science"),new Skill("Algorithms","Computer Science"),new Skill("Cloud Computing","Cloud & Security"),new Skill("Git","Tools"),new Skill("Machine Learning","AI & Data"),new Skill("Statistics","AI & Data"),new Skill("Communication","People & Business"),new Skill("LLM APIs","AI & Data"),new Skill("System Design","Backend"),new Skill("Docker","Cloud & Security"),new Skill("Cybersecurity","Cloud & Security"),new Skill("Data Engineering","AI & Data"),new Skill("Prompt Engineering","AI & Data"),new Skill("AI Literacy","AI & Data"),new Skill("Analytical Thinking","People & Business"),new Skill("Collaboration","People & Business"),new Skill("Networking","Cloud & Security"),new Skill("UX Research","Design"),new Skill("Figma","Design"),new Skill("Product Strategy","Product"),new Skill("Agile Project Management","Product"),new Skill("Market Research","Business"),new Skill("SEO","Marketing"),new Skill("Content Strategy","Marketing"),new Skill("Technical Writing","Communication"),new Skill("Customer Success","People & Business"),new Skill("Talent Acquisition","People & Business"),new Skill("Business Analysis","Business"),new Skill("Presentation Skills","People & Business"),new Skill("Data Visualization","AI & Data"));
    private static final List<Role> ROLES = List.of(
        role("AI Product Engineer","Technical","Builds software products that use AI safely and effectively.","AI", "Python","LLM APIs","System Design","TypeScript","Git","AI Literacy"),
        role("Cloud Backend Engineer","Technical","Builds secure, scalable APIs and cloud services.","Cloud", "Java","Spring Boot","SQL","Cloud Computing","Docker","Data Structures","Git","Cybersecurity"),
        role("Data Intelligence Analyst","Technical","Uses data, statistics, and clear communication to support decisions.","Data", "SQL","Python","Statistics","Communication","Data Engineering","Analytical Thinking","Data Visualization"),
        role("Machine Learning Engineer","Technical","Develops and evaluates machine learning systems.","AI", "Python","Machine Learning","Statistics","Data Structures","Cloud Computing","Git","AI Literacy"),
        role("Cybersecurity Analyst","Technical","Finds and helps reduce security risks in systems and networks.","Cybersecurity", "Cybersecurity","Cloud Computing","Python","Networking","Communication","Git"),
        role("Frontend Engineer","Technical","Creates accessible, responsive web experiences.","Software", "JavaScript","TypeScript","Figma","Communication","Git"),
        role("UX Researcher","Nontechnical","Studies user needs and turns research into product improvements.","Design", "UX Research","Figma","Analytical Thinking","Communication","Presentation Skills"),
        role("Product Manager","Nontechnical","Sets product direction and aligns teams around customer outcomes.","Product", "Product Strategy","Agile Project Management","Market Research","Communication","Analytical Thinking","Presentation Skills"),
        role("Business Analyst","Nontechnical","Maps business needs and translates them into clear requirements.","Business", "Business Analysis","SQL","Analytical Thinking","Communication","Presentation Skills"),
        role("Digital Marketing Strategist","Nontechnical","Plans measurable campaigns across search and content channels.","Marketing", "SEO","Content Strategy","Market Research","Data Visualization","Communication"),
        role("Technical Writer","Nontechnical","Turns complex product and engineering details into clear documentation.","Software", "Technical Writing","Communication","Analytical Thinking","Java","Python"),
        role("Technical Recruiter","Nontechnical","Builds candidate pipelines and evaluates technical role fit.","Business", "Talent Acquisition","Communication","Networking","Analytical Thinking","Presentation Skills"),
        role("Customer Success Manager","Nontechnical","Helps customers adopt products and achieve measurable outcomes.","Business", "Customer Success","Communication","Analytical Thinking","Presentation Skills","Product Strategy"));

    private final UserSkillRepository userSkills;
    private final CareerPreferenceRepository preferences;
    public RecommendationService(UserSkillRepository userSkills,CareerPreferenceRepository preferences){this.userSkills=userSkills;this.preferences=preferences;}
    private static Role role(String name,String type,String description,String domain,String... skills){
        List<Requirement> requirements=new ArrayList<>();
        for(int i=0;i<skills.length;i++) requirements.add(new Requirement(skills[i],i<3?1.6:1.0,i<3));
        return new Role(name,type,description,domain,List.copyOf(requirements));
    }
    public List<Recommendation> forUser(User user) {
        Map<String,UserSkill> profile=new HashMap<>();
        for(UserSkill item:userSkills.findAllByUserIdOrderByCategoryAscSkillNameAsc(user.getId()))profile.put(item.getSkillName(),item);
        // Backward-compatible bridge: old users retain their existing selected-skill data.
        if(profile.isEmpty()&&!user.getSkills().isBlank()) for(String name:user.getSkills().split(","))
            profile.put(name.trim(),null);
        CareerPreference pref=preferences.findByUserId(user.getId()).orElse(null);
        return ROLES.stream().map(role->match(role,profile,pref)).sorted(Comparator.comparingInt(Recommendation::score).reversed().thenComparing(Recommendation::name)).toList();
    }
    public List<String> careerNames(){return ROLES.stream().map(Role::name).toList();}
    public List<String> careerDomains(){return ROLES.stream().map(Role::domain).distinct().sorted().toList();}
    public List<String> careerIndustries(){return ROLES.stream().map(r->metadata(r).industry()).distinct().sorted().toList();}
    private Recommendation match(Role role,Map<String,UserSkill> profile,CareerPreference preference){
        double total=0,earned=0;List<String> gaps=new ArrayList<>(),strong=new ArrayList<>(),partial=new ArrayList<>();int core=0,coreTotal=0;
        for(Requirement req:role.requirements()){total+=req.weight();if(req.core())coreTotal++;UserSkill item=profile.get(req.skill());if(item==null&&!profile.containsKey(req.skill())){gaps.add(req.skill());continue;}double proficiency=item==null?Proficiency.BEGINNER.getFactor():item.getProficiency().getFactor();double evidence=item==null?VerificationStatus.SELF_REPORTED.getFactor():item.getVerificationStatus().getFactor();double confidence=item==null?0.88:0.7+item.getConfidence()*0.06;earned+=req.weight()*proficiency*evidence*confidence;if(req.core())core++;if(item==null||item.getProficiency()==Proficiency.BEGINNER)partial.add(req.skill());else strong.add(req.skill());}
        int base=(int)Math.round(earned/total*100);int alignment=0;List<String> preferenceReasons=new ArrayList<>();
        if(preference!=null){String preferredType=preference.getCareerType();if(preferredType!=null&&preferredType.equalsIgnoreCase(role.type())){alignment+=4;preferenceReasons.add("aligned with your "+role.type().toLowerCase()+" preference");}if(Arrays.stream(preference.getDomains().split(",")).map(String::trim).anyMatch(d->d.equalsIgnoreCase(role.domain()))){alignment+=4;preferenceReasons.add("aligned with your preferred "+role.domain()+" domain");}
            Set<String> priorities=Arrays.stream(preference.getPriorities().split(",")).map(String::trim).filter(s->!s.isBlank()).collect(java.util.stream.Collectors.toSet());
            if(priorities.contains("Technical depth")&&role.type().equals("Technical")){alignment+=2;preferenceReasons.add("fits your technical-depth priority");}
            if(priorities.contains("Creativity")&&Set.of("Design","Product","Marketing").contains(role.domain())){alignment+=2;preferenceReasons.add("fits your creativity priority");}
            if(priorities.contains("Leadership opportunities")&&role.name().endsWith("Manager")){alignment+=2;preferenceReasons.add("fits your leadership-opportunity priority");}
        }
        String priorityReason=preferenceReasons.isEmpty()?"Your skill profile is the main signal; unsupported market preferences do not change the score.":"Preference alignment: "+String.join("; ",preferenceReasons)+".";
        int score=Math.min(100,base+alignment);String why=strong.isEmpty()?"Your profile has an initial signal for this path; build evidence to strengthen the match.":"Your demonstrated strengths include "+String.join(", ",strong.subList(0,Math.min(3,strong.size())))+".";
        String gapReason=gaps.isEmpty()?"All listed role skills are represented; deepen proficiency and evidence.":"The highest-priority next skills are "+String.join(", ",gaps.subList(0,Math.min(3,gaps.size())))+".";
        CareerMeta meta=metadata(role);
        return new Recommendation(role.name(),role.type(),role.description(),score,role.requirements().stream().map(Requirement::skill).toList(),List.copyOf(gaps),List.copyOf(strong),List.copyOf(partial),core,coreTotal,base,alignment,why,gapReason,priorityReason,role.domain(),meta.intensity(),"Student / early career",meta.industry(),"Varies by employer",meta.learningEffort(),meta.difficulty(),"Depends on role, industry, and experience",meta.interviewDifficulty(),role.description(),meta.projects(),meta.stages());
    }
    private CareerMeta metadata(Role role){
        String intensity=role.type().equals("Technical")?(role.domain().equals("Data")?"Medium–high":"High"):(role.name().equals("Technical Writer")||role.name().equals("Business Analyst")?"Low–medium":"Low");
        String industry=switch(role.domain()){case "AI"->"AI & software";case "Cloud"->"Cloud & enterprise technology";case "Cybersecurity"->"Cybersecurity";case "Data"->"Data & analytics";case "Design"->"Product & design";case "Marketing"->"Marketing & media";case "Product"->"Technology & product";case "Business"->"Business services";default->"Software & digital products";};
        String effort=role.requirements().size()>6?"High":"Moderate";
        String difficulty=role.type().equals("Technical")?"Moderate–advanced":"Foundational–moderate";
        String interviews=role.type().equals("Technical")?"Technical assessment may apply":"Role and employer dependent";
        String[] projects=switch(role.domain()){case "Cloud"->new String[]{"Documented REST API","Containerized service"};case "AI"->new String[]{"Applied AI prototype","Model evaluation report"};case "Data"->new String[]{"Analytics case study","Data quality pipeline"};case "Cybersecurity"->new String[]{"Threat-model exercise","Defensive security lab"};case "Design"->new String[]{"User research study","Usability findings report"};case "Product"->new String[]{"Product discovery brief","Prioritized roadmap"};case "Marketing"->new String[]{"Campaign analysis","Content strategy"};case "Business"->new String[]{"Process analysis","Stakeholder requirements brief"};default->new String[]{"Accessible web application","Technical case study"};};
        List<String> stages=role.type().equals("Technical")?List.of("Student / personal projects","Junior / associate role","Practitioner","Senior specialist or lead","Staff / principal or engineering manager"):List.of("Student / portfolio projects","Coordinator / associate","Practitioner","Senior specialist or manager","Director / functional leader");
        return new CareerMeta(intensity,industry,effort,difficulty,interviews,List.of(projects),stages);
    }
    public record Skill(String name,String category){}
    public record Requirement(String skill,double weight,boolean core){}
    public record Role(String name,String type,String description,String domain,List<Requirement> requirements){}
    public record Recommendation(String name,String type,String description,int score,List<String> skills,List<String> gaps,List<String> strongSkills,List<String> partialSkills,int coreMatched,int coreTotal,int skillScore,int preferenceAdjustment,String whyMatch,String whyNotReady,String preferenceReason,String domain,String technicalIntensity,String experienceLevel,String industry,String workStyle,String learningEffort,String difficulty,String growthPotential,String interviewDifficulty,String responsibilities,List<String> recommendedProjects,List<String> progressionStages){}
    public record CareerMeta(String intensity,String industry,String learningEffort,String difficulty,String interviewDifficulty,List<String> projects,List<String> stages){}
}
