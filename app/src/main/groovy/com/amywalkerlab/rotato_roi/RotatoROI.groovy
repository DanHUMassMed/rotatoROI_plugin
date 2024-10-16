package com.amywalkerlab.rotato_roi

import java.awt.Font
import java.awt.Color

import ij.IJ
import ij.plugin.PlugIn

import ij.gui.GenericDialog
import ij.Prefs

import com.amywalkerlab.rotato_roi.process.RotateImages
import com.amywalkerlab.rotato_roi.process.CropImages
import io.github.dphiggs01.gldataframe.utils.GLLogger


class Rotato_ROI implements PlugIn{
    final String PIPELINE_TITLE       = "Pipeline - RotatoROI v0.1.1"
    final String PROCESS_ROTATE_TITLE = "Rotate Images"
    final String PROCESS_CROP_TITLE   = "Crop Images"
    final String PROCESS_SELECT_TITLE = "Execute Pipeline Step"   
    final String[] process_labels = [PROCESS_ROTATE_TITLE, PROCESS_CROP_TITLE]
    final int PROCESS_ROTATE = 0
    final int PROCESS_CROP   = 1
  
    @Override
	public void run(String arg) {
        try {
            runPipeline();
            IJ.showMessage("RotatoROI Run Completed.");
        } catch (RotatoROIException e) {
            IJ.showMessage("Error: " + e.getMessage());
            e.printStackTrace();
        }
	}

    def getOptions() {
        def gd = new GenericDialog(PIPELINE_TITLE)
        def messageFont = new Font("Arial", Font.BOLD, 14)
        gd.addMessage(Constants.START_MESSAGE, messageFont)
            
        def directoryRoot = (String) Prefs.get(Constants.ROOT_DIR_OPT, IJ.getDirectory("home"));
        gd.addDirectoryField("Base Directory", directoryRoot)

        boolean[] process_defaultValues = [true, true]
        String[]  process_headings = [PROCESS_SELECT_TITLE]
        gd.addCheckboxGroup(1, 2, process_labels, process_defaultValues, process_headings)

        gd.addMessage("")
        gd.setInsets(0, 0, 0 )
        messageFont = new Font("Arial", Font.BOLD, 12)
        gd.addMessage("Crop Dimensions",messageFont)
        def cropHeight = Prefs.get(Constants.CROP_HEIGHT, Constants.CROP_HEIGHT_DFLT)
        def cropWidth  = Prefs.get(Constants.CROP_WIDTH, Constants.CROP_WIDTH_DFLT)
        gd.setInsets(0, 0, 0 )
        gd.addStringField("Height:", cropHeight, 5)
        gd.addToSameRow()
        gd.addStringField("   Width :", cropWidth, 5)
        gd.showDialog()

        // Check if the dialog was canceled
        if (gd.wasCanceled()) {
            println("User canceled dialog!")
            return null
        }
        
        // Process the dialog
        directoryRoot = gd.getNextString()
        Prefs.set(Constants.ROOT_DIR_OPT, directoryRoot)
        
        def processValues = []
        processValues << gd.getNextBoolean() // Rotate Images
        processValues << gd.getNextBoolean() // Crop Images


        cropHeight = gd.getNextString()
        Prefs.set(Constants.CROP_HEIGHT, cropHeight)

        cropWidth = gd.getNextString()
        Prefs.set(Constants.CROP_WIDTH, cropWidth)
        Prefs.savePreferences() 

        return [directoryRoot, processValues, cropHeight, cropWidth]

    }


    // Program bootstrap: Display Menu and process the given folder
    def runPipeline() {
        def terminateProcess = false
        def options = getOptions()
        if (options != null) {
            def (directoryRoot, processValues, cropHeight, cropWidth) = options  
            def debugLogger = GLLogger.getLogger("debug", directoryRoot)
            /******************* LOGGER LEVEL *************************/
            debugLogger.setLevel(GLLogger.LogLevel.DEBUG)
            def logger = GLLogger.getLogger("log", directoryRoot)
            def startTime = new Date()
            logger.log("Started at: "+startTime.format('yyyy-MM-dd_HH:mm:ss')+"  | In Dir: "+directoryRoot )
            def groovyVersion = GroovySystem.getVersion()
            logger.log("Groovy "+ (' '*26)+ "| Version: " + groovyVersion )
            
            if(processValues[PROCESS_ROTATE]){
                def rotateImages = new RotateImages(directoryRoot)
                    logger.log("RotateImages","Starting rotate process")
                    terminateProcess = rotateImages.processDirectory()
            }
            logger.log("terminateProcess: " + terminateProcess )

            if(!terminateProcess && processValues[PROCESS_CROP]){
                def cropImages = new CropImages(directoryRoot,cropHeight, cropWidth)
                    logger.log("CropImages","Starting crop process")
                    terminateProcess = cropImages.processDirectory()
            }
        
            def endTime = new Date()
            logger.logDuration(startTime, endTime)
        }
        
    }

}
