package com.skillsync.web;

import com.skillsync.user.User;
import com.skillsync.user.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller public class ResumeController {
    private static final long LIMIT=2L*1024*1024;
    private final UserRepository users; private final RecommendationService recommendations;
    public ResumeController(UserRepository users,RecommendationService recommendations){this.users=users;this.recommendations=recommendations;}
    @GetMapping("/resume-intelligence") String resumePage(Authentication auth,Model model){User user=users.findByEmail(auth.getName()).orElseThrow();model.addAttribute("user",user);model.addAttribute("careers",recommendations.forUser(user));return "resume-intelligence";}
    @PostMapping("/resume-intelligence") String analyze(@RequestParam("resume") MultipartFile upload,@RequestParam(required=false) String role,Authentication auth,Model model){
        User user=users.findByEmail(auth.getName()).orElseThrow();List<RecommendationService.Recommendation> roles=recommendations.forUser(user);model.addAttribute("user",user);model.addAttribute("careers",roles);
        if(upload==null||upload.isEmpty()||upload.getSize()>LIMIT){model.addAttribute("error","Choose a non-empty PDF or TXT file smaller than 2 MB.");return "resume-intelligence";}
        String name=upload.getOriginalFilename()==null?"resume":upload.getOriginalFilename();int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);String text;
        try{byte[] bytes=upload.getBytes();if(ext.equals("pdf")){if(bytes.length<5||!new String(bytes,0,5,StandardCharsets.ISO_8859_1).equals("%PDF-")){model.addAttribute("error","The uploaded file did not look like a valid PDF.");return "resume-intelligence";}try(PDDocument pdf=Loader.loadPDF(bytes)){if(pdf.isEncrypted()||pdf.getNumberOfPages()>20){model.addAttribute("error","Use an unencrypted PDF of 20 pages or fewer.");return "resume-intelligence";}text=new PDFTextStripper().getText(pdf);}}else if(ext.equals("txt")){text=new String(bytes,StandardCharsets.UTF_8);}else{model.addAttribute("error","For privacy and reliable local parsing, this version supports PDF and plain-text resumes only.");return "resume-intelligence";}}
        catch(IOException|RuntimeException ex){model.addAttribute("error","Could not read this resume. Try a text-based PDF or plain-text file.");return "resume-intelligence";}
        if(text.length()>60000)text=text.substring(0,60000);final String analyzedText=text;String normalized=analyzedText.toLowerCase(Locale.ROOT);
        List<String> detected=RecommendationService.SKILLS.stream().map(RecommendationService.Skill::name).filter(s->Pattern.compile("(?i)(?<![\\p{L}\\p{N}])"+Pattern.quote(s)+"(?![\\p{L}\\p{N}])").matcher(analyzedText).find()).toList();
        RecommendationService.Recommendation target=roles.stream().filter(r->role!=null&&r.name().equals(role)).findFirst().orElse(roles.getFirst());Set<String> found=new HashSet<>(detected);List<String> matched=target.skills().stream().filter(found::contains).toList(),missing=target.skills().stream().filter(s->!found.contains(s)).toList();int coverage=(int)Math.round(100.0*matched.size()/target.skills().size());
        List<String> sections=List.of("education","experience","projects","certifications");List<String> sectionSignals=sections.stream().filter(s->normalized.contains(s)).toList();model.addAttribute("result",new ResumeResult(name,detected,target.name(),coverage,target.skills().size(),matched,missing,sectionSignals,sections.stream().filter(s->!sectionSignals.contains(s)).toList()));return "resume-intelligence";
    }
    public record ResumeResult(String fileName,List<String> detectedSkills,String career,int coverage,int totalSkills,List<String> matched,List<String> missing,List<String> detectedSections,List<String> sectionsToReview){}
}
