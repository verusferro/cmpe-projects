import java.io.*;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];
        Matrix matrix = new Matrix();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer, matrix);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer, Matrix matrix)
            throws IOException {

//        String[] parts = command.split("\\s+");
        String[] parts = command.split(" ");
        String operation = parts[0];

        try {
            String result = "";

            switch (operation) {
                case "spawn_host": {
                    // Format: spawn_host hostId clearanceLevel
                    String hostId = parts[1];
                    int clearanceLevel = Integer.parseInt(parts[2]);
                    result = matrix.spawn_host(hostId, clearanceLevel);
                    break;
                }
                case "link_backdoor": {
                    // Format: link_backdoor hostId1 hostId2 latency bandwidth firewall_level
                    String hostId1 = parts[1];
                    String hostId2 = parts[2];
                    int latency = Integer.parseInt(parts[3]);
                    int bandwidth = Integer.parseInt(parts[4]);
                    int firewall_level = Integer.parseInt(parts[5]);
                    result = matrix.link_backdoor(hostId1, hostId2, latency, bandwidth, firewall_level);
                    break;
                }
                case "seal_backdoor": {
                    // Format: seal_backdoor hostId1 hostId2
                    String hostId1 = parts[1];
                    String hostId2 = parts[2];
                    result = matrix.seal_backdoor(hostId1, hostId2);
                    break;
                }
                case "trace_route": {
                    // Format: trace_route sourceId destId min_bandwidth lambda
                    String sourceId = parts[1];
                    String destId = parts[2];
                    int min_bandwidth = Integer.parseInt(parts[3]);
                    int lambda = Integer.parseInt(parts[4]);
                    result = matrix.trace_route(sourceId, destId, min_bandwidth, lambda);
                    break;
                }
                case "scan_connectivity": {
                    // Format: scan_connectivity
                    result = matrix.scan_connectivity();
                    break;
                }
                case "simulate_breach": {
                    // Format: simulate_breach hostId1 (hostId2)
                    String hostId1 = parts[1];
                    if (parts.length == 3) {
                        String hostId2 = parts[2];
                        result = matrix.simulate_breach(hostId1, hostId2);
                    } else {
                        result = matrix.simulate_breach(hostId1);
                    }
                    break;
                }
                case "oracle_report": {
                    // Format: oracle_report
                    result = matrix.oracle_report();
                    break;
                }
                default:
                    result = "Unknown command: " + operation;
            }

            writer.write(result);
            writer.newLine();

        } catch (Exception e) {
            writer.write("Error processing command: " + command);
            writer.newLine();
        }
    }
}