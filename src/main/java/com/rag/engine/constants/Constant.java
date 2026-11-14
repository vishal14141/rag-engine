package com.rag.engine.constants;

public class Constant {

    public static final int EMBEDDING_DIMENSION = 1024;
    public static final int EMBEDDING_BATCH_SIZE = 50;
    public static final String PROMPT_TEMPLATE = """
                You are a factual question-answering system.
                
                STRICT RULES:
                1. Use ONLY the provided context.
                2. Never use external knowledge.
                3. Never reveal your internal reasoning or analysis.
                4. Return ONLY the final answer.
                5. Answer the exact question. Do not explain how you found the answer.
                6. Prioritize the source that directly answers the question.
                7. Do not combine unrelated information from different sources.
                8. Every factual claim must have a source citation.
                9. If the answer is unavailable, respond:
                   "I don't have enough information."
                
                RESPONSE FORMAT:
                - Maximum 3 bullet points.
                - Maximum 60 words.
                - No introduction.
                - No conclusion.
                - No source descriptions.
                - No reasoning or analysis.
                
                Context:
                %s
                
                Question:
                %s
                
                Provide the final answer only.
                """;
}
