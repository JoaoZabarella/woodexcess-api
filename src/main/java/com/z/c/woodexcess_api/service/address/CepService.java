package com.z.c.woodexcess_api.service.address;

import com.z.c.woodexcess_api.client.ViaCepClient;
import com.z.c.woodexcess_api.dto.address.ViaCepResponse;
import com.z.c.woodexcess_api.exception.address.CepApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CepService {

    private final ViaCepClient viaCepClient;

    @Cacheable(value = "cep", key = "#cep", unless = "#result == null")
    public ViaCepResponse findAddressByCep(String cep) {
        log.info("Buscando CEP {} via ViaCEP", cep);

        String normalized = normalizeCep(cep);
        validateCep(normalized);

        try {
            ViaCepResponse response = viaCepClient.findCep(normalized);

            if (response.isErro()) {
                log.warn("CEP {} not found in ViaCEP", normalized);
                throw new CepApiException("CEP not found: " + cep);
            }

            log.info("CEP {} found successfully", normalized);
            return response;

        } catch (Exception e) {
            log.error("Error calling ViaCEP for CEP {}: {}", normalized, e.getMessage());
            throw new CepApiException("Error fetching CEP data: " + e.getMessage(), e);
        }
    }

    private String normalizeCep(String cep) {
        return cep != null ? cep.replaceAll("[^0-9]", "") : "";
    }

    private void validateCep(String cep) {
        if (cep == null || cep.length() != 8) {
            throw new CepApiException("Invalid CEP. Must contain 8 digits.");
        }
    }
}
