/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.util;

/**
 *
 * @author scabrera
 */
public enum MessageCode {
    
    
  VAL_MESSAGE(101, "Error al tratar de validar mensaje"),//Error to validate message
  VAL_OVER_SIZE(102, "El tamaño del mensaje supera las especificaciones de la DIAN"), //Email over size
  VAL_VALID_DIAN(103, "Codigo de validacion DIAN no encontrado"), //DIAN validation not found
  VAL_VALID_DIAN_SEG(104, "Segmento de respuesta de validacion DIAN no encontrado"), //Segment of DIAN validation not found
  VAL_NOT_XML(105, "Email no contiene documento XML dentro del adjunto"), //Message email not content attachment file
  VAL_INVALID_SCHEMAXML(106, "Documento no cumple con las especificaciones del Esquema UBL DIAN"),//Invalid electronic document according to DIAN Schema UBL 2.1
  VAL_DSIGNATURE(107, "No es valida firma digital de documento electronico"),//Invalid digital signature electronic document
  EMAIL_SHOW_CONTENT(108, "Error al tratar de leer texto del correo"),//Error to show email content message
  VAL_SENDERPARTY_SEG(109, "No encontrada informacion de emisor en el documento electronico"),//SenderParty section not found in XML
  VAL_RECEIVERPARTY_SEG(110, "No encontrada informacion de receptor en el documento electronico"),//ReceiverParty section not found in XML
  VAL_SENDERPARTY_WRONG(111, "Emisor del documento no corresponde con Nit del asunto"),//Wrong SenderParty Subject
  VAL_RECEIVERPARTY_WRONG(112, "Receptor del documento electronico no corresponde con esta empresa del grupo"),//Receiver party is wrong 
  ERROR_DOWNLOAD_FILES(113, "Error al descargar archivos adjuntos del correo"),//Error to download files
  VAL_SUBJECT_EST(114, "El asunto del correo no cumple con las especificaciones de la DIAN"); //Error subject structure found

  private final int code;
  private final String description;

  private MessageCode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getDescription() {
     return description;
  }

  public int getCode() {
     return code;
  }

  @Override
  public String toString() {
    return code + ": " + description+" ";
  }
}