package com.rag.engine.service.ingestion.document;

import com.rag.engine.dto.ExtractedParagraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ParagraphExtractionService {

    public List<ExtractedParagraph> extractParagraphs(List<String> pages) {

        List<ExtractedParagraph> list = new ArrayList<>();

        Pattern sectionParaPattern = Pattern.compile(
                "^\\s*([IVXLCDM]+)\\.(\\d+(?:\\.\\d+)*)\\s+(.*)$"
        );

        ExtractedParagraph extractedParagraph = null;
        StringBuilder content = new StringBuilder();

        boolean textBodyStart = false;
        int pageOffset = 0;
        boolean textBodyEnd = false;


        for (int i = 0; i < pages.size(); ++i) {

            if (textBodyEnd) break;

            String pageText = pages.get(i);
            String[] lines = pageText.split("\n");

            for (String line : lines) {

                if (!textBodyStart && line.trim().startsWith("I.1")) {
                    textBodyStart = true;
                    pageOffset = i;
                }
                if (!textBodyStart) continue;

                if (line.trim().contains("APPENDIX TABLE") && i >= pages.size() / 2) textBodyEnd = true;
                if (textBodyEnd) break;

                Matcher matcher = sectionParaPattern.matcher(line);

                if (matcher.matches()) {

                    if (extractedParagraph != null) {
                        extractedParagraph.setText(content.toString());
                        list.add(extractedParagraph);
                        content = new StringBuilder();
                    }

                    String section = matcher.group(1);
                    String paragraph = matcher.group(2);

                    extractedParagraph = new ExtractedParagraph(section, paragraph, "",i - pageOffset + 1);
                    content.append(matcher.group(3));
                } else if (extractedParagraph != null && !line.isBlank()) {
                    content.append(" ").append(line.trim());
                }
            }
        }
        if (extractedParagraph != null) {
            extractedParagraph.setText(content.toString().trim());
            list.add(extractedParagraph);
        }
        return list;
    }
}
