package coo.lxt.island.common.icloud.exception;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import lombok.Getter;
import lombok.Setter;

public class ICloudSessionInvalidException extends HystrixBadRequestException {

    @Setter
    @Getter
    private String accountName;

    public ICloudSessionInvalidException(String message) {
        super(message);
    }

    public ICloudSessionInvalidException(String message, String accountName) {
        super(message);
        this.accountName = accountName;
    }
}
