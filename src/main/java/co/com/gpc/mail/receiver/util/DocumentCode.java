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
public enum DocumentCode {
    FE("01"),
    NC("91"),
    ND("92"),
    ACUSE("030"),
    RECLAMO("031"),
    RECIBO("032"),
    ACEPTEXP("033"),
    ACEPTTAC("034");

    private String codeDocument;

    DocumentCode(String codeDocument) {
        this.setCodeDocument(codeDocument);
    }

    public String getCodeDocument() {
        return codeDocument;
    }

    public void setCodeDocument(String codeDocument) {
        this.codeDocument = codeDocument;
    }

  @Override
  public String toString() {
    return codeDocument;
  }    
    
}
