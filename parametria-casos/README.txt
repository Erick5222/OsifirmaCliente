CASOS DE PRUEBA — parametria minima
===================================

Copie el JSON del caso a parametria.json (raiz del proyecto) o en client.properties:

  parametria.path=parametria-casos/CP-12-xades-xml-sin-visor.json

Variables (placeholders en JSON)
--------------------------------
  ${JWT_TOKEN}              -> env FIRMA_JWT_TOKEN o -Dfirma.jwt.token
  ${NODE_ID_ALFRESCO}       -> env FIRMA_NODE_ID_ALFRESCO (documento PDF en Alfresco)
  ${NODE_ID_ALFRESCO_XML}   -> env FIRMA_NODE_ID_ALFRESCO_XML (nodo con .xml para CP-12)
  ${NODE_ID_ALFRESCO_DOCX}  -> env FIRMA_NODE_ID_ALFRESCO_DOCX (nodo con .docx para CP-14)
  ${REFRESH_TOKEN}          -> env FIRMA_REFRESH_TOKEN
  ${CALLBACK_URL}           -> env FIRMA_CALLBACK_URL
  ${DOCUMENT_BASE64}        -> CP-07: parametria-casos/vars/document.base64

PRUEBAS XAdES y CAdES desde ALFRESCO (sin visor PDF)
--------------------------------------------------

Requisitos comunes:
  - token ${JWT_TOKEN} valido (Keycloak)
  - Nodo en Alfresco con el tipo de archivo correcto (xml / pdf / docx)
  - sinVisor=true, modo=soloFirma, FirmaArrastrar=false
  - param.signatureLevel=B (T/LTA requieren webTsa)
  - Subida post-firma: destinationPath = UUID carpeta; sube PDF, .p7s, .xml firmados

CP-12  XAdES — XML desde Alfresco
  nodeIdAlfresco: ${NODE_ID_ALFRESCO_XML}
  param.signatureFormat: 2
  param.fileName: debe terminar en .xml

  Pasos:
  1. Subir un .xml al repositorio y copiar su nodeId a FIRMA_NODE_ID_ALFRESCO_XML
  2. parametria.path=parametria-casos/CP-12-xades-xml-sin-visor.json
  3. Firmar -> descarga documento_alfresco[F].xml

CP-13  CAdES — PDF desde Alfresco
  nodeIdAlfresco: ${NODE_ID_ALFRESCO} (mismo nodo PDF que CP-01)
  param.signatureFormat: 3

  Salida: documento_alfresco[F].p7s

CP-14  CAdES — Word (.docx) desde Alfresco
  nodeIdAlfresco: ${NODE_ID_ALFRESCO_DOCX}
  param.signatureFormat: 3
  param.fileName: .docx

  Salida: documento_alfresco[F].p7s

CP-10  Negativo XAdES
  signatureFormat=2 pero rutaLocal=documento_prueba.pdf
  Debe fallar: "XAdES requiere un documento XML"

Parametros clave en param (por formato)
---------------------------------------
  signatureFormat   1=PAdES  2=XAdES  3=CAdES
  signatureLevel    B | T | LTA  (T y LTA requieren webTsa / TSA en Administracion)
  webTsa            URL TSA (solo si nivel T o LTA)
  applyImage        true solo PAdES con sello; false en XAdES/CAdES
  fileName          Nombre logico; extension debe ser .xml para XAdES

Panel Administracion
--------------------
  Si guardo nivel T sin URL TSA, la firma falla. Use signatureLevel:"B" en JSON
  o nivel B en ~/.osinergmin-firma/signing-admin.properties

Matriz resumida
---------------
CP-01   Alfresco + arrastre PAdES
CP-02   Alfresco + posicion fija PAdES
CP-05   sinVisor + Alfresco PAdES
CP-07   bytes Base64
CP-08   firma invisible PAdES
CP-09   credential alias
CP-10   Negativo XAdES (PDF + format 2)
CP-11   solo texto estilo 4
CP-12   XAdES XML desde Alfresco sin visor
CP-13   CAdES PDF desde Alfresco -> .p7s
CP-14   CAdES .docx desde Alfresco -> .p7s
CP-15   Alfresco PAdES firma por tag (signatureTagName GYAURI, TA)
CP-16   PDF local ruta Windows absoluta (origenDocumento=local, rutaLocal)
