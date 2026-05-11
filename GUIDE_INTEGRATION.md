# 📚 Guide d'Intégration des Nouvelles Fonctionnalités

## 📍 Localisation des Fichiers

### Contrôleurs
- **Client**: `src/main/java/tn/esprit/gui/VoyageSuggestionClientController.java`
- **Admin**: `src/main/java/tn/esprit/gui/VoyagePropositionAdminController.java`

### Fichiers FXML
- **Client**: `src/main/resources/fxml/front_voyage_suggestion_client.fxml`
- **Admin**: `src/main/resources/fxml/back_voyage_proposition_admin.fxml`

### Services Modifiés
- `src/main/java/tn/esprit/services/VoyageServices.java`
- `src/main/java/tn/esprit/services/UserServices.java`

---

## 🔧 Étapes d'Intégration

### ÉTAPE 1: Ajouter le Bouton dans back.fxml (Admin Menu)

Ouvrez: `src/main/resources/fxml/back.fxml`

**Cherchez cette section**:
```xml
<Button fx:id="menuSuggestion" text="  Suggestions" styleClass="sidebar-menu-item" onAction="#goSuggestion"/>
<Label text="RÉSERVATIONS" styleClass="sidebar-section"/>
```

**Ajoutez ce nouveau bouton avant "RÉSERVATIONS"**:
```xml
<Button fx:id="menuPropositions" text="  Propositions" styleClass="sidebar-menu-item" onAction="#goPropositions"/>
<Label text="RÉSERVATIONS" styleClass="sidebar-section"/>
```

---

### ÉTAPE 2: Ajouter le Champ dans BackController.java

Ouvrez: `src/main/java/tn/esprit/gui/BackController.java`

**Cherchez cette section**:
```java
@FXML private Button menuSuggestion;
@FXML private Label headerTitle;
```

**Ajoutez la nouvelle ligne**:
```java
@FXML private Button menuPropositions;  // ← Nouvelle ligne
@FXML private Label headerTitle;
```

---

### ÉTAPE 3: Ajouter la Méthode dans BackController.java

**Cherchez cette méthode**:
```java
@FXML private void goSuggestion() { 
    setActiveMenu(menuSuggestion); 
    loadContent("/fxml/back_voyage_suggestion.fxml", "Suggestions Voyage", "refresh"); 
}
```

**Ajoutez après cette méthode**:
```java
@FXML private void goPropositions() { 
    setActiveMenu(menuPropositions); 
    loadContent("/fxml/back_voyage_proposition_admin.fxml", "Gestion des Propositions", "initialize"); 
}
```

---

### ÉTAPE 4: Mettre à Jour setActiveMenu() dans BackController.java

**Cherchez cette méthode**:
```java
private void setActiveMenu(Button active) {
    Button[] all = { menuDashboard, menuUsers, menuActivite, menuDestination, menuForum, menuHotel, menuChambre,
            menuReservationHotel, menuReservationTransport, menuReservationActivite, menuReservationVoyage, menuTransportLocal, menuVoyage, menuProgramme, menuSuggestion };
    for (Button b : all) if (b != null) b.getStyleClass().remove("active");
    if (active != null) active.getStyleClass().add("active");
}
```

**Modifiez le tableau pour inclure menuPropositions**:
```java
private void setActiveMenu(Button active) {
    Button[] all = { menuDashboard, menuUsers, menuActivite, menuDestination, menuForum, menuHotel, menuChambre,
            menuReservationHotel, menuReservationTransport, menuReservationActivite, menuReservationVoyage, menuTransportLocal, menuVoyage, menuProgramme, menuSuggestion, menuPropositions };  // ← Ajoutez menuPropositions
    for (Button b : all) if (b != null) b.getStyleClass().remove("active");
    if (active != null) active.getStyleClass().add("active");
}
```

---

### ÉTAPE 5: Ajouter le Bouton dans front.fxml (Client Menu)

Ouvrez: `src/main/resources/fxml/front.fxml`

**Cherchez cette section**:
```xml
<Button fx:id="btnChatbot" text="Chatbot" styleClass="nav-btn" onAction="#goChatbot"/>
<Region HBox.hgrow="ALWAYS"/>
```

**Modifiez pour ajouter un bouton de suggestion**:
```xml
<Button fx:id="btnChatbot" text="Chatbot" styleClass="nav-btn" onAction="#goChatbot"/>
<Button fx:id="btnProposerVoyage" text="Proposer" styleClass="nav-btn" onAction="#goProposerVoyage"/>
<Region HBox.hgrow="ALWAYS"/>
```

---

### ÉTAPE 6: Ajouter le Champ dans FrontController.java

Ouvrez: `src/main/java/tn/esprit/gui/FrontController.java`

**Cherchez cette section**:
```java
@FXML private Button btnChatbot;
@FXML private Button userMenuButton;
```

**Ajoutez la nouvelle ligne**:
```java
@FXML private Button btnChatbot;
@FXML private Button btnProposerVoyage;  // ← Nouvelle ligne
@FXML private Button userMenuButton;
```

---

### ÉTAPE 7: Ajouter la Méthode dans FrontController.java

**Cherchez la section des méthodes de navigation**:
```java
public void goChatbot() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/front_chatbot.fxml"));
        Parent root = loader.load();
        // ...
    }
}
```

**Ajoutez après**:
```java
public void goProposerVoyage() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/front_voyage_suggestion_client.fxml"));
        Parent root = loader.load();
        contentStack.getChildren().setAll(root);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

---

## ✅ Compilation et Test

### Après les modifications:

1. **Compilez le projet**:
   ```bash
   mvn clean compile
   ```

2. **Lancez l'application**:
   ```bash
   mvn javafx:run
   ```

3. **Testez comme ADMIN**:
   - Se connecter en tant qu'administrateur
   - Cliquer sur "Propositions" dans le menu latéral
   - Voir les propositions en attente
   - Accepter/Refuser une proposition
   - Ajouter des programmes de jour

4. **Testez comme CLIENT**:
   - Se connecter en tant que client
   - Cliquer sur "Proposer" dans la barre de navigation
   - Remplir le formulaire de proposition
   - Envoyer la proposition

---

## 🐛 Troubleshooting

### Erreur: "FXML file not found"
- Vérifiez que les fichiers `.fxml` sont bien dans `src/main/resources/fxml/`
- Vérifiez le chemin dans le `FXMLLoader`

### Erreur: "Controller not found"
- Vérifiez que l'attribut `fx:controller` dans le FXML correspond au nom complet de la classe
- Exemple: `fx:controller="tn.esprit.gui.VoyagePropositionAdminController"`

### Le bouton n'apparaît pas
- Vérifiez que vous avez ajouté `@FXML private Button menuX;` dans le contrôleur
- Vérifiez que le `fx:id` dans le FXML correspond exactement au nom du champ

### Les modifications ne prennent pas effet
- Assurez-vous de recompiler le projet: `mvn clean compile`
- Relancez l'application: `mvn javafx:run`

---

## 📝 Notes Importantes

1. **Statut des Propositions**:
   - `EN ATTENTE`: Proposition nouvellement créée
   - `ACCEPTEE`: Acceptée par l'admin et guide assigné
   - `REFUSEE`: Refusée par l'admin

2. **Programmes de Jour**:
   - Ne peuvent être ajoutés que pour les voyages acceptés
   - Accessible via le dashboard admin

3. **Base de Données**:
   - Les colonnes `id_user_createur` et `est_proposition_client` doivent exister dans la table `voyage`
   - Si elles n'existent pas, exécutez:
   ```sql
   ALTER TABLE voyage ADD COLUMN id_user_createur INT DEFAULT 0;
   ALTER TABLE voyage ADD COLUMN est_proposition_client BOOLEAN DEFAULT FALSE;
   ```

---

## 🔄 Flux d'Intégration Résumé

```
┌─────────────────────────────────────────────┐
│ 1. Modifiez front.fxml et back.fxml         │
├─────────────────────────────────────────────┤
│ 2. Modifiez FrontController.java            │
├─────────────────────────────────────────────┤
│ 3. Modifiez BackController.java             │
├─────────────────────────────────────────────┤
│ 4. Compilez: mvn clean compile              │
├─────────────────────────────────────────────┤
│ 5. Testez: mvn javafx:run                   │
└─────────────────────────────────────────────┘
```

---

## 📞 Support

Pour toute question ou problème d'intégration, vérifiez:
- Les chemins des fichiers
- Les noms des contrôleurs dans les FXML
- Les imports dans les fichiers Java
- La compilation sans erreurs
