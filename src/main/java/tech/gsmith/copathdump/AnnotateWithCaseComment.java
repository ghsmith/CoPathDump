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

/**
 *
 * @author Geoffrey H. Smith
 */
public class AnnotateWithCaseComment {

    public static void main(String[] args) throws Exception {

        SimpleDateFormat sdfExcel = new SimpleDateFormat("MM/dd/yyyy");
    
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  (select text_data from dbo.c_spec_text where specimen_id = s.specimen_id and texttype_id = '$dxcom') as comment_text "
          + "from "
          + "  dbo.c_specimen s "
          + "where "
          + "  s.specnum_formatted = ? "
        );

        CoPathDump coPathDump;
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] { CoPathDump.class });
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            coPathDump = (CoPathDump)unmarshaller.unmarshal(new FileInputStream(args[0]));
        }
        System.out.println(coPathDump.getPatient().size() + "patients loaded");
        
        coPathDump.setVersion("0.4"); // update version to reflect addition of CaseComment
        
        int count = 0;
        
        for(CoPathDump.Patient patient : coPathDump.getPatient()) {
        
            for(CoPathDump.Patient.Case case_ : patient.getCase()) {
                
                System.out.println(String.format("%6d. %s", ++count, case_.getAccessionNumber()));
                
                pstmt.clearParameters();
                pstmt.setString(1, case_.getAccessionNumber());
                ResultSet rs = pstmt.executeQuery();
                
                while(rs.next()) {
                    case_.setCaseComment((case_.getCaseComment() != null ? "\n\n" : "") + rs.getString("comment_text"));
                }
                
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
                    if(case_.getCaseComment() != null) {
                        while(case_.getCaseComment().length() > 0 && case_.getCaseComment().startsWith("\n")) {
                            case_.setCaseComment(case_.getCaseComment().substring(1));
                        }
                        while(case_.getCaseComment().length() > 0 && case_.getCaseComment().endsWith("\n")) {
                            case_.setCaseComment(case_.getCaseComment().substring(0, case_.getCaseComment().length() - 1));
                        }
                        case_.setCaseComment("\n" + case_.getCaseComment() + "\n");
                    }
                }
            }
        }
        
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] {CoPathDump.class});
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(coPathDump, new FileOutputStream(new File(args[0].replace(".xml", "with_comment.xml"))));
        }

    }
    
}
