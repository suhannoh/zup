package com.noh.zup.domain.collection.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("local")
@SpringBootTest
@TestPropertySource(properties = {
        "app.collection.scheduler.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:zup-scheduler-disabled-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SourceWatchCollectionSchedulerDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void schedulerBeanIsNotCreatedWhenDisabled() {
        assertThat(applicationContext.getBeansOfType(SourceWatchCollectionScheduler.class)).isEmpty();
    }
}
