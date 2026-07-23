package io.oblivion.asm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Oblivion proprietary stack de-obfuscation mapping file
 * located at build/outputs/oblivion/mapping.txt.
 */
public class OblivionMappingWriter {

    public static class MappingEntry {
        public final String className;
        public final String methodName;
        public final String methodDesc;
        public final int stateId;
        public final int originalLineNumber;

        public MappingEntry(String className, String methodName, String methodDesc, int stateId, int originalLineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.stateId = stateId;
            this.originalLineNumber = originalLineNumber;
        }

        @Override
        public String toString() {
            String lineStr = (originalLineNumber > 0) ? String.valueOf(originalLineNumber) : "UNKNOWN_LINE";
            return String.format("%s.%s%s -> State[0x%08X] (Line %s)",
                    className, methodName, methodDesc, stateId, lineStr);
        }
    }

    private final List<MappingEntry> entries = new ArrayList<>();

    public synchronized void recordStateMapping(String className, String methodName, String methodDesc, int stateId, int originalLineNumber) {
        entries.add(new MappingEntry(className, methodName, methodDesc, stateId, originalLineNumber));
    }

    public synchronized void writeMappingFile(File outputFile) throws IOException {
        if (outputFile == null) return;
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, false))) {
            writer.println("# Oblivion Security De-obfuscation Mapping File");
            writer.println("# Generated automatically by io.oblivion.hardener");
            writer.println("# " + new java.util.Date());
            writer.println("--------------------------------------------------");

            for (MappingEntry entry : entries) {
                writer.println(entry.toString());
            }
        }
    }
}
