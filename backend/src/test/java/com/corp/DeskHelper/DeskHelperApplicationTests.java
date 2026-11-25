package com.corp.DeskHelper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "server.port=0",
    "cors.allowed-origins=http://localhost:5173",
    "spring.mongodb.uri=mongodb://127.0.0.1:27017/deskhelper_test",
    "spring.mongodb.database=deskhelper_test",
    "spring.data.mongodb.auto-index-creation=false"
})
class DeskHelperApplicationTests {

    @MockitoBean(name = "mongoSessionRepository")
    private MongoIndexedSessionRepository sessionRepository;

	@Test
	void contextLoads() {
	}

}
