package tech.gsmith.copathdump;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import tech.gsmith.copathdump.data.generated.CoPathDump;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient;

/**
 *
 * @author Geoffrey H. Smith
 */
public class DumpUtilityPatient {
    
    public static void main(String[] args) throws Exception {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  (select universal_mednum_stripped from dbo.r_pat_demograph pd where patdemog_id = m.patdemog_id) as empi "
          + "from "
          + "  dbo.r_medrec m join dbo.c_d_client dc on(dc.id = m.client_id) "
          + "where "
          + "  dc.abbr = ? "
          + "  and m.medrec_num_stripped = ? "
        );

        CoPathDump coPathDump = new CoPathDump();
        coPathDump.setDumpDate((new Date()).toString());
        coPathDump.setVersion("0.1");
        
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String facilityMrnCsv;
        Pattern patternFacilityMrnCsv = Pattern.compile("^([0-9]*),([A-Z]{3,4})([0-9]*)$");

        int count = 0;
        
        while ((facilityMrnCsv = stdIn.readLine()) != null && facilityMrnCsv.length() != 0) {

            Matcher matcherFacilityMrnCsv = patternFacilityMrnCsv.matcher(facilityMrnCsv);

            if(!matcherFacilityMrnCsv.matches()) {
                System.out.println(String.format("%5d [%s] ignoring", ++count, facilityMrnCsv));
                continue;
            }

            System.out.print(String.format("%5d [%s] %s/%s", ++count, facilityMrnCsv, "ECLH".equals(matcherFacilityMrnCsv.group(2)) ? "EUHM" : matcherFacilityMrnCsv.group(2), matcherFacilityMrnCsv.group(3)));

            // Note that source data from Hari uses "ECLH" (Crawford Long) instead of "EUHM"
            pstmt.setString(1, "ECLH".equals(matcherFacilityMrnCsv.group(2)) ? "EUHM" : matcherFacilityMrnCsv.group(2));
            pstmt.setString(2, matcherFacilityMrnCsv.group(3));

            Patient patient = null;

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {

                System.out.print(".");
                
                if(patient == null || !rs.getString("empi").equals(patient.getEmpi())) {
                    patient = new Patient();
                    coPathDump.getPatient().add(patient);
                    patient.setEmpi(rs.getString("empi"));
                    patient.setSourceRecord(facilityMrnCsv);
                }
                
            }
            
            System.out.println();
             
        }
        
        pstmt.close();
        conn.close();
        
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] {CoPathDump.class});
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(coPathDump, new FileOutputStream(new File("copathdump" + (args.length >= 2 && args[1] != null && args[1].length() > 0 ? args[1] : "") + ".xml")));
        }

    }
    
}
