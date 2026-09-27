package com.fixfirst;

import java.security.Principal;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IssueService {
    private final IssueRepository issues;
    private final UserRepository users;
    private final NotificationService notificationService;
    private final CampusAssetRepository assets;
    @Value("${fixfirst.replacement.repair-count-threshold:3}") private int replacementRepairCountThreshold;
    @Value("${fixfirst.replacement.cost-ratio:0.60}") private BigDecimal replacementCostRatio;
    public IssueService(IssueRepository issues, UserRepository users, NotificationService notificationService, CampusAssetRepository assets) {
        this.issues=issues; this.users=users; this.notificationService=notificationService; this.assets=assets;
    }
    public record View(Long id,String title,String description,String location,String equipment,Issue.Category category,
        Issue.Status status,Instant createdAt,Instant targetResolutionAt,boolean overdue,int supportCount,boolean supported,
        boolean reporter,Long assignedTechnicianId,String assignedTechnician,Long assetId,String assetIdentifier,
        BigDecimal repairCost,int reopenCount,PriorityCalculator.Score score,String overrideReason,long previousReports,List<String> history) {}
    public record AssetView(Long id,String assetIdentifier,String location,String category,BigDecimal estimatedReplacementCost) {}
    private AppUser user(Principal p) { return users.findByEmail(p.getName()).orElseThrow(); }
    private Issue locked(Long id) { return issues.findLocked(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Issue not found.")); }
    private static boolean active(Issue i) { return i.status==Issue.Status.OPEN || i.status==Issue.Status.IN_PROGRESS; }
    private static String normalize(String s) { return s.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }
    private static boolean sameAsset(Issue a,Issue b) { return a.category==b.category && normalize(a.location).equals(normalize(b.location)) && normalize(a.equipment).equals(normalize(b.equipment)); }
    private void event(Issue i,AppUser u,String message) { i.history.add(Instant.now()+" | "+u.role+" #"+u.id+" | "+message); }
    private AppUser technician(Long id) { AppUser u=users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Technician not found.")); if(!"TECHNICIAN".equals(u.role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected account is not a technician."); return u; }
    private String technicianName(Long id) { return id==null?null:users.findById(id).map(u->u.name).orElse("Unknown technician"); }
    @Transactional(readOnly=true)
    public List<View> list(Principal p) {
        AppUser u=user(p); List<Issue> all=issues.findAll(); Instant now=Instant.now();
        return all.stream().filter(i -> !"TECHNICIAN".equals(u.role) || Objects.equals(i.assignedTechnicianId,u.id)).map(i -> view(i,u,all,now)).sorted(Comparator.comparingInt((View v)->v.score().effective()).reversed().thenComparing(View::createdAt)).toList();
    }
    private View view(Issue i,AppUser u,List<Issue> all,Instant now) {
        var asset=i.assetId==null?null:assets.findById(i.assetId).orElse(null);
        return new View(i.id,i.title,i.description,i.location,i.equipment,i.category,i.status,i.createdAt,i.targetResolutionAt,
            active(i)&&i.targetResolutionAt!=null&&i.targetResolutionAt.isBefore(now),i.supporters.size(),i.supporters.contains(u.id),
            i.reporterId.equals(u.id),i.assignedTechnicianId,technicianName(i.assignedTechnicianId),i.assetId,asset==null?null:asset.assetIdentifier,
            i.repairCost,Objects.requireNonNullElse(i.reopenCount,0),PriorityCalculator.calculate(i,now),i.overrideReason,
            all.stream().filter(old -> !old.id.equals(i.id)&&old.createdAt.isBefore(i.createdAt)&&sameAsset(old,i)).count(),List.copyOf(i.history));
    }
    public Long create(IssueController.NewIssue input,Principal p) {
        AppUser u=user(p); Issue i=new Issue(); i.title=input.title().trim(); i.description=input.description().trim(); i.location=input.location().trim(); i.equipment=input.equipment().trim(); i.category=input.category(); i.assetId=input.assetId(); i.reporterId=u.id; i.supporters.add(u.id);
        Instant now=Instant.now(); i.targetResolutionAt=ResolutionDeadlinePolicy.initialDeadline(PriorityCalculator.calculate(i,now),now); event(i,u,"Reported issue; reporter counted as first supporter."); return issues.save(i).id;
    }
    public void support(Long id,Principal p) { Issue i=locked(id); AppUser u=user(p); if(!active(i)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only active issues accept support."); if(!i.supporters.add(u.id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"You already support this issue."); event(i,u,"Confirmed they are affected."); }
    public void assign(Long id,IssueController.Assignment input,Principal p) { Issue i=locked(id); AppUser admin=user(p); AppUser tech=technician(input.technicianId()); if(!active(i)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only active issues can be assigned."); Long old=i.assignedTechnicianId; i.assignedTechnicianId=tech.id; event(i,admin,(old==null?"Assigned":"Reassigned")+" to technician "+tech.name+"."); notificationService.notifyOnce(tech.id,i.id,"assignment:"+i.id+":"+tech.id,"You were assigned issue #"+i.id+": "+i.title); notificationService.notifyOnce(i.reporterId,i.id,"assignment-reporter:"+i.id+":"+tech.id,"Issue #"+i.id+" was assigned to "+tech.name+"."); if(old!=null&&!old.equals(tech.id)) notificationService.notifyOnce(old,i.id,"reassignment:"+i.id+":"+old+":"+tech.id,"Issue #"+i.id+" was reassigned."); }
    public void changeStatus(Long id,IssueController.StatusChange input,Principal p) { Issue i=locked(id); AppUser u=user(p); boolean staff="ADMIN".equals(u.role)||Objects.equals(i.assignedTechnicianId,u.id); if(!staff) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned technician or an admin can update this issue."); boolean allowed=(i.status==Issue.Status.OPEN&&input.status()==Issue.Status.IN_PROGRESS)||(i.status==Issue.Status.IN_PROGRESS&&input.status()==Issue.Status.RESOLVED); if(!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT,"Allowed transitions are OPEN to IN_PROGRESS and IN_PROGRESS to RESOLVED."); i.status=input.status(); if(i.status==Issue.Status.RESOLVED){i.resolvedAt=Instant.now();if(i.firstResolvedAt==null)i.firstResolvedAt=i.resolvedAt;if(input.repairCost()!=null)i.repairCost=input.repairCost();} event(i,u,"Status: "+i.status+". "+input.note().trim()); if(i.status==Issue.Status.IN_PROGRESS) notificationService.notifyOnce(i.reporterId,i.id,"started:"+i.id,"Work started on issue #"+i.id+"."); else notificationService.notifyOnce(i.reporterId,i.id,"resolved:"+i.id,"Issue #"+i.id+" was marked resolved."); }
    public void feedback(Long id,IssueController.Feedback input,Principal p) { Issue i=locked(id); AppUser u=user(p); if(!i.reporterId.equals(u.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the original reporter can verify the fix."); if(i.status!=Issue.Status.RESOLVED) throw new ResponseStatusException(HttpStatus.CONFLICT,"The issue must be resolved before verification."); i.status=input.fixed()?Issue.Status.CLOSED:Issue.Status.OPEN; if(!input.fixed()){i.resolvedAt=null;i.reopenCount=Objects.requireNonNullElse(i.reopenCount,0)+1; notificationService.notifyAdmins(i.id,"reopened:"+i.id+":"+i.reopenCount,"Reporter reopened issue #"+i.id+"."); if(i.assignedTechnicianId!=null)notificationService.notifyOnce(i.assignedTechnicianId,i.id,"reopened-tech:"+i.id+":"+i.reopenCount,"Reporter reopened issue #"+i.id+".");} else if(i.assignedTechnicianId!=null) notificationService.notifyOnce(i.assignedTechnicianId,i.id,"confirmed:"+i.id+":"+Instant.now(),"Reporter confirmed the repair for issue #"+i.id+"."); event(i,u,(input.fixed()?"Fix verified; issue closed. ":"Fix rejected; issue reopened. ")+input.note().trim()); }
    public void override(Long id,IssueController.Override input,Principal p) { Issue i=locked(id); AppUser u=user(p); if(!active(i)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only active issues can be reprioritized."); i.overrideScore=input.score(); i.overrideReason=input.score()==null?null:input.reason().trim(); event(i,u,(input.score()==null?"Removed priority override. ":"Priority overridden to "+input.score()+". ")+input.reason().trim()); }
    public void changeDeadline(Long id,IssueController.DeadlineChange input,Principal p) { Issue i=locked(id); AppUser u=user(p); if(!input.deadline().isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Deadline must be in the future."); i.targetResolutionAt=input.deadline(); event(i,u,"Deadline changed to "+input.deadline()+". Reason: "+input.reason().trim()); }
    public Map<String,Object> notifications(Principal p) { AppUser u=user(p); return Map.of("items",notificationService.list(u.id).stream().map(n->Map.of("id",n.id,"issueId",n.issueId,"message",n.message,"createdAt",n.createdAt,"read",n.readAt!=null)).toList(),"unread",notificationService.unread(u.id)); }
    public void markNotificationRead(Long id,Principal p) { notificationService.markRead(id,user(p).id); }
    public void markAllNotificationsRead(Principal p) { notificationService.markAllRead(user(p).id); }
    public List<AssetView> assets() { return assets.findAll().stream().map(this::assetView).toList(); }
    public AssetView createAsset(IssueController.AssetInput input) { if(assets.findByAssetIdentifier(input.assetIdentifier().trim()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Asset identifier already exists."); return assetView(assets.save(new CampusAsset(input.assetIdentifier().trim(),input.location().trim(),input.category().trim(),input.estimatedReplacementCost()))); }
    public AssetView updateAsset(Long id,IssueController.AssetInput input) { CampusAsset asset=assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found.")); assets.findByAssetIdentifier(input.assetIdentifier().trim()).filter(other->!other.id.equals(id)).ifPresent(other->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Asset identifier already exists.");}); asset.assetIdentifier=input.assetIdentifier().trim();asset.location=input.location().trim();asset.category=input.category().trim();asset.estimatedReplacementCost=input.estimatedReplacementCost();return assetView(asset); }
    private AssetView assetView(CampusAsset asset) { return new AssetView(asset.id,asset.assetIdentifier,asset.location,asset.category,asset.estimatedReplacementCost); }
    @Transactional(readOnly=true)
    public Map<String,Object> analytics(Instant from,Instant to,Principal p) { user(p); List<Issue> rows=issues.findAll().stream().filter(i->!i.createdAt.isBefore(from)&&i.createdAt.isBefore(to)).toList(); long activeOverdue=rows.stream().filter(this::isOverdue).count(); long reopened=rows.stream().filter(i->Objects.requireNonNullElse(i.reopenCount,0)>0).count(); double avg=rows.stream().filter(i->i.firstResolvedAt!=null).mapToLong(i->Duration.between(i.createdAt,i.firstResolvedAt).toHours()).average().orElse(0); Map<String,Long> byCategory=new TreeMap<>(),byStatus=new TreeMap<>(); rows.forEach(i->{byCategory.merge(i.category.name(),1L,Long::sum);byStatus.merge(i.status.name(),1L,Long::sum);}); BigDecimal total=rows.stream().map(i->i.repairCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add); Instant sixMonths=to.minus(Duration.ofDays(183)); List<Map<String,Object>> suggestions=new ArrayList<>(); assets.findAll().forEach(asset->{List<Issue> repairs=rows.stream().filter(i->Objects.equals(i.assetId,asset.id)&&i.firstResolvedAt!=null&&!i.firstResolvedAt.isBefore(sixMonths)).toList();BigDecimal cost=repairs.stream().map(i->i.repairCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);boolean countHit=repairs.size()>=replacementRepairCountThreshold;boolean costHit=asset.estimatedReplacementCost!=null&&cost.compareTo(asset.estimatedReplacementCost.multiply(replacementCostRatio))>=0;if(countHit||costHit)suggestions.add(Map.of("assetIdentifier",asset.assetIdentifier,"repairs",repairs.size(),"repairCost",cost,"replacementThreshold",asset.estimatedReplacementCost==null?BigDecimal.ZERO:asset.estimatedReplacementCost.multiply(replacementCostRatio),"from",sixMonths,"to",to,"reason",countHit?"At least "+replacementRepairCountThreshold+" resolved repairs in six months.":"Six-month repair cost reached the configured replacement threshold."));}); Map<String,Object> result=new LinkedHashMap<>(); result.put("from",from);result.put("to",to);result.put("byCategory",byCategory);result.put("byStatus",byStatus);result.put("averageResolutionHours",avg);result.put("reopenRate",rows.isEmpty()?0.0:(double)reopened/rows.size());result.put("overdueActive",activeOverdue);result.put("totalRepairCost",total);result.put("topRecurringAssets",rows.stream().filter(i->i.assetId!=null).collect(java.util.stream.Collectors.groupingBy(i->i.assetId,java.util.stream.Collectors.counting())).entrySet().stream().sorted(Map.Entry.<Long,Long>comparingByValue().reversed()).limit(10).toList());result.put("replacementSuggestions",suggestions);return result; }
    private boolean isOverdue(Issue i) { return active(i)&&i.targetResolutionAt!=null&&i.targetResolutionAt.isBefore(Instant.now()); }
}
