package com.z.c.woodexcess_api.client;

import com.z.c.woodexcess_api.config.FeignConfig;
import com.z.c.woodexcess_api.dto.address.ViaCepResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "viacep",
        url = "${viacep.url}",
        configuration = FeignConfig.class
)
public interface ViaCepClient {

    @GetMapping("/{cep}/json")
    ViaCepResponse findCep(@PathVariable("cep") String cep);
}
