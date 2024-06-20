package eu.ill.visa.vdi.brokers.redis.messages;

import eu.ill.visa.vdi.domain.models.ConnectedUser;

public record AccessRequestCancellationMessage(Long instanceId, ConnectedUser user, String requesterConnectionId) {
}
