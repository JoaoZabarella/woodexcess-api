package com.z.c.woodexcess_api.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViaCepResponse {

    @JsonProperty("cep")
    private String cep;

    @JsonProperty("logradouro")
    private String street;

    @JsonProperty("complemento")
    private String complement;

    @JsonProperty("bairro")
    private String district;

    @JsonProperty("localidade")
    private String city;

    @JsonProperty("uf")
    private String state;

    @JsonProperty("erro")
    private Boolean erro;

    public boolean isErro() {
        return Boolean.TRUE.equals(erro);
    }
}
