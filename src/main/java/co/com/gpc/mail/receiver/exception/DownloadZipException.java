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
public class DownloadZipException extends RuntimeException {

    private Integer errorCode;

    public DownloadZipException(String message) {
        super(message);
    }

    public DownloadZipException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadZipException(String message, Throwable cause, ErrorCodes errorCode) {
        super(message, cause);
        this.errorCode = errorCode.getCode();
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
