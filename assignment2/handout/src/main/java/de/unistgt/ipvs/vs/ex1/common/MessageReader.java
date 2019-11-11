package de.unistgt.ipvs.vs.ex1.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling the reading of messages according to the spec outlined in practical assignment 1.
 * This is accomplished by wrapping an InputStream and reading it into an internal buffer to simplify parsing.
 */
public final class MessageReader implements Iterator<String>, AutoCloseable {

    private static final Pattern messageForm = Pattern.compile("<(?<length>\\d{2}):(?<body>[^>]+?)>");

    private final StringBuilder buffer = new StringBuilder();

    private final InputStreamReader wrappedStream;
    private boolean eofEncountered = false;

    public MessageReader(InputStream wrappedStream) {
        this.wrappedStream = new InputStreamReader(wrappedStream);
    }

    /**
     * @return true, so long as no EOF has been read
     */
    @Override
    public boolean hasNext() {
        return !eofEncountered;
    }

    /**
     * Obtains the next valid message from the wrapped input stream, blocking to wait for IO if necessary.
     * @return the next syntactically valid message or an empty string
     *      if the input has been exhausted or an error occurred during reading.
     */
    @Override
    public String next() {
        Matcher bufferedMatch = messageForm.matcher(buffer);
        if (bufferedMatch.find()) {
            String result = bufferedMatch.group();
            buffer.delete(0, bufferedMatch.end());
            return result;
        }
        // read input into buffer until we find something cool
        // because the buffer is larger than 100 characters, any message conforming to the spec
        // should be readable in a single read operationo
        char[] readBuffer = new char[128];
        while (!messageForm.matcher(buffer).find()) {
            final int count;
            try {
                // read method is blocking and waits for input if necessary
                count = wrappedStream.read(readBuffer);
            } catch (IOException e) {
                System.err.println("Could not read input for message");
                e.printStackTrace();
                break;
            }
            if (count == -1) {
                eofEncountered = true;
                break;
            }
            buffer.append(readBuffer, 0, count);
        }
        bufferedMatch = messageForm.matcher(buffer);
        if (bufferedMatch.find()) {
            String result = bufferedMatch.group();
            buffer.delete(0, bufferedMatch.end());
            return result;
        }
        // no message left at the end of the input stream
        return "";
    }

    /**
     * <p>Obtains the message contents of the first message in the given string.
     * If no message could be found in the given string or the first match is not a valid message,
     * an empty array is returned.</p>
     * <p>
     * Note that multiple valid messages in the same message string are not supported, only the first string
     * conforming to the message format is examined and handled. This notably implies that <tt><09:RDY><08:RDY></tt>
     * yields an empty array, even though the second message would be valid.
     * </p>
     *
     * @param message The string to examine for messages
     * @return The separate content parts of the message, trimmed to not include surrounding whitespaces.
     */
    public String[] contents(String message) {
        Matcher m = messageForm.matcher(message);
        if (!m.find()) {
            return new String[]{};
        }
        int length = Integer.parseInt(m.group("length"));
        if (m.group().length() != length) {
            // message length is different from advertised message length.
            // handle by deeming the message invalid
            return new String[]{};
        }
        // split the body at multi-whitespace contents (after stripping leading and trailing spaces)
        return m.group("body").trim().split("\\s+");
    }

    @Override
    public void close() throws Exception {
        wrappedStream.close();
    }
}
