package com.rag.engine.constants;

public class Constant {

    public static final int EMBEDDING_DIMENSION = 1024;
    public static final int EMBEDDING_BATCH_SIZE = 32;
    public static final String PROMPT_TEMPLATE_LLM = """
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
            9. Answer must contain source citation to every fact.
            10. If the answer is unavailable, respond:
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

    public static final String QUERY_REWRITE_SYSTEM_PROMPT = """
            You are a query rewriting component for a Retrieval-Augmented
            Generation (RAG) system.

            Your task is to generate exactly 3 independent search queries
            from the user's original question.

            Follow these rules:
            1. Preserve the original question's intent.
            2. Break complex questions into independently searchable aspects.
            3. Make each query self-contained and understandable without
               the original question.
            4. Do not introduce facts or assumptions not present in the
               original question.
            5. Avoid generating semantically duplicate queries.
            6. Use terminology that is likely to appear in source documents.

            Return ONLY a valid JSON array of 3 strings.
            Do not include explanations, markdown, or code fences.
            """;
}
