package coo.lxt.island.common.icloud.api;

import coo.lxt.island.common.icloud.vo.ICloudAuthWithTokenVO;
import coo.lxt.island.common.icloud.vo.ICloudHSA2VO;
import coo.lxt.island.common.icloud.vo.ICloudLoginVO;
import coo.lxt.island.common.icloud.vo.ICloudTrustSessionVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;


public interface IICloudApi {

    enum LoginStatus {
        SUCCESS, NEED_HSA2, INCORRECT_CREDENTIALS, ACCOUNT_LOCKED, TRY_LATER
    }

    @PostMapping("/login")
    @ResponseBody
    IICloudApi.LoginStatus login(@RequestBody @Validated ICloudLoginVO authVO) throws Exception;

    @PostMapping("/verify/hsa2")
    @ResponseBody
    boolean verifyHSA2(@RequestBody @Validated ICloudHSA2VO hsa2VO) throws Exception;

    @PostMapping("/auth")
    @ResponseBody
    boolean authWithToken(@RequestBody @Validated ICloudAuthWithTokenVO authWithTokenVO) throws Exception;

    @PostMapping("/trust")
    @ResponseBody
    boolean trustSession(@RequestBody @Validated ICloudTrustSessionVO trustSessionVO) throws Exception;
}
