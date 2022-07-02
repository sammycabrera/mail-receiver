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
public enum ErrorCodes {  
    VALIDATION_PARSE_ERROR(410),
    DOWNLOAD_ZIP_ERROR(420),
    ZIP_FOLDER_NOT_FOUND(430),
    READ_ZIP_ERROR(440),
    FORMAT_DATE_ERROR(450),
    SIGNATURE_NOTFOUND_ERROR(460);

    private int code;

    ErrorCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}