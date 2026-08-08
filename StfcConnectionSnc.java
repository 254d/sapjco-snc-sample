import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class StfcConnectionSnc {

    private static final String DESTINATION_NAME = "ZHELLO_PROGRAM";

    private StfcConnectionSnc() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println(
                    "Usage: java StfcConnectionSnc <sap-snc.properties> [requestText]");
            System.exit(2);
        }

        Path propertyFile = Path.of(args[0]);
        String requestText = args.length == 2
                ? args[1]
                : "Hello from Java JCo via SNC";

        Properties destinationProperties = loadDestinationProperties(propertyFile);

        registerDestinationProvider(destinationProperties);

        JCoDestination destination = JCoDestinationManager.getDestination(DESTINATION_NAME);

        try {
            // RFC connection verification, including SNC connection and authentication
            destination.ping();

            System.out.printf(
                    "Connected: SID=%s, Client=%s, User=%s%n",
                    destination.getAttributes().getSystemID(),
                    destination.getAttributes().getClient(),
                    destination.getAttributes().getUser());

            JCoFunction function = destination.getRepository().getFunction("STFC_CONNECTION");

            if (function == null) {
                throw new IllegalStateException(
                        "STFC_CONNECTION not found.");
            }

            function.getImportParameterList()
                    .setValue("REQUTEXT", requestText);

            function.execute(destination);

            String echoText = function.getExportParameterList().getString("ECHOTEXT");

            String responseText = function.getExportParameterList().getString("RESPTEXT");

            System.out.println("ECHOTEXT : " + echoText);
            System.out.println("RESPTEXT : " + responseText);

        } catch (JCoException e) {
            System.err.printf(
                    "JCo error: group=%s, key=%s, message=%s%n",
                    e.getGroup(),
                    e.getKey(),
                    e.getMessage());
            throw e;
        }
    }

    private static Properties loadDestinationProperties(Path propertyFile)
            throws IOException {

        if (!Files.isRegularFile(propertyFile)) {
            throw new IOException(
                    "Properties file does not exist: "
                            + propertyFile.toAbsolutePath());
        }

        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(propertyFile.toFile())) {
            properties.load(input);
        }

        require(properties, DestinationDataProvider.JCO_ASHOST);
        require(properties, DestinationDataProvider.JCO_SYSNR);
        require(properties, DestinationDataProvider.JCO_CLIENT);
        require(properties, DestinationDataProvider.JCO_LANG);
        require(properties, DestinationDataProvider.JCO_SNC_MODE, "1");
        require(properties, DestinationDataProvider.JCO_SNC_PARTNERNAME);
        require(properties, DestinationDataProvider.JCO_SNC_QOP);

        // SNC SSO
        if (properties.containsKey(DestinationDataProvider.JCO_USER)
                || properties.containsKey(DestinationDataProvider.JCO_PASSWD)) {
            throw new IllegalArgumentException(
                    "Do not set jco.client.user or jco.client.passwd for SNC SSO connections.");
        }

        return properties;
    }

    private static void require(Properties properties, String key) {
        require(properties, key, null);
    }

    private static void require(
            Properties properties,
            String key,
            String expectedValue) {

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required property is missing: " + key);
        }

        if (expectedValue != null
                && !expectedValue.equals(value.trim())) {
            throw new IllegalArgumentException(
                    key + " must be " + expectedValue);
        }
    }

    private static void registerDestinationProvider(
            Properties destinationProperties) {

        if (!Environment.isDestinationDataProviderRegistered()) {
            Environment.registerDestinationDataProvider(
                    new StaticDestinationDataProvider(destinationProperties));
        }
    }

    private static final class StaticDestinationDataProvider
            implements DestinationDataProvider {

        private final Properties properties;

        private StaticDestinationDataProvider(Properties source) {
            this.properties = new Properties();
            this.properties.putAll(source);
        }

        @Override
        public Properties getDestinationProperties(String destinationName) {
            if (!DESTINATION_NAME.equals(destinationName)) {
                return null;
            }

            Properties copy = new Properties();
            copy.putAll(properties);
            return copy;
        }

        @Override
        public void setDestinationDataEventListener(
                DestinationDataEventListener eventListener) {
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }
    }
}
