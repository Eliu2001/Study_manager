CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       name          VARCHAR(100) NOT NULL,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE terms (
                       id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                       name       VARCHAR(50) NOT NULL,
                       start_date DATE        NOT NULL,
                       end_date   DATE        NOT NULL,
                       CONSTRAINT uq_terms_user_name UNIQUE (user_id, name),
                       CONSTRAINT ck_terms_dates CHECK (end_date >= start_date)
);

CREATE TABLE courses (
                         id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         term_id       BIGINT       NOT NULL REFERENCES terms (id) ON DELETE CASCADE,
                         name          VARCHAR(120) NOT NULL,
                         code          VARCHAR(20),
                         credits       SMALLINT,
                         passing_grade NUMERIC(2,1) NOT NULL DEFAULT 4.0,
                         CONSTRAINT uq_courses_term_name UNIQUE (term_id, name),
                         CONSTRAINT ck_courses_credits CHECK (credits IS NULL OR credits > 0),
                         CONSTRAINT ck_courses_passing_grade CHECK (passing_grade BETWEEN 1.0 AND 7.0)
);

CREATE TABLE assessments (
                             id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             course_id BIGINT       NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
                             name      VARCHAR(120) NOT NULL,
                             type      VARCHAR(20)  NOT NULL,
                             weight    NUMERIC(5,2) NOT NULL,
                             due_date  DATE,
                             status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                             grade     NUMERIC(2,1),
                             topics    TEXT,
                             comment   TEXT,
                             CONSTRAINT ck_assessments_type CHECK (type IN ('SUMMATIVE', 'EXAM')),
                             CONSTRAINT ck_assessments_weight CHECK (weight > 0 AND weight <= 100),
                             CONSTRAINT ck_assessments_status CHECK (status IN ('PENDING', 'SUBMITTED', 'GRADED')),
                             CONSTRAINT ck_assessments_grade CHECK (grade IS NULL OR grade BETWEEN 1.0 AND 7.0),
                             CONSTRAINT ck_assessments_grade_status CHECK (grade IS NULL OR status = 'GRADED')
);

CREATE TABLE academic_dates (
                                id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                course_id  BIGINT       REFERENCES courses (id) ON DELETE CASCADE,
                                title      VARCHAR(150) NOT NULL,
                                event_date DATE         NOT NULL,
                                type       VARCHAR(20)  NOT NULL,
                                CONSTRAINT ck_academic_dates_type CHECK (type IN ('HOLIDAY', 'TERM_START', 'TERM_END', 'DEADLINE', 'OTHER'))
);

CREATE TABLE reminders (
                           id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           assessment_id BIGINT      NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
                           days_before   SMALLINT    NOT NULL,
                           sent          BOOLEAN     NOT NULL DEFAULT FALSE,
                           sent_at       TIMESTAMPTZ,
                           CONSTRAINT uq_reminders_assessment_days UNIQUE (assessment_id, days_before),
                           CONSTRAINT ck_reminders_days CHECK (days_before >= 0)
);

CREATE INDEX idx_assessments_course_id ON assessments (course_id);
CREATE INDEX idx_academic_dates_user_id ON academic_dates (user_id);
CREATE INDEX idx_academic_dates_course_id ON academic_dates (course_id);