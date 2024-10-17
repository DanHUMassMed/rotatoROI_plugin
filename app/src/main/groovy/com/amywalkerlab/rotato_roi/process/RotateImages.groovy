package com.amywalkerlab.rotato_roi.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi

import com.amywalkerlab.rotato_roi.process.WaitForUserDialog

class RotateImages extends ProcessDirectory {
    String outputDir
    final String out_prefix="rotated_"


 	RotateImages(String directoryRoot, String inputDir = "raw", String outputDir = "rotated", String suffix = ".tif") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
    }

	@Override
	protected boolean processFile(File file, int item_num, int num_items) {
        println("processFile="+file)
        def fullDirectoryPath = file.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name
        
	    def String full_name = file.toString()
	    def ImagePlus imp = IJ.openImage(full_name)
        if (imp == null) {
            IJ.error("Error: Excpected an Image for Rotating got an Nothing.")
            return
        }

		imp.show()
		IJ.getImage().setRoi(null)
        IJ.setTool("line")

        // Prompt user to draw a line
        def termiateProcess = false
        def done = false
        WaitForUserDialog ud = new WaitForUserDialog("Draw a Line", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease draw a line on the image.\nAnd Clicked OK to continue.")
        ud.setVisible(true)
        while(!done) {            
            if(ud.getButtonClicked()=="OK") {
                def lineRoi = IJ.getImage().getRoi()
                if(lineRoi != null && lineRoi.getType() == Roi.LINE){
                    done=true
                    // Measure the angle of the line selection
                    def angle = 0
                    if (lineRoi != null) {
                        angle = lineRoi.getAngle()
                    }
                    
                    // Rotate and Save Tiff
                    IJ.run("Select None");
                    IJ.run(imp, "Rotate...", "angle=" + angle + " grid=1 interpolation=Bilinear")

                    def fileName = imp.getTitle();
                    fileName = "rotated_" + fileName
                    def fullPath = outputDir + File.separator + lastDirectory + File.separator + fileName
                    def roundedAngle = String.format("%.2f", angle)
                    editMetadata(imp, "Angle of rotations", roundedAngle )
                    IJ.saveAs("Tiff", fullPath)
                    

                    
                    IJ.showMessage("Measured Angle", "The angle of rotations is: " + roundedAngle + " degrees.")
                }else{
                    ud = new WaitForUserDialog("Draw a Line", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease draw a line on the image.\nAnd Clicked OK to continue or Cancel to End Processing.")
                    ud.setVisible(true)
                }
            }else if (ud.getButtonClicked()=="Cancel"){
                IJ.error("Processing has been terminated.")
                done=true
                termiateProcess=true
            }
            IJ.wait(50)    
        }
        
        imp.close()
        return termiateProcess
	}
}
