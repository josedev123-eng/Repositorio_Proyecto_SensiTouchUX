CREATE TABLE Usuario (
  id_usuario NUMBER PRIMARY KEY,
  nombre VARCHAR(50),
  apellido VARCHAR(50),
  correo VARCHAR(50),
  telefono VARCHAR(9),
  direccion VARCHAR(50)
);

CREATE TABLE LIBRO (
  id_libro NUMBER PRIMARY KEY,
  titulo VARCHAR2(100),
  autor VARCHAR2(50),
  categoria VARCHAR2(30),
  precio NUMBER(4,2),
  stock NUMBER
);

CREATE TABLE COMPRA (
  id_compra NUMBER PRIMARY KEY,
  fecha DATE,
  id_usuario NUMBER,
  total NUMBER(6,2),
  CONSTRAINT fk_usuario FOREIGN KEY (id_usuario)
  REFERENCES Usuario(id_usuario)
);

CREATE TABLE DETALLE_COMPRA (
  id_detalle NUMBER PRIMARY KEY,
  id_compra NUMBER,
  id_libro NUMBER,
  cantidad NUMBER,
  subtotal NUMBER(6,2),
  CONSTRAINT fk_compra FOREIGN KEY (id_compra)
  REFERENCES COMPRA(id_compra),
  CONSTRAINT fk_libro FOREIGN KEY (id_libro)
  REFERENCES LIBRO(id_libro)
);

CREATE TABLE PAGO (
  id_pago NUMBER PRIMARY KEY,
  id_compra NUMBER,
  metodo_pago VARCHAR2(30),
  monto NUMBER(6,2),
  fecha_pago DATE,
  CONSTRAINT fk_pago_compra FOREIGN KEY (id_compra)
  REFERENCES COMPRA(id_compra)
);

CREATE TABLE ENVIO (
  id_envio NUMBER PRIMARY KEY,
  id_compra NUMBER,
  direccion_envio VARCHAR2(70),
  fecha_envio DATE,
  estado_envio VARCHAR2(20),
  CONSTRAINT fk_envio_compra FOREIGN KEY (id_compra)
  REFERENCES COMPRA(id_compra)
);



