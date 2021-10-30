package coo.lxt.island.common.icloud.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ICloudLoginVO {

    @NotBlank
    private String accountName;

    @NotBlank
    private String password;

    private Boolean rememberMe;

    private List<String> trustTokens;

    public ICloudLoginVO(String accountName, String password) {
        this.accountName = accountName;
        this.password = password;
        this.rememberMe = true;
        this.trustTokens = new ArrayList<>();
    }
}
