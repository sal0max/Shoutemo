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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.msal.shoutemo.connector.model.Post;

/**
 * Builds a connection to autemo.com and gets the content of its shoutbox or sends shouts to it.
 *
 * @version 1.0
 * @since 21.09.13
 */
public class Connection {

    private final static String USER_AGENT = "Shoutemo";

    private final static int TIMEOUT = 12000;

    private final String nick, password;

    private Map<String, String> cookies;

    /**
     * Creates a new Connection, needed to connect to autemo.com, get its shoutbox content or send
     * shouts to it.
     *
     * @param mail     the users email address with which he is registered on autemo
     * @param password the users password for this account
     */
    public Connection(String mail, String password) throws IOException {
        this.nick = mail;
        this.password = password;
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
        Connection x = new Connection(nick, password).connect();
        return !(x.get().isEmpty());
    } //TODO check token here, not just password

    public static String getToken(String nick, String password) throws IOException {
        Connection x = new Connection(nick, password).connect();
        x.connect();
        Log.d("Connection", "returning session id: PHPSESSID=" + x.cookies.get("PHPSESSID"));
        return x.cookies.get("PHPSESSID");
    }

    public static List<Post> get(String authtoken) throws IOException {
        Map<String, String> m = new HashMap<String, String>(1);
        m.put("PHPSESSID", authtoken);

        Document document = Jsoup
                .connect("http://www.autemo.com/includes/js/ajax/yshout.php")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .cookies(m)
                .userAgent(USER_AGENT)
                .get();

        List<Post> posts = new ArrayList<Post>();

        for (Element element : document.getElementsByClass("ys-post")) {
            Post p = new Post(element);
            posts.add(p);
        }

        return posts;
    }

    /**
     * Sends a new shout to the server: the given message. The server accepts only a limited size
     * for the message. If it is too long, this method will split the message in several chunks and
     * make additional calls.
     *
     * @param message The message to send. return the http status code of the request or -1 if
     *                nothing was sent. Should be 200 (OK) if everything worked.:
     */
    public static int post(String authtoken, String message) throws IOException {
        final int MAX_MESSAGE_LENGTH = 250;
      /* Code when not using the splitting. Just throw if message length is too long */
        // if (message.length() > MAX_MESSAGE_LENGTH) {
        // throw new IllegalArgumentException("Connection.post(): max. 256 characters are allowed for input.");
        // }

        Map<String, String> m = new HashMap<String, String>(1);
        m.put("PHPSESSID", authtoken);

      /* Code for splitting too long strings to chunks without cutting words. */
        List<String> split = Splitter.on(" ").splitToList(message); //separate all words
        List<String> sewn = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < split.size(); j++) {              // iterate through all words
            if ((sb.length() + split.get(j).length())
                    < MAX_MESSAGE_LENGTH) {  // stich together while below max message length
                sb.append(split.get(j) + " ");
            } else {                                           // reached max message length now
                if (sb.length()
                        > MAX_MESSAGE_LENGTH) {         // oh no! chunk too big to be sent: throw exeption, send nothing
                    throw new IllegalArgumentException(
                            "Connection.post(): max. 256 characters are allowed for input.");
                } else if (sb.length()
                        != 0) {                  // chunk seems fine: add to message list
                    sewn.add(sb.toString());
                }
                sb = new StringBuilder();                       // now empty the StringBuilder
                sb.append(split.get(j)
                        + " ");                  // add the next element, else it gets lost
            }
        }

        sewn.add(sb.deleteCharAt(sb.length() - 1).toString());// finally add the last chunk

        int statusCode = -1;
        for (String stich : sewn) {                           // now send all messages from the message list
            // System.out.println(stich + " (" + stich.length() + ")"); // TODO: remove sysout and activate actual sending
            statusCode = Jsoup
                    .connect("http://www.autemo.com/includes/js/ajax/yshout.php?m=shout")
                    .timeout(TIMEOUT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookies(m)
                    .userAgent(USER_AGENT)
                    .data("x_message", stich, "submit", "Shout!")
                    .method(org.jsoup.Connection.Method.POST)
                    .execute().statusCode();
        }

        return statusCode;
    }

    /**
     * Establishes a connection to autemo.com and authenticates with the users credentials.
     *
     * @return This {@link de.msal.shoutemo.connector.Connection} for chaining.
     */
    public Connection connect() throws IOException {
        org.jsoup.Connection.Response response = Jsoup
                .connect("http://www.autemo.com/login")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .userAgent(USER_AGENT)
                .data("lgemail", this.nick,
                        "lgpassword", this.password,
                        "Submit", "Login >",
                        "submitted", "TRUE")
                .method(org.jsoup.Connection.Method.POST)
                .execute();
        //TODO: Verification if nick/pw are right here: public METHOD!

        this.cookies = response.cookies();
        return this;
    }

    /**
     * Gets the current chat history. About 3KB per call.
     *
     * @return A {@link java.util.List} of {@link de.msal.shoutemo.connector.model.Post}s,
     * containing the current chat history. The size of it <strong>should</strong> always be 50, but
     * may differ.
     */
    public List<Post> get() throws IOException {
        Document document = Jsoup
                .connect("http://www.autemo.com/includes/js/ajax/yshout.php")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .cookies(this.cookies)
                .userAgent(USER_AGENT)
                .get();

        List<Post> posts = new ArrayList<Post>();

        for (Element element : document.getElementsByClass("ys-post")) {
            Post p = new Post(element);
            posts.add(p);
        }

        return posts;
    }

    /**
     * Sends a new shout to the server: the given message. The server accepts only a limited size
     * for the message. If it is too long, this method will split the message in several chunks and
     * make additional calls.
     *
     * @param message The message to send.
     * @return the http status code of the request or -1 if nothing was sent. Should be 200 (OK) if
     * everything worked.:
     */
    public int post(String message) throws IOException {
        final int MAX_MESSAGE_LENGTH = 250;
      /* Code when not using the splitting. Just throw if message length is too long */
        // if (message.length() > MAX_MESSAGE_LENGTH) {
        // throw new IllegalArgumentException("Connection.post(): max. 256 characters are allowed for input.");
        // }

      /* Code for splitting too long strings to chunks without cutting words. */
        List<String> split = Splitter.on(" ").splitToList(message); //separate all words
        List<String> sewn = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < split.size(); j++) {              // iterate through all words
            if ((sb.length() + split.get(j).length())
                    < MAX_MESSAGE_LENGTH) {  // stich together while below max message length
                sb.append(split.get(j) + " ");
            } else {                                           // reached max message length now
                if (sb.length()
                        > MAX_MESSAGE_LENGTH) {         // oh no! chunk too big to be sent: throw exeption, send nothing
                    throw new IllegalArgumentException(
                            "Connection.post(): max. 256 characters are allowed for input.");
                } else if (sb.length()
                        != 0) {                  // chunk seems fine: add to message list
                    sewn.add(sb.toString());
                }
                sb = new StringBuilder();                       // now empty the StringBuilder
                sb.append(split.get(j)
                        + " ");                  // add the next element, else it gets lost
            }
        }

        sewn.add(sb.deleteCharAt(sb.length() - 1).toString());// finally add the last chunk

        int statusCode = -1;
        for (String stich : sewn) {                           // now send all messages from the message list
            // System.out.println(stich + " (" + stich.length() + ")"); // TODO: remove sysout and activate actual sending
            statusCode = Jsoup
                    .connect("http://www.autemo.com/includes/js/ajax/yshout.php?m=shout")
                    .timeout(TIMEOUT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookies(this.cookies)
                    .userAgent(USER_AGENT)
                    .data("x_message", stich, "submit", "Shout!")
                    .method(org.jsoup.Connection.Method.POST)
                    .execute().statusCode();
        }

        return statusCode;
    }

}
