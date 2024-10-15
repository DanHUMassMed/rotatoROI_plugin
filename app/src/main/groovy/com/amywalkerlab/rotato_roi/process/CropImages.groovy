package com.amywalkerlab.rotato_roi.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.WaitForUserDialog
import ij.gui.Roi
import ij.plugin.frame.RoiManager

class CropImages extends ProcessDirectory {
    String outputDir
    String cropHeight
    String cropWidth
    final String in_prefix="^rotated_"
    final String out_prefix="cropped_"

 	CropImages(String directoryRoot, String cropHeight, String cropWidth, String inputDir = "rotated", String outputDir = "cropped", String suffix = ".tif") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
        this.cropHeight = cropHeight
        this.cropWidth = cropWidth
    }

	@Override
	protected void processFile(File file, int item_num, int num_items) {
        def fullDirectoryPath = file.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name
        String fileNameTrimmed = file.getName().replaceFirst(in_prefix, "")

	    def String full_name = file.toString()
	    def ImagePlus imp = IJ.openImage(full_name)
		imp.show()
		IJ.getImage().setRoi(null)
        IJ.setTool("point")

        // // Prompt user to draw a line
        def done = false
        while(!done) {
            WaitForUserDialog ud = new WaitForUserDialog("Crop Image", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\nPlease select location point to start crop.")
            ud.show()
            def escapePressed = ud.escPressed()
            if(escapePressed){
                throw new Exception("Quit Violently")
            }else{
                def pointRoi = IJ.getImage().getRoi()
                if(pointRoi != null && pointRoi.getType() == Roi.POINT){
                    done=true
                    IJ.run("Select None");
                    imp.setRoi(pointRoi.x, pointRoi.y,Integer.parseInt(cropWidth),Integer.parseInt(cropHeight));
                    IJ.run("Specify...", "");
                    def rectangleRoi = IJ.getImage().getRoi()
                    def roiTitle=out_prefix + fileNameTrimmed
                    def args="title='" + roiTitle +"'"
                    IJ.run("Duplicate...", args);
                    ImagePlus roiImage = IJ.getImage();
                    // Save the ROI as a TIFF file
                    IJ.saveAs(roiImage,"Tiff", outputDir + File.separator + lastDirectory + File.separator + roiTitle)
                    roiImage.close()
                }else{
                    IJ.error("Use the menu option to select a point on the image.")
                }           
            }
        }
        imp.close()
	}
}
