package com.support.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendContextSmokeTest {

	@Test
	void should_loadApplicationContext_when_springBootStarts() {
	}

}
