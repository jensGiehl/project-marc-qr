package de.jensgiehl.marcqr.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void rendersIndexAsActiveHomePage() {
        var model = new ConcurrentModel();

        String view = new HomeController().index(model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("activePage")).isEqualTo("home");
    }
}
