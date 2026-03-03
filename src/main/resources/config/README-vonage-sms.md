# Configuration Vonage SMS (Mot de passe oublié)

Pour que l'envoi de SMS fonctionne, configurez dans `config.properties` :

1. **vonage.api.key** – Clé API Vonage (déjà configurée)
2. **vonage.api.secret** – Secret API Vonage (obligatoire)
   - Récupérez-le sur [Vonage API Dashboard](https://dashboard.nexmo.com/) → API settings
3. **vonage.from** (optionnel) – Nom de l'expéditeur (par défaut : CarthageVoyage)
   - Pour certains pays, un numéro virtuel Vonage peut être requis

L'utilisateur doit avoir un numéro de téléphone enregistré dans son profil pour recevoir le SMS.
