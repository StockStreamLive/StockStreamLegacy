import org.junit.Test;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;

public class ChatProcessorTest {

    @Test
    public void testChatProcessorInput() throws IOException {


        //CommandCache commandCache = CommandCache.getInstance();

        /*ChatProcessor chatProcessor = new ChatProcessor();

        try (BufferedReader br = new BufferedReader(new FileReader("src/test/java/bad-stuff"))) {
            String line;
            while ((line = br.readLine()) != null) {
                chatProcessor.onGenericMessage(new Message("buy " + line));
            }
        }*/

        //RankedCommands rankedCommands = new RankedCommands(commandCache);
        String stamp = "2017-03-08T20:39:37.487Z";
        String test = "2017-03-09T12:53:34Z";
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        if (test.length() > 20) {
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        }

    }


    class Message implements GenericMessageEvent {

        private String m;

        public Message(final String m) {
            this.m = m;
        }

        @Override
        public String getMessage() {
            return this.m;
        }

        @Override
        public void respondPrivateMessage(String message) {

        }

        @Override
        public void respondWith(String fullLine) {

        }

        @Override
        public UserHostmask getUserHostmask() {
            return null;
        }

        @Override
        public User getUser() {
            return null;
        }

        @Override
        public void respond(String response) {

        }

        @Override
        public <T extends PircBotX> T getBot() {
            return null;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public int compareTo(Event o) {
            return 0;
        }
    }

}
