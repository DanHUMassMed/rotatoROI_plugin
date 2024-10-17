package com.amywalkerlab.rotato_roi.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi
import ij.plugin.frame.RoiManager

import com.amywalkerlab.rotato_roi.process.FinetuneROI
import com.amywalkerlab.rotato_roi.RotatoROIException
import com.amywalkerlab.rotato_roi.process.WaitForUserDialog

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
	protected boolean processFile(File file, int item_num, int num_items) {
        def fullDirectoryPath = file.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name

        //String fileNameTrimmed = file.getName().replaceFirst(in_prefix, "")
        String fileNameTrimmed = file.getName()

	    def String full_name = file.toString()
	    def ImagePlus imp = IJ.openImage(full_name)
        if (imp == null) {
            IJ.error("Error: Excpected an Image for Cropping got an Nothing.")
            return
        }

		imp.show()
		IJ.getImage().setRoi(null)
        IJ.setTool("point")

        // Prompt user select a point
        def terminateProcess = false
        def done = false
        WaitForUserDialog ud = new WaitForUserDialog("Crop Image", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease select a location point to start crop.\nAnd Clicked OK to continue.")
        ud.setVisible(true)
        while(!done) {            
            if(ud.getButtonClicked()=="OK") {
                def pointRoi = IJ.getImage().getRoi()
                if(pointRoi != null && pointRoi.getType() == Roi.POINT){
                    done=true
                    IJ.run("Select None");
                    imp.setRoi(pointRoi.x, pointRoi.y, Integer.parseInt(cropWidth), Integer.parseInt(cropHeight));
                    FinetuneROI FinetuneROI = new FinetuneROI()
                    FinetuneROI.run()
                    if(FinetuneROI.wasCanceled()){
                        done=false
                        imp.setRoi(pointRoi.x, pointRoi.y, 0 ,0)
                    }else{
                        def rectangleRoi = IJ.getImage().getRoi()
                        def roiTitle=out_prefix + fileNameTrimmed
                        def args="title='" + roiTitle +"'"
                        IJ.run("Duplicate...", args);
                        ImagePlus roiImage = IJ.getImage();
                        // Save the ROI as a TIFF file
                        def croppedBounds = "x="+pointRoi.x+", y="+pointRoi.y+", w="+cropWidth+", h="+cropHeight
                        editMetadata(roiImage, "Cropped Bounds from Original", croppedBounds )
                        IJ.saveAs(roiImage,"Tiff", outputDir + File.separator + lastDirectory + File.separator + roiTitle)
                        roiImage.close()
                    }
                }else{
                    ud = new WaitForUserDialog("Crop Image", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease select a location point to start crop.\nAnd Clicked OK to continue or Cancel to End Processing.")
                    ud.setVisible(true)
                }
            }else if (ud.getButtonClicked()=="Cancel"){
                IJ.error("Processing has been terminated.")
                done=true
                terminateProcess = true
            }
            IJ.wait(50)              
        }
        
        imp.close()
        return terminateProcess
	}
}
