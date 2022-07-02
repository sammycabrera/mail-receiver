/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.exception;

/**
 *
 * @author scabrera
 */
public class SignatureNotFoundException extends RuntimeException {

    private Integer errorCode;

    public SignatureNotFoundException(String message) {
        super(message);
    }

    public SignatureNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureNotFoundException(String message, Throwable cause, ErrorCodes errorCode) {
        super(message, cause);
        this.errorCode = errorCode.getCode();
    }
    
    public SignatureNotFoundException(String message, ErrorCodes errorCode) {
        super(message);
        this.errorCode = errorCode.getCode();
    }    

    public Integer getErrorCode() {
        return errorCode;
    }
}
