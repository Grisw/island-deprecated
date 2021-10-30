package coo.lxt.island.common.icloud.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ICloudTrustSessionVO {

    @NotBlank
    private String accountName;

}
