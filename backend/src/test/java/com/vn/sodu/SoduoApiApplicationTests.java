package com.vn.sodu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.vn.sodu.SoduoApiApplication;

@SpringBootTest(classes = SoduoApiApplication.class)
@ActiveProfiles("dev")
class SoduoApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

