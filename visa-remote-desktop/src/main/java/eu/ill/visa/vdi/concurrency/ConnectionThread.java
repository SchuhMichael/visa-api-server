package eu.ill.visa.vdi.concurrency;

import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.core.domain.User;
import eu.ill.visa.vdi.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConnectionThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionThread.class);

    protected final String clientId;
    private final Instance instance;
    private final User user;
    private final Role role;

    public ConnectionThread(final String clientId, final Instance instance, final User user, final Role role) {
        this.clientId = clientId;
        this.instance = instance;
        this.user = user;
        this.role = role;
    }

    public abstract void closeTunnel();

    public abstract void run();

    public abstract void writeCharData(char[] data);
    public abstract void writeByteData(byte[] data);

    protected String getInstanceAndUser() {
        return "User " + this.user.getFullName() + " (" + this.user.getId() + ", " + this.role.toString() + "), Instance " + this.instance.getId() + ", Session " + this.clientId;
    }
}

