package com.cms.service;

import com.cms.dto.response.CsvImportSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CsvImportService {

    CsvImportSummaryResponse importContactsFromCsv(Long userId, MultipartFile file);
}
