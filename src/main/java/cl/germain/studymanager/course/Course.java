package cl.germain.studymanager.course;

import cl.germain.studymanager.term.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String code;

    private Short credits;

    @Column(name = "passing_grade", nullable = false, precision = 2, scale = 1)
    private BigDecimal passingGrade = new BigDecimal("4.0");
}