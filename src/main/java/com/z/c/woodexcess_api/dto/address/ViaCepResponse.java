package com.z.c.woodexcess_api.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ViaCepResponse(
        @JsonProperty("cep") String cep,
        @JsonProperty("logradouro") String street,
        @JsonProperty("complemento") String complement,
        @JsonProperty("bairro") String district,
        @JsonProperty("localidade") String city,
        @JsonProperty("uf") String state,
        @JsonProperty("ibge") String ibge,
        @JsonProperty("gia") String gia,
        @JsonProperty("ddd") String ddd,
        @JsonProperty("siafi") String siafi,
        @JsonProperty("erro") Boolean erro) {
    public boolean isErro() {
        return Boolean.TRUE.equals(erro);
    }
}
