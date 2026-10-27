package cl.germain.studymanager.academicdate;

import cl.germain.studymanager.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/academic-dates")
@RequiredArgsConstructor
public class AcademicDateController {

    private final AcademicDateService academicDateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicDateResponse create(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AcademicDateRequest request) {
        return academicDateService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<AcademicDateResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return academicDateService.list(CurrentUser.id(jwt), from, to);
    }

    @GetMapping("/{id}")
    public AcademicDateResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return academicDateService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/{id}")
    public AcademicDateResponse update(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id,
                                       @Valid @RequestBody AcademicDateRequest request) {
        return academicDateService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        academicDateService.delete(CurrentUser.id(jwt), id);
    }
}
