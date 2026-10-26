package cl.germain.studymanager.term;

import cl.germain.studymanager.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermResponse create(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody TermRequest request) {
        return termService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<TermResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return termService.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public TermResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return termService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/{id}")
    public TermResponse update(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable Long id,
                               @Valid @RequestBody TermRequest request) {
        return termService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        termService.delete(CurrentUser.id(jwt), id);
    }
}