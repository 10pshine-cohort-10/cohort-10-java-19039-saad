package com.cms.service;

public interface CsvExportService {

    byte[] exportContactsToCsv(Long userId);

    byte[] getCsvTemplate();
}
