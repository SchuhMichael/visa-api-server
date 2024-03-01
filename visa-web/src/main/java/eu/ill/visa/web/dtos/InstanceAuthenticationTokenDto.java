package eu.ill.visa.web.dtos;

import eu.ill.visa.core.domain.InstanceAuthenticationToken;

import java.util.UUID;

public class InstanceAuthenticationTokenDto {

    private UUID token;

    public InstanceAuthenticationTokenDto(final InstanceAuthenticationToken instanceAuthenticationToken) {
        this.token = instanceAuthenticationToken.getToken();
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }
}
