package cl.germain.studymanager.term;

import java.time.LocalDate;

public record TermResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {

    public static TermResponse from(Term term) {
        return new TermResponse(term.getId(), term.getName(), term.getStartDate(), term.getEndDate());
    }
}
