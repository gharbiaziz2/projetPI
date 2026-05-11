package tn.esprit.gui;

import tn.esprit.entities.User;

/**
 * Gestionnaire de session pour stocker l'utilisateur courant connecté.
 */
public class SessionManager {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getIdUser() : -1;
    }

    public static String getCurrentUserName() {
        if (currentUser != null) {
            return currentUser.getNom() + " " + currentUser.getPrenom();
        }
        return "Utilisateur inconnu";
    }
}
