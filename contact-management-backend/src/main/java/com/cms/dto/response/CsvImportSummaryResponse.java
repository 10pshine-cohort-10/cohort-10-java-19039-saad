package com.cms.dto.response;

import java.util.List;

public record CsvImportSummaryResponse(
        int totalRows,
        int importedRows,
        int duplicateRows,
        int failedRows,
        List<RowErrorDetail> errors
) {
    public record RowErrorDetail(
            int rowNumber,
            String reason
    ) {}
}
