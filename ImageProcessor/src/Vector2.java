/**
 * Two dimensional vector object for use with matrix transformations.
 * -Represents a point or vector in two dimensional space (The image plane).
 * @author Matthew Corfiatis
 */
public class Vector2 {
    public double X = 0;
    public double Y = 0;

    public Vector2(){}
    public Vector2(double X, double Y)
    {
        this.X = X;
        this.Y = Y;
    }

    /**
     * Sums two vectors
     * @param a Vector one
     * @param b Vector two
     * @return Computed addition of the vectors
     */
    public static Vector2 add(Vector2 a, Vector2 b)
    {
        return new Vector2(a.X + b.X, a.Y + b.Y);
    }

    /**
     * Subtracts two vectors
     * @param a Vector one
     * @param b Vector two
     * @return Computed subtractions of the vectors
     */
    public static Vector2 subtract(Vector2 a, Vector2 b)
    {
        return new Vector2(a.X - b.X, a.Y - b.Y);
    }
}
