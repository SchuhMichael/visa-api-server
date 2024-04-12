package eu.ill.visa.web.bundles.graphql.queries.resolvers.fields;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import eu.ill.visa.cloud.services.CloudClient;
import eu.ill.visa.cloud.services.CloudClientGateway;
import eu.ill.visa.core.domain.SecurityGroup;
import graphql.kickstart.tools.GraphQLResolver;


@Singleton
public class SecurityGroupResolver implements GraphQLResolver<SecurityGroup> {

    private final CloudClientGateway cloudClientGateway;

    @Inject
    public SecurityGroupResolver(final CloudClientGateway cloudClientGateway) {
        this.cloudClientGateway = cloudClientGateway;
    }

    public CloudClient cloudClient(SecurityGroup securityGroup) {
        return this.cloudClientGateway.getCloudClient(securityGroup.getCloudId());
    }
}
