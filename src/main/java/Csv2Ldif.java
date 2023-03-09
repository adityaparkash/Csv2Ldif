import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Csv2Ldif {

    private static final String CONFIG_FILE = "/config.properties";

    public static void main(String[] args) throws IOException {

        // input and output files
        String inputCsvFile = args[0];
        String outputLdifFile = args[1];

        // Read config prop files
        Properties props = new Properties();
        try (InputStream input = Csv2Ldif.class.getResourceAsStream(CONFIG_FILE)) {
            props.load(input);
        }

        String[] dnFormat = StringUtils.split(props.getProperty("dnFormat"), ",");
        Map<String, Boolean> ldapAttrs = new LinkedHashMap<>();
        for (String attr : StringUtils.split(props.getProperty("ldapAttrs"), ",")) {
            String[] parts = StringUtils.split(attr, ":");
            ldapAttrs.put(parts[0], Boolean.parseBoolean(parts[1]));
        }

        // Create CSV parser
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withDelimiter(',').withEscape('\\').withIgnoreSurroundingSpaces(true);
        CSVParser csvParser = new CSVParser(new FileReader(inputCsvFile), csvFormat);

        // Create LDIF writer
        PrintWriter ldifWriter = new PrintWriter(new FileWriter(outputLdifFile));

        // Parse CSV file
        List<CSVRecord> records = csvParser.getRecords();

        // Loop and convert to LDIF entry
        for (CSVRecord record : records) {
            // Build DN for entry
            String dn = "cn=" + record.get(0) + "," + StringUtils.join(dnFormat, ",");

            // Build LDIF entry
            StringBuilder ldif = new StringBuilder();
            ldif.append("dn: ").append(dn).append("\n");
            ldif.append("changetype: add\n");

            // Add LDAP attributes to LDIF entry
            for (Map.Entry<String, Boolean> entry : ldapAttrs.entrySet()) {
                String attributeName = entry.getKey();
                boolean isMultiValued = entry.getValue();
                String[] attributeValues = StringUtils.split(record.get(attributeName), "|");

                if (attributeValues.length == 0) {
                    continue;
                }

                if (isMultiValued) {
                    for (String attributeValue : attributeValues) {
                        ldif.append(attributeName).append(": ").append(attributeValue).append("\n");
                    }
                } else {
                    ldif.append(attributeName).append(": ").append(attributeValues[0]).append("\n");
                }
            }

            ldif.append("\n");

            // Write LDIF entry
            ldifWriter.write(ldif.toString());
        }

        // Close resources
        csvParser.close();
        ldifWriter.close();
    }
}
