package cl.germain.studymanager.reminder;

import cl.germain.studymanager.assessment.Assessment;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "days_before", nullable = false)
    private Short daysBefore;

    @Column(nullable = false)
    private boolean sent = false;

    @Column(name = "sent_at")
    private Instant sentAt;
}
