/**
 * CMPE 250 Project 1 - Nightpass Survivor Card Game
 * 
 * This skeleton provides file I/O infrastructure. Implement your game logic
 * as you wish. There are some import that is suggested to use written below. 
 * You can use them freely and create as many classes as you want. However,
 * you cannot import any other java.util packages with data structures, you
 * need to implement them yourself. For other imports, ask through Moodle before 
 * using.
 * 
 * TESTING YOUR SOLUTION:
 * ======================
 * 
 * Use the Python test runner for automated testing:
 * 
 * python test_runner.py              # Test all cases
 * python test_runner.py --type type1 # Test only type1  
 * python test_runner.py --type type2 # Test only type2
 * python test_runner.py --verbose    # Show detailed diffs
 * python test_runner.py --benchmark  # Performance testing (no comparison)
 * 
 * Flags can be combined, e.g.:
 * python test_runner.py -bv              # benchmark + verbose
 * python test_runner.py -bv --type type1 # benchmark + verbose + type1
 * python test_runner.py -b --type type2  # benchmark + type2
 * 
 * MANUAL TESTING (For Individual Runs):
 * ======================================
 * 
 * 1. Compile: cd src/ && javac *.java
 * 2. Run: java Main ../testcase_inputs/test.txt ../output/test.txt
 * 3. Compare output with expected results
 * 
 * PROJECT STRUCTURE:
 * ==================
 * 
 * project_root/
 * ├── src/                     # Your Java files (Main.java, etc.)
 * ├── testcase_inputs/         # Input test files  
 * ├── testcase_outputs/        # Expected output files
 * ├── output/                  # Generated outputs (auto-created)
 * └── test_runner.py           # Automated test runner
 * 
 * REQUIREMENTS:
 * =============
 * - Java SDK 8+ (javac, java commands)
 * - Python 3.6+ (for test runner)
 * 
 */

import java.io.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Check command line arguments
        if (args.length != 2) {
            System.out.println("Usage: java Main <input_file> <output_file>");
            System.out.println("Example: java Main ../testcase_inputs/test.txt ../output/test.txt");
            return;
        }

        String inFile = args[0];
        String outFile = args[1];

        // Initialize file reader
        Scanner reader = null;
        try {
            reader = new Scanner(new File(inFile));
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inFile);
            e.printStackTrace();
            return;
        }

        // Initialize file writer
        FileWriter writer = null;
        try {
            writer = new FileWriter(outFile);
        } catch (IOException e) {
            System.out.println("Writing error: " + outFile);
            e.printStackTrace();
            if (reader != null)
                reader.close();
            return;
        }

        Game game = new Game();

        // Process commands line by line
        try {
            while (reader.hasNextLine()) {
                // Replaced line parsing logic to not create a new Scanner object each loop
                // Instead it now uses the same scanner to read the next lines
                // I have no idea why the original code went with such a method
                String line = reader.nextLine();
                String[] tokens = line.trim().split("\\s+");
                
                if (tokens.length == 0) continue;
                
                String command = tokens[0];
                String out = "";

                switch (command) {
                    case "draw_card": {
                        String name = tokens.length > 1 ? tokens[1] : "";
                        int att = tokens.length > 2 ? Integer.parseInt(tokens[2]) : 0;
                        int hp = tokens.length > 3 ? Integer.parseInt(tokens[3]) : 0;
                        out = game.draw_card(name, att, hp);
                        break;
                    }
                    case "battle": {
                        int att = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;
                        int hp = tokens.length > 2 ? Integer.parseInt(tokens[2]) : 0;
                        int heal = tokens.length > 3 ? Integer.parseInt(tokens[3]) : 0;
                        out = game.battle(att, hp, heal);
                        break;
                    }
                    case "find_winning": {
                         out = game.findWinning();
                        break;
                    }
                    case "deck_count": {
                        out = game.deckCount();
                        break;
                    }
                    case "discard_pile_count": {
                        out = game.discardPileCount();
                        break;
                    }
                    case "steal_card": {
                        int att = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;
                        int hp = tokens.length > 2 ? Integer.parseInt(tokens[2]) : 0;
                        out = game.steal_card(att, hp);
                        break;
                    }
                    default: {
                        System.out.println("Invalid command: " + command);
                        writer.close();
                        reader.close();
                        return;
                    }
                }

                try {
                    writer.write(out);
                    writer.write("\n");
                } catch (IOException e2) {
                    System.out.println("Writing error");
                    e2.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("Error processing commands: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up resources
        try {
            writer.close();
        } catch (IOException e2) {
            System.out.println("Writing error");
            e2.printStackTrace();
        }

        if (reader != null) {
            reader.close();
        }

        System.out.println("end");
    }
}
