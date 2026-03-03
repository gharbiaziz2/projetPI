-- Run on MySQL (carthagevoyage) for forum comments and like/dislike.

CREATE TABLE IF NOT EXISTS forum_comment (
    id_comment INT AUTO_INCREMENT PRIMARY KEY,
    id_forum INT NOT NULL,
    id_user INT NOT NULL,
    contenu TEXT NOT NULL,
    date_creation DATETIME NOT NULL,
    FOREIGN KEY (id_forum) REFERENCES forum(id_forum) ON DELETE CASCADE,
    FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS forum_reaction (
    id_user INT NOT NULL,
    id_forum INT NOT NULL,
    reaction TINYINT NOT NULL,
    PRIMARY KEY (id_user, id_forum),
    FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_forum) REFERENCES forum(id_forum) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS forum_reaction_comment (
    id_user INT NOT NULL,
    id_comment INT NOT NULL,
    reaction TINYINT NOT NULL,
    PRIMARY KEY (id_user, id_comment),
    FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_comment) REFERENCES forum_comment(id_comment) ON DELETE CASCADE
);
