package coo.lxt.island.common.icloud.vo;

import lombok.Data;

import java.util.List;

@Data
public class ICloudSessionVO {

    private String clientId;
    private String sessionToken;
    private String trustToken;
    private String scnt;
    private String sessionId;
    private String accountCountry;
    private List<String> cookie;

}
