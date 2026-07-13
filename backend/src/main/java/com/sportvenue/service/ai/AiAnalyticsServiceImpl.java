package com.sportvenue.service.ai;

import com.sportvenue.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalyticsServiceImpl implements AiAnalyticsService {

    private final AiUsageLogRepository repository;

    @Override
    public Map<String, Object> getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> result = new HashMap<>();

        long totalRequests = repository.countTotalRequests(startDate, endDate);
        result.put("totalRequests", totalRequests);

        Double avgLatency = repository.getAverageLatency(startDate, endDate);
        result.put("averageLatencyMs", avgLatency != null ? Math.round(avgLatency) : 0);

        Double avgConfidence = repository.getAverageConfidence(startDate, endDate);
        result.put("averageConfidence", avgConfidence != null ? avgConfidence : 0.0);

        long validationErrors = repository.countValidationErrors(startDate, endDate);
        result.put("validationErrors", validationErrors);
        
        double errorRate = totalRequests > 0 ? (double) validationErrors / totalRequests : 0.0;
        result.put("errorRate", errorRate);

        List<Object[]> intentDistData = repository.getIntentDistribution(startDate, endDate);
        Map<String, Long> intentDistribution = intentDistData.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
        result.put("intentDistribution", intentDistribution);

        return result;
    }
}
