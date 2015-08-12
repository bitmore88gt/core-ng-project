package core.framework.impl.web;

import core.framework.api.web.Controller;

import java.lang.reflect.Method;

/**
 * @author neo
 */
public class ControllerHolder {
    public final Controller controller;
    public final Method targetMethod;
    public final String controllerInfo;

    public final boolean skipInterceptor;
    public String action;

    public ControllerHolder(Controller controller) {
        this(controller, null, false);
    }

    public ControllerHolder(Controller controller, boolean skipInterceptor) {
        this(controller, null, skipInterceptor);
    }

    public ControllerHolder(Controller controller, Method targetMethod) {
        this(controller, targetMethod, false);
    }

    public ControllerHolder(Controller controller, Method targetMethod, boolean skipInterceptor) {
        this.controller = controller;
        this.skipInterceptor = skipInterceptor;

        if (targetMethod == null) {
            ControllerInspector inspector = new ControllerInspector(controller);
            this.targetMethod = inspector.targetMethod;
            controllerInfo = inspector.targetClassName + "." + inspector.targetMethodName;
        } else {
            this.targetMethod = targetMethod;
            controllerInfo = targetMethod.getDeclaringClass().getCanonicalName() + "." + targetMethod.getName();
        }
    }
}
