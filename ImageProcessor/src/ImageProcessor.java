// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP112 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP112 - 2018T1, Assignment 8_and_9
 * Name: Matthew Corfiatis
 * Username: CorfiaMatt
 * ID: 300447277
 */

import ecs100.*;
import java.io.*;
import java.awt.Color;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;


/** ImageProcessor allows the user to load, display, modify, and save an
 *       image in a number of ways.
 *  The program includes
 *  - Load, commit
 *
 * CORE:
 *  - Brightness adjustment
 *  - Horizontal and vertical flips and 90 degree rotations (clockwise
 *       and anticlockwise)
 *  - Merge 
 *  - Save
 *
 * COMPLETION
 *  - Crop&Zoom
 *  - Blur (3x3 filter)
 *  - Rotate arbitrary angle
 *
 * CHALLENGE
 *  - General Convolution Filter
 *  - Pour (spread-fill)
 *  - Red-eye
 */
public class ImageProcessor {
    private Color[][] image =   null; // current version of the image
    private Color[][] result = null;  // result of applying operation to image
    private Color[][] toMerge = null;

    // current selected region (rows, and columns of the image)
    private int regionLeft=-1;
    private int regionTop=-1;
    private int regionWidth;
    private int regionHeight;

    private boolean awaitingPour = false; //Indicates whether the next click should be treated as a pour
    private Color pourFill; //Colour to use for the pour
    private double pourTolerance = 15; //Color distance tolerance for the pour
    private Stack<int[]> pourJobs = new Stack<>(); //Recursive to-do list

    private String mouseAction = "select";   // what should the mouse do?

    private static final double LEFT = 10;  // position of image .
    private static final double TOP = 10;
    private static final double MARGIN = 5; // space between result and image

    /**
     * Multiply a color by a value
     * @param color Color to multiple
     * @param multiply value to multiply by each color component
     * @return Multiplied color
     */
    private Color xCol(Color color, float multiply)
    {
        float red = color.getRed() * multiply; //get red component
        float green = color.getGreen() * multiply; //get green component
        float blue = color.getBlue() * multiply; //get blue component
        return new Color( //Build new color
                (red > 255) ? 255 : (int)red, //Get red or if red is more than 255 get 255
                (green > 255) ? 255 : (int)green, //Get green or if green is more than 255 get 255
                (blue > 255) ? 255 : (int)blue); //Get blue or if blue is more than 255 get 255
    }

    /**
     * Add a value to each color component
     * @param color Color to add value to
     * @param addition Value to add
     * @return Color with added value
     */
    private Color aCol(Color color, float addition)
    {
        float red = color.getRed() + addition; //Add red to the value
        float green = color.getGreen() + addition; //Add green
        float blue = color.getBlue() + addition; //Add blue
        return new Color(
                (red > 255) ? 255 : (red >= 0) ? (int)red : 0, //Return red or the bounds if the value is outside of the color bounds 0 - 255
                (green > 255) ? 255 : (green >= 0) ? (int)green : 0,
                (blue > 255) ? 255 : (blue >= 0) ? (int)blue : 0);
    }

    /**
     * Combine colors
     * Combines the values of two colors with simple addition
     * @param c1 Color 1
     * @param c2 Color 2
     * @return The combined color
     */
    private Color cColor(Color c1, Color c2)
    {
        return new Color(
                c1.getRed() + c2.getRed(),
                c1.getGreen() + c2.getGreen(),
                c1.getBlue() + c2.getBlue());
    }

    /**
     * Interface to use for the iteration method
     * Contains method that is invoked for each pixel
     */
    public interface XYListener {
        void i(int x, int y);
    }

    /**
     * Used as a shorthand to iterate over the image pixels
     * @param operator
     */
    private void Iterate(XYListener operator)
    {
        int width = image[0].length;
        for(int y = 0; y < image.length; ++y)
        for(int x = 0; x < width; ++x)
            operator.i(x, y); //Invoke the pixel handler void passed from the method invokation
    }

    /**
     * Same as the Iterate method but on the result image instead of the real image
     * @param operator
     */
    private void IterateResult(XYListener operator)
    {
        int width = result[0].length;
        for(int y = 0; y < result.length; ++y)
            for(int x = 0; x < width; ++x)
                operator.i(x, y);
    }

    // Constructor
    public ImageProcessor(){
        this.setupGUI();
    }

    //-------------------------------------------------------
    // Methods for the image operations
    //-------------------------------------------------------

    /**
     * CORE
     * 
     * Make the image brighter or darker.
     *  The value is between -1.0 and 1.0
     *  Sets the fraction to move the color towards the min max
     */
    public void brightness(float value){
        this.checkResult();
        Iterate((x, y) -> result[y][x] = aCol(image[y][x], value * 255)); //Iterate over all pixels in image, set result to that pixel + brightness value
    }

    /**
     * CORE
     * 
     * Flip the image horizontally (so around the vertical axis)
     */
    public void horizontalFlip(){
        this.checkResult();
        int width = result[0].length;
        Iterate((x, y) -> result[y][width - x - 1] = image[y][x]); //Set result pixels to width - horizontal co-ordinate to flip the image
    }

    /**
     * CORE
     * 
     * Flip the image vertically (so around the horizontal axis)
     */
    public void verticalFlip(){
        this.checkResult();
        int height = result.length;
        Iterate((x, y) -> result[height - y - 1][x] = image[y][x]); //Set result pixels to height - vertical co-ordinate to flip image
    }

    /**
     * CORE
     * 
     * Rotate the image 90 degrees clockwise
     */
    public void rotate90clockwise(){
        int height = image.length;
        int width = image[0].length;
        result = new Color[width][height]; //Set width to height and height to width, so the aspect ratio is rotated
        Iterate((x, y) -> result[x][height - y - 1] = image[y][x]); //Iterate over the image pixels normally then set the 90 degree equivalent in the result image
    }

    /**
     * CORE
     * 
     * Rotate the image 90 degrees anticlockwise
     */
    public void rotate90anticlockwise(){
        int height = image.length;
        int width = image[0].length;
        result = new Color[width][height]; //Set width to height and height to width, so the aspect ratio is rotated
        Iterate((x, y) -> result[width - x - 1][y] = image[y][x]); //Iterate over the image pixels normally then set the 90 degree equivalent in the result image
    }

    /** 
     * CORE
     *
     * Merges the current image and the toMerge image, if there is one.
     * Work out the rows and columns shared by the images
     * For each pixel value in the shared region, replace the current pixel value
     * by the average of the pixel value in current image and the corresponding
     * pixel value in the other image.
     */
    public void merge(float factor){
        UI.println("merging: "+factor);
        if (toMerge==null){
            UI.println("no image to merge with");
            return;
        }
        this.checkResult();
        Iterate((x, y) -> {
            Color c1 = xCol(getMergePixel(x, y), factor); //Multiply pixel by the factor
            Color c2 = xCol(image[y][x], 1 - factor); //Multiply other pixel by 1 - factor to get the opposite of the merge pixel
            result[y][x] = cColor(c1, c2); //set result after combining the colours
        });
    }

    /**
     * Safely get pixel from merge image. Returns white if pixel doesn't exist
     * @param x X co-ordinate of pixel
     * @param y Y co-ordinate of pixel
     * @return Color of requested pixel
     */
    public Color getMergePixel(int x, int y)
    {
        if(toMerge.length > y && toMerge[0].length > x) //If the pixel co-ordinates are within the bounds of the image
            return toMerge[y][x]; //Return the pixel
        else //If co-ordinates are outside of bounds of image
            return new Color(255, 255, 255); //Return white
    }

    /**
     * CORE
     *
     * Write the current image to a file
     */
    public  void saveImage() {
        String fname = UIFileChooser.save(); //get file name
        Trace.println("Saving " + fname);
        if (fname==null){ return; }
        try {
            BufferedImage img = new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_INT_RGB); //Create BufferedImage objet
            Iterate((x,y) -> img.setRGB(x, y, image[y][x].getRGB())); //set the pixels in the bufferd image to that of the image array
            ImageIO.write(img, "png", new File(fname)); //Write the buffered image to disk
            UI.printMessage("Saved "+ fname);
        } catch(IOException e){UI.println("Image save failed: "+e);}
    }

    /**
     * Loads convolution filter from a filter file
     * @return scaled 2d filter array
     */
    public float[][] loadConvolution()
    {
        String fname = UIFileChooser.open(); //gets file to open
        if (fname==null){ return null; } //If no file selectd
        try {
            File file = new File(fname); //Open file
            Scanner sc = new Scanner(file); //Open scanner from file
            int kernelSize = sc.nextInt(); //Get size of the filter
            float[][] kernel = new float[kernelSize][kernelSize]; //Create filter array
            for(int i = 0; i < kernelSize; ++i) //Iterate vertically over the filter values
                for(int j = 0; j < kernelSize; ++j) //Iterate horizontally over the filter values
                    kernel[i][j] = sc.nextFloat(); //Set the filter value in the filter array
            if(sc.hasNextFloat()) //Check for a divisor
            {
                float divisor = sc.nextFloat(); //Gets the filter divisor
                for(int i = 0; i < kernelSize; ++i)
                    for(int j = 0; j < kernelSize; ++j)
                        kernel[i][j] /= divisor; //Divides each value in the filter array by the divisor
            }
            return kernel;
        } catch(IOException e){
            UI.println("Convolution fail loaded: " + e);
            return null;
        }
    }

    /**
     * COMPLETION
     *
     * Scales the currently selected region of the image (if there is one) to fill
     * the result image.
     * This is a combination scale, translate, and crop.
     * Return true if a region was selected, false otherwise
     */
    public boolean cropAndZoom(){
        checkResult();
        if(regionTop == -1 || regionLeft == -1) return false; //If no region selected

        float scaleX = (float)regionWidth / result[0].length; //Calculate required horizontal scale for the image
        float scaleY = (float)regionHeight / result.length; //Calculate required vertical scale for the image

        IterateResult((x, y) -> { //Iterate over the result image array, applying transformation backwards from the result image
                int tempX = (int)Math.floor(x * scaleX); //get the old x value for the original image
                int tempY = (int)Math.floor(y * scaleY); //Get the old y value for the original image
                result[y][x] = image[tempY + regionTop][tempX + regionLeft]; //Set the result pixel to the transformed original pixel
        });

        return true;
    }

    /**
     * gets the distance between two colors using pythagoras
     * @param a Color 1
     * @param b Color 2
     * @return the decimal distance between them in the RGB color space
     */
    public double colorDistance(Color a, Color b)
    {
        int red = Math.abs(a.getRed() - b.getRed()); //get the difference between reds
        int green = Math.abs(a.getGreen() - b.getGreen()); //get the difference between greens
        int blue = Math.abs(a.getBlue() - b.getBlue()); //Difference of blues

        double sum = Math.pow(red, 2) + Math.pow(green, 2) + Math.pow(blue, 2); //Square and sum each object
        return Math.sqrt(sum); //Square root sum to get distance
    }

    /** 
     * COMPLETION
     *
     * CONVOLVE  Matrix   
     *   Modify each pixel to make it a weighted average of itself and the pixels around it
     *   A simple blur will weight the pixel by 0.4, its horizontal and vertical neighbours by 0.1, 
     *   and the diagonal neighbours by 0.05.
     * Hint: It is easier to make a new image array of the same size as the image,
     *       then work out the weighted averages in the new array and then assign the new array to the image field.
     */

    public void convolve(float[][] weights){   
        checkResult();
        final int kernelWidth = weights.length; //gets the width of the filter
        final int kernelSpread = (kernelWidth - 1) / 2; //Gets the size of the kernel from the centre point. Used to centre the kernel over a specific pixel regardless of size
        Iterate((x, y) -> { //Iterate over pixels in image
            if(x >= kernelSpread && y >= kernelSpread && x < image[0].length - kernelSpread && y < image.length - kernelSpread) //Keep convolution within bounds
            {
                float[] colors = new float[3]; //Stores filtered color component values
                for(int i = -kernelSpread; i <= kernelSpread; ++i) //iterate over kernel filter
                for(int j = -kernelSpread; j <= kernelSpread; j++)
                {
                    //Get each pixel color component, multiply it by the weight, then add it to the color array
                    colors[0] += (float)image[y + j][x + i].getRed() * weights[j + kernelSpread][i + kernelSpread]; //Convolute red
                    colors[1] += (float)image[y + j][x + i].getGreen() * weights[j + kernelSpread][i + kernelSpread]; //Convolute green
                    colors[2] += (float)image[y + j][x + i].getBlue() * weights[j + kernelSpread][i + kernelSpread]; //Convolute blue
                }

                //Ensures color components are within bounds of 0-255
                colors[0] = Math.min(Math.max(colors[0], 0), 255);
                colors[1] = Math.min(Math.max(colors[1], 0), 255);
                colors[2] = Math.min(Math.max(colors[2], 0), 255);
                result[y][x] = new Color((int)colors[0], (int)colors[1], (int)colors[2]); //Create resulting fo;tered color
            }
        });
    }

    /**
     * COMPLETION
     *
     * Rotate the image by the specified angle.
     * Rotates around the center of the image, or around the center
     * of the selected region if there is a selected region.
     */
    public void rotate(double angle){
        this.checkResult();
        Transform transform; //Transformation matrix to apply to the pixels
        Vector2 offset; //Offset for the rotation
        if(regionLeft == -1 || regionTop == -1) //If the user has made a selection
        {
            offset = new Vector2(image[0].length / 2, image.length / 2); //Set rotation offset to centre of image
            UI.println("Rotating around image centre");
        }
        else {
            offset = new Vector2(regionLeft + (regionWidth / 2), regionTop + (regionHeight / 2)); //Set rotation offset to centre of selection
            UI.println("Rotating around selection centre");
        }

        transform = Transform.multipy(new Transform(1, 0, 0, 1, offset), //Apply rotation offset to matrix
                Transform.rotationMatrix(angle)); //Apply rotation to matrix

        IterateResult((x, y) -> { //Iterate over pixels in result image
            Vector2 pixel = Vector2.subtract(new Vector2(x, y), offset); //Shift the whole image so the offset is in the correct location
            Vector2 newPixel = transform.multiplyVector(pixel); //Apply the rotation matrix to the pixel to get the location of other pixel

            if(image.length > newPixel.Y && image[0].length > newPixel.X && 0 <= newPixel.Y && 0 <= newPixel.X) //Ensure pixel is within bounds of image
                result[y][x] = image[(int)newPixel.Y][(int)newPixel.X]; //Set pixel of the result image to the transformed pixel in image
            else
                result[y][x] = new Color(255, 255, 255); //Set resulting pixel to white
        });
    }

    /**
     * Recursive method to fill an area of a similar colour with another colour
     * @param sourceColor Colour to replace
     * @param destColor Color to overwrite original with
     * @param x X co-ordinate of where to start the fill operation
     * @param y Y co-ordinate of where to start the fill operation
     * @param tolerance tolerance of source colour
     * @param existingImage whether the pour operation should be performed on a new copy or an existing image copy
     * @return if the method succeeded
     */
    private boolean Pour(Color sourceColor, Color destColor, int x, int y, double tolerance, boolean existingImage)
    {
        checkResult();
        if(colorDistance(destColor, sourceColor) < tolerance) //Make sure the destination colour doesn't overlap the source color, this would cause an infinite loop
        {
            UI.println("Pour fill colour is too similar!");
            UI.println("Try making the tolerance smaller.");
            UI.println("Pour canceled.");
            return false;
        }
        UI.println("Pouring...");
        pourJobs.clear(); //Remove all pour jobs
        pourJobs.push(new int[] { x, y }); //Add first job as the starting point of the pour operation

        if(!existingImage) { //If the pour should be performed on the existing result image
            result = copyImage(image); //Create new image result
            this.redisplay();
        }
        // 0 X 0
        // X 0 X
        // 0 X 0
        int jobSearchLength = 4; //Length of list of pixels around the pour to check. Pattern above
        int[] jobSearchX = new int[] { 0, -1, 0, 1}; //relative x co-ordinates of pixels to check
        int[] jobSearchY = new int[] { -1, 0, 1, 0 }; //relative y co-ordinates of pixels to check

        while (!pourJobs.empty()) //While there are jobs to do
        {
            int[] job = pourJobs.pop(); //grab a job of the stack
            result[job[1]][job[0]] = destColor; //set the pixel colour
            for(int i = 0; i < jobSearchLength; ++i) { //Search pixels around the current pixel
                int searchX = job[0] + jobSearchX[i]; //absolute x co-ordinate of pixel to check
                int searchY = job[1] + jobSearchY[i]; //absolute y co-ordinate of pixel to check
                if (searchX >= 0 && searchY >= 0 && //ensure in bounds
                        searchX < result[0].length && searchY < result.length && //ensure in bounds
                        colorDistance(result[searchY][searchX], sourceColor) < tolerance) { //Check that the colour distance is within the tolerance of the destination and source colours
                    pourJobs.push(new int[]{searchX, searchY}); //add a job for the new pixel
                }
            }
        }
        UI.println("Pour finished!");
        return  true;
    }

    /**
     * Pythagoras helper function to calculate difference between two 2d points
     * @param p1 Point 1
     * @param p2 Point 2
     * @return distance between points
     */
    private double PointDist(int[] p1, int[] p2)
    {
        int x = Math.abs(p1[0] - p2[0]); //get difference of x co-ordinates
        int y = Math.abs(p1[1] - p2[1]); //Get difference of y co-ordinates
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); //Pythagoras
    }

    /**
     * Gets the middle of two points on a 2d cartesian plane
     * @param p1
     * @param p2
     * @return
     */
    private int[] MidPoint(int[] p1, int[] p2)
    {
        return new int[] { //Array to store X and Y
                (p1[0] + p2[0]) / 2, //Add x values then divide by 2
                (p1[1] + p2[1]) / 2 //Add y values then divide by 2
        };
    }

    private void RedEye()
    {
        result = copyImage(image);
        double maxPupilPointSpace = 50; //Maximum space between pupil points before the point is counted as a new eye
        double maxIrisSize = 30; //Maximum size to search for darker iris pixels
        double pupilTolerance = 130; //Tolerance of pupil red colour for the pour and detection
        double irisTolerance = 100; //Tolerance of the dark iris colour for detection
        Color irisColor = new Color(10, 0, 50); //Rough color of irises, obviously there are different eye colours but dont have time for that
        TreeMap<Integer, int[]> pupilPoints = new TreeMap<>(); //Sorted map of pupil points.
        int firstX = Integer.MAX_VALUE; //First x co-ordinate of a red value found
        int firstY = Integer.MAX_VALUE; //First y co-ordinate of a red value found
        int consecutiveMismatches = 0; //Number of pixels that dont match the redeye value, used for detecting iris
        for(int y = 0; y < image.length; ++y) //iterate over image
        for(int x = 0; x < image[0].length; ++x)
        {
            if(colorDistance(image[y][x], Color.RED) < pupilTolerance) //If pixel is red
            {
                //Save detected values
                if(x < firstX) firstX = x;
                if(y < firstY) firstY = y;
            }
            else if(colorDistance(image[y][x], irisColor) < irisTolerance) //if pixel matches iris colour roughly
            {
                ++consecutiveMismatches; //Increment iris pixel counter
                if(consecutiveMismatches > maxIrisSize) { //If the area has lots of darker pixels around it
                    if(firstY != Integer.MAX_VALUE && firstX != Integer.MAX_VALUE) //If any pupil pixels were found
                    {
                        pupilPoints.put(firstY, new int[]{firstX, firstY}); //add the pupil point to the sorted list, sorted by the Y co-ordinate so later on the points can be separated  into different eyes
                        firstX = Integer.MAX_VALUE; //reset detected pixels
                        firstY = Integer.MAX_VALUE;
                    }
                    consecutiveMismatches = 0;
                }
            }
        }

        //======================================================================================
        //      This section is to collate pixels that are close together into separate eyes
        //      Relies on sorting the pixels by Y and X then looping and finding similar values
        //======================================================================================

        ArrayList<ArrayList<int[]>> separatedPoints = new ArrayList<>(); //Pupil points separated into individual pupils
        TreeMap<Integer, int[]> closeYPoints = new TreeMap<>(); //Points that have similar y co-ordinates

        int lastY = pupilPoints.firstKey(); //get first point
        pupilPoints.put(Integer.MAX_VALUE, new int[2]); //Add to end of list last Y integers get counted
        for(Map.Entry<Integer, int[]> yPupil : pupilPoints.entrySet()) //Loop over sorted pixels
        {
            if(yPupil.getKey() - lastY < maxPupilPointSpace) { //If this point and the last point are close
                closeYPoints.put(yPupil.getValue()[0], yPupil.getValue()); //add the point to the list of close points
            }
            else { //If the points are separate, suggests that they are different eyes/pupils
                int lastX = closeYPoints.firstKey();
                ArrayList<int[]> closeXPoints = new ArrayList<>(); //Points that have similar X and Y co-ordinates
                closeYPoints.put(Integer.MAX_VALUE, new int[2]); //Add to end of list last X integers get counted
                for(Map.Entry<Integer, int[]> xPupil : closeYPoints.entrySet()) { //Loop over points close in the Y axis
                    if(xPupil.getKey() - lastX < maxPupilPointSpace) { //If this point is close to the last
                        closeXPoints.add(xPupil.getValue());
                    }
                    else { //Not close points
                        separatedPoints.add((ArrayList<int[]>) closeXPoints.clone()); //Finish the current pupil, begin finding the next pupil
                        closeXPoints.clear();
                    }
                    lastX = xPupil.getKey();
                }
                closeYPoints.clear();
            }
            lastY = yPupil.getKey();
        }

        //======================================================================================
        //      The algorithm so far finds the left semi-circle of each pupil, the following
        //      part finds the points in each semi-circle that are the furthest apart, ie: the
        //      top and bottom of each pupil, then the middle is calculated using a midpoint
        //      algorithm.
        //======================================================================================

        for(ArrayList<int[]> pupil : separatedPoints) //List of pupils
        {
            if(pupil.size() == 0) continue;
            double maxDist = 0;
            int[] p1 = new int[2];
            int[] p2 = new int[2];;
            for(int[] point : pupil) //Find two points with the furthest distance between them in the current pupil
            {
                for(int[] point2 : pupil) {
                    double tempDist = PointDist(point, point2); //Calculate distance between both points
                    if(tempDist > maxDist) //If points have the biggest distance, overwrite the old ones
                    {
                        p1 = point;
                        p2 = point2;
                        maxDist = tempDist;
                    }
                }

                //Add visual markers for eye detection
                UI.setColor(Color.blue);
                UI.fillOval(point[0] + LEFT, point[1] + TOP, 4, 4);
            }

            /*if (Pour(Color.white, Color.black, p1[0], p1[1], pourTolerance)) {
                this.displayResult = true;
                this.arrow = "right";
                this.action = "Red Eye";
                this.redisplay();
            }*/

            int[] midpoint = MidPoint(p1, p2); //get the middle point between the two furthest pupil points (Attempt to find centre of pupil)
            Pour(image[midpoint[1]][midpoint[0]], Color.black, midpoint[0], midpoint[1], 30, true); //Fill the white spot in the middle of the red eye
            if (Pour(image[p1[1]][p1[0]], Color.black, p1[0], p1[1], 80, true)) { //fill the outer red with black
                this.displayResult = true;
                this.arrow = "right";
                this.action = "Red Eye";
                this.redisplay();
            }

            //Add visual iris detection markers
            UI.setColor(Color.cyan);
            UI.fillOval(midpoint[0] + LEFT, midpoint[1] + TOP, 4, 4);
            UI.setColor(Color.yellow);
            UI.fillOval(p1[0] + LEFT, p1[1] + TOP, 4, 4);
            UI.setColor(Color.green);
            UI.fillOval(p2[0] + LEFT, p2[1] + TOP, 4, 4);
        }
    }

    //---------------------------
    //  GUI setup and GUI methods  
    //---------------------------

    /** set up the GUI
     */
    public void setupGUI(){
        UI.setMouseListener(this::doMouse);
        UI.addButton("Load", this::load);
        UI.addButton("Save", this::buttonSave);
        UI.addButton("Commit", this::buttonCommit);
        UI.addSlider("Brightness", -100, 100, 0, this::sliderBrightness);
        UI.addButton("Horizontal Flip", this::buttonHorizontalFlip);
        UI.addButton("Vertical Flip", this::buttonVerticalFlip);
        UI.addButton("Rotate 90 Clockwise", this::buttonRotate90clockwise);
        UI.addButton("Rotate 90 Anticlockwise", this::buttonRotate90anticlockwise);
        UI.addButton("Load Merge", this::buttonLoadMerge);
        UI.addSlider("Merge level", 0, 100, 50, this::sliderMerge);

        UI.addButton("Crop&Zoom", this::buttonCropZoom);
        UI.addButton("Blur", this::buttonBlur);
        UI.addSlider("Rotate", -180, 180, 0, this::sliderRotate);

        UI.addButton("Convolute", this::buttonConvolute);
        UI.addButton("Pour", this::buttonPour);
        UI.addSlider("Pour tolerance", 0, 100, 4, (double s) -> { pourTolerance = 442d * s * 0.01d; });
        UI.addButton("Red-eye", this::buttonRedEye);

        UI.addButton("Quit", UI::quit);
        UI.setWindowSize(1400,600);
        UI.setDivider(0.15);
    }

    public void buttonRedEye()
    {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        RedEye();
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Red eye";
        //this.redisplay();
    }

    public void buttonPour()
    {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        pourFill = null;
        pourFill = JColorChooser.showDialog(null, "Choose a colour", Color.black);
        if(pourFill == null)
        {
            UI.printMessage("No colour");
            return;
        }
        UI.println("Click part of the image to pour");
        awaitingPour = true;
    }

    public void buttonConvolute()
    {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        float[][] kernel = loadConvolution();
        if(kernel == null)
        {
            UI.printMessage("No kernel filter");
            return;
        }
        convolve(kernel);
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Kernel filter";
        this.redisplay();
    }

    /** Respond to button presses */
    public void buttonSave(){
        if (this.image == null) {
            UI.printMessage("Nothing to save");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.saveImage();
        this.redisplay();
    }

    public void buttonCommit(){
        if (this.result == null) {
            UI.printMessage("Nothing to commit");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.image = copyImage(this.result); 
        this.displayResult = false;
        this.arrow = "left";
        this.action = "Commit";
        this.redisplay();
    }

    /** Respond to sliders changes */
    public void sliderBrightness(double num) {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.brightness((float)num/100);
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Brightness " + (num/100);
        this.redisplay();
    }

    public void buttonHorizontalFlip(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.horizontalFlip(); 
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Horizontal Flip";
        this.redisplay();
    }

    public void buttonVerticalFlip(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.verticalFlip(); 
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Vertical Flip";
        this.redisplay();
    }

    public void buttonRotate90clockwise(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.rotate90clockwise();
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Rotate 90 Clockwise";
        this.redisplay();
    }

    public void buttonRotate90anticlockwise(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.rotate90anticlockwise();
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Rotate 90 Anticlockwise";
        this.redisplay();
    }

    public void buttonLoadMerge(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.toMerge = this.loadImage(UIFileChooser.open("Image to merge"));
        this.merge(0.5f);
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Load Merge";
        this.redisplay();
    }

    public void sliderMerge(double num) {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.merge((float)num/100);
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Merge level " + (int)(num) + "%";
        this.redisplay();
    }

    public void buttonCropZoom(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.displayResult =  this.cropAndZoom(); 
        this.arrow = "right";
        this.action = "Crop&Zoom";
        this.redisplay();
    }

    public void buttonBlur(){
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.mouseAction="select";  // reset the current mouse action
        this.convolve(new float[][]{
                {1f/9f, 1f/9f, 1f/9f},
                {1f/9f, 1f/9f, 1f/9f},
                {1f/9f, 1f/9f, 1f/9f}
        });
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Blur";
        this.redisplay();
    }

    public void sliderRotate(double num) {
        if (this.image == null) {
            UI.printMessage("No image");
            return;
        }
        this.rotate(num);
        this.displayResult = true;
        this.arrow = "right";
        this.action = "Rotate "+num;
        this.redisplay();
    }


    /**
     * Respond to mouse events "pressed", "released"".
     * If mouseAction field is "select", then pressed and released set the region
     * (can be on either of the result or the image).
     * If mouseAction field is "pour", then released will pour the current paint
     *  at the point.
     */
    public void doMouse(String action, double x, double y) {
        int[] rowCol = getRowColAtMouse(x, y);
        int row = -1;
        int col = -1;
        if (rowCol!=null){
            row = rowCol[0];
            col = rowCol[1];
        }

        if (action.equals("pressed")){
            if (mouseAction=="select"){
                this.regionTop = row;
                this.regionLeft = col;
            }
        }
        else if (action.equals("released")){
            if (mouseAction=="select"){
                this.regionHeight = Math.abs(row-this.regionTop);
                this.regionWidth = Math.abs(col-this.regionLeft);
                this.regionTop = Math.min(row, this.regionTop);
                this.regionLeft = Math.min(col, this.regionLeft);
                this.arrow = "select";
                this.redisplay();
            }
        }
        else if(action.equals("clicked")) {
            if(awaitingPour)
            {
                awaitingPour = false;
                try {
                    if (Pour(image[row][col], pourFill, col, row, pourTolerance, false)) {
                        this.displayResult = true;
                        this.arrow = "right";
                        this.action = "Pour";
                        this.redisplay();
                    }
                }
                catch (Exception ex)
                {
                    UI.println("Error pouring: " + ex);
                }
            }
        }
    }

    //-------------------------------------------------------
    // UTILITY METHODS: load, save, copy clear, check,  computing rows/cols
    //-------------------------------------------------------

    /**
     * Returns the number of rows in an image
     */
    public int rows(Color[][] array){return array.length;}

    /**
     * Returns the number of columns in an image
     */
    public int cols(Color[][] array){return array[0].length;}

    /**
     * Returns the row and column of the image that the point (x, y) is on.
     */
    private int[] getRowColAtMouse(double x, double y){
        if (this.image==null){UI.println("no image"); return null;}
        if (y<TOP || y >= TOP+this.rows(this.image)){return null;}
        if (x<LEFT || x>= LEFT+this.cols(this.image)){return null;}

        int row = (int)(y-TOP);
        int col = (int)(x-LEFT);
        return new int[]{row, col};
    }

    /**
     * Loads an image from a file into both the current image and the result image
     */
    public void load(){
        String fname = UIFileChooser.open();
        this.image = loadImage(fname);
        this.result = copyImage(this.image);
        this.displayResult = false;
        this.arrow = "select";
        this.redisplay();
    }

    /**
     * Load image from a file and return as a two-dimensional array of Color.
     */
    public Color[][] loadImage(String imageName) {
        Trace.println("loading " + imageName);
        if (imageName==null || !new File(imageName).exists()){ return null; }
        try {
            BufferedImage img = ImageIO.read(new File(imageName));
            int rows = img.getHeight();
            int cols = img.getWidth();
            Color[][] ans = new Color[rows][cols];
            for (int row = 0; row < rows; row++){
                for (int col = 0; col < cols; col++){                 
                    Color c = new Color(img.getRGB(col, row));
                    ans[row][col] = c;
                }
            }
            UI.printMessage("Loaded "+ imageName);
            return ans;
        } catch(IOException e){UI.println("Image reading failed: "+e);}
        return null;
    }

    /**
     * Ensures that the result image is the same size as the current image.
     * Makes a new result image array if not.
     */
    public void checkResult(){
        int rows = this.rows(this.image);
        int cols = this.cols(this.image);
        if (this.rows(this.result) != rows | this.cols(this.result) != cols){
            this.result = new Color[rows][cols];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    this.result[row][col]= Color.BLACK;
                }
            }
        }
    }

    /**
     * Set the result image to all {0,0,0} (needed for convolution}
     */
    public void clearResult(){
        for(int row =0; row<result.length;row++){
            for(int col=0;col< result[0].length;col++){
                result[row][col] = Color.BLACK;

            }
        }
    }

    /**
     * Make a deep copy of an image array
     */
    public Color[][] copyImage(Color[][] from){
        int rows = from.length;
        int cols = from[0].length;
        Color[][] to = new Color[rows][cols];
        for (int row = 0 ; row < rows ; row++){
            for (int col = 0; col<cols; col++) {
                to[row][col] = new Color(from[row][col].getRGB());
            }
        }
        return to;
    }

    //=========================================================================
    boolean displayResult = false;
    String arrow = "select";
    String action;
    double arrowSize = 100;

    /** ReDisplay the images (image and result) each pixel as a square of size 1
     *  The original image is displayed on the left
     *  The result image (that results from any transformation) is displayed on the right
     *  Called after each button pressed.
     */
    public void redisplay(){
        if (this.image ==null) {
            UI.println("no image to display");
            return;
        }
        double imageRight = LEFT + this.cols(this.image);
        UI.clearGraphics();
        displayImage(this.image, LEFT);
        if (this.arrow.equals("right")) {
            UI.sleep(100);
            displayRightArrow(this.action, imageRight + MARGIN, TOP+this.regionHeight/2);
        }
        else if (this.arrow.equals("left")) {
            UI.sleep(100);
            displayLeftArrow(this.action, imageRight + MARGIN, TOP+this.regionHeight/2);
        }

        if (this.displayResult) {
            UI.sleep(100);
            displayImage(this.result, imageRight + MARGIN*2 + this.arrowSize);
        }

        if (this.regionLeft>-1){
            UI.setColor(Color.red);
            UI.drawRect(LEFT+this.regionLeft, TOP+this.regionTop,
                this.regionWidth, this.regionHeight);
        }
        UI.repaintGraphics();
    }

    public void displayImage(Color[][] img, double left){
        double y = TOP;
        for(int row=0; row<img.length; row++){
            double x = left;
            for(int col=0; col<img[row].length; col++){
                UI.setColor(img[row][col]);
                UI.fillRect(x, y, 1, 1);
                x++;
            }
            y++;
        }
    }

    public void displayRightArrow(String text, double left, double top) {
        UI.setColor(Color.green);
        UI.fillRect(left, top+this.arrowSize/3, this.arrowSize/2, this.arrowSize/3);
        double [] xPoints = {left+this.arrowSize/2, left+this.arrowSize/2, left+this.arrowSize};
        double [] yPoints = {top,top+this.arrowSize,top+this.arrowSize/2};
        UI.fillPolygon(xPoints,yPoints,3);
        UI.setColor(Color.black);
        UI.drawString(text, left+2, top+this.arrowSize/2);
    }

    public void displayLeftArrow(String text, double left, double top) {
        UI.setColor(Color.green);
        UI.fillRect(left+this.arrowSize/2, top+this.arrowSize/3, this.arrowSize/2, this.arrowSize/3);
        double [] xPoints = {left+this.arrowSize/2, left+this.arrowSize/2, left};
        double [] yPoints = {top,top+this.arrowSize,top+this.arrowSize/2};
        UI.fillPolygon(xPoints,yPoints,3);
        UI.setColor(Color.black);
        UI.drawString(text, left+this.arrowSize/2+2, top+this.arrowSize/2);
    }

    // Main
    public static void main(String[] arguments){
        ImageProcessor ob = new ImageProcessor();
    }       

}
