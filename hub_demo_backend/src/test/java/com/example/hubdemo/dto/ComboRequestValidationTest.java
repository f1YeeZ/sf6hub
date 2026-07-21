package com.example.hubdemo.dto;

import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComboRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void damageAndAdvantageFramesAreRequired() {
        ComboRequest request = new ComboRequest(
                1L,
                "2MK",
                "2MK > DR",
                "2MK > DR",
                null,
                BigDecimal.ZERO,
                0,
                " ",
                "中等",
                false,
                "classic",
                List.of(),
                "counter-hit",
                List.of("counter-hit"),
                "/uploads/combo.mp4",
                "",
                "",
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("damage", "advantageFrames");
    }
}
