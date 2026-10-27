package com.finsecure.pay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class ControlledFailureTest {

    @Test
    void blocksDeploymentForDemonstration() {
        fail("Controlled failure: deployment must not proceed");
    }
}