package coo.lxt.island.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import coo.lxt.island.common.vo.FeignExceptionWrapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * This ErrorDecoder unpack the FeignExceptionWrapper object,
 * and throw the embedded exception to the context invoking the Feign call.
 *
 * This class provide a same behavior between local method and remote method,
 * including exception handling.
 *
 * To pass client exception through Feign fallback (Hystrix),
 * You should define all client exception on your own and derive it
 * from HystrixBadRequestException.
 *
 * This class works on the Feign client side,
 * and it should work with FeignCallExceptionAdvice on the server side.
 *
 * To enable the ErrorDecoder in a module:
 * Derive from this class and annotate with @Component.
 *
 * @see FeignCallExceptionAdvice
 * @see FeignExceptionWrapper
 */
@Slf4j
public class FeignSkipBadRequestsErrorDecoder extends ErrorDecoder.Default {

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 400 && status < 500) {
            HystrixBadRequestException exception = null;
            String body = null;
            try {
                body = IOUtils.toString(response.body().asReader(Charset.defaultCharset()));
                exception = (HystrixBadRequestException) jsonMapper.readValue(body, FeignExceptionWrapper.class).getOriginThrowable();
                Objects.requireNonNull(exception, body);
            } catch (Exception e) {
                log.error("Error deserializing feign call exception.", e);
            }
            log.warn("Failed call api: {}, status: {}, response: {}.", methodKey, status, body);
            return exception;
        } else {
            log.error("Failed call api: {}, status: {}, reason: {}.", methodKey, status, response.reason());
        }

        return super.decode(methodKey, response);
    }
}