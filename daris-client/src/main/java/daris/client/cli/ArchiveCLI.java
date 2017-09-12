package daris.client.cli;

import java.io.File;

import daris.client.archive.ArchiveCreator;
import daris.client.archive.ArchiveCreatorSettings;
import daris.client.archive.ArchiveExtractor;
import daris.client.archive.ArchiveExtractorSettings;
import daris.client.archive.ArchiveType;

public class ArchiveCLI {

    public static final String APP_NAME = "daris-archive";

    public static void main(String[] args) throws Throwable {
        if (args.length < 2) {
            System.err.println("Error: missing arguments.");
            showUsage();
            System.exit(1);
        }
        /*
         * action
         */
        String action = args[0];
        if (!"extract".equals(action) && !"create".equals(action)) {
            System.err.println("Error: invalid action: " + action + ". Expects 'extract' or 'create'.");
            showUsage();
            System.exit(2);
        }

        try {
            if ("extract".equals(action)) {
                ArchiveExtractorSettings settings = new ArchiveExtractorSettings();
                for (int i = 1; i < args.length;) {
                    if (args[i].equals("--quiet")) {
                        settings.setQuiet(true);
                        i++;
                    } else if (args[i].equals("--overwrite")) {
                        settings.setOverwrite(true);
                        i++;
                    } else {
                        if (settings.inputArchiveFile() == null) {
                            File f = new File(args[i]);
                            if (!f.exists()) {
                                throw new IllegalArgumentException("Input file:'" + args[i] + "' does not exist.");
                            }
                            if (!ArchiveType.isArchiveFile(f)) {
                                throw new IllegalArgumentException("Input file:'" + args[i]
                                        + "' is not in (supported) archive format. Expects .zip or .aar files.");
                            }
                            settings.setInputArchiveFile(f);
                            i++;
                        } else if (settings.outputDirectory() == null) {
                            File dir = new File(args[i]);
                            if (!dir.exists()) {
                                throw new IllegalArgumentException(
                                        "Output directory:'" + args[i] + "' does not exist.");
                            }
                            if (!dir.isDirectory()) {
                                throw new IllegalArgumentException("'" + args[i] + "' is not a directory.");
                            }
                            settings.setOutputDirectory(dir);
                            i++;
                        } else {
                            throw new IllegalArgumentException("Invalid arguments.");
                        }
                    }
                }
                if (settings.outputDirectory() == null) {
                    settings.setOutputDirectory(new File(System.getProperty("user.dir")));
                }
                settings.validate();
                new ArchiveExtractor(settings).run();
            } else {
                ArchiveCreatorSettings settings = new ArchiveCreatorSettings();
                for (int i = 1; i < args.length;) {
                    if (args[i].equals("--quiet")) {
                        settings.setQuiet(true);
                        i++;
                    } else {
                        if (settings.outputArchiveFile() == null) {
                            File f = new File(args[i]);
                            if (f.exists()) {
                                throw new IllegalArgumentException("Output file: '" + args[i] + "' already exist.");
                            }
                            if (!ArchiveType.isArchiveFile(f)) {
                                throw new IllegalArgumentException("Output file: '" + args[i]
                                        + "' is not a (supported) archive. Expects .zip or .aar files.");
                            }
                            settings.setOutputArchiveFile(f);
                            i++;
                        } else {
                            File f = new File(args[i]);
                            if (!f.exists()) {
                                throw new IllegalArgumentException("'" + args[i] + "' does not exist.");
                            }
                            settings.addInput(f);
                            i++;
                        }
                    }
                }
                settings.setBaseDir(new File(System.getProperty("user.dir")));
                settings.validate();
                new ArchiveCreator(settings).run();
            }
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            if (ex instanceof IllegalArgumentException) {
                showUsage();
            }
            System.exit(1);
        }
    }

    private static void showUsage() {
        // @formatter:off
        System.out.println();
        System.out.println("USAGE (Extract Archive):");
        System.out.println("    " + APP_NAME + " extract [--overwrite] [--quiet] <src-archive-file> [output-directory]");
        System.out.println();
        System.out.println();
        System.out.println("USAGE (Create Archive):");
        System.out.println("    " + APP_NAME + " create [--quiet] <dst-archive-file> <input-files>");
        System.out.println();
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("    " + APP_NAME + " extract book.zip");
        System.out.println("    " + APP_NAME + " extract book.aar /home/wilson/Documents");
        System.out.println("    " + APP_NAME + " create book.zip /home/wilson/Downloads/book");
        System.out.println("    " + APP_NAME + " create book.aar /home/wilson/Downloads/book");
        System.out.println();
        // @formatter:on
    }
}
