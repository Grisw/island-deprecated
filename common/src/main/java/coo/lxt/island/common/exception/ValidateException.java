package coo.lxt.island.common.exception;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This class work as a replacement for MethodArgumentNotValidException
 * and MissingServletRequestParameterException.
 *
 * Also this is an example for customized client exception.
 *
 * Use @ResponseStatus to indicate the status code.
 *
 * You can define several serializable fields in the class, which hold the
 * values that you want to pass to the client side.
 *
 * You must define a constructor with a single String argument, which calls super(message).
 * This helps deserialization on the client side.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidateException extends HystrixBadRequestException {

    @Setter
    @Getter
    private String argument;

    @Setter
    @Getter
    private String reason;

    public ValidateException(String message) {
        super(message);
    }

    public ValidateException(String message, String argument, String reason) {
        super(message);
        this.argument = argument;
        this.reason = reason;
    }
}
