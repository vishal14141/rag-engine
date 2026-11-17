package com.rag.engine.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FinancialYearTitleFilter {

    private static final String TITLE_PREFIX =
            "RESERVE BANK OF INDIA ANNUAL REPORT ";

    private FinancialYearTitleFilter() {
    }

    public static List<String> toDocumentTitles(
            List<String> financialYears) {

        if (financialYears == null || financialYears.isEmpty()) {
            return List.of();
        }

        Set<String> titles = new LinkedHashSet<>();

        for (String year : financialYears) {

            if (year == null || !year.trim().matches("\\d{4}-\\d{2}")) {
                throw new IllegalArgumentException(
                        "Invalid financial year: " + year
                );
            }

            titles.add(TITLE_PREFIX + year.trim());
        }

        return List.copyOf(titles);
    }
}
