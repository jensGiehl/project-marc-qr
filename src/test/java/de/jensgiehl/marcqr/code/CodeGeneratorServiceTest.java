package de.jensgiehl.marcqr.code;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorServiceTest {

    private final CodeGeneratorService service = new CodeGeneratorService();

    @Test
    void calculatesPossibilitiesWithLimitedDigits() {
        var request = new CodeGenerationRequest(List.of("A", "B", "1", "2"), 2, 1, 1);

        assertThat(service.countPossibilities(request)).hasToString("12");
    }

    @Test
    void generatesUniqueCodesThatRespectRules() {
        var request = new CodeGenerationRequest(List.of("A", "B", "C", "2", "3"), 4, 1, 100);

        List<String> codes = service.generate(request);

        assertThat(codes).hasSize(100).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(code -> {
            assertThat(code).hasSize(4).matches("[ABC23]+");
            assertThat(code.chars().filter(Character::isDigit).count()).isLessThanOrEqualTo(1);
        });
    }

    @Test
    void canGenerateEntireSmallCodeSpaceWithoutDuplicates() {
        var request = new CodeGenerationRequest(List.of("A", "B"), 3, 0, 8);

        assertThat(service.generate(request)).hasSize(8).doesNotHaveDuplicates();
    }

    @Test
    void rejectsCountAbovePossibilities() {
        var request = new CodeGenerationRequest(List.of("A"), 2, 0, 2);

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(CodeValidationException.class)
                .hasMessageContaining("nur 1");
    }
}
