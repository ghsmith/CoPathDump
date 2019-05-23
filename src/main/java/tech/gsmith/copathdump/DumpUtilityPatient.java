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

        /*Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  (select universal_mednum_stripped from dbo.r_pat_demograph pd where patdemog_id = m.patdemog_id) as empi "
          + "from "
          + "  dbo.r_medrec m join dbo.c_d_client dc on(dc.id = m.client_id) "
          + "where "
          + "  dc.abbr = ? "
          + "  and m.medrec_num_stripped = ? "
        );*/

        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection conn = DriverManager.getConnection(args[0], args[1], args[2]);

        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  alias empi "
          + "from "
          + "  hnamdwh.person_alias "
          + "where "
          + "  alias_pool_cd = 667191 "
          + "  and active_ind = 1 "
          + "  and active_status_cd = 188 "
          + "  and data_status_cd = 25 "
          + "  and person_id in "
          + "  ( "
          + "    select "
          + "      person_id "
          + "    from "
          + "      hnamdwh.person_alias "
          + "    where "
          + "      alias_pool_cd = decode "
          + "      ( "
          + "        ?, "
          + "        'EUH', 667193, "
          + "        'EUHM', 667206, "
          + "        'TEC', 667211, "
          + "        'EJCH', 455595002, "
          + "        'SJH', 455595650 "
          + "      ) "
          + "      and alias = ? "
          + "      and active_ind = 1 "
          + "      and active_status_cd = 188 "
          + "      and data_status_cd = 25 "
          + "  ) "
          + "order by "
          + "  1 "
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

            Patient patient = new Patient();
            coPathDump.getPatient().add(patient);
            patient.setSourceRecord(facilityMrnCsv);

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {

                System.out.print(".");
                
                patient.setEmpi(patient.getEmpi() == null ? rs.getString("empi") : patient.getEmpi() + ", " + rs.getString("empi"));
                
            }
            
            System.out.println();
             
        }
        
        pstmt.close();
        conn.close();
        
        {
            JAXBContext jc = JAXBContext.newInstance(new Class[] {CoPathDump.class});
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(coPathDump, new FileOutputStream(new File("copathdump.xml")));
        }

    }
    
}
