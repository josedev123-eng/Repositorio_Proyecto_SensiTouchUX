
DECLARE
    nombre VARCHAR2(50) := 'Jose';
BEGIN
    DBMS_OUTPUT.PUT_LINE('Mi nombre es: ' || nombre);
END;
/

DECLARE
    edad NUMBER := 20;
BEGIN
    IF edad >= 18 THEN
        DBMS_OUTPUT.PUT_LINE('Es mayor de edad');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Es menor de edad');
    END IF;
END;
/

BEGIN
    FOR i IN 1..5 LOOP
        DBMS_OUTPUT.PUT_LINE('Número: ' || i);
    END LOOP;
END;
/

DECLARE
    x NUMBER := 10;
    y NUMBER := 0;
    resultado NUMBER;
BEGIN
    resultado := x / y; -- Error
    DBMS_OUTPUT.PUT_LINE(resultado);

EXCEPTION
    WHEN ZERO_DIVIDE THEN
        DBMS_OUTPUT.PUT_LINE('Error: División entre cero');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error desconocido');
END;
/

CREATE OR REPLACE PROCEDURE saludar(p_nombre VARCHAR2) AS
BEGIN
    DBMS_OUTPUT.PUT_LINE('Hola, ' || p_nombre);
END;
/

BEGIN
    saludar('Jose');
END;
/

CREATE OR REPLACE FUNCTION doble(p_num NUMBER)
RETURN NUMBER AS
BEGIN
    RETURN p_num * 2;
END;
/

DECLARE
    resultado NUMBER;
BEGIN
    resultado := doble(8);
    DBMS_OUTPUT.PUT_LINE('El doble es: ' || resultado);
END;
/