# NetworkAC  
*Network Binôme Alagbo koffi Uriel & Coulibaly malick axel*

---

## Master 2 MIAGE - MMM - Année 2023/2024  
## TP2 – Réseau doméstique

Ce projet consiste à construire une application Android permettant de gérer des objets connectés dans un appartement. L'application permet de créer, modifier et sauvegarder un réseau de dispositifs connectés, tout en offrant une interface interactive et bilingue (français/anglais).

---

## Description du Projet

### Fonctionnalités Principales

- **Menu Principal :**
  - Réinitialiser le réseau.
  - Sauvegarder le réseau dans la mémoire interne.
  - Charger un réseau sauvegardé depuis la mémoire interne.
  - Sélection de modes distincts pour :
    - L’ajout d’objets.
    - L’ajout de connexions.
    - La modification des objets et des connexions.

- **Modèle du Réseau :**
  - La classe `Graph` représente le modèle du réseau. Elle contient la description complète du graphe (objets, connexions, couleurs, positions, etc.) qui est sauvegardé et restauré.

- **Affichage et Interaction :**
  - **Affichage :**  
    - La classe `DrawableGraph` (héritant de `android.graphics.drawable.Drawable`) permet de générer une image à partir du modèle.
    - La classe `GraphView` (héritant de `View`) affiche le graphe sur un `Canvas` personnalisé.
    - Un plan d’appartement est affiché en taille réelle. Si l’image est trop grande, le plan peut être défilé horizontalement et/ou verticalement.
  - **Interaction :**
    - **Ajout d’objets :**  
      Un long‑click sur le plan permet d’ajouter un objet connecté en précisant son étiquette, qui s’affiche à côté de l’objet.
    - **Déplacement :**  
      Un glissement sur un objet le déplace (avec ses connexions associées).
    - **Création de connexions :**  
      Un glissement entre deux objets permet de créer une connexion (visible durant le geste). La connexion n’est établie que si le glissement se termine sur un autre objet et qu’elle n’existe pas déjà. L’étiquette de la connexion est spécifiée lors de la création et s’affiche au milieu, légèrement décalée.
    - **Menus contextuels :**  
      - Un long‑click sur un objet affiche un menu permettant de supprimer ou modifier l’objet (étiquette, couleur ou icône).
      - Un long‑click sur l’étiquette d’une connexion affiche un menu permettant de supprimer ou modifier la connexion (étiquette, couleur, épaisseur).

- **Bilinguisme :**
  - L’application est entièrement bilingue (français/anglais), avec le français comme langue par défaut.

---

## Structure du Projet

- **app/src/main/java/ci/miage/mob/networkAC**  
  - `Graph.kt` : Modèle du réseau contenant objets et connexions.
  - `GraphView.kt` : Vue customisée pour l’affichage du réseau et la gestion des interactions.
  - `DrawableGraph.kt` : Génère une image du réseau à partir du modèle.
  - `MainActivity.kt` : Activité principale de l’application.

- **app/src/main/res**  
  - **Drawable :** Images et icônes (plan d’appartement, icônes d’objets, etc.).
  - **Layout :** Fichiers de mise en page.
  - **Menu :** Menus contextuels pour les objets et connexions.
  - **Valeurs :** Fichiers de chaînes (strings), couleurs et thèmes (pour la prise en charge du bilinguisme).

- **Autres fichiers importants :**
  - Les fichiers de configuration Gradle.
  - Le fichier `.gitignore` pour exclure les fichiers indésirables.

---

## Installation et Exécution

1. **Cloner le dépôt :**
   ```bash
   git clone https://github.com/urpsychopath/NetworkAC.git
