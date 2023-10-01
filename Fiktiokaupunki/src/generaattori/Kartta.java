package generaattori;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;
import java.util.Stack;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

import matikka.Vektori;
import rakenteet.Keko;
import matikka.Funktiot;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
 * kaupunkikarttageneraattori
 * @author Ilari Kauko
 */
public class Kartta {
	
	// rajapintoja lambdafunktioille
	public static interface FunktioRuutuBoolean {
		public boolean f(Ruutu r);
	}
	
	public static interface FunktioRuutuDouble {
		public double f(Ruutu r);
	}
	
	public static interface FunktioRuutuInt {
		public int f(Ruutu r);
	}
    
	public static interface FunktioRuutuVoid {
		public void f(Ruutu r);
	}
	
	public static interface Funktio2RuutuaDouble {
		public double f(Ruutu a, Ruutu b);
	}
	
	public static interface Funktio2RuutuaDoubleDouble {
		public double f(Ruutu a, Ruutu b, double x);
	}
	
	public static interface FunktioRuutuRuutulist {
		public ArrayList<Ruutu> f(Ruutu r);
	}
	
	public static interface Funktio2RuutuaInt {
		public int f(Ruutu a, Ruutu b);
	}
	
	/**
	 * Kartta käsitellään kaksiuloitteisena ruututaulukkona, jossa ruutu on oma luokkansa.
	 * Idea on, että kukin ruutu vastaa 25 neliömetriä, mutta vain piirra-metodi suorastaan velvoittaa tätä.
	 * @author Ilari Kauko
	 */
    public static class Ruutu {
        
        private int x, y, maankaytto, rakennus, katu, tontti, e, rataa;
        private double korkeus;
        
        /**
         * @param x ruudun x-koordinaatti
         * @param y ruudun y-koordinaatti
         */
        public Ruutu(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        
        /**
         * Muokkaa ruudun tiedot vastaamaan annettua merkkijonokuvausta.
         * @param kuvaus Merkkijono, jossa ruudun tiedot on kuvattu.
         */
        public void parse(String kuvaus) {
            String[] data = kuvaus.split("\\|");
            maankaytto = Integer.parseInt(data[0]);
            katu = Integer.parseInt(data[1]);
            rakennus = Integer.parseInt(data[2]);
            tontti = Integer.parseInt(data[3]);
            e = Integer.parseInt(data[4]);
            korkeus = Double.parseDouble(data[5]);
            rataa = Integer.parseInt(data[6]);
        }
        
        
        /**
         * Ruudun merkkijonomuunnos. Tässä muodossa ruudut kirjoitetaan tiedostoon. Koordinaatteja ei merkitä, koska ne on pääteltävissä rivin järjestysnumerosta.
         * @return ruudun olennaiset tiedot merkkijonona
         */
        @Override
        public String toString() {
            return maankaytto + "|" + katu + "|" + rakennus + "|" + tontti + "|" + e + "|" + String.format("%.3f", korkeus).replace(",", ".") + "|" + rataa;
        }

        public static double tasaisuus(ArrayList<Ruutu> alue) {
            double keskiarvo = 0;
            for (Ruutu ruutu : alue) keskiarvo += ruutu.korkeus;
            keskiarvo /= alue.size();
            double varianssi = 0;
            for (Ruutu ruutu : alue) varianssi += (ruutu.korkeus - keskiarvo)*(ruutu.korkeus - keskiarvo);
            return varianssi/alue.size();
        }
    }

    public static class Katu {
        private ArrayList<Ruutu> alue;
        private String nimi;

        public Katu(ArrayList<Ruutu> alue, String nimi) {
            this.alue = alue;
            this.nimi = nimi;
        }
    }
    
    
    private Ruutu[][] sisalto;
    private int sivu;
    private int tontteja = 0;
    private ArrayList<Katu> katuverkko;
    
    
    /**
     * @param sivu ruutujen määrän neliöjuuri
     */
    public Kartta(int sivu) {
        this.sivu = sivu;
        sisalto = new Ruutu[sivu][sivu];
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) sisalto[i][j] = new Ruutu(i, j);
        }
        this.katuverkko = new ArrayList<Katu>();
    }
    
    
    /**
     * Muodostaa kartan annetun tiedoston perusteella.
     * @param tiedosto Polku, josta tiedosto löytyy
     * @throws FileNotFoundException Jos tiedostoa ei löydy.
     */
    public Kartta(String tiedosto) throws FileNotFoundException {
        try (Scanner fi = new Scanner(new FileInputStream(tiedosto))) {
            sivu = Integer.parseInt(fi.nextLine());
            sisalto = new Ruutu[sivu][sivu];
            int i = 0;
            while (i < sivu) {
                int j = 0;
                while (j < sivu && fi.hasNext()) {
                    Ruutu ruutu = new Ruutu(i, j);
                    ruutu.parse(fi.next());
                    sisalto[i][j] = ruutu;
                    j++;
                }
                i++;
            }
        }
    }
    
    
    /**
     * Muuttaa kartan ruutujen korkeuksia Perlin-kohinan mukaisesti.
     * @param lahtotarkkuus Moneenko neliön muotoiseen osaan kartta jaetaan pysty- ja vaakasuunnassa aluksi
     * @param maalitarkkuus Moneenko neliön muotoiseeen osaan kartta täytyy olla jaettuna pysty- ja vaakasuunnassa, jotta pienemmän kohinan muodostaminen isomman päälle loppuu. Jos tämä on edellinen/2, kohina lisätään vain yhdessä mittakaavassa.
     * @param maksimiero kuinka voimakasta kohina on
     */
    public void luoKorkeuserot(int lahtotarkkuus, int maalitarkkuus, double maksimiero) {
        for (int ruudunSivu = sivu/lahtotarkkuus; ruudunSivu != 1 && ruudunSivu != sivu/maalitarkkuus; ruudunSivu /= 2) {
            int ruudukonSivu = sivu/ruudunSivu+1;
            Vektori[][] vektorit = new Vektori[ruudukonSivu][ruudukonSivu];
            for (int i = 0; i < ruudukonSivu; i++) {
                for (int j = 0; j < ruudukonSivu; j++) {
                    double suunta = Math.random()*Math.PI*2;
                    vektorit[i][j] = new Vektori(Math.cos(suunta), Math.sin(suunta));
                }
            }
            for (int i = 0; i < ruudukonSivu-1; i++) {
                for (int j = 0; j < ruudukonSivu-1; j++) {
                    Vektori gradientVH = vektorit[i][j];
                    Vektori gradientOH = vektorit[i+1][j];
                    Vektori gradientVP = vektorit[i][j+1];
                    Vektori gradientOP = vektorit[i+1][j+1];
                    for (int k = 0; k < ruudunSivu; k++) {
                        for (int l = 0; l < ruudunSivu; l++) {
                            double vasen = Funktiot.perlinKayra((k + 0.5)/ruudunSivu);
                            double huippu = Funktiot.perlinKayra((l + 0.5)/ruudunSivu);
                            double oikea = vasen - 1;
                            double pohja = huippu - 1;
                            Vektori etaisyysVH = new Vektori(vasen, huippu);
                            Vektori etaisyysOH = new Vektori(oikea, huippu);
                            Vektori etaisyysVP = new Vektori(vasen, pohja);
                            Vektori etaisyysOP = new Vektori(oikea, pohja);
                            double pistetuloVH = etaisyysVH.pistetulo(gradientVH);
                            double pistetuloOH = etaisyysOH.pistetulo(gradientOH);
                            double pistetuloVP = etaisyysVP.pistetulo(gradientVP);
                            double pistetuloOP = etaisyysOP.pistetulo(gradientOP);
                            double nelioVH = vasen*huippu;
                            double nelioOH = -1*oikea*huippu;
                            double nelioVP = -1*vasen*pohja;
                            double nelioOP = oikea*pohja;
                            sisalto[i*ruudunSivu+k][j*ruudunSivu+l].korkeus += (pistetuloVH*nelioOP + pistetuloOH*nelioVP + pistetuloVP*nelioOH + pistetuloOP*nelioVH)*maksimiero*ruudunSivu/sivu;
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * Floodfill-algoritmi.
     * @param i lähtöruudun x-koordinaatti
     * @param j lähtöruudun y-koordinaatti
     * @param kasittely mitä algoritmin alaisille ruuduille tehdään
     * @param kulmittain huomioidaanko myös kulmittaiset naapurit
     * @param ehto mikä ehto ruudun on täytettävä, jotta se menee algoritmin alle
     */
    public void floodfill(int i, int j, FunktioRuutuVoid kasittely, FunktioRuutuBoolean ehto, boolean kulmittain) {
        if (!ehto.f(sisalto[i][j])) return;
        boolean[][] vierailtu = new boolean[sivu][sivu];
        Stack<Ruutu> pino = new Stack<Ruutu>();
        pino.push(sisalto[i][j]);
        while (!pino.empty()) {
            Ruutu s = pino.pop();
            if (vierailtu[s.x][s.y]) continue;
            kasittely.f(s);
            vierailtu[s.x][s.y] = true;
            if (kartalla(s.x-1, s.y) && !vierailtu[s.x-1][s.y] && ehto.f(sisalto[s.x-1][s.y])) pino.push(sisalto[s.x-1][s.y]);
            if (kartalla(s.x+1, s.y) && !vierailtu[s.x+1][s.y] && ehto.f(sisalto[s.x+1][s.y])) pino.push(sisalto[s.x+1][s.y]);
            if (kartalla(s.x, s.y-1) && !vierailtu[s.x][s.y-1] && ehto.f(sisalto[s.x][s.y-1])) pino.push(sisalto[s.x][s.y-1]);
            if (kartalla(s.x, s.y+1) && !vierailtu[s.x][s.y+1] && ehto.f(sisalto[s.x][s.y+1])) pino.push(sisalto[s.x][s.y+1]);
            if (kulmittain) {
                if (kartalla(s.x-1, s.y-1) && !vierailtu[s.x-1][s.y-1] && ehto.f(sisalto[s.x-1][s.y-1])) pino.push(sisalto[s.x-1][s.y-1]);
                if (kartalla(s.x-1, s.y+1) && !vierailtu[s.x-1][s.y+1] && ehto.f(sisalto[s.x-1][s.y+1])) pino.push(sisalto[s.x-1][s.y+1]);
                if (kartalla(s.x+1, s.y-1) && !vierailtu[s.x+1][s.y-1] && ehto.f(sisalto[s.x+1][s.y-1])) pino.push(sisalto[s.x+1][s.y-1]);
                if (kartalla(s.x+1, s.y+1) && !vierailtu[s.x+1][s.y+1] && ehto.f(sisalto[s.x+1][s.y+1])) pino.push(sisalto[s.x+1][s.y+1]);
            }
        }
    }

    /**
     * Edellinen niin, että oletuksena ei huomioida kulmittaisia naapureita.
     * @param i lähtöruudun x-koordinaatti
     * @param j lähtöruudun y-koordinaatti
     * @param kasittely mitä algoritmin alaisille ruuduille tehdään
     * @param ehto mikä ehto ruudun on täytettävä, jotta se menee algoritmin alle
     */
    public void floodfill(int i, int j, FunktioRuutuVoid kasittely, FunktioRuutuBoolean ehto) {
        this.floodfill(i, j, kasittely, ehto, false);
    }
    
    
    /**
     * Tallentaa kartan helposti tämän ohjelman luettavaan tiedostoon.
     * @param tiedosto polku, johon tiedosto kirjoitetaan
     * @throws FileNotFoundException jos tiedostoa ei löydy
     */
    public void kirjoita(String tiedosto) throws FileNotFoundException {
        try (PrintStream fo = new PrintStream(new FileOutputStream(tiedosto))) {
            fo.println(sivu); // Ensimmäisellä rivillä kuvataan kartan sivun pituus ruutuina. Ruutujen koordinaatit päätellään tästä ja niiden järjestyksestä tiedostossa.
            for (int i = 0; i < sivu; i++) {
                for (int j = 0; j < sivu; j++) fo.println(sisalto[i][j]);
            }
        }
    }
    
    
    /**
     * Piirtää kartan PNG-kuvatiedostoon.
     * @param tiedosto polku, johon tiedosto kirjoitetaan
     * @param varit kuvaa värit, joilla kutakin maankäyttötyyppiä merkitään
     * @param tiet Kuvaa värit, joilla kutakin tietyyppiä merkitään. Tässä versiossa teitä on vain yhtä tyyppiä.
     * @param tonttiraja millä värillä tonttirajaa merkitään
     * @param rata millä värillä rautatietä kuvataan
     * @param korkeuskayra millä värillä korkeuskäyrää kuvataan
     */
    public void piirra(String tiedosto, Color[] varit, Color[] tiet, Color tonttiraja, Color rata, Color korkeuskayra) {
        BufferedImage bImg = new BufferedImage(sivu, sivu, BufferedImage.TYPE_INT_RGB);
        Graphics g = (Graphics2D)bImg.getGraphics();
        int kayravali = 5;
        
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
            	if (sisalto[i][j].katu == 0) {
            		g.setColor(varit[sisalto[i][j].maankaytto]);
            		g.fillRect(i,  j, 1, 1);
            	}
            	if (sisalto[i][j].maankaytto == 0 && sisalto[i][j].katu == 0) {
            		// Metsä- tai peltoalueelle piirretään korkeuskäyrät. Ne merkitään 5 metrin välein.
	        		boolean kayraa = false;
	        		for (int k = i - 1; k <= i + 1; k++) {
	        			for (int l = j - 1; l <= j + 1; l++) {
	        				if ((i == k || l == j) && kartalla(k, l) && ((int)sisalto[k][l].korkeus/kayravali < (int)sisalto[i][j].korkeus/kayravali || sisalto[k][l].korkeus < 0 && 0 <= sisalto[i][j].korkeus)) kayraa = true;
	        			}
	        		}
	        		if (!kayraa) continue;
	                g.setColor(korkeuskayra);
	                g.fillRect(i, j, 1, 1);
            	}
				boolean tonttir = false;
        		for (int k = i - 1; k <= i + 1; k++) {
        			for (int l = j - 1; l <= j + 1; l++) {
        				if ((i == k || l == j) && kartalla(k, l) && sisalto[k][l].tontti != 0 && sisalto[i][j].tontti < sisalto[k][l].tontti) tonttir = true;
        			}
        		}
				if (!tonttir) continue;
				g.setColor(tonttiraja);
                g.fillRect(i, j, 1, 1);
            }
        }
        
        for (int i = 0; i < sivu; i++) {
        	for (int j = 0; j < sivu; j++) {
        		if (sisalto[i][j].katu != 0) {
        			g.setColor(tiet[sisalto[i][j].katu]);
        			g.fillRect(i-4, j-4, 8, 8);
        		}
        	}
        }
        g.setColor(rata);
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rataa == 1 || sisalto[i][j].rataa == 2 && Math.random() < 0.5) g.fillRect(i, j, 1, 1);
                if (sisalto[i][j].rakennus == 1) g.fillRect(i-5, j-5, 10, 10);
            }
        }

        g.setColor(Color.red);
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rakennus == 2 && sisalto[i][j].maankaytto == 4) g.fillRect(i, j, 1, 1);
            }
        }
        g.setColor(Color.orange);
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rakennus == 2 && sisalto[i][j].maankaytto == 3) g.fillRect(i, j, 1, 1);
            }
        }
        g.setColor(new Color(64,64,64));
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rakennus == 2 && sisalto[i][j].maankaytto == 2) g.fillRect(i, j, 1, 1);
            }
        }
        Font font = new Font("Arial", Font.PLAIN, 12);
        g.setFont(font);
        g.setColor(Color.black);
    
        for (Katu katu : this.katuverkko) {
            int width = g.getFontMetrics().stringWidth(katu.nimi);
            Ruutu[] aaripaat = null;
            int suurinEtaisyys = 0;
            for (int i = 0; i < katu.alue.size(); i++) {
                for (int j = 0; j < i; j++) {
                    int d = etaisyys2(katu.alue.get(i), katu.alue.get(j));
                    if (suurinEtaisyys < d) {
                        suurinEtaisyys = d;
                        aaripaat = new Ruutu[]{katu.alue.get(i), katu.alue.get(j)};
                    }
                }
            }
            if (aaripaat == null) continue;
            Ruutu puolivali = this.keskus(aaripaat[0], aaripaat[1]);
            Keko<Ruutu> paakeko = new Keko<Ruutu>(r -> Math.abs((r.x - puolivali.x)*(r.x - puolivali.x) + (r.y - puolivali.y)*(r.y - puolivali.y) - width*width/4), katu.alue);
            Ruutu toinenPaa = paakeko.pienin();
            while (toinenPaa.x*this.sivu + toinenPaa.y > puolivali.x*this.sivu + puolivali.y) toinenPaa = paakeko.pienin();
            AffineTransform affineTransform = new AffineTransform();
            double kulma = Math.atan2(aaripaat[0].y - aaripaat[1].y, aaripaat[0].x - aaripaat[1].x);
            if (Math.PI/2 < Math.abs(kulma)) kulma += Math.PI;
            affineTransform.rotate(kulma, 0, 0);
            Font rotatedFont = font.deriveFont(affineTransform);
            g.setFont(rotatedFont);
            int alkuX = toinenPaa.x - (int)(4*Math.cos(kulma - Math.PI/2));
            int alkuY = toinenPaa.y - (int)(4*Math.sin(kulma - Math.PI/2));
            g.drawString(katu.nimi, alkuX, alkuY);
        }
        g.setFont(font);
        // Vasempaan yläreunaan tulee 500 metrin mittatikku.
        g.setColor(Color.white);
        g.fillRect(0,0,300,300);
        g.setColor(Color.black);
        g.drawString("200 m", 20, 30);
        g.drawLine(10, 35, 110, 35);
        String[] nimet = new String[]{"Metsä / pelto / puisto", "Joki"};
        for (int i = 0; i < nimet.length; i++) {
            g.setColor(varit[i]);
            g.fillRect(20, 25*(i + 2), 20, 20);
            g.setColor(Color.black);
            g.drawString(nimet[i], 50, 25*(i + 2) + 15);
        }
        nimet = new String[]{"Teollisuusrakennus", "Asuinkerrostalo", "Julkinen / palvelu- / toimistorakennus"};
        varit = new Color[]{Color.gray, Color.orange, Color.red};
        for (int i = 0; i < nimet.length; i++) {
            g.setColor(varit[i]);
            g.fillRect(20, 25*(i + 4), 20, 20);
            g.setColor(Color.black);
            g.drawString(nimet[i], 50, 25*(i + 4) + 15);
        }
        g.drawLine(20, 25*(nimet.length + 4) + 12, 40, 25*(nimet.length + 4) + 12);
        g.fillRect(25, 25*(nimet.length + 4) + 7, 10, 10);
        g.drawString("Rautatie, asema", 50, 25*(nimet.length + 4) + 17);
        for (int i = 0; i < 20; i++) {
            if (Math.random() < 0.5) {
                g.fillRect(25 + i, 25*(nimet.length + 5) + 12, 1, 1);
            }
        }
        g.drawString("Rautatietunneli", 50, 25*(nimet.length + 5) + 17);
        try {
            if (ImageIO.write(bImg, "png", new File(tiedosto))) {
                System.out.println("Saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Kartan oma indeksitarkistin.
     * @param i x-koordinaatti
     * @param j y-koordinaatti
     * @return onko kartalla koordinaattien mukaista ruutua
     */
    public boolean kartalla(int i, int j) {
        return -1 < i && -1 < j && i < sivu && j < sivu;
    }
    
    
    /**
     * A*-algoritmi.
     * @param lahto ruutu, josta algoritmi aloittaa
     * @param etaisyydet taulukko, johon ruutujen etäisyydet lähdöstä tallennetaan
     * @param edelliset taulukko, johon ruuduista tallennetaan se, mistä ruudusta lyhyin reitti lähdöstä on kyseiseen ruutuun saapunut
     * @param naapurit miten haetaan ruudun naapurit eli ruudut, joihin ruudusta on suora yhteys
     * @param kaari miten kahden ruudun välisen kaaren pituus määritellään
     * @param heuristiikka miten etäisyys ruudusta maaliin arvioidaan
     * @param ehto mikä ehto ruudun on täytettävä, jotta se voidaan hyväksyä maaliksi
     * @return lähdöstä lähin ehdon täyttävä ruutu tai lähdöstä kaukaisin yhteydellinen ruutu, jos mikään yhteydellinen ruutu ei täytä ehtoa
     */
    public Ruutu aTahti(Ruutu lahto, double[][] etaisyydet, Ruutu[][] edelliset, FunktioRuutuRuutulist naapurit, Funktio2RuutuaDouble kaari, FunktioRuutuDouble heuristiikka, FunktioRuutuBoolean ehto) {
        boolean[][] vierailtu = new boolean[sivu][sivu];
        double[][] heuristiikat = new double[sivu][sivu];
    	for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) etaisyydet[i][j] = Double.POSITIVE_INFINITY;
        }
		Keko<Ruutu> reunat = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y] + heuristiikat[r.x][r.y], r -> r.x*sivu + r.y, sivu*sivu);
        Ruutu t = lahto;
		etaisyydet[t.x][t.y] = 0;
		heuristiikat[t.x][t.y] = heuristiikka.f(t);
		reunat.lisaa(t);
        while (reunat.size() != 0 && !ehto.f(t = reunat.pienin())) {
            vierailtu[t.x][t.y] = true;
            for (Ruutu n : naapurit.f(t)) {
            	int i = n.x;
            	int j = n.y;
            	if (vierailtu[i][j]) continue;
            	double k = etaisyydet[t.x][t.y] + kaari.f(t, n);
            	if (etaisyydet[i][j] <= k) continue;
            	boolean uusi = etaisyydet[i][j] == Double.POSITIVE_INFINITY;
            	etaisyydet[i][j] = k;
            	edelliset[i][j] = t;
            	if (uusi) {
            		heuristiikat[i][j] = heuristiikka.f(sisalto[i][j]);
            		reunat.lisaa(n);
            	} else reunat.laske(n);
            }
        }
        return t;
    }
    
    
    /**
     * Dijkstran algoritmi käsitellään A*-algoritmina, jossa heuristiikkafunktio palauttaa aina nollan.
     * @param lahto ruutu, josta algoritmi aloittaa
     * @param etaisyydet taulukko, johon ruutujen etäisyydet lähdöstä tallennetaan
     * @param edelliset taulukko, johon ruuduista tallennetaan se, mistä ruudusta lyhyin reitti lähdöstä on kyseiseen ruutuun saapunut
     * @param naapurit miten haetaan ruudun naapurit eli ruudut, joihin ruudusta on suora yhteys
     * @param kaari miten kahden ruudun välisen kaaren pituus määritellään
     * @param ehto mikä ehto ruudun on täytettävä, jotta se voidaan hyväksyä maaliksi
     * @return lähdöstä lähin ehdon täyttävä ruutu tai lähdöstä kaukaisin yhteydellinen ruutu, jos mikään yhteydellinen ruutu ei täytä ehtoa
     */
    public Ruutu dijkstra(Ruutu lahto, double[][] etaisyydet, Ruutu[][] edelliset, FunktioRuutuRuutulist sade, Funktio2RuutuaDouble kaari, FunktioRuutuBoolean ehto) {
    	return aTahti(lahto, etaisyydet, edelliset, sade, kaari, r -> 0, ehto);
     }
    
    
    /**
     * Etsii annetusta ruudusta linnuntietä lähimmän tietyn ehdon täyttävän ruudun.
     * @param lahto ruutu, josta etsintä alkaa
     * @param nakyvyys säde, jolta ehdon täyttävää ruutua etsitään
     * @param ehto mikä ehto etsittävän ruudun on täytettävä
     * @return linnuntietä lähin ehdon täyttävä ruutu tai null, jos annetulta säteeltä ei löydy ehdon täyttävää ruutua
     */
    public Ruutu tutka(Ruutu lahto, int nakyvyys, FunktioRuutuBoolean ehto) {
    	for (int r = 0; r < nakyvyys; r++) {
    		int r2max = (r + 1)*(r + 1);
    		int rmin = r*r;
    		int x = 0;
    		int y = r;
    		while (x <= y) {
    			for (int i = -1; i < 2; i += 2) {
    				for (int j = -1; j < 2; j += 2) {
    					if (kartalla(lahto.x+i*x, lahto.y+j*y) && ehto.f(sisalto[lahto.x+x*i][lahto.y+y*j])) return sisalto[lahto.x+x*i][lahto.y+y*j];
    					if (kartalla(lahto.x+j*x, lahto.y+i*y) && ehto.f(sisalto[lahto.x+x*j][lahto.y+y*i])) return sisalto[lahto.x+x*j][lahto.y+y*i];
    				}
    			}
    			if (rmin <= x*x + (y-1)*(y-1)) y--;
    			else {
    				x++;
    				if (r2max <= x*x + y*y) y--;
    			}
    		}
    	}
    	return null;
    }
    
    
    /**
     * Bresenhamin jana-algoritmi. Jos lasketuilla koordinaateilla ei ole vastaavaa ruutua, algoritmi sivuuttaa ne.
     * @param x1 toisen janan päätepisteen X-koordinaatti
     * @param y1 toisen janan päätepisteen y-koordinaatti
     * @param x2 toisen janan päätepisteen x-koordinaatti
     * @param y2 toisen janan päätepisteen y-koordinaatti
     * @param kasittely mitä koordinaatteja vastaavalle ruudulle tehdään, jos sellainen on olemassa
     */
    public void bresenham(int x1, int y1, int x2, int y2, FunktioRuutuVoid kasittely) {
        int minx = Math.min(x1, x2);
        int miny = Math.min(y1, y2);
        int maxx = Math.max(x1, x2);
        int maxy = Math.max(y1, y2);
        double d = 1.0*(y2 - y1)/(x2 - x1);
        if (maxy - miny < maxx - minx) {
            for (int i = minx; i <= maxx; i++) {
                int j = (int)((i - x1)*d + y1);
                if (kartalla(i, j)) kasittely.f(sisalto[i][j]);
            }
        } else {
            for (int j = miny; j <= maxy; j++) {
            	int i = (int)((j - y1)/d + x1);
                if (kartalla(i, j)) kasittely.f(sisalto[i][j]);
            }
        }
    }

    
    /**
     * Edellinen toisessa muodossa. Koordinaattien sijaan annetaan niitä vastaavat ruudut.
     * @param a toinen janan päätepiste
     * @param b toinen janan päätepiste
     * @param kasittely mitä janalla oleville ruuduille tehdään
     */
    public void bresenham(Ruutu a, Ruutu b, FunktioRuutuVoid kasittely) {
        bresenham(a.x, a.y, b.x, b.y, kasittely);
    }
  
    
    /**
     * Jakaa korttelin tontteihin rekursiivisesti. Menetelmä ei ole täysin itse keksitty.
     * @param alue alue, jota (mahdollisesti) puolitettava alue koskee
     * @param eRaja Tehokkuusluku, jonka ylittäessään alueesta tulee kerrostaloalue, jos se muuten olisi pientaloalue. Tehokkuusluku on alueen lattiapinta-ala jaettuna maapinta-alalla.
     * @param koot Kutakin maankayttötyyppiä vastaava tonttikoon maksimi. Jos alueen pinta-ala alittaa tämän, alueesta muododstetaan tontti.
     */
    public void puolita(ArrayList<Ruutu> alue, double eRaja, int[] koot) {
    	if (alue.size() == 0) return;
    	// Jos alue ei ole yhtenäinen, käsitellään osat erillään.
    	ArrayList<Ruutu> jako1 = new ArrayList<Ruutu>();
    	ArrayList<Ruutu> jako2 = new ArrayList<Ruutu>();
    	jako2.addAll(alue);
    	floodfill(jako2.get(0).x, jako2.get(0).y, r -> {
    		jako2.remove(r);
    		jako1.add(r);
    	}, r -> alue.contains(r));
    	if (jako2.size() != 0) {
    		puolita(jako2, eRaja, koot);
    		puolita(jako1, eRaja, koot);
    		return;
    	}
    	// Selvitetään käsiteltävän alueen yleisin maankäyttötyyppi ja lattiapinta-alan määrä. Jos alueen pinta-ala on alle tyyppiä vastaavan kokorajan, lopetetaan jakaminen ja muodostetaan alueesta tontti.
    	int[] yleisyydet = new int[koot.length];
    	int e = 0;
    	for (Ruutu r : jako1) {
    		e += r.e;
    		yleisyydet[r.maankaytto]++;
    	}
    	int yleisin = yleisyydet[0];
    	for (int i = 1; i < koot.length; i++) {
    		if (yleisyydet[yleisin] < yleisyydet[i]) yleisin = i;
    	}
    	if (yleisin == 2 && eRaja < 1.0*e/jako1.size()) yleisin = 4;
    	if (jako1.size() <= koot[yleisin]) {
    		luoTontti(alue, yleisin);
    		return;
    	}
    	// Etsitään raa'alla voimalla ainakin melkein pienin suorakulmio, jonka sisään alue mahtuu. Apuna on kääntömatriisi.
    	ArrayList<Ruutu> reunat = new ArrayList<Ruutu>();
    	reunat.addAll(jako1);
    	ArrayList<int[]> sade = new ArrayList<int[]>();
    	sade.add(new int[] {-1,0});
    	sade.add(new int[] {1,0});
    	sade.add(new int[] {0,-1});
    	sade.add(new int[] {0,1});
    	for (int i = reunat.size() - 1; i >= 0; i--) {
    		boolean reunalla = false;
    		for (int[] suunta : sade) reunalla = reunalla || kartalla(reunat.get(i).x + suunta[0], reunat.get(i).y + suunta[1]) && !jako1.contains(sisalto[reunat.get(i).x+suunta[0]][reunat.get(i).y+suunta[1]]);
    		if (!reunalla) reunat.remove(i);
    	}
    	Ruutu keskus = reunat.get(0);
    	double ennatys = Double.POSITIVE_INFINITY;
    	double parasKulma = 0;
    	double[] karjet = new double[4];
    	for (double x = 0; x < Math.PI/2; x += Math.PI/32) {
    		double cos = Math.cos(x);
    		double sin = Math.sin(x);
        	double maxx = cos*keskus.x - sin*keskus.y;
        	double minx = maxx;
        	double maxy = sin*keskus.x + cos*keskus.y;
        	double miny = maxy;
        	for (int i = 1; i < reunat.size(); i++) {
        		double x2 = cos*reunat.get(i).x - sin*reunat.get(i).y;
        		double y2 = sin*reunat.get(i).x + cos*reunat.get(i).y;
        		maxx = Math.max(maxx, x2);
        		minx = Math.min(minx, x2);
        		maxy = Math.max(maxy, y2);
        		miny = Math.min(miny, y2);
        	}
    		if (ennatys <= (maxx - minx)*(maxy - miny)) continue;
    		ennatys = (maxx - minx)*(maxy - miny);
    		parasKulma = x;
    		karjet = new double[] {maxx, minx, maxy, miny};
    	}
    	double cos = Math.cos(-1*parasKulma);
    	double sin = Math.sin(-1*parasKulma);
    	// Alue puolitetaan löytyneen suorakulmion mukaan siten, että jakolinja on suorakulmion lyhyempien sivujen kanssa yhdensuuntainen ja niiden puolivälissä.
		Ruutu kulma1, kulma2, kulma3, kulma4;
        kulma1 = sisalto[(int)(cos*karjet[0] - sin*karjet[2])][(int)(sin*karjet[0] + cos*karjet[2])];
        kulma2 = sisalto[(int)(cos*karjet[0] - sin*karjet[3])][(int)(sin*karjet[0] + cos*karjet[3])];
        kulma3 = sisalto[(int)(cos*karjet[1] - sin*karjet[2])][(int)(sin*karjet[1] + cos*karjet[2])];
        kulma4 = sisalto[(int)(cos*karjet[1] - sin*karjet[3])][(int)(sin*karjet[1] + cos*karjet[3])];
        Ruutu linja1 = keskus(kulma1, kulma2);
        Ruutu linja2 = keskus(kulma3, kulma4);
        if (etaisyys2(linja1, linja2) < etaisyys2(keskus(kulma1, kulma3), keskus(kulma2, kulma4))) {
        	linja1 = keskus(kulma1, kulma3);
        	linja2 = keskus(kulma2, kulma4);
        }
        ArrayList<Ruutu> osa1 = new ArrayList<Ruutu>();
        ArrayList<Ruutu> osa2 = new ArrayList<Ruutu>();
    	for (Ruutu r : jako1) {
    		if (etaisyys2(r, linja1) < etaisyys2(r, linja2)) osa1.add(r);
    		else osa2.add(r);
    	}
    	// Jos jommallakummalla puolituksen tuloksista ei ole katuyhteyttä, ei puolitusta suoritetakaan.
    	boolean katuyhteys = false;
    	for (Ruutu r : osa1) {
    		for (int[] suunta : sade) katuyhteys = katuyhteys || kartalla(r.x+suunta[0],r.y+suunta[1]) && sisalto[r.x+suunta[0]][r.y+suunta[1]].katu != 0;
    	}
    	if (!katuyhteys) {
    		luoTontti(jako1, yleisin);
    		return;
    	}
    	katuyhteys = false;
    	for (Ruutu r : osa2) {
    		for (int[] suunta : sade) katuyhteys = katuyhteys || kartalla(r.x+suunta[0],r.y+suunta[1]) && sisalto[r.x+suunta[0]][r.y+suunta[1]].katu != 0;
    	}
    	if (!katuyhteys) {
    		luoTontti(jako1, yleisin);
    		return;
    	}
    	// Jos päästiin tänne asti, jatketaan tonttien jakoa rekursiivisesti.
    	puolita(osa1, eRaja, koot);
    	puolita(osa2, eRaja, koot);
    }
    
    
    /**
     * Luo annetusta ruutulistasta oman tonttinsa. Tontin maankäytön on oltava yhtenäinen.
     * @param alue ruudut, jotka kuuluvat muodostettavaan tonttiin
     * @param tyyppi maankäyttötyyppi, jota tontti edustaa
     */
    public void luoTontti(ArrayList<Ruutu> alue, int tyyppi) {
    	int nro = ++tontteja;
    	for (Ruutu r : alue) {
    		r.maankaytto = tyyppi;
    		r.tontti = nro;
    	}
    }
        
    
    /**
     * Muodostaa karttaan tien tai vastaavan väylän annettujen lähtö- ja maaliruutujen ja viitetaulukon avulla. Käytetään usein A*-algoritmin jälkeen.
     * @param lahto ruutu, josta väylä alkaa
     * @param maali ruutu, johon väylä päättyy
     * @param viitteet taulukko, joka kertoo ruudusta, mihin ruutuun väylä seuraavaksi siirtyy
     * @param millainen mitä väylällä oleville ruuduille tehdään
     */
    public void luoTie(Ruutu lahto, Ruutu maali, Ruutu[][] viitteet, FunktioRuutuVoid millainen) {
    	Ruutu a = lahto;
    	while (a != maali) {
    		Ruutu b = viitteet[a.x][a.y];
    		bresenham(a, b, millainen);
    		a = b;
    	}
    }
    
    
    /**
     * @param a toinen ruutu
     * @param b toinen ruutu
     * @return Ruutujen etäisyyden neliö. Etäisyys voidaan laskea neliöjuuresta.
     */
    public static int etaisyys2(Ruutu a, Ruutu b) {
    	int dx = a.x - b.x;
    	int dy = a.y - b.y;
    	return dx*dx + dy*dy;
    }
    
    
    /**
     * @param a janan toinen pääteruutu
     * @param b janan toinen pääteruutu
     * @return ruutujen koordinaattien keskiarvoja vastaava ruutu
     */
    public Ruutu keskus(Ruutu a, Ruutu b) {
    	return sisalto[(a.x + b.x)/2][(a.y + b.y)/2];
    }

    public Ruutu keskus(ArrayList<Ruutu> alue) {
        if (alue.size() == 0) return null;
        Ruutu palaute = alue.get(0);
        int pieninNeliosumma = 0;
        for (int i = 0; i < alue.size(); i++) pieninNeliosumma += etaisyys2(palaute, alue.get(i));
        for (int i = 1; i < alue.size(); i++) {
            int neliosumma = 0;
            for (int j = 0; j < alue.size() && neliosumma < pieninNeliosumma; j++) neliosumma += etaisyys2(alue.get(i), alue.get(j));
            if (neliosumma < pieninNeliosumma) {
                pieninNeliosumma = neliosumma;
                palaute = alue.get(i);
            }
        }
        return palaute;
    }

    public void poistaKatua(int i, int j, FunktioRuutuBoolean tunnistus, int rekursio) {
        if (rekursio == 0) return;
        this.sisalto[i][j].katu = 0;
        for (int k = i - 1; k < i + 2; k++) {
            for (int l = j - 1; l < j + 2; l++) {
                if (kartalla(k, l) && tunnistus.f(this.sisalto[k][l])) {
                    boolean kaupungissa = false;
                    for (int m = k - 1; m < k + 2; m++) {
                        for (int n = l - 1; n < l + 2; n++) {
                            kaupungissa = kaupungissa || (this.kartalla(m, n) && this.sisalto[m][n].maankaytto == 3);
                        }
                    }
                    if (!kaupungissa) {
                        this.poistaKatua(k, l, tunnistus, rekursio - 1);
                    }
                }
            }
        }
    }    

    /**
     * Pääohjelma. Generointiprosessin mielekkyys on ehkä kyseenalainen, mutta prosessi toimii esimerkkinä 
     * muun ohjelmakoodin käytöstä.
     * @param args ei käytössä
     * @throws FileNotFoundException jos tiedoston luku tai kirjoitus ei onnistu
     */
    public static void main(String[] args) throws FileNotFoundException {
    	// perustiedot
    	final int n = 2048;
        Kartta map = new Kartta(n);
        
        // Luonnonmaantiede määritellään alussa. Keskustan vierestä virtaa joki, jonka uoma perustuu yhteen Perlin-kohinaan. Korkeuserot joen eri puolilla perustuvat kahteen eri Perlin-kohinaan.
        final double h = 128;
        double kulma = Math.random()*Math.PI*2;
        double cos = Math.cos(kulma);
        double sin = Math.sin(kulma);
        for (int i = 0; i <  n; i++) {
        	for (int j = 0; j < n; j++) map.sisalto[i][j].korkeus = h*(cos*(i - n/2) - sin*(j - n/2))/(n/2);
        }
        map.luoKorkeuserot(2, 4, h);
        final int KORTTELIN_SIVU = 50;
        final int JOKEEN = KORTTELIN_SIVU*3;
        double[] kk = Funktiot.kaanto(n/2, n/2, n/2 + JOKEEN, n/2, -kulma);
        int jokix = (int)kk[0];
        int jokiy = (int)kk[1];
        double nolla = map.sisalto[jokix][jokiy].korkeus;
        for (int i = 0; i <  n; i++) {
        	for (int j = 0; j < n; j++) {
        		map.sisalto[i][j].korkeus = Math.abs(map.sisalto[i][j].korkeus - nolla);
        	}
        }
        Kartta korkeudet1 = new Kartta(n);
        Kartta korkeudet2 = new Kartta(n);
        korkeudet1.luoKorkeuserot(2, n, 2);
        korkeudet2.luoKorkeuserot(2, n, 2);
        boolean[][] jokipuoli = new boolean[n][n];
        double joki = 2;
        map.floodfill(jokix, jokiy, r -> r.maankaytto = 1, r -> r.korkeus < joki);
        map.floodfill(n/2, n/2, r -> jokipuoli[r.x][r.y] = true, r -> r.maankaytto == 0);
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		korkeudet1.sisalto[i][j].korkeus *= korkeudet1.sisalto[i][j].korkeus;
        		korkeudet2.sisalto[i][j].korkeus *= korkeudet2.sisalto[i][j].korkeus;
        		if (map.sisalto[i][j].maankaytto == 1) continue;
        		if (jokipuoli[i][j]) map.sisalto[i][j].korkeus = joki + (map.sisalto[i][j].korkeus - joki)*korkeudet1.sisalto[i][j].korkeus;
        		else map.sisalto[i][j].korkeus = joki + (map.sisalto[i][j].korkeus - joki)*korkeudet2.sisalto[i][j].korkeus;
        	}
        }
       String pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
       map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.gray, Color.orange, Color.red}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));

        // Katujen kulma määritellään seuraavaksi. Pyritään siihen, että kadut ovat joko keskustan kohdalla yhdensuuntaisia tai kohtisuorassa joen kanssa.
        ArrayList<Ruutu> ruutukeha = new ArrayList<Ruutu>();
        for (int i = JOKEEN*2; i < JOKEEN*2 + 2; i++) {
            ArrayList<int[]> keha = Funktiot.sadekeha(i);
            for (int[] k : keha) {
                ruutukeha.add(map.sisalto[n/2 + k[0]][n/2 + k[1]]);
            }
        }
        for (int i = ruutukeha.size() - 1; i >= 0; i--) {
            if (ruutukeha.get(i).maankaytto == 1 || !jokipuoli[ruutukeha.get(i).x][ruutukeha.get(i).y]) {
                ruutukeha.remove(i);
                continue;
            }
            boolean rannalla = false;
            for (int j = -1; j < 2 && !rannalla; j++) {
                for (int k = -1; k < 2 && !rannalla; k++) {
                    rannalla = rannalla || map.sisalto[ruutukeha.get(i).x + j][ruutukeha.get(i).y + k].maankaytto == 1;
                }
            }
            if (!rannalla) ruutukeha.remove(i);
        }
        int suurinEtaisyys = 0;
        Ruutu[] vastinparit = new Ruutu[2];
        for (int i = 0; i < ruutukeha.size(); i++) {
            for (int j = 0; j < i; j++) {
                int d = map.etaisyys2(ruutukeha.get(i), ruutukeha.get(j));
                if (suurinEtaisyys < d) {
                    suurinEtaisyys = d;
                    vastinparit = new Ruutu[]{ruutukeha.get(i), ruutukeha.get(j)};
                }
            }
        }
        kulma = Math.atan2(vastinparit[1].y - vastinparit[0].y, vastinparit[1].x - vastinparit[0].x);
        cos = Math.cos(kulma);
        sin = Math.sin(kulma);
        final int KORTTELEITA_SIVULLA = n/KORTTELIN_SIVU;
        final int TEOLLISUUSRAKENNUKSEEN = 8;
        boolean[][] teollisuusrakennusrajat = new boolean[n][n];
        int[][] katunrot = new int[n][n];
        int nextKatuNro = 0;
        for (int i = -KORTTELIN_SIVU*KORTTELEITA_SIVULLA; i <= KORTTELIN_SIVU*KORTTELEITA_SIVULLA; i += KORTTELIN_SIVU) {
            double[] paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i, -n, kulma);
            double[] paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i, n*2, kulma);
            final int I = i;
            final int NEXT_KATU_NRO = nextKatuNro++;
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (I % (KORTTELIN_SIVU*4) == 0) r.katu = Math.max(r.katu, 3);
                else if (I % (KORTTELIN_SIVU*2) == 0) r.katu = Math.max(r.katu, 2);
                else r.katu = Math.max(r.katu, 1);
                teollisuusrakennusrajat[r.x][r.y] = true;
                katunrot[r.x][r.y] = NEXT_KATU_NRO;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i, -n, kulma + Math.PI/2);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i, n*2, kulma + Math.PI/2);
            final int NEXT_KATU_NRO_2 = nextKatuNro++;
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (I % (KORTTELIN_SIVU*4) == 0) r.katu = Math.max(r.katu, 4);
                else if (I % (KORTTELIN_SIVU*2) == 0) r.katu = Math.max(r.katu, 2);
                else r.katu = Math.max(r.katu, 1);
                teollisuusrakennusrajat[r.x][r.y] = true;
                katunrot[r.x][r.y] = NEXT_KATU_NRO_2;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i + KORTTELIN_SIVU/2, -n*2, kulma);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i + KORTTELIN_SIVU/2, n*2, kulma);
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                teollisuusrakennusrajat[r.x][r.y] = true;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i + KORTTELIN_SIVU/2, -n*2, kulma + Math.PI/2);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i + KORTTELIN_SIVU/2, n*2, kulma + Math.PI/2);
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                teollisuusrakennusrajat[r.x][r.y] = true;
            });
        }
        map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.gray, Color.orange, Color.red}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));
        boolean[][] puistorajat = new boolean[n][n];
        boolean[][] puistoa = new boolean[n][n];
        double[] paa1 = Funktiot.kaanto(n/2, n/2, n/2 - KORTTELIN_SIVU*2, -n, kulma);
        double[] paa2 = Funktiot.kaanto(n/2, n/2, n/2 - KORTTELIN_SIVU*2, n*2, kulma);
        map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> puistorajat[r.x][r.y] = true);
        paa1 = Funktiot.kaanto(n/2, n/2, n/2 + KORTTELIN_SIVU*2, -n, kulma);
        paa2 = Funktiot.kaanto(n/2, n/2, n/2 + KORTTELIN_SIVU*2, n*2, kulma);
        map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> puistorajat[r.x][r.y] = true);
        map.floodfill(n/2, n/2, r -> puistoa[r.x][r.y] = true, r -> !puistorajat[r.x][r.y]);
        final int PUISTOON = 6;
        paa1 = Funktiot.kaanto(n/2, n/2, -n, n/2 - KORTTELIN_SIVU*PUISTOON, kulma);
        paa2 = Funktiot.kaanto(n/2, n/2, n*2, n/2 - KORTTELIN_SIVU*PUISTOON, kulma);
        map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> puistorajat[r.x][r.y] = true);
        paa1 = Funktiot.kaanto(n/2, n/2, -n, n/2 + KORTTELIN_SIVU*PUISTOON, kulma);
        paa2 = Funktiot.kaanto(n/2, n/2, n*2, n/2 + KORTTELIN_SIVU*PUISTOON, kulma);
        map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> puistorajat[r.x][r.y] = true);
        map.floodfill(n/2, n/2, r -> puistoa[r.x][r.y] = false, r -> !puistorajat[r.x][r.y]);
        pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
        map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.gray, Color.orange, Color.red}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto == 1 && map.sisalto[i][j].katu != 3) map.sisalto[i][j].katu = 0;
            }
        }
        Ruutu torikeskus = map.tutka(map.sisalto[n/2][n/2], 4, r -> r.katu == 0);
        ArrayList<Ruutu> keskustori = new ArrayList<Ruutu>();
        map.floodfill(torikeskus.x, torikeskus.y, r -> {
            r.katu = 1;
            keskustori.add(r);
        }, r -> r.katu == 0);
        Funktio2RuutuaDouble katu = (r1, r2) -> {
        	double e = Math.sqrt(etaisyys2(r1, r2));
        	if (r2.maankaytto != 1) e += Math.abs(r1.korkeus - r2.korkeus)*16;
            else if (r2.katu == 0) return Double.POSITIVE_INFINITY;
            if (r2.katu == 0) e *= 2;
            if (r1.katu == 0) e *= 2;
        	return e;
        };
        ArrayList<int[]> katukeha = Funktiot.sadekeha(1);
        for (int i = 2; i < 4; i++) katukeha.addAll(Funktiot.sadekeha(i));
        FunktioRuutuRuutulist katunaapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : katukeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1]) && r.katu != 0) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
        	}
        	return palaute;
        };
        double[][] etaisyydet = new double[n][n];
        Ruutu[][] edelliset = new Ruutu[n][n];

        map.dijkstra(torikeskus, etaisyydet, edelliset, katunaapurit, katu, r -> false);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 0) {
                    Ruutu lahin = map.tutka(map.sisalto[i][j], KORTTELIN_SIVU, r -> r.katu != 0);
                    if (lahin != null) etaisyydet[i][j] = etaisyydet[lahin.x][lahin.y];
                }
            }
        }
        Keko<Ruutu> etaisyyskeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) etaisyyskeko.lisaa(map.sisalto[i][j]);
        }
        while (etaisyyskeko.size() >= n*n/10*6) {
            Ruutu r = etaisyyskeko.pienin();
        }
        Ruutu raja = etaisyyskeko.pienin();
        double rajaetaisyys = etaisyydet[raja.x][raja.y];
        ArrayList<ArrayList<Ruutu>> korttelit = new ArrayList<ArrayList<Ruutu>>();
        boolean[][] kartoitettu = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!kartoitettu[i][j] && map.sisalto[i][j].katu == 0 && map.sisalto[i][j].maankaytto != 1) {
                    ArrayList<Ruutu> kortteli = new ArrayList<Ruutu>();
                    map.floodfill(i, j, r -> {
                        kartoitettu[r.x][r.y] = true;
                        kortteli.add(r);
                    }, r -> r.katu == 0 && r.maankaytto != 1);
                    ArrayList<Ruutu> joetonKortteli = new ArrayList<Ruutu>();
                    map.floodfill(i, j, r -> joetonKortteli.add(r), r -> r.katu == 0);
                    if (joetonKortteli.size() == kortteli.size()) korttelit.add(kortteli);
                }
            }
        }
        for (int i = korttelit.size() - 1; 0 <= i; i--) {
            ArrayList<Ruutu> kortteli = korttelit.get(i);
            int maalla = 0;
            int puistossa = 0;
            for (Ruutu ruutu : kortteli) {
                if (rajaetaisyys < etaisyydet[ruutu.x][ruutu.y]) maalla++;
                if (puistoa[ruutu.x][ruutu.y]) puistossa++;
            }
            if (maalla < kortteli.size()/2 && puistossa < kortteli.size()/2) {
                for (Ruutu ruutu : kortteli) {
                    ruutu.maankaytto = 3;
                }
            } else korttelit.remove(i);
        }
        for (int i = 0; i < n - 1; i++) {
            if (map.sisalto[i][0].katu != 0) map.floodfill(i, 0, r -> r.katu = 0, r -> r.katu != 0 && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null, true);
            if (map.sisalto[0][i].katu != 0) map.floodfill(0, i, r -> r.katu = 0, r -> r.katu != 0 && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null, true);
            if (map.sisalto[i][n - 1].katu != 0) map.floodfill(i, n - 1, r -> r.katu = 0, r -> r.katu != 0 && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null, true);
            if (map.sisalto[n - 1][i].katu != 0) map.floodfill(n - 1, i, r -> r.katu = 0, r -> r.katu != 0 && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null, true);
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                map.floodfill(i, j, r -> r.katu = 0, r -> r.katu != 0 && r.katu != 3 && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null, true);
            }
        }
        for (Ruutu r : keskustori) r.katu = 1;
        double[] koillisuus = new double[korttelit.size()];
        double[] tasaisuus = new double[korttelit.size()];
        double[] teollisuuteen = new double[korttelit.size()];
        for (int i = 0; i < korttelit.size(); i++) {
            for (Ruutu ruutu : korttelit.get(i)) koillisuus[i] += Funktiot.kaanto(n/2, n/2, ruutu.x, ruutu.y, Math.PI/8*9)[0];
            koillisuus[i] /= korttelit.get(i).size();
            tasaisuus[i] = Ruutu.tasaisuus(korttelit.get(i));
            teollisuuteen[i] = n*n;
        }
        Keko<Integer> teollisuuskeko = new Keko<Integer>(i -> Math.pow(koillisuus[i], 15)*tasaisuus[i]*teollisuuteen[i]);
        for (int i = 0; i < korttelit.size(); i++) teollisuuskeko.lisaa(i);
        for (int j = 0; j < korttelit.size()*0.1; j++) {
            int next = teollisuuskeko.pienin();
            ArrayList<Ruutu> seuraavaTeollisuus = korttelit.get(next);
            for (Ruutu ruutu : seuraavaTeollisuus) ruutu.maankaytto = 2;
            for (int i = korttelit.size() - 1; i >= 0; i--) {
                teollisuuteen[i] = Math.min(teollisuuteen[i], etaisyys2(korttelit.get(i).get(0), seuraavaTeollisuus.get(0)));
            }
            teollisuuskeko.korjaa();
        }
        for (int i = korttelit.size() - 1; i >= 0; i--) {
            if (korttelit.get(i).get(0).maankaytto == 2) korttelit.remove(i);
        }

        Funktio2RuutuaDouble katu2 = (r1, r2) -> {
            double tulos = katu.f(r1, r2);
            if (r1.maankaytto == 1 && r1.katu == 0) tulos *= 2;
            if (r2.maankaytto == 1 && r2.katu == 0) tulos *= 2;
            return tulos;
        };
        FunktioRuutuRuutulist naapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : katukeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
        	}
        	return palaute;
        };
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, naapurit, katu2, r -> false);

        ArrayList<Ruutu> teollisuusrata = new ArrayList<Ruutu>();
        for (int i = -KORTTELIN_SIVU*KORTTELEITA_SIVULLA + KORTTELIN_SIVU; i <= KORTTELIN_SIVU*KORTTELEITA_SIVULLA; i += KORTTELIN_SIVU*2) {
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i, -n, kulma);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i, n*2, kulma);
            final int I = i;
            final boolean[][] ratakatuehdokas = new boolean[n][n];
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (map.tutka(r, 2, ru -> ru.maankaytto == 2) != null && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null) ratakatuehdokas[r.x][r.y] = true;
            });
            ArrayList<ArrayList<Ruutu>> ratakatuehdokkaat = new ArrayList<ArrayList<Ruutu>>();
            final boolean[][] vierailtu = new boolean[n][n];
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < n; j++) {
                    if (ratakatuehdokas[k][j] && !vierailtu[k][j]) {
                        ArrayList<Ruutu> ehdokas = new ArrayList<Ruutu>();
                        map.floodfill(k, j, r -> {
                            ehdokas.add(r);
                            vierailtu[r.x][r.y] = true;
                        }, r -> ratakatuehdokas[r.x][r.y] && !vierailtu[r.x][r.y], true);
                        ratakatuehdokkaat.add(ehdokas);
                    }
                }
            }  
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i, -n, kulma + Math.PI/2);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i, n*2, kulma + Math.PI/2);
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < n; j++) {
                    ratakatuehdokas[k][j] = false;
                    vierailtu[k][j] = false;
                }
            }
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (map.tutka(r, 2, ru -> ru.maankaytto == 2) != null && map.tutka(r, 2, ru -> ru.maankaytto == 3) == null) ratakatuehdokas[r.x][r.y] = true;
            });
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < n; j++) {
                    if (ratakatuehdokas[k][j] && !vierailtu[k][j]) {
                        ArrayList<Ruutu> ehdokas = new ArrayList<Ruutu>();
                        map.floodfill(k, j, r -> {
                            ehdokas.add(r);
                            vierailtu[r.x][r.y] = true;
                        }, r -> ratakatuehdokas[r.x][r.y] && !vierailtu[r.x][r.y], true);
                        ratakatuehdokkaat.add(ehdokas);
                    }
                }
            }
            for (ArrayList<Ruutu> ehdokas : ratakatuehdokkaat) {
                if (teollisuusrata.size() < ehdokas.size()) teollisuusrata = ehdokas;
            }
        }
        for (Ruutu ruutu : teollisuusrata) ruutu.rataa = 1;
        Ruutu rp1 = null;
        Ruutu rp2 = null;
        suurinEtaisyys = 0;
        for (int i = 0; i < teollisuusrata.size(); i++) {
            for (int j = 0; j < i; j++) {
                int d = etaisyys2(teollisuusrata.get(i), teollisuusrata.get(j));
                if (suurinEtaisyys < d) {
                    suurinEtaisyys = d;
                    rp1 = teollisuusrata.get(i);
                    rp2 = teollisuusrata.get(j);
                }
            }
        }
        Ruutu radanpaa1 = rp1;
        Ruutu radanpaa2 = rp2;
        Ruutu[][] rataedelliset = new Ruutu[n][n];
        Funktio2RuutuaDouble ratakaari = (r1, r2) -> {
            double kaarre = 0;
            Ruutu edellinen = rataedelliset[r1.x][r1.y];
            if (edellinen != null) {
                double vanhasuunta = Math.atan2(r1.y - edellinen.y, r1.x - edellinen.x);
                double uusisuunta = Math.atan2(r2.y - r1.y, r2.x - r1.x);
                kaarre = Math.min(Math.abs(vanhasuunta - uusisuunta), Math.PI*2 - Math.abs(vanhasuunta - uusisuunta));
                if (0.2 < kaarre) return Double.POSITIVE_INFINITY;
                kaarre *= kaarre;
            }
            double etaisyys = Math.sqrt(etaisyys2(r1, r2));
            Ruutu keskikohta = map.sisalto[(r1.x + r2.x)/2][(r1.y + r2.y)/2];
            boolean tunnelissa = map.tutka(keskikohta, KORTTELIN_SIVU, r -> r.maankaytto == 3) != null;
            boolean siltaa = !tunnelissa && keskikohta.maankaytto == 1;
            double palaute = etaisyys*(kaarre + 0.01);
            if (tunnelissa) palaute *= 3;
            if (siltaa) palaute *= 2;
            return palaute;
        };
        final int RATAOSUUS_PITUUS = 10;
        ArrayList<int[]> ratakeha = Funktiot.sadekeha(RATAOSUUS_PITUUS);
        FunktioRuutuRuutulist ratanaapurit = (r) -> {
            ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
            for (int[] suunta : ratakeha) {
                if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
            }
            return palaute;
        };
        ArrayList<Ruutu> radanpaa1keha = ratanaapurit.f(radanpaa1);
        radanpaa1keha.removeIf(r -> r.rataa == 0);
        rataedelliset[radanpaa1.x][radanpaa1.y] = radanpaa1keha.get(0);
        double[][] rataetaisyydet = new double[n][n];
        Ruutu reuna = map.dijkstra(radanpaa1, rataetaisyydet, rataedelliset, ratanaapurit, ratakaari, r -> r.x < RATAOSUUS_PITUUS || r.y < RATAOSUUS_PITUUS || n - RATAOSUUS_PITUUS - 1 < r.x || n - RATAOSUUS_PITUUS - 1 < r.y);
        double ratakustannus1 = rataetaisyydet[reuna.x][reuna.y];
        ArrayList<Ruutu> ulosrata1 = new ArrayList<Ruutu>();
        map.luoTie(reuna, radanpaa1, rataedelliset, r -> ulosrata1.add(r));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) rataedelliset[i][j] = null;
        }
        ArrayList<Ruutu> radanpaa2keha = ratanaapurit.f(radanpaa2);
        radanpaa2keha.removeIf(r -> r.rataa == 0);
        rataedelliset[radanpaa2.x][radanpaa2.y] = radanpaa2keha.get(0);
        reuna = map.dijkstra(radanpaa2, rataetaisyydet, rataedelliset, ratanaapurit, ratakaari, r -> r.x < RATAOSUUS_PITUUS || r.y < RATAOSUUS_PITUUS || n - RATAOSUUS_PITUUS < r.x || n - RATAOSUUS_PITUUS < r.y);
        double ratakustannus2 = rataetaisyydet[reuna.x][reuna.y];
        ArrayList<Ruutu> ulosrata2 = new ArrayList<Ruutu>();
        map.luoTie(reuna, radanpaa2, rataedelliset, r -> ulosrata2.add(r));
        if (ratakustannus1 < ratakustannus2) {
            for (Ruutu r : ulosrata1) r.rataa = 1;
        } else {
            for (Ruutu r : ulosrata2) r.rataa = 1;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) rataedelliset[i][j] = null;
        }
        Ruutu tunnelireuna = ratakustannus1 < ratakustannus2 ? radanpaa2 : radanpaa1;
        ArrayList<Ruutu> tunneliratakeha = ratanaapurit.f(tunnelireuna);
        tunneliratakeha.removeIf(r -> r.rataa == 0);
        rataedelliset[tunnelireuna.x][tunnelireuna.y] = tunneliratakeha.get(0);
        reuna = map.dijkstra(tunnelireuna, rataetaisyydet, rataedelliset, ratanaapurit, ratakaari, r -> etaisyys2(r, map.sisalto[n/2][n/2]) < KORTTELIN_SIVU*KORTTELIN_SIVU*4);
        map.luoTie(reuna, tunnelireuna, rataedelliset, r -> r.rataa = 2);
        reuna.rakennus = 1;
        pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
       map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.white, Color.white, Color.white}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));

        // ratapihan määritys
        Ruutu ratapihanPaa = null;
        final int RATAPIHAN_PITUUS = 240;
        final int RATAPIHAN_SIVU = KORTTELIN_SIVU - 8;
        ArrayList<Ruutu> ratapiha = new ArrayList<Ruutu>();
        Ruutu[] pihaportti1 = new Ruutu[1];
        Ruutu[] pihaportti2 = new Ruutu[1];
        if (ratakustannus1 < ratakustannus2) {
            map.floodfill(radanpaa1.x, radanpaa1.y, r -> {
                    if (RATAPIHAN_SIVU*RATAPIHAN_SIVU < etaisyys2(r, radanpaa1) && pihaportti1[0] == null) pihaportti1[0] = r;
                    else if ((RATAPIHAN_PITUUS - RATAPIHAN_SIVU)*(RATAPIHAN_PITUUS - RATAPIHAN_SIVU) < etaisyys2(r, radanpaa1) && pihaportti2[0] == null) pihaportti2[0] = r;
                    ratapiha.add(r);
                }, r -> {
                return map.tutka(r, 2, ru -> ru.maankaytto == 2) != null && r.rataa != 0 && etaisyys2(r, radanpaa1) < RATAPIHAN_PITUUS*RATAPIHAN_PITUUS;
            }, true);
        } else {
            map.floodfill(radanpaa2.x, radanpaa2.y, r -> ratapiha.add(r), r -> {
                    if (RATAPIHAN_SIVU*RATAPIHAN_SIVU < etaisyys2(r, radanpaa2) && pihaportti1[0] == null) pihaportti1[0] = r;
                    else if ((RATAPIHAN_PITUUS - RATAPIHAN_SIVU)*(RATAPIHAN_PITUUS - RATAPIHAN_SIVU) < etaisyys2(r, radanpaa2) && pihaportti2[0] == null) pihaportti2[0] = r;
                return map.tutka(r, 2, ru -> ru.maankaytto == 2) != null && r.rataa != 0 && etaisyys2(r, radanpaa2) < RATAPIHAN_PITUUS*RATAPIHAN_PITUUS;
            }, true);
        }
        double[] kulmapaikka1 = Funktiot.kaanto(pihaportti1[0].x, pihaportti1[0].y, ratapiha.get(0).x, ratapiha.get(0).y, Math.PI/2);
        Ruutu ratapihanReuna1 = map.sisalto[(int)kulmapaikka1[0]][(int)kulmapaikka1[1]];
        double[] kulmapaikka2 = Funktiot.kaanto(pihaportti2[0].x, pihaportti2[0].y, ratapiha.get(ratapiha.size() - 1).x, ratapiha.get(ratapiha.size() - 1).y, -Math.PI/2);
        Ruutu ratapihanReuna2 = map.sisalto[(int)kulmapaikka2[0]][(int)kulmapaikka2[1]];
        map.bresenham(ratapihanReuna1, ratapihanReuna2, r -> r.rataa = 1);
        map.bresenham(ratapiha.get(0), ratapihanReuna1, r -> r.rataa = 1);
        map.bresenham(ratapihanReuna2, ratapiha.get(ratapiha.size() - 1), r -> r.rataa = 1);
        boolean[][] ratapihalla = new boolean[n][n];
        map.floodfill((ratapiha.get(0).x + ratapihanReuna2.x)/2, (ratapiha.get(0).y + ratapihanReuna2.y)/2, r -> {
            ratapihalla[r.x][r.y] = true;
            r.maankaytto = 2;
        }, r -> r.rataa == 0);
        kulmapaikka1 = Funktiot.kaanto(pihaportti1[0].x, pihaportti1[0].y, ratapiha.get(0).x, ratapiha.get(0).y, -Math.PI/2);
        ratapihanReuna1 = map.sisalto[(int)kulmapaikka1[0]][(int)kulmapaikka1[1]];
        kulmapaikka2 = Funktiot.kaanto(pihaportti2[0].x, pihaportti2[0].y, ratapiha.get(ratapiha.size() - 1).x, ratapiha.get(ratapiha.size() - 1).y, Math.PI/2);
        ratapihanReuna2 = map.sisalto[(int)kulmapaikka2[0]][(int)kulmapaikka2[1]];
        map.bresenham(ratapihanReuna1, ratapihanReuna2, r -> r.rataa = 1);
        map.bresenham(ratapiha.get(0), ratapihanReuna1, r -> r.rataa = 1);
        map.bresenham(ratapihanReuna2, ratapiha.get(ratapiha.size() - 1), r -> r.rataa = 1);
        map.floodfill((ratapiha.get(0).x + ratapihanReuna2.x)/2, (ratapiha.get(0).y + ratapihanReuna2.y)/2, r -> {
            ratapihalla[r.x][r.y] = true;
            r.maankaytto = 2;
        }, r -> r.rataa == 0);
        double teollisuusratakulma = Math.atan2(radanpaa2.y - radanpaa1.y, radanpaa2.x - radanpaa1.x);
        final int RATOJEN_VALI = 10;      
        for (int i = -RATAPIHAN_SIVU*2; i <= RATAPIHAN_SIVU*2; i += RATOJEN_VALI) {
            double[] ratapaa11 = Funktiot.kaanto(radanpaa2.x, radanpaa2.y, radanpaa2.x, radanpaa2.y + i, teollisuusratakulma);
            double[] ratapaa12 = Funktiot.kaanto(radanpaa1.x, radanpaa1.y, radanpaa1.x, radanpaa1.y + i, teollisuusratakulma);
            map.bresenham(map.sisalto[(int)ratapaa11[0]][(int)ratapaa11[1]], map.sisalto[(int)ratapaa12[0]][(int)ratapaa12[1]], r -> {
                if (ratapihalla[r.x][r.y]) r.rataa = 1;
            });
        }
        pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
       map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.white, Color.white, Color.white}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));

        // Toimistokorttelien määritys
        double[] keskustaan = new double[korttelit.size()];
        double[] randomx = new double[korttelit.size()];
        for (int i = 0; i < korttelit.size(); i++) {
            Ruutu keskus = map.keskus(korttelit.get(i));
            keskustaan[i] = etaisyydet[keskus.x][keskus.y];
            randomx[i] = Math.random();
        }
        Keko<Integer> toimistokeko = new Keko<Integer>(i -> keskustaan[i]*keskustaan[i]*randomx[i]);
        for (int i = 0; i < korttelit.size(); i++) toimistokeko.lisaa(i);
        int toimistokortteleita = 0;
        int toimistoI = 0;
        int[][] toimistokorttelia = new int[n][n];
        while (toimistokortteleita++ < korttelit.size()/10) {
            toimistoI++;
            for (Ruutu r : korttelit.get(toimistokeko.pienin())) {
                r.maankaytto = 4;
                toimistokorttelia[r.x][r.y] = toimistoI;
            }
        }
        boolean[][] poistaTeollisuuskatu = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 1) {
                    Ruutu lahinTeollisuus = map.tutka(map.sisalto[i][j], 2, r -> r.maankaytto == 2);
                    Ruutu lahinMuu = map.tutka(map.sisalto[i][j], 2, r -> 2 < r.maankaytto);
                    poistaTeollisuuskatu[i][j] = lahinTeollisuus != null && lahinMuu == null;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (poistaTeollisuuskatu[i][j]) {
                    map.sisalto[i][j].katu = 0;
                    map.sisalto[i][j].maankaytto = 2;
                }
            }
        }
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, naapurit, katu2, r -> false);
        Keko<Ruutu> reunakeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
        for (int i = 0; i < n - 1; i++) {
            reunakeko.lisaa(map.sisalto[i][0]);
            reunakeko.lisaa(map.sisalto[0][i + 1]);
            reunakeko.lisaa(map.sisalto[n - 1][i]);
            reunakeko.lisaa(map.sisalto[i + 1][n - 1]);
        }
        ArrayList<Ruutu> ulosmenot = new ArrayList<Ruutu>();
        ulosmenot.add(reunakeko.pienin());
        while (ulosmenot.size() < 4) {
            Ruutu next = reunakeko.pienin();
            boolean omallaSuunnalla = true;
            for (Ruutu ruutu : ulosmenot) {
                double kulma2 = Math.atan2(ruutu.x - n/2, ruutu.y - n/2);
                double nextKulma = Math.atan2(next.x - n/2, next.y - n/2);
                omallaSuunnalla = omallaSuunnalla && 1 < Math.min(Math.abs(kulma2 - nextKulma), Math.PI*2 - Math.abs(kulma2 - nextKulma));
            }
            if (omallaSuunnalla) ulosmenot.add(next);
        }
        for (Ruutu ruutu : ulosmenot) {
            map.luoTie(ruutu, map.sisalto[n/2][n/2], edelliset, r -> {
                r.katu = Math.max(r.katu, 2);
            });
        }
        int[][] katuun = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) katuun[i][j] = n*n;
        }
        for (int i = 0; i < n; i++) {
            System.out.println(i);
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu != 0) {
                    for (int k = i - KORTTELIN_SIVU; k < i + KORTTELIN_SIVU; k++) {
                        for (int l = j - KORTTELIN_SIVU; l < j + KORTTELIN_SIVU; l++) {
                            if (map.kartalla(k, l)) katuun[k][l] = Math.min(katuun[k][l], (i - k)*(i - k) + (j - l)*(j - l));
                        }
                    }
                }
            }
        }

        final int RAKENNUS_RAJA = 4*4;
        final int PIHA_RAJA = 10*10;
        int[] toimisto_runkosyvyydet = new int[toimistokortteleita];
        for (int i = 0; i < toimistokortteleita; i++) {
            toimisto_runkosyvyydet[i] = (int)(8 + Math.random()*((KORTTELIN_SIVU - 16)/2));
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto == 3 && RAKENNUS_RAJA <= katuun[i][j] && katuun[i][j] <= PIHA_RAJA) map.sisalto[i][j].rakennus = 2;
                else if (map.sisalto[i][j].maankaytto == 4 && RAKENNUS_RAJA <= katuun[i][j] && katuun[i][j] <= toimisto_runkosyvyydet[toimistokorttelia[i][j]]*toimisto_runkosyvyydet[toimistokorttelia[i][j]]) map.sisalto[i][j].rakennus = 2;
            }
        }
        boolean[][] teollisuusrakennusvierailtu = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!teollisuusrakennusvierailtu[i][j] && map.sisalto[i][j].maankaytto == 2 && RAKENNUS_RAJA <= katuun[i][j]) {
                    ArrayList<Ruutu> teollisuusrakennusalue = new ArrayList<Ruutu>();
                    boolean[] ratapihaa = new boolean[]{false};
                    map.floodfill(i, j, r -> {
                        teollisuusrakennusalue.add(r);
                        teollisuusrakennusvierailtu[r.x][r.y] = true;
                        ratapihaa[0] = ratapihaa[0] || ratapihalla[r.x][r.y];
                    }, r -> !teollisuusrakennusrajat[r.x][r.y] && RAKENNUS_RAJA <= katuun[r.x][r.y]);
                    double pRakennus = ratapihaa[0] ? 0.25 : 0.5;
                    if (Math.random() < pRakennus) {
                        for (Ruutu r : teollisuusrakennusalue) r.rakennus = 2;
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (teollisuusrakennusrajat[i][j]) {
                    int rakennustaYmparilla = 0;
                    if (map.kartalla(i-1,j) && map.sisalto[i-1][j].maankaytto == 2 && map.sisalto[i-1][j].rakennus == 2) rakennustaYmparilla++;
                    if (map.kartalla(i+1,j) && map.sisalto[i+1][j].maankaytto == 2 && map.sisalto[i+1][j].rakennus == 2) rakennustaYmparilla++;
                    if (map.kartalla(i,j-1) && map.sisalto[i][j-1].maankaytto == 2 && map.sisalto[i][j-1].rakennus == 2) rakennustaYmparilla++;
                    if (map.kartalla(i,j+1) && map.sisalto[i][j+1].maankaytto == 2 && map.sisalto[i][j+1].rakennus == 2) rakennustaYmparilla++;
                    if (2 <= rakennustaYmparilla) map.sisalto[i][j].rakennus = 2;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 0 || map.sisalto[i][j].maankaytto == 1) katunrot[i][j] = 0;
            }
        }
        nextKatuNro = 1;
        int[][] lopullisetKatunrot = new int[n][n];
        boolean[][] vierailtu = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            System.out.println(i);
            for (int j = 0; j < n; j++) {
                if (katunrot[i][j] != 0 && !vierailtu[i][j]) {
                    ArrayList<Ruutu> katualue = new ArrayList<Ruutu>();
                    for (int k = 0; k < n; k++) {
                        for (int l = 0; l < n; l++) {
                            if (katunrot[k][l] == katunrot[i][j]) {
                                katualue.add(map.sisalto[k][l]);
                                vierailtu[k][l] = true;
                            };
                        }
                    }
                    Keko<Ruutu> katujarjestys = new Keko<Ruutu>(r -> r.x, katualue);
                    Ruutu next = katujarjestys.pienin();
                    lopullisetKatunrot[next.x][next.y] = nextKatuNro;
                    while (katujarjestys.size() != 0) {
                        Ruutu next2 = katujarjestys.pienin();
                        if (2 < Math.abs(next2.x - next.x)) nextKatuNro++;
                        lopullisetKatunrot[next2.x][next2.y] = nextKatuNro;
                        next = next2;
                    }
                    nextKatuNro++;
                }
            }
        }
        ArrayList<Katu> kadut = new ArrayList<Katu>();
        for (int k = 1; k < nextKatuNro; k++) {
            System.out.println(k);
            ArrayList<Ruutu> katualue = new ArrayList<Ruutu>();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (map.sisalto[i][j].katu != 0 && lopullisetKatunrot[i][j] == k) katualue.add(map.sisalto[i][j]);
                }
            }
            if (KORTTELIN_SIVU/2 < katualue.size()) kadut.add(new Katu(katualue, ""));
        }
        Keko<Katu> pituusjarjestys = new Keko<Katu>(k -> -k.alue.size(), kadut);
        int k = 1;
        ArrayList<String> katunimet = new ArrayList<String>();
        try (Scanner fi = new Scanner(new FileInputStream("katunimet.txt"))) {
            while (fi.hasNext()) {
                katunimet.add(fi.next());
            }
        }
        Collections.shuffle(katunimet);
        
        while (k + 1 < katunimet.size() && 0 < pituusjarjestys.size()) {
            System.out.println(k);
            Katu next = pituusjarjestys.pienin();
            next.nimi = katunimet.get(k++ - 1);
            map.katuverkko.add(next);
        }

      //  map.floodfill(torikeskus.x, torikeskus.y, r -> r.katu = 5, r -> r.katu < 2);
        /**
        final boolean puistonSuunta = 0 < cos;
        for (int i = KORTTELIN_SIVU/2; i < n; i += KORTTELIN_SIVU) {
            double[] paa1 = Funktiot.kaanto(n/2, n/2, n/2 + i, -n, kulma);
            double[] paa2 = Funktiot.kaanto(n/2, n/2, n/2 + i, n*2, kulma);
            final int I = i;
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (puistonSuunta && (I + KORTTELIN_SIVU/2*11) % (KORTTELIN_SIVU*4) == 0) r.katu = 2;
                else if (!puistonSuunta && (I + KORTTELIN_SIVU/2*17) % (KORTTELIN_SIVU*4) == 0) r.katu = 2;
                else if (puistonSuunta && (I + KORTTELIN_SIVU/2*11) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (!puistonSuunta && (I + KORTTELIN_SIVU/2*17) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (r.maankaytto != 1 && r.katu == 0) r.katu = 1;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, n/2 - i, -n, kulma);
            paa2 = Funktiot.kaanto(n/2, n/2, n/2 - i, n*2, kulma);
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if (puistonSuunta && (I + KORTTELIN_SIVU/2*21) % (KORTTELIN_SIVU*4) == 0) r.katu = 2;
                else if (!puistonSuunta && (I + KORTTELIN_SIVU/2*15) % (KORTTELIN_SIVU*4) == 0) r.katu = 2;
                else if (puistonSuunta && (I + KORTTELIN_SIVU/2*21) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (!puistonSuunta && (I + KORTTELIN_SIVU/2*15) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (r.maankaytto != 1 && r.katu == 0) r.katu = 1;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, -n, n/2 + i, kulma);
            paa2 = Funktiot.kaanto(n/2, n/2, n*2, n/2 + i, kulma);
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if ((I - KORTTELIN_SIVU/2) % (KORTTELIN_SIVU*4) == 0) r.katu = 3;
                else if ((I - KORTTELIN_SIVU/2) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (r.maankaytto != 1 && r.katu == 0) r.katu = 1;
            });
            paa1 = Funktiot.kaanto(n/2, n/2, -n, n/2 - i, kulma);
            paa2 = Funktiot.kaanto(n/2, n/2, n*2, n/2 - i, kulma);
            map.bresenham((int)paa1[0], (int)paa1[1], (int)paa2[0], (int)paa2[1], r -> {
                if ((I - KORTTELIN_SIVU/2*3) % (KORTTELIN_SIVU*4) == 0) r.katu = 3;
                else if ((I - KORTTELIN_SIVU/2*3) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                //else if (puistonSuunta && (I - KORTTELIN_SIVU/2*7) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                //else if (!puistonSuunta && (I - KORTTELIN_SIVU/2) % (KORTTELIN_SIVU*2) == KORTTELIN_SIVU) r.katu = 4;
                else if (r.maankaytto != 1 && r.katu == 0) r.katu = 1;
            });
        }

        map.floodfill(n/2, n/2, r -> r.katu = 1, r -> r.katu == 0);

        boolean[][] puistoa = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double[] kaannetty = Funktiot.kaanto(n/2, n/2, i, j, -kulma);
                int keskusX = puistonSuunta ? n/2 - KORTTELIN_SIVU/2*3 : n/2 + KORTTELIN_SIVU/2*3;
                if (Math.abs(kaannetty[0] - keskusX) < KORTTELIN_SIVU*2 && KORTTELIN_SIVU*4 < Math.abs(kaannetty[1] - n/2)) {
                    puistoa[i][j] = true;
                }
            }
        }


        
        ArrayList<int[]> katukeha = Funktiot.sadekeha(1);
        for (int i = 2; i < 4; i++) katukeha.addAll(Funktiot.sadekeha(i));
        FunktioRuutuRuutulist katunaapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : katukeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1]) && r.katu != 0) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
        	}
        	return palaute;
        };
        double[][] etaisyydet = new double[n][n];
        Ruutu[][] edelliset = new Ruutu[n][n];

        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, katunaapurit, katu, r -> false);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 0) {
                    Ruutu lahinKatu = map.tutka(map.sisalto[i][j], n, r -> r.katu != 0);
                    etaisyydet[i][j] = etaisyydet[lahinKatu.x][lahinKatu.y];
                }
            }
        }
        Keko<Ruutu> etaisyyskeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) etaisyyskeko.lisaa(map.sisalto[i][j]);
        }
        while (etaisyyskeko.size() >= n*n/10*7) {
            Ruutu r = etaisyyskeko.pienin();
        }
        Ruutu raja = etaisyyskeko.pienin();
        double rajaetaisyys = etaisyydet[raja.x][raja.y];
        for (int i = korttelit.size() - 1; 0 <= i; i--) {
            ArrayList<Ruutu> kortteli = korttelit.get(i);
            int maalla = 0;
            int puistossa = 0;
            for (Ruutu ruutu : kortteli) {
                if (rajaetaisyys < etaisyydet[ruutu.x][ruutu.y]) maalla++;
                if (puistoa[ruutu.x][ruutu.y]) puistossa++;
            }
            if (maalla < kortteli.size()/2 && puistossa < kortteli.size()/2) {
                for (Ruutu ruutu : kortteli) ruutu.maankaytto = 3;
            }
        }
        boolean[][] vierailtu = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!vierailtu[i][j] && map.sisalto[i][j].maankaytto == 1) {
                    ArrayList<Ruutu> jokialue = new ArrayList<Ruutu>();
                    map.floodfill(i, j, r -> {
                        vierailtu[r.x][r.y] = true;
                        jokialue.add(r);
                    }, r -> r.katu == 0);
                    for (Ruutu ruutu : jokialue) {
                        if (ruutu.maankaytto != 1) ruutu.maankaytto = 4;
                    }
                }
            }
        }
        for (int i = korttelit.size() - 1; i >= 0; i--) {
            if (korttelit.get(i).get(0).maankaytto != 3) korttelit.remove(i);
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 0 || map.sisalto[i][j].katu == 2) continue;
                boolean kaupungissa = false;
                for (int k = i - 1; k < i + 2; k++) {
                    for (int l = j - 1; l < j + 2; l++) {
                        kaupungissa = kaupungissa || (map.kartalla(k, l) && map.sisalto[k][l].maankaytto == 3);
                    }
                }
                if (!kaupungissa) map.sisalto[i][j].katu = 0;
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto == 1 && map.sisalto[i][j].katu == 3) map.poistaKatua(i, j, r -> r.katu == 3, 1000);
            }
        }
        /**
        map.floodfill(n/2, n/2, r -> r.katu = 1, r -> r.katu == 0);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) etaisyydet[i][j] = Double.POSITIVE_INFINITY;
        }
        edelliset = new Ruutu[n][n];
        FunktioRuutuRuutulist naapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : katukeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
        	}
        	return palaute;
        };
        Funktio2RuutuaDouble katu2 = (r1, r2) -> {
            double tulos = katu.f(r1, r2);
            if (r1.maankaytto == 1) tulos *= 2;
            if (r2.maankaytto == 1) tulos *= 2;
            return tulos;
        };
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, naapurit, katu2, r -> false);
        Keko<Ruutu> reunakeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
        for (int i = 0; i < n - 1; i++) {
            reunakeko.lisaa(map.sisalto[i][0]);
            reunakeko.lisaa(map.sisalto[0][i + 1]);
            reunakeko.lisaa(map.sisalto[n - 1][i]);
            reunakeko.lisaa(map.sisalto[i + 1][n - 1]);
        }
        ArrayList<Ruutu> ulosmenot = new ArrayList<Ruutu>();
        ulosmenot.add(reunakeko.pienin());
        while (ulosmenot.size() < 4) {
            Ruutu next = reunakeko.pienin();
            boolean omallaSuunnalla = true;
            for (Ruutu ruutu : ulosmenot) {
                kulma = Math.atan2(ruutu.x - n/2, ruutu.y - n/2);
                double nextKulma = Math.atan2(next.x - n/2, next.y - n/2);
                omallaSuunnalla = omallaSuunnalla && 1 < Math.min(Math.abs(kulma - nextKulma), Math.PI*2 - Math.abs(kulma - nextKulma));
            }
            if (omallaSuunnalla) ulosmenot.add(next);
        }
        for (Ruutu ruutu : ulosmenot) {
            map.luoTie(ruutu, map.sisalto[n/2][n/2], edelliset, r -> {
                if (r.katu == 0) r.katu = 2;
            });
        }
        double[] koillisuus = new double[korttelit.size()];
        double[] tasaisuus = new double[korttelit.size()];
        double[] teollisuuteen = new double[korttelit.size()];
        for (int i = 0; i < korttelit.size(); i++) {
            for (Ruutu ruutu : korttelit.get(i)) koillisuus[i] += n - Funktiot.kaanto(n/2, n/2, ruutu.x, ruutu.y, Math.PI/8)[0];
            koillisuus[i] /= korttelit.get(i).size();
            tasaisuus[i] = Ruutu.tasaisuus(korttelit.get(i));
            teollisuuteen[i] = n*n;
        }
        System.out.println(korttelit.size());
        Keko<Integer> teollisuuskeko = new Keko<Integer>(i -> tasaisuus[i]*koillisuus[i]*koillisuus[i]*koillisuus[i]*teollisuuteen[i]*teollisuuteen[i]);
        for (int i = 0; i < korttelit.size(); i++) teollisuuskeko.lisaa(i);
        for (int j = 0; j < korttelit.size()/10; j++) {
            ArrayList<Ruutu> seuraavaTeollisuus = korttelit.get(teollisuuskeko.pienin());
            for (Ruutu ruutu : seuraavaTeollisuus) ruutu.maankaytto = 2;
            System.out.println(seuraavaTeollisuus.size());
            for (int i = 0; i < korttelit.size(); i++) {
                teollisuuteen[i] = Math.min(teollisuuteen[i], etaisyys2(korttelit.get(i).get(0), seuraavaTeollisuus.get(0)));
            }
            teollisuuskeko.korjaa();
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].katu == 4) {
                    Ruutu lahinTeollisuus = map.tutka(map.sisalto[i][j], 2, r -> r.maankaytto == 3);
                    Ruutu lahinKerrostalo = map.tutka(map.sisalto[i][j], 2, r -> r.maankaytto == 4);
                }
            }
        }
    	final Ruutu[][] rataedelliset = new Ruutu[n][n];
        int ratapituus = 16;
        Funktio2RuutuaDoubleDouble ratakaari = (r1, r2, he) -> {
        	Ruutu edellinen = rataedelliset[r1.x][r1.y];
        	double kaarre = 0;
        	if (edellinen != null) {
            	int dx = (r2.x - r1.x) - (r1.x - edellinen.x);
            	int dy = (r2.y - r1.y) - (r1.y - edellinen.y);
            	kaarre = dx*dx + dy*dy;
        	}
        	double e = kaarre + Math.abs(he - r2.korkeus);
        	if (r2.maankaytto == 2) e /= 2;
            else if (r2.maankaytto != 0) e *= 2;
        	return e;
        };
        ArrayList<int[]> ratakeha = Funktiot.sadekeha(ratapituus);
        FunktioRuutuRuutulist ratanaapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : ratakeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x+suunta[0]][r.y+suunta[1]]);
        	}
        	return palaute;
        };
       FunktioRuutuBoolean lahellaReunaa = r -> r.x < ratapituus || r.y < ratapituus || n - ratapituus - 1 < r.x || n - ratapituus - 1 < r.y;
       Ruutu maali = map.dijkstra(map.sisalto[n/2][n/2], new double[n][n], rataedelliset, ratanaapurit, (r1, r2) -> ratakaari.f(r1, r2, map.sisalto[n/2][n/2].e), lahellaReunaa);
       */
       pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
       map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.white, Color.white, Color.white}, new Color[] {null, Color.white, Color.white, Color.white, Color.white}, Color.red, Color.black, new Color(102,51,0));
       // map.kirjoita("/home/ilari-perus/kaupungit/tietokannat/"+pvm+".dat");
    }
}