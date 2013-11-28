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

package de.msal.shoutemo.connector.model;

import org.jsoup.nodes.Element;

/**
 * A Message contains the text, both as html and plain text, and the type of a {@link
 * de.msal.shoutemo.connector.model.Post}.
 *
 * @version 1.0
 * @since 21.09.13
 */
public class Message implements Comparable<Message> {

    private String html, text;
    private Type type;

    /**
     * Creates a Message by parsing the given {@link Element} and getting the message, both in html
     * format and as plain text, and the type from it.
     *
     * @param e needs to be of type getElementsByClass("ys-post")!
     */
    Message(Element e) {
        this.text = e.getElementsByClass("ys-post-message").text();
        this.html = e.getElementsByClass("ys-post-message").html();

      /* message type: new thread announcement or shout or global announcement? */
        String type = e.ownText();
        if (type.equals("says:")) {
            this.type = Type.SHOUT;
        } else if (e.hasClass("ys-isglobal")) {
            this.type = Type.GLOBAL;
        } else if (type.equals("just started a new thread:")) {
            this.type = Type.THREAD;
        } else if (type.equals("just got a new chopping award in:")) {
            this.type = Type.AWARD;
        } else if (type.equals("just created a new competition:")) {
            this.type = Type.COMPETITION;
        } else if (type.equals("Just got Promoted")) {
            this.type = Type.PROMOTION;
        }
    }

    /**
     * @return the html-code of this Message or null if not existing.
     */
    public String getHtml() {
        return html;
    }

    /**
     * @return the plain text of this Message or null if not existing. Everything except the plain
     * text gets stripped, like links or embedded images.
     */
    public String getText() {
        return text;
    }

    /**
     * @return the {@link de.msal.shoutemo.connector.model.Message.Type} of this Message or null if
     * not existing.
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return ("(" + this.type + ") " + this.text);
    }

    /**
     * Compares this Message with the specified Message for order. Compares the type first, then the
     * message itself. Returns a negative integer, zero, or a positive integer as this Message is
     * less than, equal to, or greater than the specified Message.
     *
     * @param message the {@link de.msal.shoutemo.connector.model.Message} to be compared.
     * @return a negative integer, zero, or a positive integer as this Message is less than, equal
     * to, or greater than the specified Message. Global announcement comes before Award comes
     * before new Thread comes before Shout. Html comes before plain text.
     */
    @Override
    public int compareTo(Message message) {
        int tmp;
        tmp = this.type.compareTo(message.type);
        tmp = tmp == 0 ? this.type.compareTo(message.type) : tmp;
        return tmp == 0 ? this.text.compareTo(message.text) : tmp;
    }

    /**
     * Defines of what type the message is. E.g. a shout from a user or a global announcement.
     */
    public enum Type {
        /**
         * Global announcement: Should be something important, pinned to the top of the shoutbox
         */
        GLOBAL,
        /**
         * Generated announcement: Someone got an award in something
         */
        AWARD,
        /**
         * Generated announcement: Someone created a new thread.
         */
        THREAD,
        /**
         * A user sent a post to the server/shoutbox.
         */
        SHOUT,
        /**
         * Generated announcement: Someone created a new competition.
         */
        COMPETITION,
        /**
         * Generated announcement: Someone got promoted.
         */
        PROMOTION;
    }

}
