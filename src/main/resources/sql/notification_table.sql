-- Run this SQL in your MySQL database (carthagevoyage) to create the notification table

CREATE TABLE IF NOT EXISTS notification (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    message VARCHAR(500) NOT NULL,
    date_creation DATETIME NOT NULL,
    lu TINYINT(1) DEFAULT 0,
    FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE CASCADE
);
