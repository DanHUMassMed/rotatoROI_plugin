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
    GLLogger logger
    String directoryRoot
    String inputDir
    String outputDir
    String suffix

    ProcessDirectory(String directoryRoot, String inputDir, String outputDir, String suffix = ".tif") {
        this.logger = GLLogger.getLogger("debug", directoryRoot) 
        this.directoryRoot = directoryRoot
        this.inputDir = directoryRoot + File.separator + inputDir 
        this.outputDir = directoryRoot + File.separator + outputDir
        this.suffix = suffix

        def mkCleanDir = { dir_nm -> 
            def dir = new File(dir_nm) 
            dir.deleteDir()
            dir.mkdirs()
        }
        
        // Check if the input directory exists if it does, make the output directory if needed.
        def directoryExists = new File(this.inputDir).isDirectory()

		if (!directoryExists) {
    		IJ.error("Error", "Input Directory '$this.inputDir' does not exist.")
		} else {
			mkCleanDir(this.outputDir)
		}
    }

    private List<String> listNextLevelDirectories(String dirPath) {
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
                logger.error("Error reading directory: " + e.getMessage());
            }
        } else {
            logger.error("The given path is not a valid directory.");
        }

        return nextLevelDirs;
    }


    // Process all the files in the provided root/raw directory
    public void processDirectory() {
        def list = listNextLevelDirectories(inputDir)
        def directoriesString = list.join(', ')
        logger.debug("directories to process: " + directoriesString)
        if(!list){
            IJ.error("Error", "Input Directory '$this.inputDir' MUST have subdirectories for controls and experimental conditions. (e.g. raw/EV, raw/sams-1)")
        }
        processSubDirectories(list)
    }

    // Process all the files in the ontrols and experimental conditions directories
    private void processSubDirectories(nextLevelDirectories) {
        nextLevelDirectories.each { dirPath ->
	    	def list = (new File(dirPath)).listFiles().findAll { file ->
                file.name.endsWith(suffix) // Filter files based on suffix
                }

            //Make the output directory
            def lastDirectory = new File(dirPath).name
            def dir = new File(outputDir + File.separator + lastDirectory )         
            dir.mkdirs()

            int num_items = list.size()  // Total number of matching files

            //create the subdirectory

            for (int index = 0; index < num_items; index++) {
                def file = list[index]
                int item_num = index + 1  // Current file's position (1-based index)
                processFile(file, item_num, num_items)
            }

	    }        
    }

    // Abstract method to be implemented by subclasses
    protected abstract void processFile(File file, int item_num, int num_items)
}
