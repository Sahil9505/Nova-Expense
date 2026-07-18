package com.nova.finance.analytics.web;

import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import com.nova.finance.analytics.AnalyticsFilter;
import com.nova.finance.analytics.AnalyticsService;
import com.nova.finance.analytics.ReportExportService;
import com.nova.finance.analytics.web.dto.AnalyticsExportRequest;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.BudgetAnalyticsResponse;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.ExportFormat;
import com.nova.finance.analytics.web.dto.Granularity;
import com.nova.finance.analytics.web.dto.GoalAnalyticsResponse;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.user.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * The Analytics API. All endpoints are additive (no existing contract changes) and
 * return the standard {@link ApiResponse} envelope, except the export endpoint which
 * streams a file. Every figure is computed by the reusable {@link AnalyticsService}
 * from the authenticated user's real transactions, budgets, and goals.
 */
@RestController
@RequestMapping("/api/analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;
    private final UserRepository userRepository;

    public AnalyticsController(
            AnalyticsService analyticsService,
            ReportExportService reportExportService,
            UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.reportExportService = reportExportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewResponse> overview(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId) {
        AnalyticsFilter filter = resolveFilter(principal.getUserId(), period, from, to, accountId, categoryId);
        String currency = userRepository.findPreferredCurrencyById(principal.getUserId());
        return ApiResponse.ok(analyticsService.overview(filter, currency));
    }

    @GetMapping("/spending")
    public ApiResponse<SpendingOverviewResponse> spending(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId) {
        AnalyticsFilter filter = resolveFilter(principal.getUserId(), period, from, to, accountId, categoryId);
        return ApiResponse.ok(analyticsService.spendingOverview(filter, userRepository.findPreferredCurrencyById(principal.getUserId())));
    }

    @GetMapping("/cash-flow")
    public ApiResponse<CashFlowResponse> cashFlow(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Granularity granularity) {
        AnalyticsFilter filter = resolveFilter(principal.getUserId(), period, from, to, accountId, categoryId);
        return ApiResponse.ok(analyticsService.cashFlow(filter, granularity, userRepository.findPreferredCurrencyById(principal.getUserId())));
    }

    @GetMapping("/categories")
    public ApiResponse<CategoryAnalysisResponse> categories(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId) {
        AnalyticsFilter filter = resolveFilter(principal.getUserId(), period, from, to, accountId, categoryId);
        return ApiResponse.ok(analyticsService.categoryAnalysis(filter, userRepository.findPreferredCurrencyById(principal.getUserId())));
    }

    /** Budget health always reflects each budget's current period (date filter is ignored). */
    @GetMapping("/budgets")
    public ApiResponse<BudgetAnalyticsResponse> budgets(@AuthenticationPrincipal NovaUserPrincipal principal) {
        return ApiResponse.ok(analyticsService.budgetAnalytics(principal.getUserId(), userRepository.findPreferredCurrencyById(principal.getUserId())));
    }

    /** Goal progress always reflects current goals (date filter is ignored). */
    @GetMapping("/goals")
    public ApiResponse<GoalAnalyticsResponse> goals(@AuthenticationPrincipal NovaUserPrincipal principal) {
        return ApiResponse.ok(analyticsService.goalAnalytics(principal.getUserId(), userRepository.findPreferredCurrencyById(principal.getUserId())));
    }

    /**
     * Streams a CSV or PDF report built from the Analytics domain, respecting the exact
     * filter supplied in the body. The bytes are a downloadable file (not the JSON
     * envelope) so the browser saves them directly.
     */
    @PostMapping("/reports/export")
    public ResponseEntity<Resource> export(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @Valid @RequestBody AnalyticsExportRequest request) {
        ExportFormat format = request.format() != null ? request.format() : ExportFormat.CSV;
        AnalyticsFilter filter = new AnalyticsFilter(
                principal.getUserId(), request.from(), request.to(), request.accountId(), request.categoryId());
        String currency = userRepository.findPreferredCurrencyById(principal.getUserId());
        AnalyticsOverviewResponse overview = analyticsService.overview(filter, currency);
        byte[] bytes = reportExportService.export(overview, request);

        String filename = "nova-analytics-" + LocalDate.now() + "." + reportExportService.fileExtension(format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(reportExportService.contentType(format)))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    // -----------------------------------------------------------------------
    // Filter resolution
    // -----------------------------------------------------------------------

    private AnalyticsFilter resolveFilter(
            UUID userId, String period, String from, String to, UUID accountId, UUID categoryId) {
        OffsetDateTime fromDt = from != null ? OffsetDateTime.parse(from) : null;
        OffsetDateTime toDt = to != null ? OffsetDateTime.parse(to) : null;
        if (fromDt == null || toDt == null) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if ("weekly".equalsIgnoreCase(period)) {
                LocalDate start = now.toLocalDate().with(java.time.DayOfWeek.MONDAY);
                fromDt = start.atStartOfDay().atOffset(ZoneOffset.UTC);
                toDt = start.plusWeeks(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            } else if ("yearly".equalsIgnoreCase(period)) {
                LocalDate start = now.toLocalDate().withDayOfYear(1);
                fromDt = start.atStartOfDay().atOffset(ZoneOffset.UTC);
                toDt = start.plusYears(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            } else { // monthly (default) or custom-without-dates
                LocalDate start = now.toLocalDate().withDayOfMonth(1);
                fromDt = start.atStartOfDay().atOffset(ZoneOffset.UTC);
                toDt = start.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            }
        }
        return new AnalyticsFilter(userId, fromDt, toDt, accountId, categoryId);
    }
}
