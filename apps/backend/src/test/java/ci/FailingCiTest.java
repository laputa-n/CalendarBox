package ci;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class FailingCiTest {
    @Test
    void intentionalFail() {
        fail("Intentional CI failure to verify branch protection.");
    }
}
