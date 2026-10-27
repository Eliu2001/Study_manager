package cl.germain.studymanager.grading;

import cl.germain.studymanager.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GradeSummaryController {

    private final GradeSummaryService gradeSummaryService;

    @GetMapping("/courses/{id}/summary")
    public CourseSummaryResponse course(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return gradeSummaryService.courseSummary(CurrentUser.id(jwt), id);
    }

    @GetMapping("/terms/{id}/summary")
    public TermSummaryResponse term(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return gradeSummaryService.termSummary(CurrentUser.id(jwt), id);
    }

    @GetMapping("/summary")
    public OverallSummaryResponse overall(@AuthenticationPrincipal Jwt jwt) {
        return gradeSummaryService.overallSummary(CurrentUser.id(jwt));
    }
}