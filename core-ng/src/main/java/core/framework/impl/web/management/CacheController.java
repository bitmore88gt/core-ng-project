package core.framework.impl.web.management;

import core.framework.api.http.ContentType;
import core.framework.api.web.Request;
import core.framework.api.web.Response;
import core.framework.api.web.exception.NotFoundException;
import core.framework.impl.cache.CacheImpl;
import core.framework.impl.cache.CacheManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class CacheController {
    private final CacheManager cacheManager;

    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Response get(Request request) throws Exception {
        ControllerHelper.validateFromLocalNetwork(request.clientIP());
        String name = request.pathParam("name");
        String key = request.pathParam("key");
        CacheImpl<?> cache = cache(name);
        String value = cache.get(key).orElseThrow(() -> new NotFoundException("cache key not found, name=" + name + ", key=" + key));
        return Response.text(value, ContentType.APPLICATION_JSON);
    }

    public Response delete(Request request) throws Exception {
        ControllerHelper.validateFromLocalNetwork(request.clientIP());
        String name = request.pathParam("name");
        String key = request.pathParam("key");
        CacheImpl<?> cache = cache(name);
        cache.evict(key);
        return Response.text("cache evicted, name=" + name + ", key=" + key, ContentType.TEXT_PLAIN);
    }

    public Response list(Request request) throws Exception {
        ControllerHelper.validateFromLocalNetwork(request.clientIP());
        List<CacheView> caches = cacheManager.caches().stream().map(this::view).collect(Collectors.toList());
        return Response.bean(caches);
    }

    private CacheImpl<?> cache(String name) {
        return cacheManager.get(name).orElseThrow(() -> new NotFoundException("cache not found, name=" + name));
    }

    private CacheView view(CacheImpl<?> cache) {
        CacheView view = new CacheView();
        view.name = cache.name;
        view.type = cache.valueType.getTypeName();
        view.duration = (int) cache.duration.getSeconds();
        return view;
    }
}
