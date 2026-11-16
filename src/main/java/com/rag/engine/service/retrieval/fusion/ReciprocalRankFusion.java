package com.rag.engine.service.retrieval.fusion;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.service.retrieval.dto.FusedChunk;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReciprocalRankFusion implements FusionStrategy{


    private final RetrievalProperties.FusionConfig fusionConfig;

    public ReciprocalRankFusion(RetrievalProperties properties) {
        this.fusionConfig = properties.getFusion();
    }

    @Override
    public String getName() {
        return "rrf";
    }

    @Override
    public List<RankedChunk> fuse(
            List<Map<String, List<RankedChunk>>> results,
            int limit) {

        if (results == null || results.isEmpty() || limit <= 0) {
            return List.of();
        }

        int rrfK = fusionConfig.getRrfK();

        Map<UUID, FusionAccumulator> accumulators = new HashMap<>();

        for (int queryIndex = 0; queryIndex < results.size(); queryIndex++) {

            Map<String, List<RankedChunk>> strategyResults =
                    results.get(queryIndex);

            if (strategyResults == null) {
                continue;
            }

            for (Map.Entry<String, List<RankedChunk>> entry
                    : strategyResults.entrySet()) {

                String rankingListName =
                        "query-" + queryIndex + ":" + entry.getKey();

                List<RankedChunk> chunks = entry.getValue();

                if (chunks == null || chunks.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < chunks.size(); i++) {

                    RankedChunk rankedChunk = chunks.get(i);
                    UUID chunkId = rankedChunk.chunk().id();

                    FusionAccumulator accumulator =
                            accumulators.computeIfAbsent(
                                    chunkId,
                                    id -> new FusionAccumulator(
                                            rankedChunk.chunk()
                                    )
                            );

                    int rank = i + 1;

                    accumulator.addRank(rankingListName, rank);
                    accumulator.addScore(1.0 / (rrfK + rank));
                }
            }
        }

        return accumulators.values().stream()
                .map(FusionAccumulator::toFusedChunk)
                .sorted(Comparator.comparingDouble(
                        FusedChunk::score
                ).reversed())
                .limit(limit)
                .map(chunk -> (RankedChunk) chunk)
                .toList();
    }

    private static class FusionAccumulator {

        private final RetrievedChunk chunk;
        private final Map<String, Integer> strategyRanks = new LinkedHashMap<>();
        private double score;

        private FusionAccumulator(RetrievedChunk chunk) {
            this.chunk = chunk;
        }

        private void addRank(String strategy, int rank) {
            strategyRanks.put(strategy, rank);
        }

        private void addScore(double score) {
            this.score += score;
        }

        private FusedChunk toFusedChunk() {
            return new FusedChunk(chunk, strategyRanks, score);
        }
    }
}
