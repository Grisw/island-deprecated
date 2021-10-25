package coo.lxt.island.common.icloud.exception;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ICloudAuthenticationException extends HystrixBadRequestException {

    @Setter
    @Getter
    private String accountName;

    @Setter
    @Getter
    private String response;

    public ICloudAuthenticationException(String message) {
        super(message);
    }

    public ICloudAuthenticationException(String message, String accountName, String response) {
        super(message);
        this.accountName = accountName;
        this.response = response;
    }
}
