package eu.ill.visa.vdi.concurrency;

import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.core.domain.User;
import eu.ill.visa.vdi.domain.Role;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;

import static org.apache.guacamole.net.GuacamoleTunnel.INTERNAL_DATA_OPCODE;

public class GuacamoleWebsocketConnectionThread extends ConnectionThread {

    private static final Logger          logger = LoggerFactory.getLogger(GuacamoleWebsocketConnectionThread.class);
    private final        GuacamoleTunnel tunnel;
    private final Session session;

    public GuacamoleWebsocketConnectionThread(final Session session, final GuacamoleTunnel tunnel, final Instance instance, final User user, final Role role) {
        super(instance, user, role);
        this.tunnel = tunnel;
        this.session = session;
    }

    @Override
    public void closeTunnel() {
        try {
            this.tunnel.close();

        } catch (GuacamoleException exception) {
            logger.debug("Unable to close connection to guacd for {} : {}", this.getInstanceAndUser(), exception.getMessage());
        }
    }

    @Override
    public void run() {
        sendIdentifierInstruction();
        try {
            read();
        } catch (IOException error) {

        }
    }

    @Override
    public void writeCharData(char[] data) {
        try {
            final GuacamoleWriter writer = tunnel.acquireWriter();
            writer.write(data);

        } catch (GuacamoleConnectionClosedException exception) {
            logger.debug("Connection to guacd closed", exception);
        } catch (GuacamoleException exception) {
            logger.debug("WebSocket tunnel write failed", exception);
        } finally {
            tunnel.releaseWriter();
        }
    }

    public void writeByteData(byte[] data) {
        logger.error("writeByteData Not implemented");
    }

    private void read() throws IOException {
        final StringBuilder   buffer = new StringBuilder(8192);
        final GuacamoleReader reader = tunnel.acquireReader();
        char[]                readMessage;

        try {
            while ((readMessage = reader.read()) != null) {

                buffer.append(readMessage);

                // Flush if we expect to wait or buffer is getting full
                if (!reader.available() || buffer.length() >= 8192) {
                    this.session.getBasicRemote().sendText(buffer.toString());
                    buffer.setLength(0);
                }

            }
            session.close();
        } catch (GuacamoleClientException exception) {
            logger.error("WebSocket connection terminated for {} due to client error {}", this.getInstanceAndUser(), exception.getMessage());
            session.close();
        } catch (GuacamoleConnectionClosedException exception) {
            logger.info("Connection to guacd closed for {} : {}", this.getInstanceAndUser(), exception.getMessage());
            session.close();
        } catch (GuacamoleException exception) {
            logger.error("Connection to guacd for {} terminated abnormally: {}", this.getInstanceAndUser(), exception.getMessage());
            session.close();
        }
    }

    private void sendIdentifierInstruction() {
        final String               uuid        = tunnel.getUUID().toString();
        final GuacamoleInstruction instruction = new GuacamoleInstruction(INTERNAL_DATA_OPCODE, uuid);

        try {
            session.getBasicRemote().sendText(instruction.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

