CREATE UNIQUE INDEX uq_assessments_exam_per_course
    ON assessments (course_id)
    WHERE type = 'EXAM';
