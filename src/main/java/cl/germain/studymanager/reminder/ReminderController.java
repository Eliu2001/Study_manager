package cl.germain.studymanager.reminder;

import cl.germain.studymanager.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/assessments/{assessmentId}/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long assessmentId,
                                   @Valid @RequestBody ReminderRequest request) {
        return reminderService.create(CurrentUser.id(jwt), assessmentId, request);
    }

    @GetMapping("/assessments/{assessmentId}/reminders")
    public List<ReminderResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long assessmentId) {
        return reminderService.list(CurrentUser.id(jwt), assessmentId);
    }

    @DeleteMapping("/reminders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        reminderService.delete(CurrentUser.id(jwt), id);
    }

    @GetMapping("/reminders/upcoming")
    public List<ReminderNotification> upcoming(@AuthenticationPrincipal Jwt jwt) {
        return reminderService.upcoming(CurrentUser.id(jwt));
    }

    @PostMapping("/reminders/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        reminderService.dismiss(CurrentUser.id(jwt), id);
    }
}
