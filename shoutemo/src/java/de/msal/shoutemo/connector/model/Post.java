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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A Post contains the {@link Message} itself, the {@link java.util.Date} of the post and (if
 * existing) the {@link Author}.
 *
 * @version 1.0
 * @since 22.09.13
 */
public class Post implements Comparable<Post> {

    private Author author;
    private Message message;
    private Date date;

    /**
     * Creates a new Post through parsing the given {@link Element}. Tries to set {@link Message},
     * {@link java.util.Date} and (if existing) the {@link Author}.
     *
     * @param e needs to be of type getElementsByClass("ys-post")!
     */
    public Post(Element e) {
        if (e.getAllElements().hasClass("ys-post-nickname")) {
            this.author = new Author(e.getElementsByClass("ys-post-nickname").first(), null);
        }
        this.message = new Message(e);
        this.date = toDate(e.getElementsByClass("ys-post-info").text());
    }

    /**
     * @return the {@link java.util.Date} of the Post or null if not existing.
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the {@link Author} of the Post or null if not existing.
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * @return the {@link Message} of the Post or null if not existing.
     */
    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return date + " - " + author + ":\t" + message;
    }

    /**
     * Compares this Post with the specified Post for order. Compares the date first, then the
     * author, then the message. Returns a negative integer, zero, or a positive integer as this
     * Post is less than, equal to, or greater than the specified Post.
     *
     * @param post the {@link de.msal.shoutemo.connector.model.Post} to be compared.
     * @return a negative integer, zero, or a positive integer as this Post is less than, equal to,
     * or greater than the specified Post. Date comes before Authors comes before Message.
     */
    @Override
    public int compareTo(Post post) {
        int tmp;
        tmp = this.date.compareTo(post.date);
        tmp = tmp == 0 ? this.author.compareTo(post.author) : tmp;
        return tmp == 0 ? this.message.compareTo(post.message) : tmp;
    }

    /**
     * Parses the given {@link String} that contains date and time and transforms it to a {@link
     * java.util.Date}.
     *
     * @param date the {@link String} that should be transformed in the format {@code "EEEE MMM dd,
     *             HH:mm:ss"}
     * @return the generated date.
     */
    private Date toDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMM dd, HH:mm:ss", Locale.US);

        Calendar cal = new GregorianCalendar();
        try {
            cal.setTime(sdf.parse(date));

            /*
             * As autemo doesn't provide year identifier, see that on year swich, the old messages
             * from december won't be transfered 1 year to the future.
             * Assumes that no messages are on autemo.com that are older than 1 year!
             */
            Calendar now = Calendar.getInstance();
            if (cal.get(Calendar.MONTH) > now.get(Calendar.MONTH)) {
                cal.set(Calendar.YEAR, now.get(Calendar.YEAR) - 1);
            } else {
                cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return cal.getTime();
    }

}
