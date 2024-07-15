package me.kpavlov.await4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.*;

class LoomUtilsTest {

    @Test
    void shouldFailOnPlatformThread() {
        assumeThat(Thread.currentThread().isVirtual()).isFalse();
        assertThrows(AssertionError.class, LoomUtils::assertVirtualThread);
    }

    @Test
    void shouldSucceedOnVirtualThread() throws InterruptedException {
        Thread.ofVirtual()
            .start(LoomUtils::assertVirtualThread)
            .join();
    }

}
