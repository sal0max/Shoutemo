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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * An Authors exists of his (nick)name and his rank (admin, mod or user).
 *
 * @version 1.0
 * @since 22.09.13
 */
public class Author implements Comparable<Author>, Parcelable {

    private final String name;

    private final Type type;

    private final String avatar;

    /**
     * Creates a new Authors by parsing the given Element and stripping out the (nick)name and type
     * of it.
     *
     * @param e1 needs to look like
     *          <pre>{@code <a href="profiles/?id=user" [class="autemo_color"]>user</a>}</pre>
     * @param avatarUrl url to the users avatar, or null
     */
    public Author(Element e1, String avatarUrl) {
        this.name = e1.text();
        if (e1.getAllElements().hasClass("autemo_admin_color")) {
            this.type = Type.ADMIN;
        } else if (e1.getAllElements().hasClass("autemo_color")) {
            this.type = Type.MOD;
        } else {
            this.type = Type.USER;
        }
        this.avatar = avatarUrl;
    }

    public Author(Parcel in) {
        this.name = in.readString();
        this.type = (Type) in.readSerializable();
        this.avatar = in.readString();
    }

    /**
     * @return the (nick)name of this Authors
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the {@link de.msal.shoutemo.connector.model.Author.Type} of the user.
     */
    public Type getType() {
        return this.type;
    }

   /**
    * @return the link to the users avatar.
    */
    public String getAvatar() {
        return avatar;
    }

   @Override
    public String toString() {
        String s = this.name;
        if (this.type == Type.ADMIN) {
            s += " [Admin]";
        } else if (this.type == Type.MOD) {
            s += " [Mod]";
        }
        return s;
    }

    /**
     * Compares this Authors with the specified Authors for order. Compares the rank first, then the
     * nickname. Returns a negative integer, zero, or a positive integer as this Authors is less
     * than, equal to, or greater than the specified Authors.
     *
     * @param author the {@link de.msal.shoutemo.connector.model.Author} to be compared.
     * @return a negative integer, zero, or a positive integer as this Authors is less than, equal
     * to, or greater than the specified Authors. Admin comes before Mod comes before User.
     */
    @Override
    public int compareTo(Author author) {
        int tmp = this.type.compareTo(author.type);
        return tmp == 0 ? this.name.compareTo(author.name) : tmp;
    }

    /**
     * Defines of what type the author is. E.g. a normal user or administrator.
     */
    public enum Type {
        /**
         * Administrator. Basically owns the same rights as a Moderator in the autemo shoutbox.
         */
        ADMIN,
        /**
         * Moderator. Can controll the shoutbox.
         */
        MOD,
        /**
         * * A regular user with no special rights.
         */
        USER
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeSerializable(type);
        dest.writeString(avatar);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Author createFromParcel(Parcel in) {
            return new Author(in);
        }

        public Author[] newArray(int size) {
            return new Author[size];
        }
    };

}
