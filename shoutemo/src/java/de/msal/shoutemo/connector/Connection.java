/*
 * Copyright 2013 Maximilian Salomon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package de.msal.shoutemo.connector;

import com.google.common.base.Splitter;
import com.google.common.primitives.Doubles;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.connector.model.Post;

/**
 * Builds a connection to <a href="http://autemo.com">autemo.com</a>.
 * <p/>
 * Provides methods for authenticating, getting the content of its shoutbox, sending shouts and
 * more.
 *
 * @version 1.0
 * @since 21.09.13
 */
public class Connection {

    private static final String TAG = "Shoutemo|Connection";
    private static final String USER_AGENT = "Shoutemo";
    private static final int TIMEOUT = 12000;

    /**
     * Class for connecting to autemo.com, getting its shoutbox content and send shouts to it.
     */
    private Connection() throws IOException {
    }

    /**
     * Checks if the given nickname and password combination can successfully authenticate on the
     * server and recieve messages from it.
     *
     * @param nick     the username. On autemo that is the email address which is registered.
     * @param password the password of that account.
     * @return {@code true} if the attempt was successful, else {@code false}.
     */
    public static boolean isCredentialsCorrect(String nick, String password) throws IOException {
        Map<String, String> cookies = connect(nick, password);
        String token = cookies.get("PHPSESSID");
        return !(getPosts(token).isEmpty());
    }

    /**
     * @param nick     the username. On autemo that is the email address which is registered.
     * @param password the users password for this account.
     * @return The session id that authenticates this user.
     */
    public static String getToken(String nick, String password) throws IOException {
        Map<String, String> cookies = connect(nick, password);
        Log.v(TAG, "Returning session id: PHPSESSID=" + cookies.get("PHPSESSID"));
        return cookies.get("PHPSESSID");
    }

    /**
     * Gets the current chat history. About 3KB per call.
     *
     * @param authtoken this sessions authtoken.
     * @return A {@link java.util.List} of {@link de.msal.shoutemo.connector.model.Post}s,
     * containing the current chat history. The size of it <strong>should</strong> always be 50, but
     * may differ.
     */
    public static List<Post> getPosts(String authtoken) throws IOException {
        Document document = Jsoup
                .connect("http://www.autemo.com/includes/js/ajax/yshout.php")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .cookie("PHPSESSID", authtoken)
                .userAgent(USER_AGENT)
                .get();

        List<Post> posts = new LinkedList<Post>();

        for (Element element : document.getElementsByClass("ys-post")) {
            posts.add(new Post(element));
        }

        return posts;
    }

    /**
     * Gets the currently online users. Should be about 7KB per call. There is no need to
     * authenticate here, as the data is publicly viewable.
     *
     * @return A {@link java.util.List List} of {@link de.msal.shoutemo.connector.model.Author
     * Author}s, containting the currently online users.
     */
    public static List<Author> getOnlineUsers() throws IOException {
        List<Author> authors = new ArrayList<Author>(25);

        Document document = Jsoup
                .connect("http://www.autemo.com/forums/")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .userAgent(USER_AGENT)
                .get();

        Elements elements = document
                .getElementsByAttributeValue("style", "width:170px;display:block;float:left;");
        if (elements == null)
            return authors;
        for (Element element : elements) {
            Element user = element.getElementsByTag("a").first();
            if (user != null)
                authors.add(new Author(user));
        }

        return authors;
    }

    /**
     * Sends a new shout to the server: the given message. The server accepts only a limited size
     * for the message. If it is too long, this method will split the message in several chunks and
     * make additional calls.
     *
     * @param authtoken this sessions authtoken.
     * @param message   the message to send. return the http status code of the request or -1 if
     *                  nothing was sent. Should be 200 (OK) if everything worked.:
     * @return the http status code of the response.
     */
    public static int post(String authtoken, String message) throws IOException {
        final int MAX_MESSAGE_LENGTH = 250;
      /* Code when not using the splitting. Just throw if message length is too long */
        // if (message.length() > MAX_MESSAGE_LENGTH) {
        // throw new IllegalArgumentException("Connection.post(): max. 256 characters are allowed for input.");
        // }

      /* Code for splitting too long strings to chunks without cutting words. */
        List<String> splits = Splitter.on(" ").splitToList(message); //separate all words
        List<String> sewns = new LinkedList<String>();
        StringBuilder sb = new StringBuilder();

        for (String split : splits) { // iterate through all words
            if ((sb.length() + split.length())
                    < MAX_MESSAGE_LENGTH) { // stich together while below max message length
                sb.append(split).append(" ");
            } else { // reached max message length now
                if (sb.length() > MAX_MESSAGE_LENGTH) { // chunk too big to be sent: send nothing
                    throw new IllegalArgumentException(
                            "Connection.post(): max. 256 characters are allowed for input.");
                } else if (sb.length() != 0) { // chunk seems fine: add to message list
                    sewns.add(sb.toString());
                }
                sb = new StringBuilder(); // now empty the StringBuilder
                sb.append(split).append(" "); // add the next element, else it gets lost
            }
        }

        sewns.add(sb.deleteCharAt(sb.length() - 1).toString());// finally add the last chunk

        int statusCode = -1;
        for (String stitch : sewns) { // now send all messages from the message list
            statusCode = Jsoup
                    .connect("http://www.autemo.com/includes/js/ajax/yshout.php?m=shout")
                    .timeout(TIMEOUT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookie("PHPSESSID", authtoken)
                    .userAgent(USER_AGENT)
                    .data("x_message", stitch, "submit", "Shout!")
                    .method(org.jsoup.Connection.Method.POST)
                    .execute()
                    .statusCode();
        }

        return statusCode;
    }

    /**
     * <strong>Tries to correctly set the users timezone on autemo.com.</strong>
     * <p/>
     * Since autemos online timezone setting is plain wrong try to take care of it here. We have to
     * subtract 1h of the real offset. Two problems, with that: <ol><li> There are timezones with
     * half hours, that can't always be reduced by one. e.g. UTC+9.5h can't go down to UTC+8.5, as
     * such a timezon doesn't exist. In that case we just reduce it by 1.5h, to UTC+8. This way
     * messages display 30min older than they are, but this is still better than them being
     * displayed 30min in the future. </li><li> Can't decrease UTC-12 as it is the international
     * date line. Just ignore that case, however, as only two uninhabited islands (Baker Island and
     * Howland Island) use UTC-12. </li></ol>
     *
     * @param authtoken     this sessions authtoken.
     * @param offsetInHours the real offset of the desired timezone, <strong>including
     *                      <i>possible</i> daylight savings time</strong>, in hours!
     * @return the http status code of the response. If the provided {@code offsetInHours} couldn't
     * be mapped to a timezone value, {@code -1} is returned.
     */
    public static int setUserTimezone(String authtoken, double offsetInHours) throws IOException {

        double[] timezoneMapping = new double[35];
        timezoneMapping[0] = -12;    // value="1"    GMT/UTC -12:00 hours
        timezoneMapping[1] = -11;    // value="2"    GMT/UTC -11:00 hours
        timezoneMapping[2] = -10;    // value="3"    GMT/UTC -10:00 hours
        timezoneMapping[3] = -9.5;   // value="4"    GMT/UTC  -9:30 hours
        timezoneMapping[4] = -9;     // value="5"    GMT/UTC  -9:00 hours
        timezoneMapping[5] = -8.5;   // value="6"    GMT/UTC  -8:30 hours
        timezoneMapping[6] = -8;     // value="7"    GMT/UTC  -8:00 hours
        timezoneMapping[7] = -7;     // value="8"    GMT/UTC  -7:00 hours
        timezoneMapping[8] = -6;     // value="9"    GMT/UTC  -6:00 hours
        timezoneMapping[9] = -5;     // value="10"   GMT/UTC  -5:00 hours
        timezoneMapping[10] = -4;    // value="11"   GMT/UTC  -4:00 hours
        timezoneMapping[11] = -3.5;  // value="12"   GMT/UTC  -3:30 hours
        timezoneMapping[12] = -3;    // value="13"   GMT/UTC  -3:00 hours
        timezoneMapping[13] = -2;    // value="14"   GMT/UTC  -2:00 hours
        timezoneMapping[14] = -1;    // value="15"   GMT/UTC  -1:00 hours
        timezoneMapping[15] = 0;     // value="16"   GMT/UTC
        timezoneMapping[16] = 1;     // value="17"   GMT/UTC  +1:00 hours
        timezoneMapping[17] = 2;     // value="18"   GMT/UTC  +2:00 hours
        timezoneMapping[18] = 3;     // value="19"   GMT/UTC  +3:00 hours
        timezoneMapping[19] = 4;     // value="20"   GMT/UTC  +4:00 hours
        timezoneMapping[20] = 5;     // value="21"   GMT/UTC  +5:00 hours
        timezoneMapping[21] = 5.5;   // value="22"   GMT/UTC  +5:30 hours
        timezoneMapping[22] = 6;     // value="23"   GMT/UTC  +6:00 hours
        timezoneMapping[23] = 6.5;   // value="24"   GMT/UTC  +6:30 hours
        timezoneMapping[24] = 7;     // value="25"   GMT/UTC  +7:00 hours
        timezoneMapping[25] = 8;     // value="26"   GMT/UTC  +8:00 hours
        timezoneMapping[26] = 9;     // value="27"   GMT/UTC  +9:00 hours
        timezoneMapping[27] = 9.5;   // value="28"   GMT/UTC  +9:30 hours
        timezoneMapping[28] = 10;    // value="29"   GMT/UTC +10:00 hours
        timezoneMapping[29] = 10.5;  // value="30"   GMT/UTC +10:30 hours
        timezoneMapping[30] = 11;    // value="31"   GMT/UTC +11:00 hours
        timezoneMapping[31] = 11.5;  // value="32"   GMT/UTC +11:30 hours
        timezoneMapping[32] = 12;    // value="33"   GMT/UTC +12:00 hours
        timezoneMapping[33] = 13;    // value="34"   GMT/UTC +13:00 hours
        timezoneMapping[34] = 14;    // value="35"   GMT/UTC +14:00 hours

        /*
         * Since autemos online timezone setting is plain wrong try to take care of it here. We have
         * to subtract 1h of the real offset. Two problems, with that:
         *   1. There are timezones with half hours, that can't always be reduced by one.
         *      e.g. UTC+9.5h can't go down to UTC+8.5, as such a timezon doesn't exist. In that
         *      case we just reduce it by 1.5h, to UTC+8. This way messages display 30min older than
         *      they are, but this is still better than them being displayed 30min in the future.
         *   2. Can't decrease UTC-12 as it is the international date line. Just ignore that case,
         *      however, as only two uninhabited islands (Baker Island and Howland Island) use
         *      UTC-12.
         */
        double autemoOffsetInHours = Doubles.indexOf(timezoneMapping, offsetInHours - 1) == -1 ?
                offsetInHours - 1.5 : offsetInHours - 1;
        // find the mapped code (== index)
        int offsetCode = Doubles.indexOf(timezoneMapping, autemoOffsetInHours);

        if (offsetCode > 0) {
        /* first get necessary data */
            Document doc = Jsoup
                    .connect("http://www.autemo.com/myaccount/?m=settings")
                    .timeout(TIMEOUT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookie("PHPSESSID", authtoken)
                    .userAgent(USER_AGENT)
                    .get();

            String firstName = doc.getElementsByAttributeValue("name", "x_firstname").val();
            String lastName = doc.getElementsByAttributeValue("name", "x_lastname").val();
            String email = doc.getElementsByAttributeValue("name", "x_email").val();
            String countryid = doc.getElementsByAttributeValue("name", "x_countryid").get(0)
                    .getElementsByAttributeValue("selected", "selected").val();

        /* now update the timezone */
            int statusCode = Jsoup
                    .connect("http://www.autemo.com/myaccount/?m=settings")
                    .timeout(TIMEOUT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookie("PHPSESSID", authtoken)
                    .userAgent(USER_AGENT)
                    .data("x_timezoneid", String.valueOf(offsetCode))
                    .data("submitted", "true")
                    .data("x_countryid", countryid)
                    .data("x_email", email)
                    .data("x_firstname", firstName)
                    .data("x_lastname", lastName)
                    .method(org.jsoup.Connection.Method.POST)
                    .execute()
                    .statusCode();
            Log.v(TAG, "Changing user timezone to " + offsetCode + " (timezone code) (GMT+"
                    + offsetInHours + " (offsetInHours); autemoOffsetInHours=" + autemoOffsetInHours
                    + "). Server answer=" + statusCode);
            return statusCode;
        } else {
            return -1;
        }
    }

    /**
     * Establishes a connection to autemo.com and authenticates with the users credentials. Further
     * communication is handled via session cookies, so this method returns this sessions cookies.
     *
     * @return this sessions cookies.
     */
    private static Map<String, String> connect(String nick, String password) throws IOException {
        org.jsoup.Connection.Response response = Jsoup
                .connect("http://www.autemo.com/login")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .userAgent(USER_AGENT)
                .data("lgemail", nick,
                        "lgpassword", password,
                        "Submit", "Login >",
                        "submitted", "TRUE")
                .method(org.jsoup.Connection.Method.POST)
                .execute();

        return response.cookies();
    }

}
