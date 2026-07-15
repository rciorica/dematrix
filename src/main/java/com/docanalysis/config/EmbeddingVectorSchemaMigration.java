package com.docanalysis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repairs legacy schema drift where vector_data was previously created as bytea.
 * The current entity mapping expects a PostgreSQL float array.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingVectorSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String udtName = jdbcTemplate.query(
                    """
                    SELECT udt_name
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'embedding_vectors'
                      AND column_name = 'vector_data'
                    """,
                    rs -> rs.next() ? rs.getString("udt_name") : null
            );

            if (udtName == null) {
                log.debug("Skipping embedding schema migration: embedding_vectors.vector_data not found");
                return;
            }

            if (!"bytea".equalsIgnoreCase(udtName)) {
                log.debug("Embedding schema is compatible (udt_name={})", udtName);
                return;
            }

            Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM embedding_vectors", Integer.class);
            if (rowCount != null && rowCount > 0) {
                // Embeddings are derived data and can be regenerated from chunks.
                log.warn("Detected legacy bytea embedding column. Removing {} embedding rows before migration.", rowCount);
                jdbcTemplate.update("TRUNCATE TABLE embedding_vectors");
            }

            jdbcTemplate.update(
                    "ALTER TABLE embedding_vectors " +
                    "ALTER COLUMN vector_data TYPE double precision[] " +
                    "USING ARRAY[]::double precision[]"
            );

            log.info("Migrated embedding_vectors.vector_data from bytea to double precision[]");
        } catch (Exception ex) {
            log.error("Failed embedding schema compatibility migration", ex);
        }
    }
}