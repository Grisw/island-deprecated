package coo.lxt.island.common.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This class wrap a Throwable object, and transfer it through a Feign call.
 * And throw a same exception on the client side.
 *
 * @see coo.lxt.island.common.config.FeignCallExceptionAdvice
 * @see coo.lxt.island.common.config.FeignSkipBadRequestsErrorDecoder
 */
@Slf4j
@Data
@NoArgsConstructor
public class FeignExceptionWrapper {

    private String className;
    private String message;
    private Map<String, Object> properties;
    private FeignExceptionWrapper cause;

    public FeignExceptionWrapper(Throwable e) {
        this.message = e.getMessage();
        this.className = e.getClass().getName();
        if (e.getCause() != null) {
            this.cause = new FeignExceptionWrapper(e.getCause());
        } else {
            this.cause = null;
        }
        this.properties = new HashMap<>();
        for (Field field : e.getClass().getDeclaredFields()) {
            if (StringUtils.equals(field.getName(), "serialVersionUID")) {
                continue;
            }
            field.setAccessible(true);
            try {
                this.properties.put(field.getName(), field.get(e));
            } catch (IllegalAccessException illegalAccessException) {
                log.warn("Error accessing field: {}, won't serialized. Reason: {}", field.getName(), illegalAccessException.getMessage());
            }
        }
    }

    @JsonIgnore
    public Throwable getOriginThrowable() throws ReflectiveOperationException {
        Class<?> eClass = Class.forName(this.className);
        Throwable e = (Throwable) eClass.getConstructor(String.class).newInstance(message);
        if (cause != null) {
            e.initCause(cause.getOriginThrowable());
        }
        for (Map.Entry<String, Object> kv : properties.entrySet()) {
            Field field = eClass.getDeclaredField(kv.getKey());
            field.setAccessible(true);
            field.set(e, kv.getValue());
        }
        return e;
    }
}
