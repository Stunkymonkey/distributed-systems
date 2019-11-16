package de.unistgt.ipvs.vs.ex1.common;


import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds support for batching message capabilities while automatically handling message length requirements
 * of the protocol outlined in Practical Assignment 1.
 */
public class MessageWriter implements AutoCloseable {
    // message contents may be 94 long before overflowing the size field
    // this comes from the fact that 5 chars are necessary for the protocol overhead
    // <dd:[..]>
    private static final int SAFE_LENGTH = 94;
    // %02d -> zero-padded two digit number
    private static final String MESSAGE_FORMAT = "<%02d:%s>";
    private static final String PART_SEPARATOR = " ";
    // storage for the message parts
    private final StringBuilder buffer = new StringBuilder(100);
    private final OutputStream writeTarget;

    public MessageWriter(OutputStream writeTarget) {
        this.writeTarget = writeTarget;
    }

    /**
     * Forcibly sends the given part to the encapsulated output stream, flushing the buffer in the process.
     * @param part The message part to be sent. This is not checked for validity.
     */
    public void forceSend(String part) throws IOException {
        safeAppend(part);
        send();
    }

    /**
     * Adds a part to the message buffer to be sent at a later date.
     * If the resulting buffer would be too large to send in a single message, the current buffer is written before sending.
     * @param part The message part to be sent. This is not checked for validity.
     */
    public void enqueue(String part) throws IOException {
        safeAppend(part);
    }

    /**
     * Handle message size requirements and part delimiter when adding to the message buffer
     */
    private void safeAppend(String part) throws IOException {
        // adding message to the buffer would overflow message size
        if (buffer.length() + part.length() + PART_SEPARATOR.length() >= SAFE_LENGTH) {
            // therefore flush the buffer to output
            send();
        }
        // if buffer isn't empty, separate the parts
        if (buffer.length() != 0) {
            buffer.append(PART_SEPARATOR);
        }
        buffer.append(part);
    }

    /**
     * Writes the current buffer and subsequently clears it.
     */
    private void send() throws IOException {
        writeTarget.write(String.format(MESSAGE_FORMAT, buffer.length() + 5, buffer.toString()).getBytes());
        writeTarget.flush();
        // clear buffer after sending
        buffer.delete(0, buffer.length());
    }

    @Override
    public void close() throws Exception {
        writeTarget.close();
    }
}
