package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatsDto {
    private long totalCount;
    private long openCount;
    private long progressCount;
    private long escalatedCount;
    private long resolvedCount;
}
