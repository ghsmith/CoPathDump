package tech.gsmith.copathdump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import tech.gsmith.copathdump.data.generated.CoPathDump;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.Procedures;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.Procedures.Procedure;

/**
 *
 * @author Geoffrey H. Smith
 */
public class AnnotateWithProcedures {

    public static void main(String[] args) throws Exception {

        SimpleDateFormat sdfExcel = new SimpleDateFormat("MM/dd/yyyy");
    
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select " +
            "  c_d_sprotype.name proc_name, " +
            "  (select text_data from c_spec_text where specimen_id = p_special_proc.specimen_id and link_inst = p_special_proc.sp_inst and texttype_id = '$procint') as procint_text, " +
            "  (select text_data from c_spec_text where specimen_id = p_special_proc.specimen_id and link_inst = p_special_proc.sp_inst and texttype_id = '$procres') as procres_text " +
            "from " +
            "  p_special_proc " +
            "  join c_d_sprotype on (c_d_sprotype.id = p_special_proc.sprotype_id) " +
            "where " +
            "  p_special_proc.specimen_id = (select specimen_id from c_specimen where specnum_formatted = ?) " +
            "order by " +
            "  p_special_proc.sp_inst  "
        );

        CoPathDump coPathDump;
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] { CoPathDump.class });
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            coPathDump = (CoPathDump)unmarshaller.unmarshal(new FileInputStream(args[1]));
        }

        System.out.println(coPathDump.getPatient().size() + " patients loaded");
        int caseCount = 0;
        for(CoPathDump.Patient patient : coPathDump.getPatient()) {
            for(CoPathDump.Patient.Case case_ : patient.getCase()) {
                caseCount++;
            }
        }
        
        coPathDump.setVersion("0.5"); // update version to reflect addition of procedures
        
        int count = 0;
        int procCount = 0;
        
        for(CoPathDump.Patient patient : coPathDump.getPatient()) {
            
            for(CoPathDump.Patient.Case case_ : patient.getCase()) {

                System.out.println(String.format("%6d/%6d. %s", ++count, caseCount, case_.getAccessionNumber()));

                if(count <= Integer.parseInt(args[2])) {
                    continue;
                }
                
                pstmt.clearParameters();
                pstmt.setString(1, case_.getAccessionNumber());
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()) {

                    System.out.println(String.format("procCount=%d", ++procCount));
                    
                    if(rs.getString("procint_text") != null || rs.getString("procres_text") != null) {
                        Procedure procedure = new Procedure();
                        if(case_.getProcedures() == null) { case_.setProcedures(new Procedures()); }
                        case_.getProcedures().getProcedure().add(procedure);
                        procedure.setProcedureName(rs.getString("proc_name"));
                        procedure.setProcedureInterp(
                            rs.getString("procint_text") != null
                            ? rs.getString("procint_text")
                                .replace("\u0008", "") // there are ASCII 08 (backspace?) characters in this column
                                .replace("\r", "")
                                .replaceAll("\\s+$","")
                            : null
                        );
                        procedure.setProcedureResult(
                            rs.getString("procres_text") != null
                            ? rs.getString("procres_text")
                                .replace("\u0008", "") // there are ASCII 08 (backspace?) characters in this column
                                .replace("\r", "")
                                .replaceAll("\\s+$","")
                            : null
                        );
                    }

                }
                
                if(count == Integer.parseInt(args[3])) {
                    break;
                }
                
            }
        
            if(count == Integer.parseInt(args[3])) {
                break;
            }

        }

        System.out.println();
        
        pstmt.close();
        conn.close();

        // fix-ups:
        // 1. strip leading/trailing newline(s) from comment text narratives
        // 2. add ONE leading and ONE trailing newline to comment text narratives
        {
            for(CoPathDump.Patient patient : coPathDump.getPatient()) {
                for(CoPathDump.Patient.Case case_ : patient.getCase()) {
                    if(case_.getProcedures() != null) {
                        for(CoPathDump.Patient.Case.Procedures.Procedure procedure : case_.getProcedures().getProcedure()) {
                            if(procedure.getProcedureInterp() != null) {
                                while(procedure.getProcedureInterp().length() > 0 && procedure.getProcedureInterp().startsWith("\n")) {
                                    procedure.setProcedureInterp(procedure.getProcedureInterp().substring(1));
                                }
                                while(procedure.getProcedureInterp().length() > 0 && procedure.getProcedureInterp().endsWith("\n")) {
                                    procedure.setProcedureInterp(procedure.getProcedureInterp().substring(0, procedure.getProcedureInterp().length() - 1));
                                }
                                procedure.setProcedureInterp("\n" + procedure.getProcedureInterp() + "\n");
                            }
                            if(procedure.getProcedureResult() != null) {
                                while(procedure.getProcedureResult().length() > 0 && procedure.getProcedureResult().startsWith("\n")) {
                                    procedure.setProcedureResult(procedure.getProcedureResult().substring(1));
                                }
                                while(procedure.getProcedureResult().length() > 0 && procedure.getProcedureResult().endsWith("\n")) {
                                    procedure.setProcedureResult(procedure.getProcedureResult().substring(0, procedure.getProcedureResult().length() - 1));
                                }
                                procedure.setProcedureResult("\n" + procedure.getProcedureResult() + "\n");
                            }
                        }
                    }
                }
            }
        }
        
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] {CoPathDump.class});
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(coPathDump, new FileOutputStream(new File(args[1].replace(".xml", ".with_procs.xml"))));
        }

    }
    
}
