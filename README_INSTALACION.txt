D0cCtor's Claims 0.3.5 - ID nueva + regeneración del Nexo

Compilar:
  gradle clean build

Cambios:
- Cambié la namespace / ID visible de registros:
  antes: shibuyaclaims
  ahora: d0cctors_claims

IDs nuevas:
  /give @s d0cctors_claims:claim_core
  /give @s d0cctors_claims:combustible_del_ciclo
  /give @s d0cctors_claims:fragmento_expansion

- El nombre visible sigue como D0cCtor's Claims, con apostrofe normal para evitar caracteres rotos.
- Jade debe mostrar la entidad como: Nexo de Protección.
- El Nexo regenera vida si no recibe daño durante 10 minutos.
  Regeneración: 5 de vida por segundo.
- El daño recibido reinicia el contador de 10 minutos.
- /claim setfuel y /claim testmode ahora requieren OP nivel 2.
- /claim setfuel ahora permite hasta 720h, coherente con el límite de 30 días.
- Comandos normales para usuarios:
  /claim info
  /claim list
  /claim here
  /claim trust <jugador>
  /claim untrust <jugador>
  /claim preview
  /claim remove

Importante:
- Cambiar la namespace de shibuyaclaims a d0cctors_claims cambia las IDs.
- En mundos viejos de prueba, items/entidades guardadas con la ID vieja pueden no migrar.
- Para server nuevo queda limpio y evita confundir a los usuarios.
