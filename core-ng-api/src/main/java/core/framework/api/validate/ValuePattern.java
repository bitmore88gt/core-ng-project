package core.framework.api.validate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author neo
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ValuePattern {
    String value();

    String message() default "value must match pattern";
}
