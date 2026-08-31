package com.jf.PetApp.infrastructure.controller.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetRegistrationDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final BigDecimal QUANTITY = BigDecimal.valueOf(10.0);
    private static final BigDecimal PRICE = BigDecimal.valueOf(34.5);

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static AssetRegistrationDto valid() {
        return new AssetRegistrationDto("PETR4", QUANTITY, PRICE, LocalDate.of(2026, 1, 1), InvestmentType.STOCKS);
    }

    @Test
    void accessorsReturnConstructedValues() {
        AssetRegistrationDto dto = valid();

        assertEquals("PETR4", dto.name());
        assertEquals(QUANTITY, dto.quantity());
        assertEquals(PRICE, dto.purchasePrice());
        assertEquals(LocalDate.of(2026, 1, 1), dto.purchaseDate());
        assertEquals(InvestmentType.STOCKS, dto.type());
    }

    @Test
    void validate_WithAllFieldsValid_HasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void validate_WithBlankName_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("", QUANTITY, PRICE, LocalDate.now(), InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithNullQuantity_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", null, PRICE, LocalDate.now(), InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithZeroQuantity_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", BigDecimal.ZERO, PRICE, LocalDate.now(), InvestmentType.STOCKS);

        Set<ConstraintViolation<AssetRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithNegativeQuantity_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", BigDecimal.valueOf(-5.0), PRICE, LocalDate.now(), InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithNullPurchasePrice_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", QUANTITY, null, LocalDate.now(), InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithZeroPurchasePrice_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", QUANTITY, BigDecimal.ZERO, LocalDate.now(), InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithNullPurchaseDate_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", QUANTITY, PRICE, null, InvestmentType.STOCKS);

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithNullType_ProducesAViolation() {
        AssetRegistrationDto dto = new AssetRegistrationDto("PETR4", QUANTITY, PRICE, LocalDate.now(), null);

        assertFalse(validator.validate(dto).isEmpty());
    }
}
