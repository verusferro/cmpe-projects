import java.io.*;
import java.util.Locale;

/**
 * Main entry point for GigMatch Pro platform.
 */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        Matcher matcher = new Matcher();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer, matcher);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer, Matcher matcher)
            throws IOException {

//        String[] parts = command.split("\\s+");
        // Saves 500ms
        String[] parts = command.split(" ");
        String operation = parts[0];

        try {
            String result = "";
            switch (operation) {
                case "register_customer": {
                    // Format: register_customer customerID
                    String id = parts[1];
                    result = matcher.register_customer(id);
                    break;
                }
                case "register_freelancer": {
                    // Format: register_freelancer freelancerID serviceName basePrice T C R E A
                    String id = parts[1];
                    String service = parts[2];
                    int price = Integer.parseInt(parts[3]);
                    int T = Integer.parseInt(parts[4]);
                    int C = Integer.parseInt(parts[5]);
                    int R = Integer.parseInt(parts[6]);
                    int E = Integer.parseInt(parts[7]);
                    int A = Integer.parseInt(parts[8]);
                    result = matcher.register_freelancer(id,service,price,T,C,R,E,A);
                    break;
                }
                case "request_job": {
                    // Format: request_job customerID serviceName topK
                    String cust = parts[1];
                    String service = parts[2];
                    int topK = Integer.parseInt(parts[3]);
                    result = matcher.request_job(cust, service, topK);
                    break;
                }
                case "employ_freelancer": {
                    // Format: employ_freelancer customerID freelancerID
                    String cust = parts[1];
                    String free = parts[2];
                    result = matcher.employ_freelancer(cust, free);
                    break;
                }
                case "complete_and_rate": {
                    // Format: complete_and_rate freelancerID rating
                    String free = parts[1];
                    int rating = Integer.parseInt(parts[2]);
                    result = matcher.complete_and_rate(free, rating);
                    break;
                }
                case "cancel_by_freelancer": {
                    // Format: cancel_by_freelancer freelancerID
                    String free = parts[1];
                    result = matcher.cancel_by_freelancer(free);
                    break;
                }
                case "cancel_by_customer": {
                    // Format: cancel_by_customer customerID freelancerID
                    String cust = parts[1];
                    String free = parts[2];
                    result = matcher.cancel_by_customer(cust, free);
                    break;
                }
                case "blacklist": {
                    // Format: blacklist customerID freelancerID
                    String cust = parts[1];
                    String free = parts[2];
                    result = matcher.blacklist(cust, free);
                    break;
                }
                case "unblacklist": {
                    // Format: unblacklist customerID freelancerID
                    String cust = parts[1];
                    String free = parts[2];
                    result = matcher.unblacklist(cust, free);
                    break;
                }
                case "change_service": {
                    // Format: change_service freelancerID newService newPrice
                    String free = parts[1];
                    String newService = parts[2];
                    int newPrice = Integer.parseInt(parts[3]);
                    result = matcher.change_service(free, newService, newPrice);
                    break;
                }
                case "simulate_month": {
                    // Format: simulate_month
                    result = matcher.simulate_month();
                    break;
                }
                case "query_freelancer": {
                    // Format: query_freelancer freelancerID
                    String id = parts[1];
                    result = matcher.query_freelancer(id);
                    break;
                }
                case "query_customer": {
                    // Format: query_customer customerID
                    String id = parts[1];
                    result = matcher.query_customer(id);
                    break;
                }
                case "update_skill": {
                    // Format: update_skill freelancerID T C R E A
                    String id = parts[1];
                    int T = Integer.parseInt(parts[2]);
                    int C = Integer.parseInt(parts[3]);
                    int R = Integer.parseInt(parts[4]);
                    int E = Integer.parseInt(parts[5]);
                    int A = Integer.parseInt(parts[6]);
                    result = matcher.update_skill(id,T,C,R,E,A);
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