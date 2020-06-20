/**
 * Class to store and apply two dimensional matrix transformations to vectors.
 * @author Matthew Corfiatis
 */
public class Transform extends Vector2 {

    public double
            X1, Y1,
            X2, Y2;
    public Vector2 offset = new Vector2(0,0);

    public Transform() {}
    public Transform(
            double X1, double Y1,
            double X2, double Y2,
            Vector2 offset)
    {
        this.X1 = X1;
        this.Y1 = Y1;

        this.X2 = X2;
        this.Y2 = Y2;
        this.offset = offset;
    }
    public Transform(
            double X1, double Y1,
            double X2, double Y2)
    {
        this.X1 = X1;
        this.Y1 = Y1;

        this.X2 = X2;
        this.Y2 = Y2;
    }

    /**
     * Applies linear transformation matrix to a vector
     * @param vec Vector to apply the transformation to
     * @return Transformed vector
     */
    public Vector2 multiplyVector(Vector2 vec)
    {
        return new Vector2(
                (X1 * vec.X) + (Y1 * vec.Y) + offset.X,
                (X2 * vec.X) + (Y2 * vec.Y) + offset.Y);
    }

    /**
     * Multiples two matrices together to combine multiple linear transformations into one matrix
     * @param a Transform one
     * @param b Transform two
     * @return Product of two matrix transformations
     */
    public static Transform multipy(Transform a, Transform b)
    {
        return new Transform(
                (a.X1 * b.X1) + (a.Y1 * b.X2), (a.X1 * b.Y1) + (a.Y1 * b.Y2), //Two dimensional multiplication
                (a.X2 * b.X1) + (a.Y2 * b.X2), (a.X2 * b.Y1) + (a.Y2 * b.Y2),
                Vector2.add(a.offset, b.offset)); //Add position offset
    }

    /**
     * Multiplies multiple transformation matrices together into one.
     * @param transforms Transforms to multiply.
     * @return Combined linear transform matrix.
     */
    public static Transform X(Transform... transforms)
    {
        Transform tr = transforms[0];
        for(int i = 1; i < transforms.length; i++)
        {
            tr = multipy(tr, transforms[i]);
        }
        return tr;
    }

    /**
     * Generates matrix that will rotate vectors by specified amount
     * @param degrees Amount to rotate
     * @return Transformation matrix for specified rotation
     */
    public static Transform rotationMatrix(double degrees)
    {
        double angle = Math.toRadians(degrees);
        return new Transform( //Create 2d rotational matrix
                Math.cos(angle), -Math.sin(angle),
                Math.sin(angle), Math.cos(angle));
    }
}
