-- Run on MySQL (carthagevoyage) to create favorite_hotel table.

CREATE TABLE IF NOT EXISTS favorite_hotel (
    id_user INT NOT NULL,
    id_hotel INT NOT NULL,
    PRIMARY KEY (id_user, id_hotel),
    FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_hotel) REFERENCES hotel(id_hotel) ON DELETE CASCADE
);
