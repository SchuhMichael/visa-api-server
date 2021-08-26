package eu.ill.visa.cloud.providers.web.converters;

import eu.ill.visa.cloud.domain.CloudInstance;
import eu.ill.visa.cloud.domain.CloudInstanceFault;
import eu.ill.visa.cloud.domain.CloudInstanceState;
import org.joda.time.DateTime;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class InstanceConverter {

    public static CloudInstance fromJson(final JsonObject json) {
        final String id = json.getString("id");
        final String name = json.getString("name");
        final DateTime createdAt = new DateTime(json.getString("createdAt"));
        final String imageId = json.getString("imageId");
        final String flavorId = json.getString("flavorId");
        final String address = address(json);
        final CloudInstanceState state = convertStatus(json.getString("state"));

        final CloudInstance.Builder builder = CloudInstance.newBuilder();

        final CloudInstanceFault fault = fault(json);
        final List<String> securityGroups = securityGroups(json);
        builder.address(address);
        builder.id(id);
        builder.name(name);
        builder.state(state);
        builder.imageId(imageId);
        builder.flavorId(flavorId);
        builder.createdAt(createdAt);
        builder.fault(fault);
        builder.securityGroups(securityGroups);
        return builder.build();
    }

    private static List<String> securityGroups(final JsonObject json) {
        final JsonArray data = json.getJsonArray("securityGroups");
        if (data != null) {
            final List<String> securityGroups = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                securityGroups.add(data.getString(i));
            }
            return securityGroups;
        }
        return new ArrayList<>();
    }

    private static String address(final JsonObject json) {
        if (json.isNull("address")) {
            return null;
        }
        return json.getString("address");
    }

    private static CloudInstanceFault fault(final JsonObject json) {
        if (json.isNull("fault")) {
            return null;
        }
        final JsonObject fault = json.getJsonObject("fault");
        return new CloudInstanceFault(
            fault.getString("message"),
            fault.getInt("code"),
            fault.containsKey("details") ? fault.getString("details") : null,
            fault.getString("createdAt"));
    }


    // TODO Create a more generic status mapping
    private static CloudInstanceState convertStatus(String openStackStatus) {
        switch (openStackStatus) {
            case "BUILD":
            case "REBUILD":
                return CloudInstanceState.BUILDING;
            case "ACTIVE":
                return CloudInstanceState.ACTIVE;
            case "HARD_REBOOT":
            case "REBOOT":
                return CloudInstanceState.REBOOTING;
            case "MIGRATING":
            case "RESCUE":
            case "RESIZE":
            case "REVERT_RESIZE":
            case "VERIFY_SIZE":
                return CloudInstanceState.UNAVAILABLE;
            case "DELETED":
            case "SHELVED":
            case "SHELVED_OFFLOADED":
            case "SOFT_DELETED":
                return CloudInstanceState.DELETED;
            case "PAUSED":
            case "SHUTOFF":
            case "SUSPENDED":
                return CloudInstanceState.STOPPED;
            case "ERROR":
                return CloudInstanceState.ERROR;
            case "UNKNOWN":
            default:
                return CloudInstanceState.UNKNOWN;
        }
    }
}
