package pe.edu.upeu.sitraoro;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(SitraOroApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
