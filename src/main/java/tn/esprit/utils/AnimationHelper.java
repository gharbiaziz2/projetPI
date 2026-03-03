package tn.esprit.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationHelper {

    /**
     * Applique un effet de zoom arrière/avant fluide sur une carte au survol.
     */
    public static void applyHoverScaleEffect(Node card) {
        // Animation d'entrée (grossissement)
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setToX(1.02); // 102% de la taille
        scaleIn.setToY(1.02);

        // Animation de sortie (retour normal)
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), card);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        card.setOnMouseEntered(e -> {
            scaleOut.stop();
            scaleIn.play();
            // Optionnel : Changer la souris en main
            card.setCursor(javafx.scene.Cursor.HAND);
        });

        card.setOnMouseExited(e -> {
            scaleIn.stop();
            scaleOut.play();
        });
    }

    /**
     * Apparition en fondu de toute la page/composant.
     */
    public static void applyFadeIn(Node rootNode) {
        if (rootNode == null)
            return;
        rootNode.setOpacity(0); // Commence invisible
        FadeTransition ft = new FadeTransition(Duration.millis(600), rootNode);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}
