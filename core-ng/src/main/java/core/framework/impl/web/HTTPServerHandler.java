package core.framework.impl.web;

import core.framework.api.web.Response;
import core.framework.api.web.ResponseImpl;
import core.framework.impl.log.ActionLog;
import core.framework.impl.log.LogManager;
import core.framework.impl.web.response.ResponseHandler;
import core.framework.impl.web.route.Route;
import core.framework.impl.web.session.SessionManager;
import core.framework.impl.web.site.SiteManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
public class HTTPServerHandler implements HttpHandler {
    public static final String HEADER_REF_ID = "ref-id";
    public static final String HEADER_TRACE = "trace";
    public static final String HEADER_CLIENT = "client";

    public final BeanValidator validator = new BeanValidator();
    public final Route route = new Route();
    public final Interceptors interceptors = new Interceptors();
    public final WebContextImpl webContext = new WebContextImpl();
    public final HTTPServerErrorHandler errorHandler;

    private final Logger logger = LoggerFactory.getLogger(HTTPServerHandler.class);
    private final RequestParser requestParser = new RequestParser();
    private final LogManager logManager;
    private final SessionManager sessionManager;
    private final ResponseHandler responseHandler;

    public HTTPServerHandler(LogManager logManager, SiteManager siteManager) {
        this.logManager = logManager;
        sessionManager = siteManager.sessionManager;

        responseHandler = new ResponseHandler(validator, siteManager.templateManager);
        errorHandler = new HTTPServerErrorHandler(responseHandler, logManager);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        logManager.start();
        RequestImpl request = new RequestImpl(exchange, validator);
        try {
            logger.debug("=== http transaction begin ===");
            ActionLog actionLog = logManager.currentActionLog();
            requestParser.parse(request, exchange, actionLog);
            request.session = sessionManager.load(request);

            HeaderMap headers = exchange.getRequestHeaders();

            String client = headers.getFirst(HTTPServerHandler.HEADER_CLIENT);
            if (client != null) actionLog.putContext("client", client);

            String refId = headers.getFirst(HTTPServerHandler.HEADER_REF_ID);
            if (refId != null) actionLog.refId(refId);

            ControllerHolder controller = route.get(request.path(), request.method(), request.pathParams);
            actionLog.action(controller.action);
            actionLog.putContext("controller", controller.controllerInfo);
            logger.debug("controllerClass={}", controller.controller.getClass().getCanonicalName());

            // trigger trace after action is determined due to trace log use action as part of path, is there better way?
            if ("true".equals(headers.getFirst(HEADER_TRACE))) {
                actionLog.triggerTraceLog();
            }

            webContext.initialize(request);
            Response response = new InvocationImpl(controller, interceptors, request, webContext).proceed();
            sessionManager.save(request, exchange);
            responseHandler.handle((ResponseImpl) response, exchange, request);
        } catch (Throwable e) {
            errorHandler.handleError(e, exchange, request);
        } finally {
            webContext.cleanup();
            logger.debug("=== http transaction end ===");
            logManager.end();
        }
    }
}
