package eu.ill.visa.web.bundles.graphql.queries.inputs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApplicationCredentialInput {

    @NotNull
    @Size(max = 250)
    private String name;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
