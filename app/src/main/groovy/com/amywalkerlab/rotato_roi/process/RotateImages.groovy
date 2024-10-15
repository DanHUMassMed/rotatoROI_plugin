package com.amywalkerlab.rotato_roi.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.WaitForUserDialog
import ij.gui.Roi

class RotateImages extends ProcessDirectory {
    String outputDir

 	RotateImages(String directoryRoot, String inputDir = "raw", String outputDir = "rotated", String suffix = ".tif") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
    }

	@Override
	protected void processFile(File file, int item_num, int num_items) {
        def fullDirectoryPath = file.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name
        
	    def String full_name = file.toString()
	    def ImagePlus imp = IJ.openImage(full_name)
		imp.show()
		IJ.getImage().setRoi(null)
        IJ.setTool("line")

        // // Prompt user to draw a line
        WaitForUserDialog ud = new WaitForUserDialog("Draw a Line", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\nPlease draw a line on the image.")
        ud.show()
        def escapePressed = ud.escPressed()
	    if(escapePressed){
            throw new Exception("Quit Violently")
        }else{
            def lineRoi = IJ.getImage().getRoi()

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
            IJ.saveAs("Tiff", outputDir + File.separator + lastDirectory + File.separator + fileName)

            def roundedAngle = String.format("%.2f", angle)
            IJ.showMessage("Measured Angle", "The angle of rotations is: " + roundedAngle + " degrees.")
        }
        imp.close()

	}
}
