package core.framework.impl.search;

import core.framework.api.log.ActionLogContext;
import core.framework.api.log.Markers;
import core.framework.api.search.BulkIndexRequest;
import core.framework.api.search.DeleteRequest;
import core.framework.api.search.ElasticSearchType;
import core.framework.api.search.GetRequest;
import core.framework.api.search.Index;
import core.framework.api.search.IndexRequest;
import core.framework.api.search.SearchException;
import core.framework.api.search.SearchRequest;
import core.framework.api.util.StopWatch;
import core.framework.impl.json.JSONReader;
import core.framework.impl.json.JSONWriter;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * @author neo
 */
public final class ElasticSearchTypeImpl<T> implements ElasticSearchType<T> {
    private final Logger logger = LoggerFactory.getLogger(ElasticSearchTypeImpl.class);

    private final Client client;
    private final String index;
    private final String type;
    private final DocumentValidator<T> validator;
    private final long slowOperationThresholdInMs;
    private final JSONReader<T> reader;
    private final JSONWriter<T> writer;

    ElasticSearchTypeImpl(Client client, Class<T> documentClass, Duration slowOperationThreshold) {
        this.client = client;
        this.slowOperationThresholdInMs = slowOperationThreshold.toMillis();
        Index index = documentClass.getDeclaredAnnotation(Index.class);
        this.index = index.index();
        this.type = index.type();
        validator = new DocumentValidator<>(documentClass);
        reader = JSONReader.of(documentClass);
        writer = JSONWriter.of(documentClass);
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        StopWatch watch = new StopWatch();
        long esTookTime = 0;
        String index = request.index == null ? this.index : request.index;
        try {
            SearchRequestBuilder builder = client.prepareSearch(index)
                .setQuery(request.query);
            request.aggregations.forEach(builder::addAggregation);
            request.sorts.forEach(builder::addSort);
            if (request.skip != null) builder.setFrom(request.skip);
            if (request.limit != null) builder.setSize(request.limit);
            logger.debug("search, index={}, type={}, request={}", index, type, builder);
            SearchResponse response = builder.get();
            esTookTime = response.getTookInMillis();
            if (response.getFailedShards() > 0) logger.warn("some shard failed, response={}", response);
            return response;
        } catch (ElasticsearchException e) {
            throw new SearchException(e);   // due to elastic search uses async executor to run, we have to wrap the exception to retain the original place caused the exception
        } finally {
            long elapsedTime = watch.elapsedTime();
            ActionLogContext.track("elasticsearch", elapsedTime);
            logger.debug("search, esTookTime={}, elapsedTime={}", esTookTime, elapsedTime);
            checkSlowOperation(elapsedTime);
        }
    }

    @Override
    public Optional<T> get(GetRequest request) {
        StopWatch watch = new StopWatch();
        String index = request.index == null ? this.index : request.index;
        try {
            GetResponse response = client.prepareGet(index, type, request.id).get();
            if (!response.isExists()) return Optional.empty();
            return Optional.of(reader.fromJSON(response.getSourceAsBytes()));
        } catch (ElasticsearchException e) {
            throw new SearchException(e);   // due to elastic search uses async executor to run, we have to wrap the exception to retain the original place caused the exception
        } finally {
            long elapsedTime = watch.elapsedTime();
            ActionLogContext.track("elasticsearch", elapsedTime);
            logger.debug("get, index={}, type={}, id={}, elapsedTime={}", index, type, request.id, elapsedTime);
            checkSlowOperation(elapsedTime);
        }
    }

    @Override
    public void index(IndexRequest<T> request) {
        StopWatch watch = new StopWatch();
        String index = request.index == null ? this.index : request.index;
        validator.validate(request.source);
        byte[] document = writer.toJSON(request.source);
        try {
            client.prepareIndex(index, type, request.id)
                .setSource(document)
                .get();
        } catch (ElasticsearchException e) {
            throw new SearchException(e);   // due to elastic search uses async executor to run, we have to wrap the exception to retain the original place caused the exception
        } finally {
            long elapsedTime = watch.elapsedTime();
            ActionLogContext.track("elasticsearch", elapsedTime);
            logger.debug("index, index={}, type={}, id={}, elapsedTime={}", index, type, request.id, elapsedTime);
            checkSlowOperation(elapsedTime);
        }
    }

    @Override
    public void bulkIndex(BulkIndexRequest<T> request) {
        StopWatch watch = new StopWatch();
        String index = request.index == null ? this.index : request.index;
        BulkRequestBuilder builder = client.prepareBulk();
        for (Map.Entry<String, T> entry : request.sources.entrySet()) {
            String id = entry.getKey();
            T source = entry.getValue();
            validator.validate(source);
            byte[] document = writer.toJSON(source);
            builder.add(client.prepareIndex(index, type, id)
                .setSource(document));
        }
        long esTookTime = 0;
        try {
            BulkResponse response = builder.get();
            esTookTime = response.getTookInMillis();
            if (response.hasFailures()) throw new SearchException(response.buildFailureMessage());
        } catch (ElasticsearchException e) {
            throw new SearchException(e);   // due to elastic search uses async executor to run, we have to wrap the exception to retain the original place caused the exception
        } finally {
            long elapsedTime = watch.elapsedTime();
            ActionLogContext.track("elasticsearch", elapsedTime);
            logger.debug("bulkIndex, index={}, type={}, size={}, esTookTime={}, elapsedTime={}", index, type, request.sources.size(), esTookTime, elapsedTime);
            checkSlowOperation(elapsedTime);
        }
    }

    @Override
    public boolean delete(DeleteRequest request) {
        StopWatch watch = new StopWatch();
        String index = request.index == null ? this.index : request.index;
        try {
            DeleteResponse response = client.prepareDelete(index, type, request.id).get();
            return response.isFound();
        } catch (ElasticsearchException e) {
            throw new SearchException(e);   // due to elastic search uses async executor to run, we have to wrap the exception to retain the original place caused the exception
        } finally {
            long elapsedTime = watch.elapsedTime();
            ActionLogContext.track("elasticsearch", elapsedTime);
            logger.debug("delete, index={}, type={}, id={}, elapsedTime={}", index, type, request.id, elapsedTime);
            checkSlowOperation(elapsedTime);
        }
    }

    private void checkSlowOperation(long elapsedTime) {
        if (elapsedTime > slowOperationThresholdInMs) {
            logger.warn(Markers.errorCode("SLOW_ES"), "slow elasticsearch operation, elapsedTime={}", elapsedTime);
        }
    }
}
