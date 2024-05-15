package eu.ill.visa.web.graphql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class GraphqlExtensionProcessor {
    private static final String FEATURE = "visa-web-graphql";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
