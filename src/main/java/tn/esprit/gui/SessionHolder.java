package tn.esprit.gui;

import tn.esprit.entities.User;

/** Holds the currently logged-in user (e.g. ADMIN) for the back office. */
public final class SessionHolder {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
