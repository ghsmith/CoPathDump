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
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport.CPSRItem;
import tech.gsmith.copathdump.data.generated.CoPathDump.Patient.Case.CasePart.CasePartSynopticReport.CPSRItem.CPSRItemValue;

/**
 *
 * @author Geoffrey H. Smith
 */
public class DumpUtility {
    
    public static void main(String[] args) throws Exception {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection conn = DriverManager.getConnection(args[0]);
        
        PreparedStatement pstmt = conn.prepareStatement(
            "select "
          + "  (select universal_mednum_stripped from dbo.r_pat_demograph pd where patdemog_id = s.patdemog_id) as empi, "
          + "  (select abbr from dbo.c_d_client where id = s.client_id) as mrn_facility, "
          + "  (select max(medrec_num_stripped) from dbo.r_medrec where client_id = s.client_id and patdemog_id = s.patdemog_id) as mrn, "
          + "  s.specnum_formatted as accession_no, "
          + "  p.part_designator as part_designator, "
          + "  dp.abbr as part_type, "
          + "  dp.name as part_type_name, "
          + "  p.part_description as part_description, "
          + "  dsw.abbr as worksheet, "
          + "  dsw.version as worksheet_version, "
          + "  dsw.wstitle as worksheet_name, "
          + "  ssd.inst as item_no, "
          + "  dsc.abbr as item, "
          + "  dsc.name as item_name, "
          + "  dsv.abbr as val, "
          + "  dsv.name as val_name, "
          + "  dsv.fillin_type as val_freetext_type, "
          + "  ssd.fillin_char as val_freetext_char, "
          + "  ssd.fillin_number as val_freetext_number, "
          + "  dsv.snomed_id as snomed_id, "
          + "  (select count(*) from dbo.r_medrec where client_id = s.client_id and patdemog_id = s.patdemog_id) as mrn_count "
          + "from "
          + "  dbo.c_specimen s "
          + "  join dbo.p_part p on(p.specimen_id = s.specimen_id) "
          + "  join dbo.c_d_parttype dp on(dp.id = p.parttype_id) "
          + "  left outer join "
          + "  ( "
          + "    dbo.c_spec_synoptic_ws ssw "
          + "    join dbo.c_d_synoptic_ws dsw on(dsw.id = ssw.worksheet_id) "
          + "    left outer join "
          + "    ( "
          + "      dbo.c_spec_synoptic_dx ssd "
          + "      join dbo.c_d_synoptic_value dsv on(dsv.id = ssd.synoptic_value_id) "
          + "      join dbo.c_d_synoptic_category dsc on(dsc.id = dsv.category_id) "
          + "    ) on(ssd.specimen_id = ssw.specimen_id and ssd.worksheet_inst = ssw.ws_inst) "
          + "  ) on(ssw.specimen_id = s.specimen_id and ssw.part_inst = p.part_inst) "
          + "where "
          + "  s.patdemog_id = (select patdemog_id from dbo.r_medrec m join dbo.c_d_client dc on(dc.id = m.client_id) where dc.abbr = ? and m.medrec_num_stripped = ?) "
          + "order by 1, s.accession_date, 4, 5, 9, cast(ssd.inst as int), 15 "
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
            Case case_ = null;
            CasePart casePart = null;
            CasePartSynopticReport casePartSynopticReport = null;
            CPSRItem cpsrItem = null;
            CPSRItemValue cpsrItemValue = null;

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {

                System.out.print(".");
                
                if(patient == null || !rs.getString("empi").equals(patient.getEmpi())) {
                    patient = new Patient();
                    case_ = null;
                    casePart = null;
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    coPathDump.getPatient().add(patient);
                    patient.setEmpi(rs.getString("empi"));
                    patient.setSourceRecord(facilityMrnCsv);
                }
                
                if(case_ == null || !rs.getString("accession_no").equals(case_.getAccessionNumber())) {
                    case_ = new Case();
                    casePart = null;
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    patient.getCase().add(case_);
                    case_.setAccessionNumber(rs.getString("accession_no"));
                    case_.setMrn(rs.getString("mrn_facility") + "_" + rs.getString("mrn"));
                    case_.setMrnCount(rs.getInt("mrn_count"));
                }
                
                if(casePart == null || !rs.getString("part_designator").equals(casePart.getDesignator())) {
                    casePart = new CasePart();
                    casePartSynopticReport = null;
                    cpsrItem = null;
                    cpsrItemValue = null;
                    case_.getCasePart().add(casePart);
                    casePart.setDesignator(rs.getString("part_designator"));
                    casePart.setPartType(rs.getString("part_type"));
                    casePart.setPartTypeDisp(rs.getString("part_type_name"));
                    casePart.setDescription(rs.getString("part_description"));
                }
                
                if(rs.getString("worksheet") != null) {
                
                    if(casePartSynopticReport == null || !rs.getString("worksheet").equals(casePartSynopticReport.getSrName())) {
                        casePartSynopticReport = new CasePartSynopticReport();
                        cpsrItem = null;
                        cpsrItemValue = null;
                        casePart.getCasePartSynopticReport().add(casePartSynopticReport);
                        casePartSynopticReport.setSrName(rs.getString("worksheet"));
                        casePartSynopticReport.setSrVersion(rs.getString("worksheet_version"));
                        casePartSynopticReport.setSrNameDisp(rs.getString("worksheet_name"));
                    }

                    if(cpsrItem == null || rs.getInt("item_no") != cpsrItem.getNumber()) {
                        cpsrItem = new CPSRItem();
                        cpsrItemValue = null;
                        casePartSynopticReport.getCPSRItem().add(cpsrItem);
                        cpsrItem.setNumber(rs.getInt("item_no"));
                        cpsrItem.setItemName(rs.getString("item"));
                        cpsrItem.setItemNameDisp(rs.getString("item_name"));
                    }

                    {
                        cpsrItemValue = new CPSRItemValue();
                        cpsrItem.getCPSRItemValue().add(cpsrItemValue);
                        cpsrItemValue.setValName(rs.getString("val"));
                        cpsrItemValue.setValNameDisp(rs.getString("val_name"));
                        cpsrItemValue.setSnomedConceptId(rs.getString("snomed_id"));
                        cpsrItemValue.setValue(rs.getString("val_freetext_char") != null ? rs.getString("val_freetext_char") : rs.getString("val_freetext_number"));
                    }
                    
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
