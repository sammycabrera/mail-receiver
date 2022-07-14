/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver;

import static co.com.gpc.mail.receiver.util.Constants.SPLIT_CHAR;
import static co.com.gpc.mail.receiver.util.Constants.SPLIT_CHAR_SUBJECT;
import static co.com.gpc.mail.receiver.util.MessageCode.VAL_SUBJECT_EST;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author scabrera
 */
@Slf4j
public class DocumentTypeSubject {

    public static void main(String[] ags) {
        String emailSubject = "860002595; NEWELL BRANDS DE COLOMBIA; FESP74345041806; 860002595; NEWELL BRANDS DE COLOMBIA; FESP7434504";
        List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
        for (int i = 0; i < subjectList.size(); i++) {
            log.info("Elemento " + subjectList.get(i) + " Pos " + i);
        }
        String codetype = "01, 91, 92, 030, 031, 032, 033, 034";
        int posCodeFE = 3;
        int posCodeEvent = 5;
        String documentType = "";
        if (subjectList.size() < 4) {
            log.error(VAL_SUBJECT_EST + " 1 {" + emailSubject + "}");
        } else {
            List<String> codetypeList = new ArrayList<>(Arrays.asList(codetype.split(SPLIT_CHAR)));
            codetypeList.replaceAll(String::trim);
            subjectList.replaceAll(String::trim);
            log.info(codetypeList.toString());
            if (subjectList.size() > posCodeFE && subjectList.get(posCodeFE) != null && codetypeList.contains(subjectList.get(posCodeFE))) {
                documentType = subjectList.get(posCodeFE);
                log.info("ES FACTURA");
            }else{
                log.error("No se pudo obtener documentType 1"+codetypeList.contains(subjectList.get(posCodeFE)));
            }
            if (documentType.length() == 0 && subjectList.size() > posCodeEvent && subjectList.get(posCodeEvent) != null
                    && codetypeList.contains(subjectList.get(posCodeEvent))) {
                documentType = subjectList.get(posCodeEvent);
                log.info("ES EVENTO");
            }else{
                log.error("No se pudo obtener documentType 2");
            }

            if (documentType.length() == 0) {
                log.error(VAL_SUBJECT_EST + " 2 {" + emailSubject + "}");

            }

        }

    }

}
