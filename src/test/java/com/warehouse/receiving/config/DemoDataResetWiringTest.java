package com.warehouse.receiving.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.warehouse.receiving.service.DemoDataResetService;

/**
 * The reset deletes every row in the demo tables, so which profiles wire it is a
 * safety property rather than a detail. Contexts load without a database because
 * spring.sql.init.mode is never.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.demo.reset.initial-delay=PT24H")
class DemoDataResetWiringTest {

    @Autowired private ApplicationContext context;

    @Test
    void devProfileWiresTheResetService() {
        assertThat(context.getBeanNamesForType(DemoDataResetService.class)).hasSize(1);
    }

    @Test
    void devProfileRegistersTheScheduler() {
        assertThat(context.getBeanNamesForType(DemoDataResetConfig.DemoDataResetScheduler.class))
                .hasSize(1);
    }
}

/** Production holds real receiving data; nothing may schedule a delete against it. */
@SpringBootTest
@ActiveProfiles("prod")
class DemoDataResetAbsentInProdTest {

    @Autowired private ApplicationContext context;

    @Test
    void prodProfileWiresNoResetAtAll() {
        assertThat(context.getBeanNamesForType(DemoDataResetService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DemoDataResetConfig.DemoDataResetScheduler.class))
                .isEmpty();
    }
}

/** The kill switch has to actually remove the beans, not just idle them. */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.demo.reset.enabled=false")
class DemoDataResetDisabledTest {

    @Autowired private ApplicationContext context;

    @Test
    void disablingTheFlagRemovesTheResetBeans() {
        assertThat(context.getBeanNamesForType(DemoDataResetService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DemoDataResetConfig.DemoDataResetScheduler.class))
                .isEmpty();
    }
}
