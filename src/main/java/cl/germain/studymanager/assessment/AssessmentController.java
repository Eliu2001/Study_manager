package cl.germain.studymanager.assessment;

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
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/courses/{courseId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResponse create(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long courseId,
                                     @Valid @RequestBody AssessmentRequest request) {
        return assessmentService.create(CurrentUser.id(jwt), courseId, request);
    }

    @GetMapping("/courses/{courseId}/assessments")
    public List<AssessmentResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long courseId) {
        return assessmentService.list(CurrentUser.id(jwt), courseId);
    }

    @GetMapping("/assessments/{id}")
    public AssessmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return assessmentService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/assessments/{id}")
    public AssessmentResponse update(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long id,
                                     @Valid @RequestBody AssessmentRequest request) {
        return assessmentService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/assessments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        assessmentService.delete(CurrentUser.id(jwt), id);
    }
}