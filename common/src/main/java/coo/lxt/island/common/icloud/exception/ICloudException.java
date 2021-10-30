package coo.lxt.island.common.icloud.exception;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import lombok.Getter;
import lombok.Setter;

public class ICloudException extends HystrixBadRequestException {

    @Setter
    @Getter
    private String accountName;

    @Setter
    @Getter
    private String response;

    public ICloudException(String message) {
        super(message);
    }

    public ICloudException(String message, String accountName, String response) {
        super(message);
        this.accountName = accountName;
        this.response = response;
    }
}
