package com._ach.backend.search.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for generating text embeddings for semantic search.
 *
 * This is a basic implementation that generates deterministic vectors based on text hashing.
 * In a production environment, you should replace this with:
 * - OpenAI Embeddings API
 * - Sentence Transformers (via REST API or local deployment)
 * - Hugging Face Inference API
 * - Custom ML model
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final int EMBEDDING_DIMENSION = 768;

    /**
     * Generates a 768-dimensional embedding vector for the given text.
     *
     * TODO: Replace this with actual embedding generation using:
     * - OpenAI API: https://platform.openai.com/docs/guides/embeddings
     * - Sentence Transformers: https://www.sbert.net/
     * - Hugging Face: https://huggingface.co/models
     *
     * Example integration with OpenAI:
     * ```java
     * OpenAIService service = new OpenAIService("your-api-key");
     * EmbeddingRequest request = EmbeddingRequest.builder()
     *     .model("text-embedding-ada-002")
     *     .input(Collections.singletonList(text))
     *     .build();
     * List<Embedding> embeddings = service.createEmbeddings(request).getData();
     * return embeddings.get(0).getEmbedding();
     * ```
     */
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text: {}", text.substring(0, Math.min(text.length(), 50)));

        try {
            // This is a placeholder implementation
            // It creates a deterministic but semantically meaningless vector
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.toLowerCase().getBytes(StandardCharsets.UTF_8));

            float[] embedding = new float[EMBEDDING_DIMENSION];

            // Use the hash to seed a deterministic random generator
            long seed = 0;
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }

            java.util.Random random = new java.util.Random(seed);

            // Generate normalized random vector
            double sumSquares = 0;
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] = (float) (random.nextGaussian());
                sumSquares += embedding[i] * embedding[i];
            }

            // Normalize to unit length
            float norm = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] /= norm;
            }

            return embedding;

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    /**
     * Calculates cosine similarity between two embedding vectors
     */
    public float cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
