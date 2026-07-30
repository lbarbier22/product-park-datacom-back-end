CREATE TABLE products (
                          id               BIGSERIAL PRIMARY KEY,

                          name             VARCHAR(255),
                          reference        VARCHAR(255),
                          description      VARCHAR(1000),

                          category         VARCHAR(255),
                          subcategory      VARCHAR(255),
                          manufacturer     VARCHAR(150),
                          country          VARCHAR(255),

                          lot              VARCHAR(255),
                          certification    VARCHAR(255),

                          status           VARCHAR(50) NOT NULL,
                          current_step     INTEGER     NOT NULL,
                          rejection_reason VARCHAR(1000),

                          created_by       BIGINT      NOT NULL REFERENCES users(id),
                          created_at       TIMESTAMP   NOT NULL,
                          updated_at       TIMESTAMP
);