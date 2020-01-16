package matikka;


/**
 * Kaksiuloitteisen avaruuden vektori.
 * @author Ilari Kauko
 */
public class Vektori {

    private double x, y;
    
    
    /**
     * @param x vektorin x-koordinaatti
     * @param y vektorin y-koordinaatti
     */
    public Vektori(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    
    /**
     * @param toinen vektori, jonka kanssa pistetulo lasketaan
     * @return vektorin pistetulo toisen kanssa
     */
    public double pistetulo(Vektori toinen) {
        return x*toinen.x + y*toinen.y;
    }
}