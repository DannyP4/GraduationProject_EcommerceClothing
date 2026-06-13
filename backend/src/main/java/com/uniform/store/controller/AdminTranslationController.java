package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.AutoTranslateReport;
import com.uniform.store.service.CatalogTranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/translations")
@RequiredArgsConstructor
@Tag(name = "Admin Translations")
@SecurityRequirement(name = "bearerAuth")
public class AdminTranslationController {

    private final CatalogTranslationService catalogTranslationService;

    @PostMapping("/run")
    @Operation(summary = "Auto-translate the catalog into a target locale (vi/ja) via DeepL; idempotent, fills only missing rows")
    public ApiResponse<AutoTranslateReport> run(
            @RequestParam String locale,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long maxChars) {
        return ApiResponse.ok("Auto-translate complete", catalogTranslationService.run(locale, limit, maxChars));
    }
}
