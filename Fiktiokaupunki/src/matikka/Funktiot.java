package matikka;

import java.util.ArrayList;


/**
 * Matemaattisia funktioita. Näille on todennäköisesti käyttöä muuallakin kuin kaupunkikarttageneraattorissa.
 * @author Ilari Kauko
 */
public class Funktiot {

    public static interface Funktio2IntiaVoid {
        public void f(int i, int j);
    }
	
	private static final double GAUSS_ALKU = 1/Math.sqrt(2*Math.PI);

	/**
	 * Perlin-kohinan käyttämä polynomi, joka tekee tuloksesta hienomman
	 * @param x polynomin muuttuja
	 * @return polynomin tulos, joka noudattaa hienoa käyrää x:n ollessa välillä [0,1]
	 */
    public static double perlinKayra(double x) {
        return 6*x*x*x*x*x - 15*x*x*x*x + 10*x*x*x;
    }
    
    
    /**
     * normaalijakauman tiheysfunktio varianssin ollessa 1 ja keskiarvon ollessa 0
     * @param x parametri
     * @return normaalijakauman tiheys x:ssä.
     */
    public static double gauss(double x) {
        return GAUSS_ALKU*Math.exp(-x*x/2);
    }
    
    
    /**
     * @param r
     * @return lista kokonaislukuvektoreista, joiden pituus on vähintään r mutta alle r + 1
     */
    public static ArrayList<int[]> sadekeha(int r) {
    	int minr2 = r*r;
    	int maxr2 = (r + 1)*(r + 1);
    	ArrayList<int[]> palaute = new ArrayList<int[]>();
    	for (int i = -r; i <= r; i++) {
    		for (int j = -r; j <= r; j++) {
    			int r2 = i*i + j*j;
    			if (minr2 <= r2 && r2 < maxr2) palaute.add(new int[] {i, j});
    		}
    	}
    	return palaute;
    }
    

    /**
     * Laskee kolmion kulman kärkipisteiden ja kosinilauseen perusteella.
     * @param x1 Kulmaa vastaavan kärkipisteen x-koordinaatti
     * @param y1 Kulmaa vastaavan kärkipisteen y-koordinaatti
     * @param x2 Toisen kärkipisteen x-koordinaatti
     * @param y2 Toisen kärkipisteen y-koordinaatti
     * @param x3 Kolmannen kärkipisteen x-koordinaatti
     * @param y3 Kolmannen kärkipisteen y-koordinaatti
     * @return x1- ja y1-koordinaatteja vastaava kulma radiaaneina
     */
    public static double kulma(double x1, double y1, double x2, double y2, double x3, double y3) {
    	double a2 = (x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2);
    	double b2 = (x1 - x3)*(x1 - x3) + (y1 - y3)*(y1 - y3);
    	double c2 = (x2 - x3)*(x2 - x3) + (y2 - y3)*(y2 - y3);
		return Math.acos((a2 + b2 - c2)/2/Math.sqrt(a2)/Math.sqrt(b2));
    }

    public static double[] kaanto(double x0, double y0, double x, double y, double kulma) {
        double cos = Math.cos(kulma);
        double sin = Math.sin(kulma);
        x -= x0;
        y -= y0;
        return new double[]{x0 + x*cos - y*sin, y0 + x*sin + y*cos};
    }

    public static void bresenham(int x1, int y1, int x2, int y2, Funktio2IntiaVoid kasittely) {
        int minx = Math.min(x1, x2);
        int miny = Math.min(y1, y2);
        int maxx = Math.max(x1, x2);
        int maxy = Math.max(y1, y2);
        double d = 1.0*(y2 - y1)/(x2 - x1);
        if (maxy - miny < maxx - minx) {
            for (int i = minx; i <= maxx; i++) {
                int j = (int)((i - x1)*d + y1);
                kasittely.f(i, j);
            }
        } else {
            for (int j = miny; j <= maxy; j++) {
            	int i = (int)((j - y1)/d + x1);
                kasittely.f(i, j);
            }
        }
    }
}