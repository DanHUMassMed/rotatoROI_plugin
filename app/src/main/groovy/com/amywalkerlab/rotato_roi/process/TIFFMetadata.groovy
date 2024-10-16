package com.amywalkerlab.rotato_roi.process

import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.tiff.TiffImageParser
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata
import org.apache.commons.imaging.formats.tiff.TiffField
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants

import java.io.File

class TIFFMetadata {
    private String filePath
    private TiffImageMetadata tiffMetadata

    // Constructor takes a file path for the TIFF image
    TIFFMetadata(String filePath) {
        this.filePath = filePath
        loadMetadata()
    }

    // Load metadata from the file
    private void loadMetadata() {
        def file = new File(filePath)
        def metadata = Imaging.getMetadata(file)

        if (metadata instanceof TiffImageMetadata) {
            this.tiffMetadata = (TiffImageMetadata) metadata
        } else {
            this.tiffMetadata = null
        }
    }

    // Method to get metadata as a list of name-value pairs
    List<Map<String, Object>> getMetadata() {
        if (tiffMetadata != null) {
            return tiffMetadata.getAllFields().collect { TiffField field ->
                [name: field.getTagName(), value: field.getValue()]
            }
        } else {
            return [] // Return an empty list if no metadata is found
        }
    }

    // Method to set a metadata field (name-value pair)
    void setMetadata(String name, Object value) {
        if (tiffMetadata != null) {
            def newField = new TiffField(
                TiffTagConstants.TIFF_TAG_USER_COMMENT, 
                TiffField.TYPE_ASCII, 
                value.toString().bytes
            )
            tiffMetadata.addField(newField)
        }
    }

    // Method to write metadata to a new file or overwrite the original file
    void writeMetadata(String outputFilePath = null) {
        if (tiffMetadata != null) {
            def outputFile = new File(outputFilePath ?: filePath)
            Imaging.writeImage(
                Imaging.getBufferedImage(new File(filePath)), 
                outputFile, 
                org.apache.commons.imaging.ImageFormats.TIFF, 
                tiffMetadata
            )
            println("Saved updated image with metadata to: ${outputFile.getPath()}")
        } else {
            println("No metadata available to write.")
        }
    }
}

