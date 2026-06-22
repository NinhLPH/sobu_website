package com.vn.sodu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.vn.sodu.SobuApiApplication;

@SpringBootTest(classes = SobuApiApplication.class)
@ActiveProfiles("dev")
class SobuApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

