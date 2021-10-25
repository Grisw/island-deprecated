package coo.lxt.island.common.icloud.api;

import coo.lxt.island.common.exception.ValidateException;
import coo.lxt.island.common.icloud.vo.ICloudAuthVO;
import coo.lxt.island.common.icloud.vo.ICloudSessionVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

public interface IICloudApi {

    @PostMapping("/login")
    ICloudSessionVO login(@RequestBody @Validated ICloudAuthVO authVO) throws IOException, ValidateException;
}
