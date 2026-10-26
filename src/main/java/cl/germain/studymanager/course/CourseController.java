package cl.germain.studymanager.course;

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
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/terms/{termId}/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable Long termId,
                                 @Valid @RequestBody CourseRequest request) {
        return courseService.create(CurrentUser.id(jwt), termId, request);
    }

    @GetMapping("/terms/{termId}/courses")
    public List<CourseResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long termId) {
        return courseService.list(CurrentUser.id(jwt), termId);
    }

    @GetMapping("/courses/{id}")
    public CourseResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return courseService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/courses/{id}")
    public CourseResponse update(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable Long id,
                                 @Valid @RequestBody CourseRequest request) {
        return courseService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        courseService.delete(CurrentUser.id(jwt), id);
    }
}