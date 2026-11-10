package cl.germain.studymanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("cl.germain.studymanager.TestDatabase#isAvailable")
class StudyManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
