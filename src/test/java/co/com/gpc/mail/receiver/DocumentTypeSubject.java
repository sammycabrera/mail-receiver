/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver;

import static co.com.gpc.mail.receiver.util.Constants.SPLIT_CHAR_SUBJECT;
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
    public static void main(String[] ags){
         String emailSubject = "890319193; SISTEMAS DE INFORMACION EMPRESARIAL S.A; 07IZ8966; 01; SISTEMAS DE INFORMACION EMPRESARIAL S.A.";
         List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
         for(int i=0; i < subjectList.size(); i++){
             log.info("Elemento "+subjectList.get(i)+ " Pos "+i);
         }
         
    }
    
}
