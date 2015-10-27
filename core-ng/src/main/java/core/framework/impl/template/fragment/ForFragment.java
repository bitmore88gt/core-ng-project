package core.framework.impl.template.fragment;

import core.framework.api.util.Exceptions;
import core.framework.impl.reflect.GenericTypes;
import core.framework.impl.template.CallStack;
import core.framework.impl.template.expression.CallTypeStack;
import core.framework.impl.template.expression.ExpressionBuilder;
import core.framework.impl.template.expression.ExpressionHolder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neo
 */
public class ForFragment extends ContainerFragment {
    private static final Pattern STATEMENT_PATTERN = Pattern.compile("([a-zA-Z1-9]+):([#a-zA-Z1-9\\.\\(\\)]+)");
    public final String variable;
    public final Class<?> valueClass;
    private final ExpressionHolder expression;

    public ForFragment(String statement, CallTypeStack stack, String location) {
        Matcher matcher = STATEMENT_PATTERN.matcher(statement);
        if (!matcher.matches())
            throw Exceptions.error("statement must match \"var:list\", statement={}, location={}", statement, location);

        variable = matcher.group(1);
        String list = matcher.group(2);

        ExpressionBuilder builder = new ExpressionBuilder(list, stack, location);
        expression = builder.build();
        if (!GenericTypes.isGenericList(expression.returnType))
            throw Exceptions.error("for statement must return List<T>, list={}, returnType={}, location={}",
                list, expression.returnType.getTypeName(), location);
        valueClass = GenericTypes.listValueClass(expression.returnType);
    }

    @Override
    public void process(StringBuilder builder, CallStack stack) {
        List<?> list = (List<?>) expression.eval(stack);
        for (Object item : list) {
            stack.contextObjects.put(variable, item);
            processChildren(builder, stack);
        }
        stack.contextObjects.remove(variable);
    }
}
