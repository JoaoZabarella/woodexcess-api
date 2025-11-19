package com.z.c.woodexcess_api.service.address;

import com.z.c.woodexcess_api.client.ViaCepClient;
import com.z.c.woodexcess_api.dto.address.ViaCepResponse;
import com.z.c.woodexcess_api.exception.address.CepApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CepService {

    private final ViaCepClient viaCepClient;

    @Cacheable(value = "cep", key = "#cep", unless = "#result == null")
    //Logo terá a adição do Circuit Breaker e Cache Redis
    public ViaCepResponse findAddressByCep(String cep) {
        log.info("Buscando CEP {} via ViaCEP", cep);

        String normalizedCep = normalizeCep(cep);
        validateCep(normalizedCep);

        try {
            ViaCepResponse response = viaCepClient.findCep(normalizedCep);

            if (response.isErro()) {
                log.warn("CEP {} não encontrado no ViaCEP", normalizedCep);
                throw new CepApiException("CEP não encontrado: " + cep);
            }

            log.info("CEP {} encontrado com sucesso", normalizedCep);
            return response;

        } catch (Exception e) {
            log.error("Erro ao buscar CEP {}: {}", normalizedCep, e.getMessage());
            throw new CepApiException("Erro ao buscar CEP: " + e.getMessage(), e);
        }
    }

    private String normalizeCep(String cep) {
        return cep != null ? cep.replaceAll("[^0-9]", "") : "";
    }


    private void validateCep(String cep) {
        if (cep == null || cep.length() != 8) {
            throw new CepApiException("CEP inválido. Deve conter 8 dígitos.");
        }
    }


}
