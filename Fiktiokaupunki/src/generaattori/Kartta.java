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

import javax.imageio.ImageIO;

import matikka.Vektori;
import rakenteet.Keko;
import matikka.Funktiot;


import java.awt.*;
import java.awt.image.BufferedImage;

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
	
    public static interface FunktioDoubleDouble {
        public double f(double x);
    }
	/**
	 * Kartta käsitellään kaksiuloitteisena ruututaulukkona, jossa ruutu on oma luokkansa.
	 * Idea on, että kukin ruutu vastaa 25 neliömetriä, mutta vain piirra-metodi suorastaan velvoittaa tätä.
	 * @author Ilari Kauko
	 */
    public static class Ruutu {
        
        private int x, y, maankaytto, rakennus, katu, tontti, e;
        private double korkeus;
        private boolean rataa;
        
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
            rataa = Boolean.parseBoolean(data[6]);
        }
        
        
        /**
         * Ruudun merkkijonomuunnos. Tässä muodossa ruudut kirjoitetaan tiedostoon. Koordinaatteja ei merkitä, koska ne on pääteltävissä rivin järjestysnumerosta.
         * @return ruudun olennaiset tiedot merkkijonona
         */
        @Override
        public String toString() {
            return maankaytto + "|" + katu + "|" + rakennus + "|" + tontti + "|" + e + "|" + String.format("%.3f", korkeus).replace(",", ".") + "|" + rataa;
        }
    }
    
    
    private Ruutu[][] sisalto;
    private int sivu;
    private int tontteja = 0;
    
    
    /**
     * @param sivu ruutujen määrän neliöjuuri
     */
    public Kartta(int sivu) {
        this.sivu = sivu;
        sisalto = new Ruutu[sivu][sivu];
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) sisalto[i][j] = new Ruutu(i, j);
        }
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
     * @param ehto mikä ehto ruudun on täytettävä, jotta se menee algoritmin alle
     */
    public void floodfill(int i, int j, FunktioRuutuVoid kasittely, FunktioRuutuBoolean ehto) {
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
        }
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
        			g.fillRect(i - 1, j - 1, 3, 3);
        		}
        	}
        }
        
        g.setColor(rata);
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rataa) g.fillRect(i, j, 1, 1);
                if (sisalto[i][j].rakennus == 1) g.fillRect(i-1, j-1, 3, 3);
            }
        }
        
        // Vasempaan yläreunaan tulee 500 metrin mittatikku.
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("500 m", 20, 30);
        g.drawLine(10, 35, 110, 35);
        
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
    

    /**
     * Pääohjelma. Generointiprosessin mielekkyys on ehkä kyseenalainen, mutta prosessi toimii esimerkkinä 
     * muun ohjelmakoodin käytöstä.
     * @param args ei käytössä
     * @throws FileNotFoundException jos tiedoston luku tai kirjoitus ei onnistu
     */
    public static void main(String[] args) throws FileNotFoundException {
    	// perustiedot
    	int n = 2048;
        Kartta map3 = new Kartta(n);
        // Luonnonmaantiede määritellään alussa. Keskustan vierestä virtaa joki, jonka uoma perustuu yhteen Perlin-kohinaan. Korkeuserot joen eri puolilla perustuvat kahteen eri Perlin-kohinaan.
        double h = 1;
        map3.luoKorkeuserot(2, n, h);
        Kartta map2 = new Kartta(n);
        double rinteisyys = 500;
        map2.luoKorkeuserot(2, 8, rinteisyys);
        double jokisuunta = Math.random()*Math.PI*2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                map3.sisalto[i][j].korkeus *= map3.sisalto[i][j].korkeus;
                map2.sisalto[i][j].korkeus += 2*rinteisyys*Funktiot.kaanto(0, 0, i, j, jokisuunta)[0]/n;
            }
        }
        double jokietaisyys = 50;
        double[] jokipaikka = Funktiot.kaanto(n/2, n/2, n/2 + jokietaisyys, n/2, -jokisuunta);
        double jokipohja = map2.sisalto[(int)jokipaikka[0]][(int)jokipaikka[1]].korkeus;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) map2.sisalto[i][j].korkeus = Math.abs(map2.sisalto[i][j].korkeus - jokipohja);
        }
        double jokisyvyys = 10;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map2.sisalto[i][j].korkeus < jokisyvyys) map2.sisalto[i][j].maankaytto = 1;
                else map2.sisalto[i][j].korkeus = jokisyvyys + (map2.sisalto[i][j].korkeus - jokisyvyys)*map3.sisalto[i][j].korkeus;
            }
        }
        Kartta map = map2;

        Funktio2RuutuaDouble ulosKatu = (r1, r2) -> {
            double d = Math.sqrt(etaisyys2(r1, r2));
            if (r1.maankaytto == 1) d *= 2;
            if (r2.maankaytto == 1) d *= 2;
            if (r1.maankaytto != 1 || r2.maankaytto != 1) d += 100*(r1.korkeus - r2.korkeus)*(r1.korkeus - r2.korkeus);
            return d;
        };

        ArrayList<int[]> sadekeha = Funktiot.sadekeha(1);
        for (int i = 2; i < 4; i++) sadekeha.addAll(Funktiot.sadekeha(i));
        FunktioRuutuRuutulist naapurit = (r) -> {
            ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
            for (int[] suunta : sadekeha) {
                if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
            }
            return palaute;
        };

        double[][] etaisyydet = new double[n][n];
        Ruutu[][] edelliset = new Ruutu[n][n];
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, naapurit, ulosKatu, r -> false);
        Keko<Ruutu> etaisyyskeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) etaisyyskeko.lisaa(map.sisalto[i][j]);
        }
        Keko<Ruutu> reunat = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]/Math.sqrt(etaisyys2(r, map.sisalto[n/2][n/2])));
        for (int i = 0; i < n - 1; i++) {
            reunat.lisaa(map.sisalto[i][0]);
            reunat.lisaa(map.sisalto[0][i + 1]);
            reunat.lisaa(map.sisalto[n - 1][i]);
            reunat.lisaa(map.sisalto[i + 1][n - 1]);
        }
        ArrayList<Ruutu> portit = new ArrayList<Ruutu>();
        while (portit.size() < 3) {
            Ruutu next = reunat.pienin();
            boolean omallaSuunnalla = true;
            for (Ruutu r : portit)
                omallaSuunnalla = omallaSuunnalla && 1.5 < Funktiot.kulma(n/2, n/2, r.x, r.y, next.x, next.y);
            if (omallaSuunnalla) {
                portit.add(next);
                map.luoTie(next, map.sisalto[n/2][n/2], edelliset, r -> r.katu = 1);
            }
        }

        Funktio2RuutuaDouble vanhaKatu = (r1, r2) -> {
            double d = Math.sqrt(etaisyys2(r1, r2));
            if (r1.maankaytto != 1 || r2.maankaytto != 1) d += 100*(r1.korkeus - r2.korkeus)*(r1.korkeus - r2.korkeus);
            else if (r2.katu == 0) d *= 4;
            return d;
        };
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, naapurit, vanhaKatu, r -> false);
        double reunalle = 0;
        double asemaetaisyys = 0;
        for (int i = 0; i < 83000; i++) {
            Ruutu next = etaisyyskeko.pienin();
            reunalle = etaisyydet[next.x][next.y];
            asemaetaisyys = Math.sqrt(etaisyys2(next, map.sisalto[n/2][n/2]));
        }
        double kokoReunalle = reunalle;
        for (int i = 0; i < 2500000; i++) {
            Ruutu next = etaisyyskeko.pienin();
            kokoReunalle = etaisyydet[next.x][next.y];
        }
        ArrayList<Ruutu> kohteet = new ArrayList<Ruutu>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto != 1 && Math.random() < 1 - Math.max(0, Math.min(1, Funktiot.perlinKayra((etaisyydet[i][j] - reunalle)/100))))
                    kohteet.add(map.sisalto[i][j]);
            }
        }
        Collections.shuffle(kohteet);
        while (3000 < kohteet.size()) kohteet.remove(kohteet.size() - 1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto != 1 && Math.random() < 1 - Math.max(0, Math.min(1, Funktiot.perlinKayra((etaisyydet[i][j] - kokoReunalle)/100))))
                    kohteet.add(map.sisalto[i][j]);
            }
        }
        Collections.shuffle(kohteet);
        while (30000 < kohteet.size()) kohteet.remove(kohteet.size() - 1);
        int[][] rakennusta = new int[n][n];
        for (Ruutu r : kohteet) {
            double kaanto = Math.random()*Math.PI;
            for (int i = r.x - 10; i < r.x + 10; i++) {
                for (int j = r.y - 10; j < r.y + 10; j++) {
                    double[] doubleKaannetty = Funktiot.kaanto(r.x, r.y, i, j, kaanto);
                    int[] kaannetty = new int[]{(int)doubleKaannetty[0], (int)doubleKaannetty[1]};
                    if (map.kartalla(kaannetty[0], kaannetty[1]) && map.sisalto[kaannetty[0]][kaannetty[1]].maankaytto != 1) {
                        map.sisalto[kaannetty[0]][kaannetty[1]].maankaytto = Math.max(map.sisalto[kaannetty[0]][kaannetty[1]].maankaytto, 2);
                        if (rakennusta[kaannetty[0]][kaannetty[1]]++ == 10) map.sisalto[kaannetty[0]][kaannetty[1]].maankaytto = 4;
                    }
                }
            }
        }

        ArrayList<Ruutu> rata = null;
        double halvinRatahinta = Double.POSITIVE_INFINITY;
        Ruutu asema = null;
        for (double k = 0; k < Math.PI*2; k += 0.1) {
            int i = n/2 + (int)(Math.cos(k)*asemaetaisyys);
            int j = n/2 + (int)(Math.sin(k)*asemaetaisyys);
            ArrayList<Ruutu> rataehdokas = new ArrayList<Ruutu>();
            double[] kustannus = new double[]{0};
            map.bresenham(i + (int)(Math.cos(k + Math.PI/2)*n), j + (int)(Math.sin(k + Math.PI/2)*n),
                i - (int)(Math.cos(k + Math.PI/2)*n), j - (int)(Math.sin(k + Math.PI/2)*n),
                r -> {
                    kustannus[0] += (r.korkeus - map.sisalto[i][j].korkeus);
                    if (r.maankaytto == 1 || r.katu != 0) kustannus[0] += 1;
                    rataehdokas.add(r);
                }
            );
            if (kustannus[0] < halvinRatahinta) {
                halvinRatahinta = kustannus[0];
                rata = rataehdokas;
                asema = map.sisalto[i][j];
            }
        }
        for (Ruutu r : rata) r.rataa = true;
        double[][] rataan = new double[n][n];
        double[][] renkaaseen = new double[n][n];
        double[][] rinteisyysalue = new double[n][n];
        int[][] teollisuutta = new int[n][n];
        asema.rakennus = 1;
        int rinnetta = 2;
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		renkaaseen[i][j] = Math.max(0, Math.min(1, Funktiot.perlinKayra((etaisyydet[i][j] - reunalle)/100)))
                 - Math.max(0, Math.min(1, Funktiot.perlinKayra((etaisyydet[i][j] - kokoReunalle)/100)));
        		double rinteet = 0;
        		for (int k = i - rinnetta; k <= i + rinnetta; k++) {
        			for (int l = j - rinnetta; l <= j + rinnetta; l++) {
        				if ((k - i)*(k - i) + (l - j)*(l - j) <= rinnetta*rinnetta && map.kartalla(k, l) && map.sisalto[i][j].maankaytto != 1) rinteet += Math.abs(map.sisalto[i][j].korkeus - map.sisalto[k][l].korkeus);
        			}
        		}
        		rinteisyysalue[i][j] = rinteet;
        		rataan[i][j] = Double.POSITIVE_INFINITY;
        		teollisuutta[i][j] = 1;
        	}
        }
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		if (!map.sisalto[i][j].rataa) continue;
        		for (int k = 0; k < n; k++) {
        			for (int l = 0; l < n; l++) rataan[k][l] = Math.min(rataan[k][l], Math.sqrt((k - i)*(k - i) + (l - j)*(l - j)));
        		}
        	}
        }

        ArrayList<Ruutu> teollisuusehdokkaat = new ArrayList<Ruutu>();
        boolean[][] keossa = new boolean[n][n];
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		if (1 < map.sisalto[i][j].maankaytto) {
        			teollisuusehdokkaat.add(map.sisalto[i][j]);
        			keossa[i][j] = true;
        		}
        	}
        }
        
        Keko<Ruutu> teollisuuskeko = new Keko<Ruutu>(r -> rinteisyysalue[r.x][r.y]*rataan[r.x][r.y]/renkaaseen[r.x][r.y]/teollisuutta[r.x][r.y], teollisuusehdokkaat, r -> r.x*n + r.y, n*n);
        for (int k = 0; k < teollisuusehdokkaat.size()/10; k++) {
        	Ruutu s = teollisuuskeko.pienin();
        	s.maankaytto = 3;
        	keossa[s.x][s.y] = false;
            int maxteollisuus = 10;
        	for (int i = s.x - maxteollisuus; i <= s.x + maxteollisuus; i++) {
        		for (int j = s.y - maxteollisuus; j <= s.y + maxteollisuus; j++) {
        			if ((i - s.x)*(i - s.x) + (j - s.y)*(j - s.y) <= maxteollisuus*maxteollisuus && map.kartalla(i, j) && keossa[i][j]) {
        				if ((i - s.x)*(i - s.x) + (j - s.y)*(j - s.y) <= maxteollisuus*maxteollisuus) {
        					teollisuutta[i][j]++;
        					teollisuuskeko.nosta(map.sisalto[i][j]);
        				}
        			}
        		}
        	}
        }
        ArrayList<Ruutu> lahdot = new ArrayList<Ruutu>(kohteet.subList(0, kohteet.size()/20));
        ArrayList<Ruutu> maalit = new ArrayList<Ruutu>(kohteet.subList(kohteet.size()/20, kohteet.size()/10));
        for (int i = 0; i < lahdot.size() && 0 < maalit.size(); i++) {
            System.out.println(i);
            Ruutu lahto = kohteet.get(i);
            final int EHDOKKAITA = maalit.size();
            double[][] katuetaisyydet = new double[n][n];
            Funktio2RuutuaDouble uusiKatu = (r1, r2) -> {
                double e = vanhaKatu.f(r1, r2);
                if (r2.katu == 0) e *= 2;
                return e;
            };
            Ruutu katulahto = map.dijkstra(lahto, katuetaisyydet, new Ruutu[n][n], naapurit, uusiKatu, r -> r.katu != 0 || 160 < katuetaisyydet[r.x][r.y]);
            final Ruutu finalLahto = katulahto.katu != 0 ? katulahto : lahto;
            Keko<Ruutu> maalikeko = new Keko<Ruutu>(r -> etaisyys2(finalLahto, r), new ArrayList<Ruutu>(kohteet.subList(0, EHDOKKAITA)));
            Ruutu[] maalitjarjestys = new Ruutu[EHDOKKAITA];
            double[] maalitetaisyys = new double[EHDOKKAITA];
            double etaisyyssumma = 0;
            int j = 0;
            while (0 < maalikeko.size()) {
                Ruutu next = maalikeko.pienin();
                etaisyyssumma += 1.0/etaisyys2(next, finalLahto);
                maalitjarjestys[j] = next;
                maalitetaisyys[j++] = etaisyyssumma;
            }
            double rajaarvo = Math.random()*etaisyyssumma;
            int maaliIndeksi = 0;
            while (maalitetaisyys[maaliIndeksi] < rajaarvo) maaliIndeksi++;
            maalit.remove(maalitetaisyys[maaliIndeksi]);
            edelliset = new Ruutu[n][n];
            Ruutu katumaali = map.dijkstra(maalitjarjestys[maaliIndeksi], katuetaisyydet, new Ruutu[n][n], naapurit, uusiKatu, r -> r.katu != 0 || 160 < katuetaisyydet[r.x][r.y]);
            final Ruutu finalMaali = katumaali.katu != 0 ? katumaali : maalitjarjestys[maaliIndeksi];
            map.aTahti(finalLahto, new double[n][n], edelliset, naapurit, uusiKatu, r -> Math.sqrt(etaisyys2(finalMaali, r)), r -> r == finalMaali);

            map.luoTie(finalMaali, finalLahto, edelliset, r -> r.katu = 1);
        }
        String pvm = new SimpleDateFormat("yyMMddHHmm").format(new Date());
        map.piirra("./piirrokset/jarjestys/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.pink, Color.gray, Color.orange, Color.green, Color.red}, new Color[] {null, Color.white}, Color.red, Color.black, new Color(102,51,0));
        /**
        int RANTAAN = 200;
        for (int i = n/2 - RANTAAN; i <= n/2 + RANTAAN; i++) {
            for (int j = n/2 - RANTAAN; j <= n/2 + RANTAAN; j++) {
                if ((i - n/2)*(i - n/2) + (j - n/2)*(j - n/2) < RANTAAN*RANTAAN/4) merenKorkeus = Math.min(merenKorkeus, map.sisalto[i][j].korkeus);
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].korkeus < merenKorkeus) map.sisalto[i][j].maankaytto = 1;
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (map.sisalto[i][j].maankaytto == 0) {
                    double korkeus = map.sisalto[i][j].korkeus;
                    boolean pohja = !map.kartalla(i - 1, j) || korkeus < map.sisalto[i - 1][j].korkeus;
                    pohja = pohja && (!map.kartalla(i, j - 1) || korkeus < map.sisalto[i][j - 1].korkeus);
                    pohja = pohja && (!map.kartalla(i, j + 1) || korkeus < map.sisalto[i][j + 1].korkeus);
                    pohja = pohja && (!map.kartalla(i + 1, j) || korkeus < map.sisalto[i + 1][j].korkeus);
                    if (pohja) {
                        ArrayList<Ruutu> jarvi = new ArrayList<Ruutu>();
                        Keko<Ruutu> jarvenRanta = new Keko<Ruutu>(r -> r.korkeus);
                        jarvenRanta.lisaa(map.sisalto[i][j]);
                        while (true) {
                            Ruutu next = jarvenRanta.pienin();
                            boolean jatkuu = !map.kartalla(next.x - 1, next.y) || korkeus < map.sisalto[next.x - 1][next.y].korkeus;
                            jatkuu = jatkuu && (!map.kartalla(next.x, next.y - 1) || korkeus < map.sisalto[next.x][next.y - 1].korkeus);
                            jatkuu = jatkuu && (!map.kartalla(next.x, next.y + 1) || korkeus < map.sisalto[next.x][next.y + 1].korkeus);
                            jatkuu = jatkuu && (!map.kartalla(next.x + 1, next.y) || korkeus < map.sisalto[next.x + 1][next.y].korkeus);
                            if (jatkuu) {
                                jarvi.add(next);
                                if (map.kartalla(next.x - 1, next.y)) jarvenRanta.lisaa(map.sisalto[next.x - 1][next.y]);
                                if (map.kartalla(next.x, next.y - 1)) jarvenRanta.lisaa(map.sisalto[next.x][next.y - 1]);
                                if (map.kartalla(next.x, next.y + 1)) jarvenRanta.lisaa(map.sisalto[next.x][next.y + 1]);
                                if (map.kartalla(next.x + 1, next.y)) jarvenRanta.lisaa(map.sisalto[next.x + 1][next.y]);
                            } else break;
                        }
                        double v = 0;
                        double jarvenPinta = jarvi.get(0).korkeus;
                        for (int k = 1; k < jarvi.size(); k++) jarvenPinta = Math.max(jarvenPinta, jarvi.get(k).korkeus);
                        for (int k = 0; k < jarvi.size(); k++) v += jarvenPinta - jarvi.get(k).korkeus;
                        if (100000 < v) {
                            for (Ruutu r : jarvi) r.maankaytto = 2;
                        }
                        else if (5000 < v) {
                            for (Ruutu r : jarvi) r.maankaytto = 1;
                        }
                    }
                }
            }
        }
    
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) edelliset[i][j] = null;
        }
        Ruutu ekamutka = naapurit.f(tuloasema).stream().filter(r -> r.rataa).findFirst().get();
        System.out.println(ekamutka);
        edelliset[tuloasema.x][tuloasema.y] = ekamutka;
        final Ruutu finalReuna = tuloreuna;
        Ruutu ulostulo = map.dijkstra(tuloasema, etaisyydet2, edelliset, naapurit, ratakaari, r -> (r.x < 20 || r.y < 20 || n - 20 < r.x || n - 20 < r.y) && 1 < Funktiot.kulma(n/2, n/2, finalReuna.x, finalReuna.y, r.x, r.y));
        System.out.println(ulostulo);
        map.luoTie(ulostulo, tuloasema, edelliset, r -> r.rataa = true);
        System.out.println("RATA VALMIS");
        Ruutu keskus = map.tutka(map.sisalto[n/2][n/2], RANTAAN, r -> r.maankaytto == 0);
        Ruutu asema = map.tutka(keskus, n/2, r -> r.rataa);
        final int ASEMA_PITUUS = 20;
        for (int i = asema.x - ASEMA_PITUUS/2; i <= asema.x + ASEMA_PITUUS/2; i++) {
            for (int j = asema.y - ASEMA_PITUUS/2; j <= asema.y + ASEMA_PITUUS/2; j++) {
                if ((i - asema.x)*(i - asema.x) + (j - asema.y)*(j - asema.y) <= ASEMA_PITUUS*ASEMA_PITUUS/4 && map.sisalto[i][j].rataa) map.sisalto[i][j].rakennus = 1;
            }
        }
        Funktio2RuutuaDouble paavaylat = (r1, r2) -> {
            double d = Math.sqrt((r1.x - r2.x)*(r1.x - r2.x) + (r1.y - r2.y)*(r1.y - r2.y));
            if (r2.katu == 0 && r2.maankaytto == 1) d *= 5;
            else if (r2.maankaytto != 1) d += (r1.korkeus - r2.korkeus)*(r1.korkeus - r2.korkeus)*2;
            boolean[] radalla = new boolean[1];
            map.bresenham(r1, r2, r -> radalla[0] = radalla[0] || r.rataa);
            if (radalla[0] && r2.katu == 0) d *= 2;
            else if (r2.katu != 0) d /= 2;
            return d;
        };
        ArrayList<int[]> sadekeha2 = Funktiot.sadekeha(1);
        for (int i = 2; i < 4; i++) sadekeha.addAll(Funktiot.sadekeha(i));
        FunktioRuutuRuutulist naapurit2 = r -> {
            ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
            for (int[] suunta : sadekeha) {
                if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x + suunta[0]][r.y + suunta[1]]);
            }
            return palaute;
        };
        final int TALOT_SD = 200;
        final int SILTARAJA = 2;
        ArrayList<Ruutu> portit = new ArrayList<Ruutu>();
        for (int i = 0; i < 3; i++) {
            Ruutu[][] reunalle = new Ruutu[n][n];
            double[][] etaisyydet = new double[n][n];
            map.dijkstra(keskus, etaisyydet, reunalle, naapurit2, paavaylat, r -> false);
            Keko<Ruutu> reunat = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]/Math.sqrt((r.x - keskus.x)*(r.x - keskus.x) + (r.y - keskus.y)*(r.y - keskus.y)));
            for (int j = 0; j < n - 1; j++) {
                reunat.lisaa(map.sisalto[0][j]);
                reunat.lisaa(map.sisalto[j + 1][0]);
                reunat.lisaa(map.sisalto[n - 1][j + 1]);
                reunat.lisaa(map.sisalto[j][n - 1]);
            }
            while (true) {
                Ruutu next = reunat.pienin();
                System.out.println(reunat.size());
                boolean omallaSuunnalla = true;
                for (int j = 0; j < portit.size(); j++) omallaSuunnalla = omallaSuunnalla && 1 < Funktiot.kulma(keskus.x, keskus.y, portit.get(j).x, portit.get(j).y, next.x, next.y);
                if (omallaSuunnalla) {
                    portit.add(next);
                    map.luoTie(next, keskus, reunalle, r -> {
                        if (TALOT_SD < etaisyydet[r.x][r.y]) r.katu = 1;
                    });
                    break;
                }
            }
        }
        Funktio2RuutuaDouble aluemaaritelma = (r1, r2) -> {
            double d = Math.sqrt((r1.x - r2.x)*(r1.x - r2.x) + (r1.y - r2.y)*(r1.y - r2.y));
            if (r2.katu == 0 && r2.maankaytto == 1) d *= 5;
            else if (r2.maankaytto != 1) d += (r1.korkeus - r2.korkeus)*(r1.korkeus - r2.korkeus)*2;
            return d;
        };
        double[][] etaisyydet = new double[n][n];
        int siltoja = 0;
        while (true) {
            final Ruutu[][] edelliset2 = new Ruutu[n][n];
            map.dijkstra(keskus, etaisyydet, edelliset2, naapurit, aluemaaritelma, r -> etaisyydet[r.x][r.y] < TALOT_SD*SILTARAJA);
            Keko<Ruutu> etaisyyskeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y]);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (etaisyydet[i][j] != Double.POSITIVE_INFINITY) etaisyyskeko.lisaa(map.sisalto[i][j]);
                }
            }
            Ruutu next = etaisyyskeko.pienin();
            System.out.println(keskus == next);
            System.out.println(etaisyyskeko.size());
            System.out.println(siltoja++);
            while ((next = etaisyyskeko.pienin()) != null && ((next.maankaytto == 1 && next.katu == 0) || edelliset2[next.x][next.y].maankaytto != 1));
            System.out.println(next);
            if (next == null) break;
            Ruutu toinenPaa = edelliset2[next.x][next.y];
            while (toinenPaa != null && toinenPaa.maankaytto == 1) toinenPaa = edelliset2[toinenPaa.x][toinenPaa.y];
            if (toinenPaa != null) map.bresenham(next, toinenPaa, r -> r.katu = 1);
        }
        ArrayList<Ruutu> talot = new ArrayList<Ruutu>();
        final int TALOJA = 25000;
        while (talot.size() < TALOJA) {
            System.out.println(talot.size());
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (map.sisalto[i][j].maankaytto == 0 && Math.random() < Funktiot.gauss(etaisyydet[i][j]/TALOT_SD)) talot.add(map.sisalto[i][j]);
                }
            }
        }
        Collections.shuffle(talot);
        for (int i = talot.size() - 1; i >= TALOJA; i--) talot.remove(i);
        int[][] taloja = new int[n][n];
        for (Ruutu t : talot) {
            System.out.println(t);
            double TALON_SIVU = 7;
            double kulma = Math.random()*Math.PI/2;
            for (int i = t.x - (int)TALON_SIVU - 1; i <= t.x + (int)TALON_SIVU + 1; i++) {
                for (int j = t.y - (int)TALON_SIVU - 1; j <= t.y + (int)TALON_SIVU + 1; j++) {
                    double[] kaannetty = Funktiot.kaanto(t.x, t.y, i, j, kulma);
                    if (Math.abs(kaannetty[0] - t.x) + Math.abs(kaannetty[1] - t.y) < TALON_SIVU) taloja[i][j]++;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (12 < taloja[i][j]) map.sisalto[i][j].maankaytto = 4;
                else if (0 < taloja[i][j]) map.sisalto[i][j].maankaytto = 2;
            }
        }
        // Kartta yksilöidään valmistumisajankohtansa mukaan.
        String pvm = new SimpleDateFormat("ddMMyyHHmm").format(new Date());
        map.piirra("./piirrokset/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.pink, Color.gray, Color.orange, Color.green, Color.red}, new Color[] {null, Color.white}, Color.red, Color.black, new Color(102,51,0));
       // map.kirjoita("/home/ilari-perus/kaupungit/tietokannat/"+pvm+".dat");
       */
    }
}