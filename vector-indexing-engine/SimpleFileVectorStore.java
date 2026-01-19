package io.codeintel.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import io.codeintel.model.CodeChunk;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * File-based Vector Store with:
 *  - metadata filtering
 *  - deletion & stale cleanup
 *  - incremental embedding cache
 *  - optional memory-mapped embeddings
 *
 * No external DB required.
 */
public class SimpleFileVectorStore implements VectorIndex {

    /* ==================================================
       CONFIG
       ================================================== */

    private static final int MMAP_THRESHOLD = 10_000;
    private static final int FLOAT_BYTES = 4;

    /* ==================================================
       DEPENDENCIES
       ================================================== */

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /* ==================================================
       STORAGE
       ================================================== */

    private final Path metaFile;
    private final Path binFile;

    // symbol -> entry
    private Map<String, VectorEntry> store = new HashMap<>();

    // hash -> embedding (incremental cache)
    private final Map<String, List<Double>> embeddingCache = new HashMap<>();

    // memory-mapped binary storage
    private MappedByteBuffer mmap;
    private FileChannel channel;
    private boolean mmapEnabled = false;
    private long mmapPosition = 0;

    /* ==================================================
       INIT
       ================================================== */

    public SimpleFileVectorStore(
            EmbeddingModel embeddingModel,
            Path storageDir
    ) throws Exception {

        this.embeddingModel = embeddingModel;
        this.metaFile = storageDir.resolve("vectors.json");
        this.binFile = storageDir.resolve("vectors.bin");

        Files.createDirectories(storageDir);

        loadMetadata();
        maybeEnableMmap();
    }

    /* ==================================================
       VectorIndex API
       ================================================== */

    @Override
    public void upsert(CodeChunk chunk) {

        lock.writeLock().lock();
        try {
            // stale cleanup
            deleteIfHashMismatch(chunk.symbol(), chunk.hash());

            // reuse embedding if possible
            List<Double> embedding =
                    embed(chunk.content(), chunk.hash());

            VectorPointer ptr = mmapEnabled
                    ? writeToMmap(embedding)
                    : null;

            Map<String, Object> meta = new HashMap<>(chunk.meta());
            meta.put("hash", chunk.hash());
            meta.put("kind", chunk.kind().name());

            store.put(
                    chunk.symbol(),
                    new VectorEntry(
                            chunk.symbol(),
                            ptr,
                            embedding,
                            meta
                    )
            );

            persistMetadata();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<CodeChunk> search(
            String query,
            int k,
            Map<String, Object> filter
    ) {

        List<Double> qVec = embed(query, "QUERY:" + query);

        lock.readLock().lock();
        try {
            return store.values().stream()
                    .filter(e -> matchesFilter(e.metadata(), filter))
                    .map(e -> Map.entry(
                            e,
                            cosineSimilarity(
                                    qVec,
                                    readEmbedding(e)
                            )
                    ))
                    .sorted(Map.Entry.<VectorEntry, Double>
                            comparingByValue().reversed())
                    .limit(k)
                    .map(e -> new CodeChunk(
                            e.getKey().id(),
                            CodeChunk.Kind.UNKNOWN,
                            "",
                            e.getKey().metadata(),
                            ""
                    ))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(String symbol) {
        lock.writeLock().lock();
        try {
            store.remove(symbol);
            persistMetadata();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        lock.writeLock().lock();
        try {
            store.keySet().removeIf(k -> k.startsWith(prefix));
            persistMetadata();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteIfHashMismatch(String symbol, String newHash) {
        VectorEntry e = store.get(symbol);
        if (e == null) return;

        String oldHash = (String) e.metadata().get("hash");
        if (!newHash.equals(oldHash)) {
            store.remove(symbol);
        }
    }

    /* ==================================================
       FILTERING
       ================================================== */

    private boolean matchesFilter(
            Map<String, Object> meta,
            Map<String, Object> filter
    ) {
        if (filter == null || filter.isEmpty()) return true;
        if (meta == null) return false;

        for (var f : filter.entrySet()) {
            Object v = meta.get(f.getKey());
            if (v == null || !v.toString().equals(f.getValue().toString())) {
                return false;
            }
        }
        return true;
    }

    /* ==================================================
       EMBEDDINGS
       ================================================== */

    private List<Double> embed(String text, String hash) {

        if (embeddingCache.containsKey(hash)) {
            return embeddingCache.get(hash);
        }

        EmbeddingResponse response =
                embeddingModel.embedForResponse(List.of(text));

        List<Double> vec = response.getResults()
                .get(0)
                .getOutput()
                .stream()
                .map(Number::doubleValue)
                .toList();

        embeddingCache.put(hash, vec);
        return vec;
    }

    /* ==================================================
       MMAP STORAGE
       ================================================== */

    private void maybeEnableMmap() throws Exception {
        if (store.size() < MMAP_THRESHOLD) return;

        mmapEnabled = true;
        channel = FileChannel.open(
                binFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        mmap = channel.map(
                FileChannel.MapMode.READ_WRITE,
                0,
                1024L * 1024 * 1024 // 1GB initial
        );
    }

    private VectorPointer writeToMmap(List<Double> vec) {
        long offset = mmapPosition;
        for (double d : vec) {
            mmap.putFloat((float) d);
            mmapPosition += FLOAT_BYTES;
        }
        return new VectorPointer(offset, vec.size());
    }

    private List<Double> readEmbedding(VectorEntry e) {
        if (!mmapEnabled || e.pointer() == null) {
            return e.embedding();
        }

        VectorPointer p = e.pointer();
        ByteBuffer dup = mmap.duplicate();
        dup.position((int) p.offset());

        List<Double> out = new ArrayList<>();
        for (int i = 0; i < p.dim(); i++) {
            out.add((double) dup.getFloat());
        }
        return out;
    }

    /* ==================================================
       PERSISTENCE
       ================================================== */

    private void loadMetadata() throws Exception {
        if (!Files.exists(metaFile)) {
            persistMetadata();
            return;
        }

        store = mapper.readValue(
                metaFile.toFile(),
                new TypeReference<>() {}
        );
    }

    private void persistMetadata() throws Exception {
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(metaFile.toFile(), store);
    }

    /* ==================================================
       MATH
       ================================================== */

    private double cosineSimilarity(
            List<Double> a,
            List<Double> b
    ) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-9);
    }

    /* ==================================================
       INTERNAL RECORDS
       ================================================== */

    public record VectorEntry(
            String id,
            VectorPointer pointer,
            List<Double> embedding,
            Map<String, Object> metadata
    ) {}

    public record VectorPointer(
            long offset,
            int dim
    ) {}
}
