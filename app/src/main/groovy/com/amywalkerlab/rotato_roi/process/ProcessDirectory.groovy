package com.amywalkerlab.rotato_roi.process

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import ij.IJ
import io.github.dphiggs01.gldataframe.GLDataframe
import io.github.dphiggs01.gldataframe.GLDataframeException
import io.github.dphiggs01.gldataframe.utils.GLLogger

abstract class ProcessDirectory {
    //GLLogger logger
    String directoryRoot
    String inputDir
    String outputDir
    String suffix

    ProcessDirectory(String directoryRoot, String inputDir, String outputDir, String suffix = ".tif") {
        //this.logger = GLLogger.getLogger("debug", directoryRoot) 
        this.directoryRoot = directoryRoot
        this.inputDir = directoryRoot + File.separator + inputDir 
        this.outputDir = directoryRoot + File.separator + outputDir
        this.suffix = suffix
        //this.stillToProcess = new ArrayList<>()
        //this.toBeRemoved = new ArrayList<>()
        
        def inputDirExists = new File(this.inputDir).isDirectory()
        def outputDirExists = new File(this.outputDir).isDirectory()
		if (!inputDirExists) {
    		IJ.error("Error", "Input Directory '$this.inputDir' does not exist.")
		} else if (!outputDirExists){
			def dir = new File(this.outputDir)
            dir.mkdirs()
		} else {
            compareSubdirectories(this.inputDir, this.outputDir)
        }
    }

    // Function to get immediate subdirectories of a given directory
    List<Path> getImmediateSubdirectories(Path dirPath) throws IOException {
        List<Path> subDirs = []
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, Files::isDirectory)) {
            for (Path entry : stream) {
                subDirs.add(entry)
            }
        }
        return subDirs
    }

    // Groovy-specific method to delete a directory and its contents
    void deleteUnkownDirectory(String dirPath) {
        def dir = new File(dirPath)
        if (dir.exists()) {
            dir.deleteDir()  // Deletes the directory and all its contents
        }
    }

    // Function to compare directories and delete subdirectories in 'save' that are not in 'master'
    Set<String> compareSubdirectories(String inputDir, String outputDir) throws IOException {
        Path inputPath = Paths.get(inputDir)
        Path outputPath = Paths.get(outputDir)

        // Get immediate subdirectories of both directories
        List<Path> inputSubDirs = getImmediateSubdirectories(inputPath)
        List<Path> outputSubDirs = getImmediateSubdirectories(outputPath)

        // Create a set of subdirectory names in master for easier comparison
        Set<String> masterSubDirNames = new HashSet<>()
        for (Path subDir : inputSubDirs) {
            masterSubDirNames.add(subDir.getFileName().toString())
        }

        // Check save subdirectories and delete those not in master
        for (Path inputSubDir : outputSubDirs) {
            if (!masterSubDirNames.contains(inputSubDir.getFileName().toString())) {
                deleteUnkownDirectory(inputSubDir.toString())  // Use the full path as a string
                println "Deleted directory: $inputSubDir"
            }
        }
        return masterSubDirNames
    }

    // Finished setup
    //////////////////////////////////////////////////////////////////////////////

    // Function to get a list of .tif files from a given directory
    List<String> getTifFilesFromDir(Path dirPath) throws IOException {
        List<String> tifFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.tif")) {
            for (Path file : stream) {
                tifFiles.add(file.getFileName().toString());
            }
        }
        return tifFiles;
    }

    private void deleteUnknownFiles(List<String> toBeRemoved) {
        for (String fileName : toBeRemoved) {
            try {
                Path filePath = Paths.get(fileName); // Assuming fileName contains the full path
                Files.deleteIfExists(filePath); // Delete the file if it exists
                System.out.println("Deleted: " + filePath);
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + fileName + " due to: " + e.getMessage());
            }
        }
    }

    // method to compare directories and populate the lists
    List<String> compareDirectories(String masterDirPath, String slaveDirPath) throws IOException {
        List<String> stillToProcess;
         List<String> toBeRemoved;
        Path masterPath = Paths.get(masterDirPath);
        Path slavePath = Paths.get(slaveDirPath);
        
        String prefixName = slavePath.getName(slavePath.getNameCount() - 2).toString();

        // Get the list of .tif files in both directories
        List<String> masterFiles = getTifFilesFromDir(masterPath);
        List<String> slaveFiles = getTifFilesFromDir(slavePath);

        List<String> updatedSlaveFileNames = new ArrayList<>();
        for (String fileName : slaveFiles) {
            // Remove the "rotated_" prefix if it exists
            updatedSlaveFileNames.add(fileName.replaceFirst("^"+prefixName+"_", ""));
        }

        // Convert lists to sets for easier comparison
        Set<String> masterFileSet = new HashSet<>(masterFiles);
        Set<String> slaveFileSet = new HashSet<>(updatedSlaveFileNames);

        // Populate the instance variables with full paths
        stillToProcess = new ArrayList<>(masterFileSet);
        stillToProcess.removeAll(slaveFileSet);

        toBeRemoved = new ArrayList<>(slaveFileSet);
        toBeRemoved.removeAll(masterFileSet);
        println("toBeRemoved="+toBeRemoved)

        deleteUnknownFiles(toBeRemoved);
        println("stillToProcess size="+stillToProcess.size());
        println("stillToProcess="+stillToProcess)

        return stillToProcess
    }

    List<String> listNextLevelDirectories(String dirPath) {
        List<String> nextLevelDirs = new ArrayList<>();
        Path parentDir = Paths.get(dirPath);

        // Check if the directory exists and is indeed a directory
        if (Files.exists(parentDir) && Files.isDirectory(parentDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        nextLevelDirs.add(entry.toAbsolutePath().toString()); 
                    }
                }
            } catch (IOException e) {
                println("Error reading directory: " + e.getMessage());
            }
        } else {
            println("The given path is not a valid directory.");
        }

        return nextLevelDirs;
    }


    // Process all the files in the provided root/raw directory
    public boolean processDirectory() {
        def list = listNextLevelDirectories(inputDir)
        def directoriesString = list.join(', ')
        //logger.debug("directories to process: " + directoriesString)
        if(!list){
            IJ.error("Error", "Input Directory '$this.inputDir' MUST have subdirectories for controls and experimental conditions. (e.g. raw/EV, raw/sams-1)")
        }
        return processSubDirectories(list)
    }

    // Process all the files in the ontrols and experimental conditions directories
    private boolean processSubDirectories(nextLevelDirectories) {
        def terminateProcess = false

        for (int i = 0; i < nextLevelDirectories.size(); i++) {
            def dirPath = nextLevelDirectories[i]
            //def list = (new File(dirPath)).listFiles().findAll { file ->
            //    file.name.endsWith(suffix)  // Filter files based on suffix
            //}

            // Make the output directory
            def lastDirectory = new File(dirPath).name

            def outputDirExp = new File(outputDir + File.separator + lastDirectory)
            outputDirExp.mkdirs()
            def list = compareDirectories(dirPath, outputDirExp.getAbsolutePath())


            int num_items = list.size()  // Total number of matching files

            // Create the subdirectory and process files
            for (int index = 0; index < num_items; index++) {
                def file = list[index]
                int item_num = index + 1  // Current file's position (1-based index)
                terminateProcess = processFile(new File(dirPath+ File.separator +file), item_num, num_items)
                if (terminateProcess) return terminateProcess
            }
        }

        return terminateProcess      
    }

    protected boolean processFile_test(File file, int item_num, int num_items){
        println("file="+file+" item_num="+item_num+" num_items="+num_items)
        return false
    }

    // Abstract method to be implemented by subclasses
    protected abstract boolean processFile(File file, int item_num, int num_items)
}
