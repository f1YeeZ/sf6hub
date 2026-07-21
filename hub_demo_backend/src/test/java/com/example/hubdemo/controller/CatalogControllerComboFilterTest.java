package com.example.hubdemo.controller;

import com.example.hubdemo.common.GlobalExceptionHandler;
import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.service.AntiScrapingService;
import com.example.hubdemo.service.CatalogService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogControllerComboFilterTest {
    private CatalogService catalogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.from(any())).thenReturn(Optional.empty());
        CatalogController controller = new CatalogController(
                catalogService,
                currentUserService,
                mock(RateLimitService.class),
                mock(AntiScrapingService.class)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void decimalDriveCostWithNoMatchesReturnsEmptyPages() throws Exception {
        PageResult<Combo> emptyPage = new PageResult<>(List.of(), 1, 12, 0);
        BigDecimal driveCost = new BigDecimal("5.9");
        when(catalogService.characterCombos(eq(1L), eq("classic"), isNull(), isNull(), isNull(),
                eq(driveCost), eq(99999), isNull(), eq("createdAt"), isNull(), eq(1L), eq(12L)))
                .thenReturn(emptyPage);
        when(catalogService.worldTourCombos(isNull(), isNull(), isNull(), eq(driveCost),
                eq(99999), isNull(), eq("createdAt"), isNull(), eq(1L), eq(12L)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/characters/1/combos")
                        .param("controlType", "classic")
                        .param("driveCost", "5.9")
                        .param("damageMin", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/world-tour/combos")
                        .param("driveCost", "5.9")
                        .param("damageMin", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isEmpty())
                .andExpect(jsonPath("$.total").value(0));

        verify(catalogService).characterCombos(eq(1L), eq("classic"), isNull(), isNull(), isNull(),
                eq(driveCost), eq(99999), isNull(), eq("createdAt"), isNull(), eq(1L), eq(12L));
        verify(catalogService).worldTourCombos(isNull(), isNull(), isNull(), eq(driveCost),
                eq(99999), isNull(), eq("createdAt"), isNull(), eq(1L), eq(12L));
    }

    @Test
    void malformedDriveCostReturnsBadRequestInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/characters/1/combos").param("driveCost", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数格式错误：driveCost"));
    }
}
