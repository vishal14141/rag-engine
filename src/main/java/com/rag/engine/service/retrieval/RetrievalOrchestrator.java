package com.rag.engine.service.retrieval;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.strategy.RetrievalStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * it will be responsible for chunk retrieval based on the strategy provided in config
 */
@Slf4j
@Service
public class RetrievalOrchestrator {

    /**
     * this class is responsible to fetch the retrieval result for all the strategy
     * for each strategy, it needs the configuration parameters like limit.
     */

    private final RetrievalProperties retrievalProperties;
    private final Map<String, RetrievalStrategy> strategies;


    public RetrievalOrchestrator(RetrievalProperties retrievalProperties, List<RetrievalStrategy> retrievalStrategies) {
        this.retrievalProperties = retrievalProperties;

        this.strategies = retrievalStrategies.stream()
                .collect(Collectors.toMap(
                        RetrievalStrategy::getName,
                        Function.identity()
                ));
    }

    public List<Map<String, List<RankedChunk>>> retrieve(List<String> queryList, List<String> financialYears) {

        List<Map<String, List<RankedChunk>>> list = new ArrayList<>();

        for (String query : queryList) {
            log.info("Retrieving results for query: {}", query);

            Map<String, List<RankedChunk>> map = new HashMap<>();

            for (RetrievalProperties.StrategyConfig strategyConfig : retrievalProperties.getStrategies()) {
                if (!strategyConfig.isEnabled()) continue;

                RetrievalStrategy strategy = strategies.get(strategyConfig.getName());

                if (strategy == null) {
                    throw new IllegalStateException(
                            "No RetrievalStrategy implementation found for: "
                                    + strategyConfig.getName()
                    );
                }

                long start = System.currentTimeMillis();
                List<RankedChunk> chunks = strategy.retrieve(query, strategyConfig.getCandidateLimit(), financialYears);
                log.info("Retrieval Strategy '{}' returned {} results", strategy.getName(), chunks.size());
                log.info("Retrieval Strategy '{}' execution time: {} ms", strategyConfig.getName(), System.currentTimeMillis() - start);
                if (chunks.isEmpty()) continue;
                map.put(strategyConfig.getName(), chunks);
            }
            list.add(map);
        }
        return list;
    }
}
