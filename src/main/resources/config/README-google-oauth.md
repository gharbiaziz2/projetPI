# Configuration Google OAuth

Pour activer « Se connecter avec Google », configurez les credentials OAuth 2.0 dans Google Cloud Console :

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Créez ou modifiez des identifiants OAuth 2.0 (type « Desktop app » / « Application installée »)
3. **URIs de redirection autorisés** – ajoutez :
   - `http://localhost:45678`
   - `http://127.0.0.1:45678`
4. Copiez `google-credentials.example.json` vers `google-credentials.json` et remplissez avec vos valeurs (client_id, client_secret)

**Sécurité** : Le fichier `google-credentials.json` est dans `.gitignore` – ne le commitez pas.
