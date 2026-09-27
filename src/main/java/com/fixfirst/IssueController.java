package com.fixfirst;
import java.security.Principal;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IssueController {
    private final IssueService service;
    public IssueController(IssueService service) { this.service=service; }
    public record NewIssue(@NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=1500) String description,@NotBlank @Size(max=80) String location,
        @NotBlank @Size(max=80) String equipment,@NotNull Issue.Category category, Long assetId) {}
    public record StatusChange(@NotNull Issue.Status status,@NotBlank @Size(max=300) String note,
        @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal repairCost) {}
    public record Feedback(@NotNull Boolean fixed,@NotBlank @Size(max=300) String note) {}
    public record Override(@Min(0) @Max(100) Integer score,@NotBlank @Size(max=300) String reason) {}
    public record Assignment(@NotNull Long technicianId) {}
    public record DeadlineChange(@NotNull Instant deadline,@NotBlank @Size(max=300) String reason) {}
    @GetMapping("/issues") public List<IssueService.View> list(Principal p) { return service.list(p); }
    @PostMapping("/issues") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Long> create(@Valid @RequestBody NewIssue input,Principal p) { return Map.of("id",service.create(input,p)); }
    @PostMapping("/issues/{id}/support") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void support(@PathVariable Long id,Principal p) { service.support(id,p); }
    @PostMapping("/admin/issues/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(@PathVariable Long id,@Valid @RequestBody StatusChange input,Principal p) { service.changeStatus(id,input,p); }
    @PostMapping("/issues/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void technicianStatus(@PathVariable Long id,@Valid @RequestBody StatusChange input,Principal p) { service.changeStatus(id,input,p); }
    @PostMapping("/issues/{id}/feedback") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable Long id,@Valid @RequestBody Feedback input,Principal p) { service.feedback(id,input,p); }
    @PostMapping("/admin/issues/{id}/priority") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void override(@PathVariable Long id,@Valid @RequestBody Override input,Principal p) { service.override(id,input,p); }
    @PostMapping("/admin/issues/{id}/assignment") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable Long id,@Valid @RequestBody Assignment input,Principal p) { service.assign(id,input,p); }
    @PostMapping("/admin/issues/{id}/deadline") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deadline(@PathVariable Long id,@Valid @RequestBody DeadlineChange input,Principal p) { service.changeDeadline(id,input,p); }
    @GetMapping("/notifications") public Map<String,Object> notifications(Principal p) { return service.notifications(p); }
    @PostMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readNotification(@PathVariable Long id,Principal p) { service.markNotificationRead(id,p); }
    @PostMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAllNotifications(Principal p) { service.markAllNotificationsRead(p); }
    @GetMapping("/admin/analytics") public Map<String,Object> analytics(@RequestParam Optional<Instant> from,
        @RequestParam Optional<Instant> to,Principal p) { return service.analytics(from.orElse(Instant.EPOCH),to.orElse(Instant.now()),p); }
    @GetMapping("/admin/assets") public List<IssueService.AssetView> assets() { return service.assets(); }
    public record AssetInput(@NotBlank @Size(max=80) String assetIdentifier,@NotBlank @Size(max=120) String location,
        @NotBlank @Size(max=80) String category,@DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal estimatedReplacementCost) {}
    @PostMapping("/admin/assets") @ResponseStatus(HttpStatus.CREATED)
    public IssueService.AssetView asset(@Valid @RequestBody AssetInput input) { return service.createAsset(input); }
    @PutMapping("/admin/assets/{id}")
    public IssueService.AssetView updateAsset(@PathVariable Long id,@Valid @RequestBody AssetInput input) { return service.updateAsset(id,input); }
}
