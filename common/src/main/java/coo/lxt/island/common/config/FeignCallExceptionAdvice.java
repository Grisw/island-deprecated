package coo.lxt.island.common.config;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import coo.lxt.island.common.exception.ValidateException;
import coo.lxt.island.common.vo.FeignExceptionWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Optional;

/**
 * This ExceptionHandler catch the exception and pack it into a FeignExceptionWrapper object,
 *
 * This class provide a same behavior between local method and remote method,
 * including exception handling.
 *
 * This class works on the Feign server side,
 * and it should work with FeignSkipBadRequestsErrorDecoder on the client side.
 *
 * To enable the ExceptionHandler in a module:
 * Derive from this class and annotate with @RestControllerAdvice.
 *
 * @see FeignSkipBadRequestsErrorDecoder
 * @see FeignExceptionWrapper
 */
@Slf4j
public class FeignCallExceptionAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public FeignExceptionWrapper handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        Optional<FieldError> fieldError = Optional.ofNullable(exception.getBindingResult().getFieldError());
        return new FeignExceptionWrapper(new ValidateException(exception.getMessage(),
                fieldError.map(FieldError::getField).orElse("<unknown>"),
                fieldError.map(FieldError::getDefaultMessage).orElse("<unknown>")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public FeignExceptionWrapper handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return new FeignExceptionWrapper(new ValidateException(exception.getMessage(), exception.getParameterName(), "missing"));
    }

    @ExceptionHandler(HystrixBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public FeignExceptionWrapper handleHystrixBadRequestException(HystrixBadRequestException exception) {
        return new FeignExceptionWrapper(exception);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public FeignExceptionWrapper feignExceptionHandler(Exception exception){
        log.error("Unknown exception.", exception);
        return new FeignExceptionWrapper(exception);
    }
}
