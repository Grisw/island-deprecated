package coo.lxt.island.common.icloud.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ICloudHSA2VO {

    @NotBlank
    private String accountName;

    @NotBlank
    private String securityCode;

}
