package com.fixfirst;

import java.io.*;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import javax.imageio.ImageIO;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/issues/{issueId}/photos")
public class PhotoController {
    private final IssueRepository issues; private final IssuePhotoRepository photos; private final UserRepository users; private final Path root;
    public PhotoController(IssueRepository issues, IssuePhotoRepository photos, UserRepository users, @Value("${fixfirst.photo-dir:./fixfirst-photos}") String directory) throws IOException { this.issues=issues;this.photos=photos;this.users=users;root=Paths.get(directory).toAbsolutePath().normalize();Files.createDirectories(root); }
    @GetMapping public List<Map<String,Object>> list(@PathVariable Long issueId,Principal p) { access(issueId,p); return photos.findByIssueIdOrderByUploadedAtAsc(issueId).stream().map(x->Map.<String,Object>of("id",x.id,"kind",x.kind,"url","/api/issues/"+issueId+"/photos/"+x.id,"uploadedAt",x.uploadedAt)).toList(); }
    @GetMapping("/{photoId}") public ResponseEntity<InputStreamResource> get(@PathVariable Long issueId,@PathVariable Long photoId,Principal p) throws IOException { access(issueId,p); IssuePhoto photo=photos.findById(photoId).filter(x->x.issueId.equals(issueId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Photo not found.")); Path file=root.resolve(photo.storedName).normalize(); if(!file.getParent().equals(root)||!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Photo file not found."); return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.contentType)).body(new InputStreamResource(Files.newInputStream(file))); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> upload(@PathVariable Long issueId,@RequestParam IssuePhoto.Kind kind,@RequestPart("file") @NotNull MultipartFile file,Principal p) throws IOException { AppUser u=access(issueId,p); Issue issue=issues.findById(issueId).orElseThrow(); if(kind==IssuePhoto.Kind.BEFORE&&!u.id.equals(issue.reporterId))deny(); if(kind==IssuePhoto.Kind.AFTER&&!(("ADMIN".equals(u.role))||Objects.equals(issue.assignedTechnicianId,u.id)))deny(); if(file.isEmpty()||file.getSize()>5_000_000||photos.countByIssueIdAndKind(issueId,kind)>=3)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use up to three images of at most 5 MB each."); String type=actualType(file); String name=UUID.randomUUID()+extension(type); Path target=root.resolve(name).normalize(); if(!target.getParent().equals(root))deny(); try(InputStream in=file.getInputStream()){Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);} photos.save(new IssuePhoto(issueId,u.id,kind,name,type)); return Map.of("kind",kind,"url","/api/issues/"+issueId+"/photos"); }
    private String actualType(MultipartFile file) throws IOException { byte[] b=file.getBytes(); if(b.length>=8&&b[0]==(byte)0x89&&b[1]==0x50&&b[2]==0x4e&&b[3]==0x47)return "image/png"; if(b.length>=3&&b[0]==(byte)0xff&&b[1]==(byte)0xd8&&b[2]==(byte)0xff)return "image/jpeg"; if(b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P')return "image/webp"; throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only valid JPEG, PNG, or WebP images are accepted."); }
    private AppUser access(Long issueId,Principal p) { AppUser u=users.findByEmail(p.getName()).orElseThrow(); Issue i=issues.findById(issueId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Issue not found.")); if(!"ADMIN".equals(u.role)&&!u.id.equals(i.reporterId)&&!u.id.equals(i.assignedTechnicianId))deny(); return u; }
    private static void deny(){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot access this issue photo.");}
    private static String extension(String type){return switch(type){case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};}
}