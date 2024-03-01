package eu.ill.visa.vdi.concurrency;

import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.core.domain.User;
import eu.ill.visa.vdi.domain.Role;

public abstract class ConnectionThread implements Runnable {

    private final Instance instance;
    private final User user;
    private final Role role;

    public ConnectionThread(final Instance instance, final User user, final Role role) {
        this.instance = instance;
        this.user = user;
        this.role = role;
    }

    public abstract void closeTunnel();

    public abstract void run();

    public abstract void writeCharData(char[] data);
    public abstract void writeByteData(byte[] data);

    protected String getInstanceAndUser() {
        return "User " + this.user.getFullName() + " (" + this.user.getId() + ", " + this.role.toString() + "), Instance " + this.instance.getId();
    }
}

