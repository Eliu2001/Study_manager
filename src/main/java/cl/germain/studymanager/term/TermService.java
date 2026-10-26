package cl.germain.studymanager.term;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ConflictException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserRepository userRepository;

    @Transactional
    public TermResponse create(Long userId, TermRequest request) {
        String name = request.name().trim();
        validateDates(request);
        if (termRepository.existsByUserIdAndName(userId, name)) {
            throw new ConflictException("A term named '" + name + "' already exists");
        }

        Term term = new Term();
        term.setUser(userRepository.getReferenceById(userId));
        apply(term, name, request);
        return TermResponse.from(termRepository.save(term));
    }

    @Transactional(readOnly = true)
    public List<TermResponse> list(Long userId) {
        return termRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(TermResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TermResponse get(Long userId, Long id) {
        return TermResponse.from(find(userId, id));
    }

    @Transactional
    public TermResponse update(Long userId, Long id, TermRequest request) {
        Term term = find(userId, id);
        String name = request.name().trim();
        validateDates(request);
        if (termRepository.existsByUserIdAndNameAndIdNot(userId, name, id)) {
            throw new ConflictException("A term named '" + name + "' already exists");
        }

        apply(term, name, request);
        return TermResponse.from(term);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        termRepository.delete(find(userId, id));
    }

    private Term find(Long userId, Long id) {
        return termRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Term", id));
    }

    private void validateDates(TermRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("endDate must not be before startDate");
        }
    }

    private void apply(Term term, String name, TermRequest request) {
        term.setName(name);
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
    }
}
